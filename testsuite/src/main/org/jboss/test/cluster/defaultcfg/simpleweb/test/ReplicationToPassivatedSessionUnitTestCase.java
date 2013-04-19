/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

import java.io.File;
import java.util.Collections;

import junit.framework.Test;
import junit.framework.TestCase;

import org.infinispan.manager.CacheContainer;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.metadata.web.jboss.ReplicationTrigger;
import org.jboss.test.cluster.testutil.CacheConfigTestSetup;
import org.jboss.test.cluster.testutil.SessionTestUtil;
import org.jboss.test.cluster.web.mocks.SetAttributesRequestHandler;
import org.jboss.web.tomcat.service.session.JBossCacheManager;

/**
 * Unit tests of session expiration
 * 
 * @author Brian Stansberry
 */
public class ReplicationToPassivatedSessionUnitTestCase extends TestCase
{
   protected static CacheContainer[] cacheContainers = new CacheContainer[2];
   
   protected static long testId = System.currentTimeMillis();
   
   protected Logger log = Logger.getLogger(getClass());   
   
   protected JBossCacheManager<?>[] managers = new JBossCacheManager[cacheContainers.length];
   
   public ReplicationToPassivatedSessionUnitTestCase(String name)
   {
      super(name);
   }
   
   public static Test suite() throws Exception
   {
      File tmpDir = new File(System.getProperty("java.io.tmpdir"));
      File root = new File(tmpDir, ReplicationToPassivatedSessionUnitTestCase.class.getSimpleName());
      return CacheConfigTestSetup.getTestSetup(ReplicationToPassivatedSessionUnitTestCase.class, cacheContainers, false, root.getAbsolutePath(), true);
   }

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();

      for (JBossCacheManager<?> manager : managers)
      {
         if (manager != null)
         {
            manager.stop();
         }
      }
   }
   
   protected ReplicationGranularity getReplicationGranularity()
   {
      return ReplicationGranularity.SESSION;
   }
   
   protected ReplicationTrigger getReplicationTrigger()
   {
      return ReplicationTrigger.SET_AND_NON_PRIMITIVE_GET;
   }
   
   public void testReplicationToPassivatedSession() throws Exception
   {
      log.info("++++ Starting testReplicationToPassivatedSession ++++");
      
      String warname = String.valueOf(++testId);
      
      // A war with a maxInactive of 30 mins and a maxIdle of 1
      this.startManagers(warname, 1800000, 1, -1);
      
      Object value = "0";
      SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", value), false);
      SessionTestUtil.invokeRequest(managers[0], setHandler, null);
      
      String id = setHandler.getSessionId();
      
      SessionTestUtil.sleepThread(1100); 
      
      managers[0].backgroundProcess();
      managers[1].backgroundProcess();     
      
      value = "1";
      setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", value), false);
      SessionTestUtil.invokeRequest(managers[0], setHandler, id);
      
      assertEquals("0", setHandler.getCheckedAttributes().get("count"));
      
      value = "2";
      setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", value), false);
      SessionTestUtil.invokeRequest(managers[1], setHandler, id);
      
      assertEquals("1", setHandler.getCheckedAttributes().get("count"));
   }
   
   public void testFailoverToPassivatedSession() throws Exception
   {
      log.info("++++ Starting testFailoverToPassivatedSession ++++");
      
      String warname = String.valueOf(++testId);
      
      // A war with a maxInactive of 30 mins and a maxIdle of 1
      this.startManagers(warname, 1800000, 1, -1);
      
      Object value = "0";
      SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", value), false);
      SessionTestUtil.invokeRequest(managers[0], setHandler, null);
      
      String id = setHandler.getSessionId();
      
      SessionTestUtil.sleepThread(1100); 
      
      managers[0].backgroundProcess();
      managers[1].backgroundProcess();     
      
      value = "1";
      setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", value), false);
      SessionTestUtil.invokeRequest(managers[1], setHandler, id);
      
      assertEquals("0", setHandler.getCheckedAttributes().get("count"));
   }
   
   protected void startManagers(String warname, int maxInactive, int maxIdle, int maxUnreplicated) throws Exception
   {
      for (int i = 0; i < cacheContainers.length; ++i)
      {
         managers[i] = SessionTestUtil.createManager(warname, maxInactive, cacheContainers[i], null);
         JBossWebMetaData metadata = SessionTestUtil.createWebMetaData(getReplicationGranularity(), getReplicationTrigger(), -1, true, maxIdle, -1, true, (i == 0) ? maxUnreplicated : -1);
         managers[i].init(warname, metadata);
         managers[i].start();
      }
   }

}
