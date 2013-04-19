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

import java.rmi.dgc.VMID;

import javax.naming.InitialContext;

import junit.framework.Assert;

import org.jboss.logging.Logger;
import org.jboss.test.cluster.ejb3.stateful.nested.base.xpc.Customer;
import org.jboss.test.cluster.ejb3.stateful.nested.base.xpc.NestedXPCMonitor;
import org.jboss.test.cluster.ejb3.stateful.nested.base.xpc.ShoppingCart;

/**
 * Encapsulates the functions needed for tests of ExtendedPersistenceContext
 * management by nested SFSBs in a class that can be used either by a 
 * JBossTestCase subclass or a JBossClusteredTestCase subclass.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 108925 $
 */
public class XPCTestRunner extends BaseTestRunner
{
   public XPCTestRunner(InitialContext context, Logger log)
   {
      super(context, log);
   }

   public void testBasic() throws Exception
   {
      BeanSet beanSet = getBeanSet();
      ShoppingCart cart = beanSet.cart;
      NestedXPCMonitor monitor = beanSet.monitor;
      
      Customer customer;

      long id = cart.createCustomer();    
      customer = monitor.find(id);
      Assert.assertEquals("Stateless has proper initial name",
                          "William", customer.getName());
      customer = cart.find(id);
      Assert.assertEquals("ShoppingCart has proper initial name",
                          "William", customer.getName());
      
      cart.update();
      customer = monitor.find(id);
      Assert.assertEquals("Stateless has proper updated name",
                           "Bill", customer.getName());
      customer = cart.find(id);
      Assert.assertEquals("ShoppingCart has proper updated name",
                          "Bill", customer.getName());
      
      cart.update2();
      customer = monitor.find(id);
      Assert.assertEquals("Stateless has proper update2() name",
                          "Billy", customer.getName());
      customer = cart.find(id);
      Assert.assertEquals("ShoppingCart has proper update2() name",
                          "Billy", customer.getName());
      
      cart.update3();
      customer = monitor.find(id);
      Assert.assertEquals("Stateless has proper update3() name",
                          "Bill Jr.", customer.getName());
      customer = cart.find(id);
      Assert.assertEquals("ShoppingCart has proper update3() name",
                          "Bill Jr.", customer.getName());
      
      removeBean(cart);
      removeBean(monitor);
   }
   
   public void testXPCSharing() throws Exception
   {
      getLog().debug("Running testXPCSharing()");
      getLog().debug("==================================");
      
      BeanSet beanSet = getBeanSet();
      ShoppingCart parent = beanSet.cart;
      NestedXPCMonitor monitor = beanSet.monitor;
      
      // Confirm the nested beans are there
      String deepId = monitor.getDeepNestedId();
      Assert.assertNotNull("Got a deepId", deepId);
      Assert.assertFalse("Got a non-ERROR deepId", "ERROR".equals(deepId));
      String localDeepId = monitor.getLocalDeepNestedId();
      Assert.assertNotNull("Got a localDeepId", localDeepId);
      Assert.assertFalse("Got a non-ERROR localDeepId", "ERROR".equals(localDeepId));
      
      long id = parent.createCustomer();  
      Customer customer = parent.find(id);
      Assert.assertNotNull("Customer created and found on parent", customer);
      
      parent.setContainedCustomer();
      Assert.assertTrue("Parent and local contained share customer", parent.checkContainedCustomer());
      
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
      
      // Confirm this survives passivation/activation
      
      parent.reset();
      
      sleep(getSleepTime());
      
      // DeepContained beans ignore replication in their 
      // activation/passivation counters, so we can test them
      // to confirm activation/passivation
      Assert.assertEquals("Local deep contained passivated once", 1,
                          monitor.getLocalDeepNestedPassivations());
      Assert.assertEquals("Local deep contained activated once", 1,
                          monitor.getLocalDeepNestedActivations());
      Assert.assertEquals("Deep contained passivated once", 1,
                          monitor.getDeepNestedPassivations());
      Assert.assertEquals("Deep contained activated once", 1,
                          monitor.getDeepNestedActivations());
      
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
      
      removeBean(parent);
      removeBean(monitor);      
   }
   
