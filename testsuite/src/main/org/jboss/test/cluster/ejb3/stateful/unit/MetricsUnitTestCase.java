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
package org.jboss.test.cluster.ejb3.stateful.unit;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import junit.framework.Test;

import org.jboss.logging.Logger;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.jboss.test.JBossTestCase;
import org.jboss.test.cluster.ejb3.stateful.Stateful;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public class MetricsUnitTestCase
extends JBossTestCase
{
   private static final Logger log = Logger.getLogger(MetricsUnitTestCase.class);
   
   private SecurityClient client = null;

   public MetricsUnitTestCase(String name)
   {
      super(name);
   }
   
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      this.client = SecurityClientFactory.getSecurityClient();
      client.setSimple("somebody", "password");
      client.login();
   }
   
   public static Test suite() throws Exception
   {
      return getDeploySetup(MetricsUnitTestCase.class, "stateful-test.jar");
   }

   @Override
   protected void tearDown() throws Exception
   {
      if(client == null)
         return;
      client.logout();
      client = null;
      
      super.tearDown();
   }
   
   protected void testJmxMetrics(MBeanServerConnection server, String jndiBinding, String objectName) throws Exception
   {
      ObjectName testerName = new ObjectName(objectName);
      
      // Get the start cache size 
      int startCacheSize = (Integer)server.getAttribute(testerName, "CacheSize");
      int startTotalSize = (Integer)server.getAttribute(testerName, "TotalSize");
      
      // Establish how many beans are already active
      int prevCount = (Integer)server.getAttribute(testerName, "CreateCount");
      System.out.println("prevCount = " + prevCount);
      assertTrue("can't have negative creation count", prevCount >= 0);
      
      Stateful stateful = (Stateful)getInitialContext().lookup(jndiBinding);
      assertNotNull(stateful);
      stateful.setState("state");
      
      int createCount = (Integer)server.getAttribute(testerName, "CreateCount");
      assertEquals(1, createCount - prevCount);
      
      // Ensure cache is incremented
      int newCacheSize = (Integer)server.getAttribute(testerName, "CacheSize");
      assertEquals(startCacheSize+1, newCacheSize);
      
      int totalSize = (Integer)server.getAttribute(testerName, "TotalSize");
      assertEquals(startTotalSize+1, totalSize);
      
      assertEquals("state", stateful.getState());
      stateful.testSerializedState("state");
      stateful.clearPassivated();
      assertEquals(null, stateful.getInterceptorState());
      stateful.setInterceptorState("hello world");
      assertFalse(stateful.testSessionContext());
      Thread.sleep(10 * 1000);
      
      int cacheSize = (Integer)server.getAttribute(testerName, "CacheSize");
      assertEquals(0, cacheSize);
      
      int passivatedCount = (Integer)server.getAttribute(testerName, "PassivatedCount");
      assertEquals(1, passivatedCount - prevCount);
      assertTrue(stateful.wasPassivated());
      
      totalSize = (Integer)server.getAttribute(testerName, "TotalSize");
      assertEquals(cacheSize + passivatedCount, totalSize);
      
      assertEquals("state", stateful.getState());
      assertEquals("hello world", stateful.getInterceptorState());
      
      passivatedCount = (Integer)server.getAttribute(testerName, "PassivatedCount");
      assertEquals(0, passivatedCount - prevCount);

      Stateful another = (Stateful)getInitialContext().lookup(jndiBinding);
      assertEquals(null, another.getInterceptorState());
      another.setInterceptorState("foo");
      assertEquals("foo", another.getInterceptorState());
      assertEquals("hello world", stateful.getInterceptorState());
      
      assertFalse(stateful.testSessionContext());
      
      stateful.testResources();
      
      createCount = (Integer)server.getAttribute(testerName, "CreateCount");
      assertEquals(2, createCount - prevCount);
      
      // the injected beans are passivated at this point in time
      
      cacheSize = (Integer)server.getAttribute(testerName, "CacheSize");
      assertEquals(2, cacheSize);
      
      // keep in mind, we're not removing already injected beans
      
      int removeCount = (Integer)server.getAttribute(testerName, "RemoveCount");
      assertEquals(0, removeCount);
      
      another.removeMe();
      cacheSize = (Integer)server.getAttribute(testerName, "CacheSize");
      assertEquals(1, cacheSize);
      
      removeCount = (Integer)server.getAttribute(testerName, "RemoveCount");
      assertEquals(1, removeCount);
      
      totalSize = (Integer)server.getAttribute(testerName, "TotalSize");
      assertEquals(cacheSize + passivatedCount, totalSize);
      
      stateful.removeMe();
      cacheSize = (Integer)server.getAttribute(testerName, "CacheSize");
      assertEquals(0, cacheSize);
      
      removeCount = (Integer)server.getAttribute(testerName, "RemoveCount");
      assertEquals(2, removeCount);
      
      totalSize = (Integer)server.getAttribute(testerName, "TotalSize");
      assertEquals(cacheSize + passivatedCount, totalSize);
      
   }

   public void testJmxMetricsStateful() throws Exception
   {
      MBeanServerConnection server = getServer();

      // Note: we got one creation in ServiceBean
      
      testJmxMetrics(server, "Stateful", "jboss.j2ee:jar=stateful-test.jar,name=StatefulBean,service=EJB3");
   }
   
   public void testJmxMetricsTreeCacheStateful() throws Exception
   {
      MBeanServerConnection server = getServer();

      testJmxMetrics(server, "TreeCacheStateful", "jboss.j2ee:jar=stateful-test.jar,name=TreeCacheStatefulBean,service=EJB3");
   }
   
   public void testJmxMetricsTreeCacheRemovalStateful() throws Exception
   {
      MBeanServerConnection server = getServer();

      testJmxMetrics(server, "TreeCacheRemovalStateful", "jboss.j2ee:jar=stateful-test.jar,name=TreeCacheRemovalStatefulBean,service=EJB3");
   }
}
