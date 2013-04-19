/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.test.cluster.defaultcfg.simpleweb.test;

import junit.framework.TestCase;

import org.infinispan.manager.CacheContainer;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.EmptyMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.PassivationConfig;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.metadata.web.jboss.ReplicationTrigger;
import org.jboss.metadata.web.jboss.SnapshotMode;
import org.jboss.test.cluster.testutil.SessionTestUtil;
import org.jboss.web.tomcat.service.session.JBossCacheManager;

/**
 * Unit tests of session count management.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 108925 $
 */
public class JBossCacheManagerConfigurationUnitTestCase extends TestCase
{
   private static final Logger log = Logger.getLogger(JBossCacheManagerConfigurationUnitTestCase.class);
   
   private static long testCount = System.currentTimeMillis();

   private CacheContainer cacheContainer;
   private JBossCacheManager<?> manager;
   private String tempDir;
   
   /**
    * Create a new SessionCountUnitTestCase.
    * 
    * @param name
    */
   public JBossCacheManagerConfigurationUnitTestCase(String name)
   {
      super(name);
   }

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      
      if (manager != null)
      {
         try
         {
            manager.stop();
         }
         catch (Exception e)
         {
            log.error(e.getMessage(), e);
         }
      }
      
      if (cacheContainer != null)
      {
         try
         {
            cacheContainer.stop();
         }
         catch (Exception e)
         {
            log.error(e.getMessage(), e);
         }
      }
      
      if (tempDir != null)
      {
         SessionTestUtil.cleanFilesystem(tempDir);
      }
   }
   
   public void testUseJK() throws Exception
   {
      log.info("Enter testUseJK");
      
      ++testCount;
      cacheContainer = SessionTestUtil.createCacheContainer(true, null, false, false);
      cacheContainer.start();
      
      manager = SessionTestUtil.createManager("test" + testCount, 5, cacheContainer, null);
      JBossWebMetaData webMetaData = createWebMetaData(null, null, null, null, null);
      manager.init("test.war", webMetaData);      
      manager.start();

      assertFalse("With no config, not using JK", manager.getUseJK());
      
      manager.stop();
      
      manager = SessionTestUtil.createManager("test" + ++testCount, 5, cacheContainer, null);
      
      webMetaData = createWebMetaData(null, null, null, null, Boolean.TRUE);
      manager.init("test.war", webMetaData);      
      manager.start();

      assertTrue("With no jvmRoute but a config, using JK", manager.getUseJK());
      
      manager.stop();
      
      manager = SessionTestUtil.createManager("test" + ++testCount, 5, cacheContainer, "test");
      
      webMetaData = createWebMetaData(null, null, null, null, null);
      manager.init("test.war", webMetaData);
      manager.start();

      assertTrue("With jvmRoute set, using JK", manager.getUseJK());
      
      manager.stop();
      
      manager = SessionTestUtil.createManager("test" + ++testCount, 5, cacheContainer, "test");
      
      webMetaData = createWebMetaData(null, null, null, null, Boolean.FALSE);
      manager.init("test.war", webMetaData);      
      manager.start();

      assertFalse("With a jvmRoute but config=false, not using JK", manager.getUseJK());
   }
   
   public void testSnapshot() throws Exception
   {
      log.info("Enter testSnapshot");
      
      ++testCount;
      
      cacheContainer = SessionTestUtil.createCacheContainer(true, null, false, false);
      cacheContainer.start();
      manager = SessionTestUtil.createManager("test" + testCount, 5, cacheContainer, null);
      
      JBossWebMetaData webMetaData = createWebMetaData(null, null, null, null, null);
      manager.init("test.war", webMetaData);      
      manager.start();

      assertEquals("With no config, using instant", SnapshotMode.INSTANT, manager.getSnapshotMode());
      
      manager.stop();
      
      manager = SessionTestUtil.createManager("test" + ++testCount, 5, cacheContainer, null);
      
      webMetaData = createWebMetaData(null, null, null, null, Boolean.TRUE);
      webMetaData.getReplicationConfig().setSnapshotMode(SnapshotMode.INTERVAL);
      webMetaData.getReplicationConfig().setSnapshotInterval(new Integer(2));
      manager.init("test.war", webMetaData);      
      manager.start();

      assertEquals("With config, using interval", SnapshotMode.INTERVAL, manager.getSnapshotMode());
      assertEquals("With config, using 2 second interval", 2, manager.getSnapshotInterval());
   }
   
   private JBossWebMetaData createWebMetaData(Integer maxSessions, 
                                              Boolean passivation,
                                              Integer maxIdle, 
                                              Integer minIdle,
                                              Boolean useJK)
   {
      JBossWebMetaData webMetaData = new JBossWebMetaData();
      webMetaData.setDistributable(new EmptyMetaData());
      webMetaData.setMaxActiveSessions(maxSessions);
      PassivationConfig pcfg = new PassivationConfig();
      pcfg.setUseSessionPassivation(passivation);
      pcfg.setPassivationMaxIdleTime(maxIdle);
      pcfg.setPassivationMinIdleTime(minIdle);
      webMetaData.setPassivationConfig(pcfg);
      ReplicationConfig repCfg = new ReplicationConfig();
      repCfg.setReplicationGranularity(ReplicationGranularity.SESSION);
      repCfg.setReplicationTrigger(ReplicationTrigger.SET_AND_NON_PRIMITIVE_GET);
      repCfg.setUseJK(useJK);
      webMetaData.setReplicationConfig(repCfg);
      return webMetaData;
   }
}