   public void testDependentLifecycle()
   throws Exception
   {
      getLog().debug("Running testDependentLifecycle()");
      getLog().debug("==================================");
      
      BeanSet beanSet = getBeanSet();
      ShoppingCart parent = beanSet.cart;
      NestedXPCMonitor monitor = beanSet.monitor;
      
      // Confirm the nested beans are there
      String deepId = monitor.getDeepNestedId();
      Assert.assertNotNull("Got a deepId", deepId);
      Assert.assertFalse("Got a non-ERROR deepId", "ERROR".equals(deepId));
      String localDeepId = monitor.getLocalDeepNestedId();
      Assert.assertNotNull("Got a localDeepId", localDeepId);
      Assert.assertFalse("Got a non-ERROR localDeepId", "ERROR".equals(localDeepId));
      
      long id = parent.createCustomer();  
      Customer customer = parent.find(id);
      Assert.assertNotNull("Customer created and found on parent", customer);
      
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
      
      // Remove the parent and see that things still work
      Assert.assertTrue("Parent removed", monitor.removeParent());
       
      // Confirm parent is dead
      Assert.assertFalse("parent.remove() fails", monitor.removeParent());
      
      Assert.assertTrue("Remote nested and local nested do not share a ref",
            monitor.compareNestedToLocalNested(id));
      Assert.assertTrue("Remote nested and deepNested share a ref", 
            monitor.compareNestedToDeepNested(id));
      Assert.assertTrue("Local nested and localDeepNested share a ref", 
            monitor.compareLocalNestedToLocalDeepNested(id));
      
      // Remove the bottom tier
      Assert.assertTrue("Local deep nested removed", monitor.removeLocalDeepNested());
      
      // Confirm it is dead
      Assert.assertFalse("localDeepNested.remove() fails", monitor.removeLocalDeepNested());
      
      // Confirm other beans still work following remove
      Assert.assertNotNull("Local nested still works", monitor.findLocalNested(id));
      Assert.assertTrue("Remote nested and local nested do not share a ref",
            monitor.compareNestedToLocalNested(id));
      Assert.assertTrue("Remote nested and deepNested share a ref", 
            monitor.compareNestedToDeepNested(id));
      
      // Remove the nested bean
      Assert.assertTrue("Remote nested removed", monitor.removeNested());
      
      // Confirm it is dead
      Assert.assertFalse("nested.remove() fails", monitor.removeNested());
      
      // Confirm other beans still work
      Assert.assertNotNull("Local nested still works", monitor.findLocalNested(id));
      Assert.assertNotNull("Remote deep nested still works", monitor.findDeepNested(id));
      
      // Remove the local nested bean
      Assert.assertTrue("Local nested removed", monitor.removeLocalNested());
      
      // Confirm it is dead
      Assert.assertFalse("localNested.remove() fails", monitor.removeLocalNested());
      
      // Confirm other beans still work following parent remove
      Assert.assertNotNull("Deep nested not null", monitor.findDeepNested(id));
      
      // Remove the deep nested bean
      Assert.assertTrue("Deep removed", monitor.removeDeepNested());
      
      // Confirm it is dead
      Assert.assertEquals("Local nested removed", "ERROR", monitor.getDeepNestedId());
      Assert.assertFalse("deepNested.remove() fails", monitor.removeDeepNested());
      
      removeBean(monitor);
      
      getLog().debug("ok");
      
   }

