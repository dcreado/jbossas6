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

import java.io.File;

import junit.framework.TestCase;

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.test.cluster.testutil.JGroupsSystemPropertySupport;
import org.jboss.test.cluster.testutil.SessionTestUtil;
import org.jboss.web.tomcat.service.session.JBossCacheManager;

/**
 * Unit tests of session count management.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 109806 $
 */
public class SessionCountUnitTestCase extends TestCase
{
   private static final Logger log = Logger.getLogger(SessionCountUnitTestCase.class);
   
   private static int managerIndex = 1;
   
   private JGroupsSystemPropertySupport jgroupsSupport;
   private EmbeddedCacheManager[] cacheContainers = new EmbeddedCacheManager[2];
   private JBossCacheManager<?>[] managers = new JBossCacheManager[cacheContainers.length];
   private String tempDir;
   
   /**
    * Create a new SessionCountUnitTestCase.
    * 
    * @param name
    */
   public SessionCountUnitTestCase(String name)
   {
      super(name);
   }  
   
   
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      // Set system properties to properly bind JGroups channels
      jgroupsSupport = new JGroupsSystemPropertySupport();
      jgroupsSupport.setUpProperties();
      
      File tmpDir = new File(System.getProperty("java.io.tmpdir"));
      File root = new File(tmpDir, getClass().getSimpleName());
      tempDir = root.getAbsolutePath();
    }

   @Override
   protected void tearDown() throws Exception
   {
      try
      {
         // Restore any system properties we set in setUp
         if (jgroupsSupport != null)
         {
            jgroupsSupport.restoreProperties();
         }
         
         for (JBossCacheManager<?> manager: managers)
         {
            if (manager != null)
            {
               try
               {
                  manager.stop();
               }
               catch (Throwable e)
               {
                  e.printStackTrace(System.err);
               }
            }
         }
         
         for (CacheContainer cacheContainer: cacheContainers)
         {
            if (cacheContainer != null)
            {
               try
               {
                  cacheContainer.stop();
               }
               catch (Throwable e)
               {
                  e.printStackTrace(System.err);
               }
            }
         }
         
         super.tearDown();
      }
      finally
      {            
         if (tempDir != null)
         {
            SessionTestUtil.cleanFilesystem(tempDir);
         }
      }
   }

   public void testStandaloneMaxSessions() throws Exception
   {
      log.info("Enter testStandaloneMaxSessions");
      
      String warName = "test" + ++managerIndex;
      
      cacheContainers[0] = SessionTestUtil.createCacheContainer(true, null, false, false);
      cacheContainers[0].start();
      managers[0] = SessionTestUtil.createManager(warName, 5, cacheContainers[0], null);
       
      JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(2);
      managers[0].init(warName, webMetaData);
      
      managers[0].start();
      
      assertFalse("Passivation is disabled", managers[0].isPassivationEnabled());
      assertEquals("Correct max active count", 2, managers[0].getMaxActiveAllowed());
      
      // Set up a session
      Session sess1 = createAndUseSession(managers[0], "1", true, true);
      
      assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      
      createAndUseSession(managers[0], "2", true, true);
      
      assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 2, managers[0].getLocalActiveSessionCount());
      
      // Should fail to create a 3rd
      createAndUseSession(managers[0], "3", false, false);
      
      // Confirm a session timeout clears space
      sess1.setMaxInactiveInterval(1);       
      SessionTestUtil.sleepThread(1100);      
      
      createAndUseSession(managers[0], "3", true, true);      
      
      assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 2, managers[0].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 3, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 1, managers[0].getExpiredSessionCount());
   }
   
   public void testStandaloneMaxSessionsWithMaxIdle()
         throws Exception
   {
      log.info("Enter testStandaloneMaxSessionsWithMaxIdle");
      
      String warName = "test" + ++managerIndex;
      String passDir = getPassivationDir(managerIndex, 1);
      cacheContainers[0] = SessionTestUtil.createCacheContainer(true, passDir, false, false);
      cacheContainers[0].start();
      managers[0] = SessionTestUtil.createManager(warName, 5, cacheContainers[0], null);
       
      JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(1, true, 1, -1);
      managers[0].init(warName, webMetaData);
      
      managers[0].start();
      
      assertTrue("Passivation is enabled", managers[0].isPassivationEnabled());
      assertEquals("Correct max active count", 1, managers[0].getMaxActiveAllowed());
      assertEquals("Correct max idle time", 1, managers[0].getPassivationMaxIdleTime());
      assertEquals("Correct min idle time", -1, managers[0].getPassivationMinIdleTime());

      // Set up a session
      Session sess1 = createAndUseSession(managers[0], "1", true, true);
      
      assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      
      // Should fail to create a 2nd
      createAndUseSession(managers[0], "2", false, false);
      
      // Confirm a session timeout clears space
      sess1.setMaxInactiveInterval(1);       
      SessionTestUtil.sleepThread(1100);      
      
      createAndUseSession(managers[0], "2", true, true);      
      
      assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 2, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 1, managers[0].getExpiredSessionCount());
      assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());

      //    Sleep past maxIdleTime
      SessionTestUtil.sleepThread(1100);        
      
      assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());
      
      createAndUseSession(managers[0], "3", true, true);      
      
      assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 3, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 1, managers[0].getExpiredSessionCount());
      assertEquals("Passivated session count correct", 1, managers[0].getPassivatedSessionCount());
   }
   
   public void testStandaloneMaxSessionsWithMinIdle() throws Exception
   {
      log.info("Enter testStandaloneMaxSessionsWithMinIdle");
      
      String warName = "test" + ++managerIndex;
      String passDir = getPassivationDir(managerIndex, 1);
      cacheContainers[0] = SessionTestUtil.createCacheContainer(true, passDir, false, false);
      cacheContainers[0].start();
      managers[0] = SessionTestUtil.createManager(warName, 5, cacheContainers[0], null);
      
      JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(1, true, 3, 1);
      managers[0].init(warName, webMetaData);
      
      managers[0].start();
      
      assertTrue("Passivation is enabled", managers[0].isPassivationEnabled());
      assertEquals("Correct max active count", 1, managers[0].getMaxActiveAllowed());
      assertEquals("Correct max idle time", 3, managers[0].getPassivationMaxIdleTime());
      assertEquals("Correct min idle time", 1, managers[0].getPassivationMinIdleTime());
      
      // Set up a session
      Session sess1 = createAndUseSession(managers[0], "1", true, true);
      
      assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      
      // Should fail to create a 2nd
      createAndUseSession(managers[0], "2", false, false);
      
      // Confirm a session timeout clears space
      sess1.setMaxInactiveInterval(1);       
      SessionTestUtil.sleepThread(1100);      
      
      createAndUseSession(managers[0], "2", true, false);      
      
      assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());     
      assertEquals("Created session count correct", 2, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 1, managers[0].getExpiredSessionCount());

      //    Sleep past minIdleTime
      SessionTestUtil.sleepThread(1100);        
      
