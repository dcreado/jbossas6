/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009 Red Hat, Inc., and individual contributors
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

import static org.jboss.test.cluster.testutil.SessionTestUtil.validateNewSession;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import junit.framework.TestCase;

import org.infinispan.manager.CacheContainer;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.test.cluster.testutil.JGroupsSystemPropertySupport;
import org.jboss.test.cluster.testutil.SessionTestUtil;
import org.jboss.test.cluster.web.mocks.InvalidateSessionRequestHandler;
import org.jboss.test.cluster.web.mocks.SetAttributesRequestHandler;
import org.jboss.web.tomcat.service.session.JBossCacheManager;

/**
 * A FailedExpirationTestCase.
 * 
 * @author Brian Stansberry
 * @version $Revision: 1.1 $
 */
public class FailedExpirationTestCase extends TestCase
{
   private static final Logger log = Logger.getLogger(ConcurrentFailoverRequestsTestCase.class);
   
   private static long testCount = System.currentTimeMillis();
   
   private final JGroupsSystemPropertySupport jgSupport = new JGroupsSystemPropertySupport();
   private CacheContainer cacheContainer;
   private JBossCacheManager<?> manager;
   private String tempDir;

   /**
    * Create a new FailedExpirationTestCase.
    * 
    * @param name
    */
   public FailedExpirationTestCase(String name)
   {
      super(name);
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      jgSupport.setUpProperties();
      
      File tmpDir = new File(System.getProperty("java.io.tmpdir"));
      File root = new File(tmpDir, getClass().getSimpleName());
      root.mkdirs();
      root.deleteOnExit();
      tempDir = root.getAbsolutePath();
   }

   @Override
   protected void tearDown() throws Exception
   {
      try
      {
         super.tearDown();
      }
      finally
      {      
         jgSupport.restoreProperties();
         
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
         
         SessionTestUtil.cleanFilesystem(tempDir);
      }
   }
   
   public void testFailedInvalidation() throws Exception
   {
      ++testCount;
      
      cacheContainer = SessionTestUtil.createCacheContainer(true, null, false, false);
      cacheContainer.start();
      JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(10, true, 1, -1);
      webMetaData.getReplicationConfig().setMaxUnreplicatedInterval(0);
      
      String warName = "test" + testCount;
      manager = SessionTestUtil.createManager(warName, 2, cacheContainer, null);
      manager.init(warName, webMetaData);
      manager.start();
      
      assertEquals(0, manager.getActiveSessionCount());
      assertEquals(0, manager.getPassivatedSessionCount());
      
      Map<String, Object> attrs = new HashMap<String, Object>(); 
      attrs.put("failInvalid", new FailInvalidationAttribute());
      attrs.put("failDeserialization", new FailDeserializationAttribute());
      
      // Establish session.
      SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(attrs, false);
      SessionTestUtil.invokeRequest(manager, setHandler, null);
      validateNewSession(setHandler);
      String id = setHandler.getSessionId();
      assertEquals(1, manager.getActiveSessionCount());
      assertEquals(0, manager.getPassivatedSessionCount());
      
      InvalidateSessionRequestHandler invalHandler = new InvalidateSessionRequestHandler(attrs.keySet(), false);
      try
      {
         SessionTestUtil.invokeRequest(manager, invalHandler, id);
         fail("Invalidation not rejected");
      }
      catch (RejectedException ok) {}
      assertEquals(0, manager.getActiveSessionCount());
      assertEquals(0, manager.getPassivatedSessionCount());
   }
   
   public void testFailedExpiration() throws Exception
   {
      ++testCount;
      
      cacheContainer = SessionTestUtil.createCacheContainer(true, null, false, false);
      cacheContainer.start();
      JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(10, true, 1, -1);
      webMetaData.getReplicationConfig().setMaxUnreplicatedInterval(0);
      String warName = "test" + testCount;
      manager = SessionTestUtil.createManager(warName, 2, cacheContainer, null);
      manager.init(warName, webMetaData);      
      manager.start();
      
      assertEquals(0, manager.getActiveSessionCount());
      assertEquals(0, manager.getPassivatedSessionCount());
      
      Map<String, Object> attrs = new HashMap<String, Object>(); 
      attrs.put("failInvalid", new FailInvalidationAttribute());
      attrs.put("failDeserialization", new FailDeserializationAttribute());
      
      // Establish session.
      SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(attrs, false);
      SessionTestUtil.invokeRequest(manager, setHandler, null);
      validateNewSession(setHandler);

      assertEquals(1, manager.getActiveSessionCount());
      assertEquals(0, manager.getPassivatedSessionCount());
      
      SessionTestUtil.sleepThread(2010);
      manager.backgroundProcess();
      assertEquals(0, manager.getActiveSessionCount());
      assertEquals(0, manager.getPassivatedSessionCount());      
   }
   
   public void testFailedExpirationAfterPassivation() throws Exception
   {
      ++testCount;
      
      String passivationDir = SessionTestUtil.getPassivationDir(tempDir, testCount, 1);
      cacheContainer = SessionTestUtil.createCacheContainer(true, passivationDir, false, false);
      cacheContainer.start();
      JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(10, true, 1, -1);
      webMetaData.getReplicationConfig().setMaxUnreplicatedInterval(0);
      String warName = "test" + testCount;
      manager = SessionTestUtil.createManager(warName, 2, cacheContainer, null);
      manager.init(warName, webMetaData);      
      manager.start();
      
      assertEquals(0, manager.getActiveSessionCount());
      assertEquals(0, manager.getPassivatedSessionCount());
      
      Map<String, Object> attrs = new HashMap<String, Object>(); 
      attrs.put("failInvalid", new FailInvalidationAttribute());
      attrs.put("failDeserialization", new FailDeserializationAttribute());
      
      // Establish session.
      SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(attrs, false);
      SessionTestUtil.invokeRequest(manager, setHandler, null);
      validateNewSession(setHandler);

      assertEquals(1, manager.getActiveSessionCount());
      assertEquals(0, manager.getPassivatedSessionCount());
      
      SessionTestUtil.sleepThread(1010);
      manager.backgroundProcess();
      assertEquals(0, manager.getActiveSessionCount());
      assertEquals(1, manager.getPassivatedSessionCount());   
      
      SessionTestUtil.sleepThread(1010);
      manager.backgroundProcess();
      assertEquals(0, manager.getActiveSessionCount());
      assertEquals(0, manager.getPassivatedSessionCount());     
   }
   
   public static class FailInvalidationAttribute extends FailDeserializationAttribute
      implements HttpSessionBindingListener
   {
      /** The serialVersionUID */
      private static final long serialVersionUID = 1L;

      public void valueBound(HttpSessionBindingEvent event)
      {
         // no-op
      }

      public void valueUnbound(HttpSessionBindingEvent arg0)
      {
         throw new RejectedException();
      }
   }
   
   public static class FailDeserializationAttribute implements Serializable
   {
      /** The serialVersionUID */
      private static final long serialVersionUID = 1L;
      
      private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException
      {
         throw new RejectedException();
      }
      
   }
   
   public static class RejectedException extends RuntimeException
   {
      /** The serialVersionUID */
      private static final long serialVersionUID = 1L;
      
   }

}
