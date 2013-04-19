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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

import org.infinispan.manager.CacheContainer;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.metadata.web.jboss.ReplicationTrigger;
import org.jboss.test.JBossTestCase;
import org.jboss.test.cluster.testutil.CacheConfigTestSetup;
import org.jboss.test.cluster.testutil.SessionTestUtil;
import org.jboss.test.cluster.web.mocks.BasicRequestHandler;
import org.jboss.test.cluster.web.mocks.SetAttributesRequestHandler;
import org.jboss.web.tomcat.service.session.JBossCacheManager;

/**
 * Tests for JBAS-7205. Deploy two wars on each node, sharing a cache between
 * them. Confirm that redeploying one war results in state being received.
 * 
 * @author Brian Stansberry
 */
public class MultipleWarSingleRedeployTestCase extends JBossTestCase
{
   protected static CacheContainer[] cacheContainers = new CacheContainer[2];

   protected static long testId = System.currentTimeMillis();
   
   protected Logger log = Logger.getLogger(getClass());   
   
   protected JBossCacheManager<?>[] managersA = new JBossCacheManager[cacheContainers.length];
   protected JBossCacheManager<?>[] managersB = new JBossCacheManager[cacheContainers.length];
   
   protected Map<String, Object> allAttributes;
   
   public MultipleWarSingleRedeployTestCase(String name)
   {
      super(name);
   }
   
   public static Test suite() throws Exception
   {
      return CacheConfigTestSetup.getTestSetup(MultipleWarSingleRedeployTestCase.class, cacheContainers, false, null, null);
   }

   
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      allAttributes = new HashMap<String, Object>();
      
      allAttributes.put("key", "value");
      
      allAttributes = Collections.unmodifiableMap(allAttributes);
   }

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      
      for (JBossCacheManager<?> manager : managersA)
      {
         if (manager != null)
         {
            manager.stop();
         }
      }
      
      for (JBossCacheManager<?> manager : managersB)
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
   
   public void testMultipleWarSingleRedeploy() throws Exception
   {
      String warnameA = "A" + String.valueOf(++testId);
      this.startManagers(warnameA, managersA);
      
      String warnameB = "B" + String.valueOf(testId);
      this.startManagers(warnameB, managersB);
      
      // Establish session.
      SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(allAttributes, false);
      SessionTestUtil.invokeRequest(managersA[0], setHandler, null);
      validateNewSession(setHandler);
      String idA = setHandler.getSessionId();
      
      setHandler = new SetAttributesRequestHandler(allAttributes, false);
      SessionTestUtil.invokeRequest(managersB[0], setHandler, null);
      validateNewSession(setHandler);
      String idB = setHandler.getSessionId();
      
      BasicRequestHandler getHandler = new BasicRequestHandler(allAttributes.keySet(), false);
      SessionTestUtil.invokeRequest(managersA[1], getHandler, idA);
      
      validateExpectedAttributes(allAttributes, getHandler);
      
      getHandler = new BasicRequestHandler(allAttributes.keySet(), false);
      SessionTestUtil.invokeRequest(managersB[1], getHandler, idB);
      
      validateExpectedAttributes(allAttributes, getHandler);
      
      // Undeploy one webapp on node 1
      managersB[1].stop();
      log.info("jbcmB1 stopped");

      // Deploy again
      managersB[1] = this.startManager(warnameB, cacheContainers[1]);

      log.info("jbcmB1 started");
      
//      log.info(dcmFactories[0].getCache().getMembers());
//      log.info(dcmFactories[1].getCache().getMembers());
      
      getHandler = new BasicRequestHandler(allAttributes.keySet(), false);
      SessionTestUtil.invokeRequest(managersA[1], getHandler, idA);
      
      validateExpectedAttributes(allAttributes, getHandler);
      
      getHandler = new BasicRequestHandler(allAttributes.keySet(), false);
      SessionTestUtil.invokeRequest(managersB[1], getHandler, idB);
      
      validateExpectedAttributes(allAttributes, getHandler);
   }
   
   protected void startManagers(String warname, JBossCacheManager<?>[] managers) throws Exception
   {
      for (int i = 0; i < cacheContainers.length; i++)
      {
         managers[i] = this.startManager(warname, cacheContainers[i]);
      }
   }
   
   protected JBossCacheManager<?> startManager(String warname, CacheContainer cacheContainer) throws Exception
   {
      JBossCacheManager<?> manager = SessionTestUtil.createManager(warname, 100, cacheContainer, null);
      JBossWebMetaData metadata = SessionTestUtil.createWebMetaData(getReplicationGranularity(), getReplicationTrigger(), true, 30);
      manager.init(warname, metadata);
      manager.start();
      
      return manager;
   }
   
   protected void validateExpectedAttributes(Map<String, Object> expected, BasicRequestHandler handler)
   {
      assertFalse(handler.isNewSession());
      
      if (handler.isCheckAttributeNames())
      {
         assertEquals(expected.size(), handler.getAttributeNames().size());
      }
      Map<String, Object> checked = handler.getCheckedAttributes();
      assertEquals(expected.size(), checked.size());
      for (Map.Entry<String, Object> entry : checked.entrySet())
      {
         assertEquals(entry.getKey(), expected.get(entry.getKey()), entry.getValue());
      }
      
   }
   
   protected void validateNewSession(BasicRequestHandler handler)
   {
      assertTrue(handler.isNewSession());
      assertEquals(handler.getCreationTime(), handler.getLastAccessedTime());
      if (handler.isCheckAttributeNames())
      {
         assertEquals(0, handler.getAttributeNames().size());
      }
      Map<String, Object> checked = handler.getCheckedAttributes();
      for (Map.Entry<String, Object> entry : checked.entrySet())
         assertNull(entry.getKey(), entry.getValue());
   }
   

}
