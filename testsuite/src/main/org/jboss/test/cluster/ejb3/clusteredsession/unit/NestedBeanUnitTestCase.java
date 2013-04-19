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

package org.jboss.test.cluster.ejb3.clusteredsession.unit;

import java.rmi.dgc.VMID;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.Test;

import org.jboss.test.JBossClusteredTestCase;
import org.jboss.test.cluster.ejb3.stateful.nested.base.std.ParentStatefulRemote;
import org.jboss.test.cluster.ejb3.stateful.unit.NestedBeanTestRunner;
import org.jboss.test.cluster.ejb3.stateful.unit.NestedBeanTestRunner.NestedBeanSet;

/**
 * Overrides the parent test to use clustered versions of
 * the beans.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 108940 $
 */
public class NestedBeanUnitTestCase 
   extends JBossClusteredTestCase
{
   private NestedBeanTestRunner runner;
   
   
   public NestedBeanUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(NestedBeanUnitTestCase.class,
                            "clusteredsession-nested.jar");
   }
   
   
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      runner = new NestedBeanTestRunner(getInitialContext(0), getLog());
      runner.setUp();
      // Use a sleep time equal to 2 thread runs + a 100 ms fudge
      runner.setSleepTime(10100L);
      // For clustered beans, an invocation is a passivation
      runner.setPassivationPerInvocation(1);
      // For clustered beans, passivation occurs after already called
      // @PrePassivate for replication, so don't get a 2nd event
      runner.setPassivationPerSleep(0);
   }

   private InitialContext getInitialContext(int node) throws Exception {
      // Connect to the serverX JNDI
      String[] urls = getNamingURLs();
      Properties env1 = new Properties();
      env1.setProperty(Context.INITIAL_CONTEXT_FACTORY,
         "org.jnp.interfaces.NamingContextFactory");
      env1.setProperty(Context.PROVIDER_URL, urls[node]);
      return new InitialContext(env1);
   }

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      
      if (runner != null)
         runner.tearDown();
   }

   public void testBasic()
   throws Exception
   {
      runner.testBasic();
   }
   
   public void testDependentLifecycle()
   throws Exception
   {
      runner.testDependentLifecycle();      
   }

   public void testStatefulPassivation()
   throws Exception
   {
      runner.testStatefulPassivation();
   }


   public void testStatefulBeanCounterFailoverWithRemote()
   throws Exception
   {
      getLog().debug("Test Nested Stateful Bean Counter Failover with Remote");
      getLog().debug("======================================================");
      
      NestedBeanSet beanSet = runner.getNestedBeanSet();
      ParentStatefulRemote stateful = beanSet.parent;
      VMID node1 = stateful.getVMID();
      assertNotNull("State node: ", node1);
      getLog ().debug ("Node 1 ID: " +node1);

      assertEquals("Counter: ", 1, stateful.increment());
      assertEquals("Counter: ", 2, stateful.increment());
      sleep(300);

      // Now we switch to the other node, simulating a failure on node 1
      stateful.setUpFailover("once");
      VMID node2 = stateful.getVMID();
      assertNotNull("State node: ", node2);
      getLog ().debug ("Node 2 ID : " +node2);

      assertFalse("Failover has occured", node1.equals(node2));

      assertEquals("Counter: ", 3, stateful.increment());
      assertEquals("Counter: ", 4, stateful.increment());

      runner.removeBean(stateful);
      getLog().debug("ok");
   }


   public void testStatefulBeanCounterFailover()
   throws Exception
   {
      getLog().debug("Test Nested Stateful Bean Counter Failover");
      getLog().debug("==========================================");
      
      NestedBeanSet beanSet = runner.getNestedBeanSet();
      ParentStatefulRemote stateful = beanSet.parent;
      VMID node1 = stateful.getVMID();
      assertNotNull("State node: ", node1);
      getLog ().debug ("Node 1 ID: " +node1);

      assertEquals("Counter: ", 1, stateful.incrementLocal());
      assertEquals("Counter: ", 2, stateful.incrementLocal());
      sleep(300);

      // Now we switch to the other node, simulating a failure on node 1
      stateful.setUpFailover("once");
      VMID node2 = stateful.getVMID();
      assertNotNull("State node: ", node2);
      getLog ().debug ("Node 2 ID : " +node2);

      assertFalse("Failover has occured", node1.equals(node2));

      assertEquals("Counter: ", 3, stateful.incrementLocal());
      assertEquals("Counter: ", 4, stateful.incrementLocal());

      runner.removeBean(stateful);
      getLog().debug("ok");
   }

}