//      assertTrue("Session 2 still valid", sess2.isValid());
      assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());
      
      createAndUseSession(managers[0], "3", true, true);      
      
      assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 3, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 1, managers[0].getExpiredSessionCount());
      assertEquals("Passivated session count correct", 1, managers[0].getPassivatedSessionCount());
   }
   
   public void testReplicatedMaxSessions() throws Exception
   {
      log.info("Enter testReplicatedMaxSessions");
      
      String warName = "test" + ++managerIndex;
      JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(1);
      for (int i = 0; i < cacheContainers.length; ++i)
      {
         cacheContainers[i] = SessionTestUtil.createCacheContainer(false, null, false, false);
         cacheContainers[i].start();
         
         managers[i] = SessionTestUtil.createManager(warName, 1, cacheContainers[i], null);
         managers[i].init(warName, webMetaData);
         managers[i].start();
         
         assertFalse("Passivation is disabled", managers[i].isPassivationEnabled());
         assertEquals("Correct max active count", 1, managers[i].getMaxActiveAllowed());
         assertEquals("Correct max inactive interval", 1, managers[i].getMaxInactiveInterval());
      }
      
      // Set up a session
      Session session = createAndUseSession(managers[0], "1", true, true);
      
      assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());      
      assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
      assertEquals("Local session count correct", 0, managers[1].getLocalActiveSessionCount());
      
      // Should fail to create a 2nd
      createAndUseSession(managers[1], "2", false, false);
      
      // Confirm a session timeout clears space
      session.setMaxInactiveInterval(1);     
      useSession(managers[0], "1");
      SessionTestUtil.sleepThread(managers[0].getMaxInactiveInterval() * 1000 + 100);      
      
      createAndUseSession(managers[1], "2", true, true);      
      
      assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());      
      
      assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
   }
   
   public void testReplicatedMaxSessionsWithMaxIdle() throws Exception
   {
      log.info("Enter testReplicatedMaxSessionsWithMaxIdle");
      
      String warName = "test" + ++managerIndex;
      JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(1, true, 1, -1);
      for (int i = 0; i < cacheContainers.length; ++i)
      {
         String passDir = getPassivationDir(managerIndex, i + 1);
         cacheContainers[i] = SessionTestUtil.createCacheContainer(false, passDir, false, false);
         cacheContainers[i].start();
         
         managers[i] = SessionTestUtil.createManager(warName, 1, cacheContainers[i], null);
         managers[i].init(warName, webMetaData);
         managers[i].start();
         
         assertTrue("Passivation is enabled", managers[i].isPassivationEnabled());
         assertEquals("Correct max active count", 1, managers[i].getMaxActiveAllowed());
         assertEquals("Correct max idle time", 1, managers[i].getPassivationMaxIdleTime());
         assertEquals("Correct min idle time", -1, managers[i].getPassivationMinIdleTime());
      }
      
      // Set up a session
      createAndUseSession(managers[0], "1", true, true);
      
      assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());
      assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
      assertEquals("Local session count correct", 0, managers[1].getLocalActiveSessionCount());
      assertEquals("Passivated session count correct", 0, managers[1].getPassivatedSessionCount());
      
      // Should fail to create a 2nd
      createAndUseSession(managers[1], "2", false, false);      
      
      //    Sleep past maxIdleTime      
      SessionTestUtil.sleepThread(1100);        
      
      assertEquals("Passivated session count correct", 0, managers[1].getPassivatedSessionCount());
       
      createAndUseSession(managers[1], "2", true, true);      
       
      assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());  
      assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());
       
      assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[1].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 1, managers[1].getCreatedSessionCount());
      assertEquals("Expired session count correct", 0, managers[1].getExpiredSessionCount()); 
      assertEquals("Passivated session count correct", 1, managers[1].getPassivatedSessionCount());
   }
   
   public void testReplicatedMaxSessionsWithMinIdle() throws Exception
   {
      log.info("Enter testReplicatedMaxSessionsWithMinIdle");
      
      String warName = "test" + ++managerIndex;
      JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(1, true, 3, 1);
      for (int i = 0; i < cacheContainers.length; ++i)
      {
         String passDir = getPassivationDir(managerIndex, i + 1);
         cacheContainers[i] = SessionTestUtil.createCacheContainer(false, passDir, false, false);
         cacheContainers[i].start();
         
         managers[i] = SessionTestUtil.createManager(warName, 1, cacheContainers[i], null);
         managers[i].init(warName, webMetaData);
         managers[i].start();
         
         assertTrue("Passivation is enabled", managers[i].isPassivationEnabled());
         assertEquals("Correct max active count", 1, managers[i].getMaxActiveAllowed());
         assertEquals("Correct max idle time", 3, managers[i].getPassivationMaxIdleTime());
         assertEquals("Correct min idle time", 1, managers[i].getPassivationMinIdleTime());
      }
      
      // Set up a session
      createAndUseSession(managers[0], "1", true, true);
      
      assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());      
      assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
      assertEquals("Local session count correct", 0, managers[1].getLocalActiveSessionCount());
      assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());
      
      // Should fail to create a 2nd
      createAndUseSession(managers[1], "2", false, false);      
      
      // Sleep past maxIdleTime      
      SessionTestUtil.sleepThread(1100);        
      
      assertEquals("Passivated session count correct", 0, managers[1].getPassivatedSessionCount());
       
      createAndUseSession(managers[1], "2", true, true);      
       
      assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());  
      assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());    
       
      assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[1].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 1, managers[1].getCreatedSessionCount());
      assertEquals("Expired session count correct", 0, managers[1].getExpiredSessionCount()); 
      assertEquals("Passivated session count correct", 1, managers[1].getPassivatedSessionCount());     
      
   }
   
   public void testTotalReplication() throws Exception
   {
      log.info("Enter testTotalReplication");
      
      String warName = "test" + ++managerIndex;
      JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(1, true, 3, 1);
      for (int i = 0; i < cacheContainers.length; ++i)
      {
         String passDir = getPassivationDir(managerIndex, i + 1);
         cacheContainers[i] = SessionTestUtil.createCacheContainer(false, passDir, true, false);
         cacheContainers[i].start();
         
         managers[i] = SessionTestUtil.createManager(warName, 1, cacheContainers[i], null);
         managers[i].init(warName, webMetaData);
         managers[i].start();
         
         assertTrue("Passivation is enabled", managers[i].isPassivationEnabled());
         assertEquals("Correct max active count", 1, managers[i].getMaxActiveAllowed());
         assertEquals("Correct max idle time", 3, managers[i].getPassivationMaxIdleTime());
         assertEquals("Correct min idle time", 1, managers[i].getPassivationMinIdleTime());
      }
      
      // Set up a session
      createAndUseSession(managers[0], "1", true, true);
      
      assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());      
      assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
      assertEquals("Local session count correct", 0, managers[1].getLocalActiveSessionCount());
      
      // Should fail to create a 2nd
      createAndUseSession(managers[1], "2", false, false);      
      
      // Sleep past maxIdleTime      
      SessionTestUtil.sleepThread(1100);        
      
      assertEquals("Passivated session count correct", 0, managers[1].getPassivatedSessionCount());
       
      createAndUseSession(managers[1], "2", true, true);      
       
      assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());      
       
      assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());      
      
   }
   
   public void testStandaloneRedeploy() throws Exception
   {
      log.info("Enter testStandaloneRedeploy");
      
      standaloneWarRedeployTest(false);
   }
   
   public void testStandaloneRestart() throws Exception
   {
      log.info("Enter testStandaloneRedeploy");
      
      standaloneWarRedeployTest(true);
   }
   
   private void standaloneWarRedeployTest(boolean restartCache)
         throws Exception
   {
      String warName = "test" + ++managerIndex;
      String passDir = getPassivationDir(managerIndex, 1);
      cacheContainers[0] = SessionTestUtil.createCacheContainer(true, passDir, false, false);
      cacheContainers[0].start();
      managers[0] = SessionTestUtil.createManager(warName, 300, cacheContainers[0], null);
      
      JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(2, true, 3, 1);
      managers[0].init(warName, webMetaData);
      
      managers[0].start();
      
      assertTrue("Passivation is enabled", managers[0].isPassivationEnabled());
      assertEquals("Correct max active count", 2, managers[0].getMaxActiveAllowed());
      assertEquals("Correct max idle time", 3, managers[0].getPassivationMaxIdleTime());
      assertEquals("Correct min idle time", 1, managers[0].getPassivationMinIdleTime());
      
      // Set up a session
      createAndUseSession(managers[0], "1", true, true);
      
      assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      
      // And a 2nd
      createAndUseSession(managers[0], "2", true, true);     
      
      assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 2, managers[0].getLocalActiveSessionCount());     
      assertEquals("Created session count correct", 2, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());

      //    Sleep past minIdleTime
      SessionTestUtil.sleepThread(1100);
      
      assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());
      
      createAndUseSession(managers[0], "3", true, true);      
      
      assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 2, managers[0].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 3, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
      assertEquals("Passivated session count correct", 1, managers[0].getPassivatedSessionCount());
      
      managers[0].stop();
      
      if (restartCache)
      {
         cacheContainers[0].stop();
         
         passDir = getPassivationDir(managerIndex, 1);
         cacheContainers[0] = SessionTestUtil.createCacheContainer(true, passDir, false, false);
      }
      
      managers[0] = SessionTestUtil.createManager(warName, 5, cacheContainers[0], null);
      
      managers[0].init(warName, webMetaData);
      
      managers[0].start();
      
      assertTrue("Passivation is enabled", managers[0].isPassivationEnabled());
      assertEquals("Correct max active count", 2, managers[0].getMaxActiveAllowed());
      assertEquals("Correct max idle time", 3, managers[0].getPassivationMaxIdleTime());
      assertEquals("Correct min idle time", 1, managers[0].getPassivationMinIdleTime());     
      
      assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 0, managers[0].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 0, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
      assertEquals("Passivated session count correct", 1, managers[0].getPassivatedSessionCount());
      
      // Sleep past minIdleTime
      SessionTestUtil.sleepThread(1100);
      
      createAndUseSession(managers[0], "4", true, true); 
   }
   
   public void testReplicatedRedeploy() throws Exception
   {
      log.info("Enter testReplicatedRedeploy");
      
      replicatedWarRedeployTest(false, false, false, false);
   }
   
   public void testReplicatedRedeployWarAndCache() throws Exception
   {
      log.info("Enter testReplicatedRedeployWarAndCache");
      
      replicatedWarRedeployTest(true, false, false, false);
   }
   
   public void testReplicatedRestart() throws Exception
   {
      log.info("Enter testReplicatedRestart");
      
      replicatedWarRedeployTest(true, true, false, false);
      
   }
   
   public void testReplicatedRestartWithPurge() throws Exception
   {
      log.info("Enter testReplicatedRestartWithPurge");
      
      replicatedWarRedeployTest(true, true, false, true);
      
   }
   
   private void replicatedWarRedeployTest(boolean restartCache, 
                                          boolean fullRestart,
                                          boolean totalReplication,
                                          boolean purgeOnStartStop)
         throws Exception
   {
      String warName = "test" + ++managerIndex;
      JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(2, true, 30, 1);
      for (int i = 0; i < cacheContainers.length; ++i)
      {
         String passDir = getPassivationDir(managerIndex, i + 1);
         cacheContainers[i] = SessionTestUtil.createCacheContainer(false, passDir, totalReplication, purgeOnStartStop);
         cacheContainers[i].start();
         
         managers[i] = SessionTestUtil.createManager(warName, 300, cacheContainers[i], null);
         managers[i].init(warName, webMetaData);
         managers[i].start();
         
         assertTrue("Passivation is enabled", managers[i].isPassivationEnabled());
         assertEquals("Correct max active count", 2, managers[i].getMaxActiveAllowed());
         assertEquals("Correct max idle time", 30, managers[i].getPassivationMaxIdleTime());
         assertEquals("Correct min idle time", 1, managers[i].getPassivationMinIdleTime());
      }
      
      SessionTestUtil.blockUntilViewsReceived(cacheContainers, 10000);
      
      // Set up a session
      createAndUseSession(managers[0], "1", true, true);
      
      assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());    
      assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
      assertEquals("Local session count correct", 0, managers[1].getLocalActiveSessionCount());
      
      // Create a 2nd
      createAndUseSession(managers[1], "2", true, true);     
      
      assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());     
      assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
      assertEquals("Session count correct", 2, managers[1].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[1].getLocalActiveSessionCount());     
      assertEquals("Created session count correct", 1, managers[1].getCreatedSessionCount());
      assertEquals("Expired session count correct", 0, managers[1].getExpiredSessionCount());

      //    Sleep past minIdleTime
      SessionTestUtil.sleepThread(1100);
      
      assertEquals("Passivated session count correct", 0, managers[1].getPassivatedSessionCount());
      
      createAndUseSession(managers[1], "3", true, true);      
      
      // jbcm has 3 active because receipt of repl doesn't trigger passivation
      assertEquals("Session count correct", 3, managers[0].getActiveSessionCount());
      assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
      assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
      assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());
      // jbcm1 only has 2 active since it passivated one when it created 3rd 
      assertEquals("Session count correct", 2, managers[1].getActiveSessionCount());
      // Both active sessions are local, as the remote session is oldest so we passivate it first 
      assertEquals("Local session count correct", 2, managers[1].getLocalActiveSessionCount());
      assertEquals("Created session count correct", 2, managers[1].getCreatedSessionCount());
      assertEquals("Expired session count correct", 0, managers[1].getExpiredSessionCount());
      assertEquals("Passivated session count correct", 1, managers[1].getPassivatedSessionCount());
      
      if (fullRestart)
      {
         managers[1].stop();
         cacheContainers[1].stop();
      }
      
      managers[0].stop();
      
      if (restartCache)
      {
         cacheContainers[0].stop();
         
         String passDir = getPassivationDir(managerIndex, 1);
         cacheContainers[0] = SessionTestUtil.createCacheContainer(false, passDir, totalReplication, purgeOnStartStop);
         cacheContainers[0].start();
      }
      
      managers[0] = SessionTestUtil.createManager(warName, 300, cacheContainers[0], null);
      
      managers[0].init(warName, webMetaData);
      managers[0].start();
      
      assertTrue("Passivation is enabled", managers[0].isPassivationEnabled());
      assertEquals("Correct max active count", 2, managers[0].getMaxActiveAllowed());
      assertEquals("Correct max idle time", 30, managers[0].getPassivationMaxIdleTime());
      assertEquals("Correct min idle time", 1, managers[0].getPassivationMinIdleTime());        
      
      // Do we expect content?
      boolean expectContent = true;
      // First, see if we expect a purge on redeploy