   public void testPassivation() throws Exception
   {
      BeanSet beanSet = getBeanSet();
      ShoppingCart cart = beanSet.cart;
      NestedXPCMonitor monitor = beanSet.monitor;
      
      Customer customer;
      
      long id = cart.createCustomer();      
      customer = monitor.find(id);
      Assert.assertEquals("Stateless has proper initial name",
                          "William", customer.getName());
      customer = cart.find(id);
      Assert.assertEquals("ShoppingCart has proper initial name",
                          "William", customer.getName());
      
      cart.update();
      customer = monitor.find(id);
      Assert.assertEquals("Stateless has proper updated name",
                           "Bill", customer.getName());
      customer = cart.find(id);
      Assert.assertEquals("ShoppingCart has proper updated name",
                          "Bill", customer.getName());
      
      cart.update2();
      customer = monitor.find(id);
      Assert.assertEquals("Stateless has proper update2() name",
                          "Billy", customer.getName());
      customer = cart.find(id);
      Assert.assertEquals("ShoppingCart has proper update2() name",
                          "Billy", customer.getName());
      
      cart.update3();
      customer = monitor.find(id);
      Assert.assertEquals("Stateless has proper update3() name",
                          "Bill Jr.", customer.getName());
      customer = cart.find(id);
      Assert.assertEquals("ShoppingCart has proper update3() name",
                          "Bill Jr.", customer.getName());
      
      // Tell the contained bean to get the customer
      cart.setContainedCustomer();
      
      // Clear the passivation counts
      cart.reset();
      
      sleep(getSleepTime()); // passivation   
      
      Assert.assertEquals("Contained bean was passivated", 1, monitor.getLocalNestedPassivations());
      
      Assert.assertTrue("Parent and local contained share customer", cart.checkContainedCustomer());
      
      Assert.assertTrue("Contained bean was activated", monitor.getLocalNestedActivations() > 0);  
      
      cart.findAndUpdateStateless();
      cart.updateContained();
      
      customer = monitor.find(id);
      Assert.assertEquals("contained modified", customer.getName());
      customer = cart.find(id);
      Assert.assertEquals("contained modified", customer.getName());
          
      removeBean(monitor);
      removeBean(cart);
   }

   public BeanSet getBeanSet() throws Exception
   {
      // We want a colocated monitor and SFSB to test passivation/destruction
      // NestedXPCMonitorBean is not clustered, so once we have it's VMID
      // we can keep creating SFSBs until we get one we want
      NestedXPCMonitor monitor = (NestedXPCMonitor) getInitialContext().lookup("NestedXPCMonitorBean/remote");
      VMID statelessVM = monitor.getVMID();
      addRemovable(monitor);

      ShoppingCart cart = null;      
      boolean vmMatch = false;
      for (int i = 0; i < 10 && !vmMatch; i++)
      {
         cart = (ShoppingCart) getInitialContext().lookup("testShoppingCart/remote");
         
         vmMatch = statelessVM.equals(cart.getVMID());
         addRemovable(cart);
         if (!vmMatch)
            removeBean(cart);
      }
      
      Assert.assertTrue("Monitor and SFSB in same VM", vmMatch);
      
      // Reset the parent to initialize all the beans
      cart.reset();
      
      // Monitor the parent
      monitor.monitor(cart);
      
      // Reset the parent to clean things up
      cart.reset();
      
      BeanSet result = new BeanSet();
      result.cart = cart;
      result.monitor = monitor;
      
      return result;
   }
   
   public NestedXPCMonitor getXPCMonitor(InitialContext[] ctxs, VMID vmid)
   throws Exception
   {
      NestedXPCMonitor monitor = null;      
      boolean vmMatch = false;
      for (int i = 0; i < 10 && !vmMatch; i++)
      {
         InitialContext ctx = ctxs[i % ctxs.length];
         monitor = (NestedXPCMonitor) ctx.lookup("NestedXPCMonitorBean/remote");
         
         vmMatch = vmid.equals(monitor.getVMID());
         addRemovable(monitor);
         if (!vmMatch)
            removeBean(monitor);
      }
      
      Assert.assertTrue("SLSB and SFSB in same VM", vmMatch);
      
      return monitor;
   }
   
   public static class BeanSet
   {
      public ShoppingCart cart;
      public NestedXPCMonitor monitor;
   }
}
