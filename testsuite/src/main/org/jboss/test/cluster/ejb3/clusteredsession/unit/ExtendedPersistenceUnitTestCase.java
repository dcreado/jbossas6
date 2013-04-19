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

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.test.JBossClusteredTestCase;
import org.jboss.test.cluster.ejb3.common.unit.DBSetup;
import org.jboss.test.cluster.ejb3.stateful.nested.base.xpc.Customer;
import org.jboss.test.cluster.ejb3.stateful.nested.base.xpc.NestedXPCMonitor;
import org.jboss.test.cluster.ejb3.stateful.nested.base.xpc.ShoppingCart;
import org.jboss.test.cluster.ejb3.stateful.unit.XPCTestRunner;
import org.jboss.test.cluster.ejb3.stateful.unit.XPCTestRunner.BeanSet;

/**
 * Tests for ExtendedPersistenceContext management.
 * 
 * This class uses a delegate to execute the tests so the clustered
 * version of the tests (which derive from a different base class)
 * can use the same delegate code.
 *
 * @author Ben Wang
 * @version $Id: ExtendedPersistenceUnitTestCase.java 108940 2010-10-27 18:02:19Z pferraro $
 */
public class ExtendedPersistenceUnitTestCase 
   extends JBossClusteredTestCase
{
   private XPCTestRunner runner;
   
   public ExtendedPersistenceUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      TestSuite suite = new TestSuite();
      Test t1 = getDeploySetup(ExtendedPersistenceUnitTestCase.class,
                               "clusteredsession-ds.xml, clusteredsession-xpc.jar");

      suite.addTest(t1);

      // Create an initializer for the test suite
      DBSetup wrapper = new DBSetup(suite);
      return wrapper;     
   }

   
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      runner = new XPCTestRunner(getInitialContext(0), getLog());
      runner.setUp();
      // Use a sleep time equal to 2 thread runs + a 100 ms fudge
      runner.setSleepTime(10100L);
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


   public void testBasic() throws Exception
   {
      runner.testBasic();
   }
   
   public void testDependentLifecycle() throws Exception
   {
      runner.testDependentLifecycle();
   }
   
   public void testXPCSharing() throws Exception
   {
      runner.testXPCSharing();
   }

   public void testPassivation() throws Exception
   {
      runner.testPassivation();
   }
   
   public void testFailover() throws Exception
   {
      failoverTest(false, false, false);
   }
   
   public void testRepeatedFailover() throws Exception
   {
      failoverTest(false, false, true);
   }
   
   public void testFailoverWithPassivation() throws Exception
   {
      failoverTest(true, false, false);
   }
   
   public void testRepeatedFailoverWithPassivation() throws Exception
   {
      failoverTest(true, true, true);
   }
   
   private void failoverTest(boolean passivate, boolean passivateAgain, boolean repeated)
   throws Exception
   {
      BeanSet beanSet = runner.getBeanSet();
      ShoppingCart parent = beanSet.cart;
      NestedXPCMonitor monitor = beanSet.monitor;
      
      // Confirm the nested beans are there
      String deepId = monitor.getDeepNestedId();
      Assert.assertNotNull("Got a deepId", deepId);
      Assert.assertFalse("Got a non-ERROR deepId", "ERROR".equals(deepId));
      String localDeepId = monitor.getLocalDeepNestedId();
      Assert.assertNotNull("Got a localDeepId", localDeepId);
      Assert.assertFalse("Got a non-ERROR localDeepId", "ERROR".equals(localDeepId));
      
      VMID origId = parent.getVMID();
      Assert.assertNotNull("Got a VMID", origId);
      
      long id = parent.createCustomer();  
      Customer customer = parent.find(id);
      Assert.assertNotNull("Customer created and found on parent", customer);
      
      parent.setContainedCustomer();
      Assert.assertTrue("Parent and contained share customer", parent.checkContainedCustomer());
      
      Assert.assertTrue("Parent and remote nested do not share ref", 
                        monitor.compareTopToNested(id));
      Assert.assertTrue("Parent and local nested do share ref", 
                         monitor.compareTopToLocalNested(id));
      Assert.assertTrue("Remote nested and local nested do not share a ref",
                        monitor.compareNestedToLocalNested(id));
      Assert.assertTrue("Remote nested and deepNested share a ref", 
                        monitor.compareNestedToDeepNested(id));
      Assert.assertTrue("Local nested and localDeepNested share a ref", 
                        monitor.compareLocalNestedToLocalDeepNested(id));
      
      VMID newId = parent.getVMID();
      Assert.assertTrue("VMID remained the same", origId.equals(newId));
      
      if (passivate)
      {
         runner.sleep();
      }
      
      parent.setUpFailover("once");
      
      newId = parent.getVMID();
      assertNotNull("Got a new VMID", newId);
      assertFalse("Failover has occurred", origId.equals(newId));
      
      InitialContext[] ctxs = new InitialContext[2];
      ctxs[0] = getInitialContext(1);
      ctxs[1] = getInitialContext(0);
      monitor = runner.getXPCMonitor(ctxs, newId);      
      
      monitor.monitor(parent);
      
      Assert.assertTrue("Parent and remote nested do not share ref", 
                        monitor.compareTopToNested(id));
      Assert.assertTrue("Parent and local nested do share ref", 
                         monitor.compareTopToLocalNested(id));
      Assert.assertTrue("Remote nested and local nested do not share a ref",
                        monitor.compareNestedToLocalNested(id));
      Assert.assertTrue("Remote nested and deepNested share a ref", 
                        monitor.compareNestedToDeepNested(id));
      Assert.assertTrue("Local nested and localDeepNested share a ref", 
                        monitor.compareLocalNestedToLocalDeepNested(id));
      
      if (repeated)
      {
         if (passivateAgain)
         {
            runner.sleep();
         }
         origId = newId;
         
         parent.setUpFailover("once");
         
         newId = parent.getVMID();
         assertNotNull("Got a new VMID", newId);
         assertFalse("Failover has occurred", origId.equals(newId));
         
         // Swap the contexts so we try node0 first
         InitialContext ctx = ctxs[1];
         ctxs[1] = ctxs[0];
         ctxs[0] = ctx;
         
         monitor = runner.getXPCMonitor(ctxs, newId);      
         
         monitor.monitor(parent);
         
         Assert.assertTrue("Parent and remote nested do not share ref", 
                           monitor.compareTopToNested(id));
         Assert.assertTrue("Parent and local nested do share ref", 
                            monitor.compareTopToLocalNested(id));
         Assert.assertTrue("Remote nested and local nested do not share a ref",
                           monitor.compareNestedToLocalNested(id));
         Assert.assertTrue("Remote nested and deepNested share a ref", 
                           monitor.compareNestedToDeepNested(id));
         Assert.assertTrue("Local nested and localDeepNested share a ref", 
                           monitor.compareLocalNestedToLocalDeepNested(id));
         
      }  
   }

}