//      boolean expectPurge = purgeOnStartStop && (!totalReplication || marshalling);
      boolean expectPurge = purgeOnStartStop && restartCache;
      // Even with a purge, if the other cache is available we may have state transfer
      // on redeploy
      if (expectPurge)
      {
         expectContent = !fullRestart && !totalReplication;
      }
      
      if (expectContent)
      {
         assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
         assertEquals("Local session count correct", 0, managers[0].getLocalActiveSessionCount());
         assertEquals("Created session count correct", 0, managers[0].getCreatedSessionCount());
         assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
         assertEquals("Passivated session count correct", 1, managers[0].getPassivatedSessionCount());
      }      
      else
      {
         assertEquals("Session count correct", 0, managers[0].getActiveSessionCount());
         assertEquals("Local session count correct", 0, managers[0].getLocalActiveSessionCount());
         assertEquals("Created session count correct", 0, managers[0].getCreatedSessionCount());
         assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
         assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());         
      }

      // Due to ISPN-658 workaround, In DIST, restarting cache IS a full restart, so ignore this case for now
      if (!fullRestart && totalReplication)
      {
         assertEquals("Session count correct", 2, managers[1].getActiveSessionCount());
         assertEquals("Local session count correct", 2, managers[1].getLocalActiveSessionCount());
         assertEquals("Created session count correct", 2, managers[1].getCreatedSessionCount());
         assertEquals("Expired session count correct", 0, managers[1].getExpiredSessionCount());
         assertEquals("Passivated session count correct", 1, managers[1].getPassivatedSessionCount());
      }

      // Sleep past minIdleTime
      SessionTestUtil.sleepThread(1100);
      
      createAndUseSession(managers[0], "4", true, true);
   }
   
   public void testTotalReplicatedRedeploy() throws Exception
   {
      log.info("Enter testTotalReplicatedRedeploy");
      
      replicatedWarRedeployTest(false, false, true, false);
      
   }
   
   public void testTotalReplicatedRedeployWarAndCache() throws Exception
   {
      log.info("Enter testTotalReplicatedRedeployWarAndCache");
      
      replicatedWarRedeployTest(true, false, true, false);
      
   }
   
   public void testTotalReplicatedRestart() throws Exception
   {
      log.info("Enter testTotalReplicatedRestart");
      
      replicatedWarRedeployTest(true, true, true, false);
      
   }
   
   public void testTotalReplicatedRestartWithPurge() throws Exception
   {
      log.info("Enter testTotalReplicatedRestartWithPurge");
      
      replicatedWarRedeployTest(true, true, true, true);
      
   }
   
   private Session createAndUseSession(JBossCacheManager<?> manager, String id, boolean canCreate, boolean access) throws Exception
   {
      //    Shift to Manager interface when we simulate Tomcat
      Manager mgr = manager;
      Session sess = mgr.findSession(id);
      assertNull("session does not exist", sess);
      try
      {
         sess = mgr.createSession(id);
         if (!canCreate)
            fail("Could not create session" + id);
      }
      catch (IllegalStateException ise)
      {
         if (canCreate)
         {
            log.error("Failed to create session " + id, ise);
            fail("Could create session " + id);
         }
      }
      
      if (access)
      {
         sess.access();
         sess.getSession().setAttribute("test", "test");
         
         manager.storeSession(sess);
         
         sess.endAccess();
      }
      
      return sess;
   }
   
   private void useSession(JBossCacheManager<?> manager, String id) throws Exception
   {
      //    Shift to Manager interface when we simulate Tomcat
      Manager mgr = manager;
      Session sess = mgr.findSession(id);
      assertNotNull("session exists", sess);
      
      sess.access();
      sess.getSession().setAttribute("test", "test");
      
      manager.storeSession(sess);
      
      sess.endAccess();
   }
   
   private String getPassivationDir(long testCount, int cacheCount)
   {
      File dir = new File(tempDir);
      dir = new File(dir, String.valueOf(testCount));
      dir.mkdirs();
      dir.deleteOnExit();
      dir = new File(dir, String.valueOf(cacheCount));
      dir.mkdirs();
      dir.deleteOnExit();
      return dir.getAbsolutePath();
   }
}
