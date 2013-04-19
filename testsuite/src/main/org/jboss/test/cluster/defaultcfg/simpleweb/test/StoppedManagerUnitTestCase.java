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

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.infinispan.manager.CacheContainer;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.test.cluster.testutil.SessionTestUtil;
import org.jboss.web.tomcat.service.session.JBossCacheManager;

/**
 * Tests that a stopped session manager reacts correctly to Manager API calls.
 * Used to validate that races during undeploy will not lead to errors.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 85945 $
 */
public class StoppedManagerUnitTestCase extends TestCase
{
   private static final Logger log = Logger.getLogger(StoppedManagerUnitTestCase.class);
   
   private static long testCount = System.currentTimeMillis();
   
   private CacheContainer cacheContainer;
   private JBossCacheManager<?> manager;
   private boolean stopManager = false;
   
   /**
    * Create a new SessionCountUnitTestCase.
    * 
    * @param name
    */
   public StoppedManagerUnitTestCase(String name)
   {
      super(name);
   } 

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      
      if ((manager != null) && stopManager)
      {
         manager.stop();
      }
      
      if (cacheContainer != null)
      {
         cacheContainer.stop();
      }
   }

   public void testStoppedManager() throws Exception
   {
      log.info("Enter testStoppedManager");
      
      ++testCount;
      
      cacheContainer = SessionTestUtil.createCacheContainer(true, null, false, false);
      manager = SessionTestUtil.createManager("test" + testCount, 30, cacheContainer, null);
       
      JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(100);
      manager.init("test.war", webMetaData);
      
      manager.start();
      stopManager = true;
      
      // Set up a session
      Session sess1 = createAndUseSession(manager, "1", true, true);      
      Session sess2 = createAndUseSession(manager, "2", true, true);
      
      // Sanity check
      Session[] sessions = manager.findSessions();
      assertNotNull(sessions);
      assertEquals(2, sessions.length);
      
      manager.stop();
      stopManager = false;
      
      assertNull(manager.findSession("1"));
      assertNull(manager.findSession("2"));
      assertNull(manager.findSessions());
      assertNull(manager.createEmptySession());
      assertNull(manager.createSession());
      assertNull(manager.createSession("3"));
      
      assertFalse(sess1.isValid());
      assertFalse(sess2.isValid());
      manager.add(sess1); // shouldn't blow up
      assertFalse(sess1.isValid());
      
      manager.remove(sess2);
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
}
