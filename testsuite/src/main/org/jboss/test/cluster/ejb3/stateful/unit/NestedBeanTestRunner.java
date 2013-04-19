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
import org.jboss.test.cluster.ejb3.stateful.nested.base.std.NestedBeanMonitor;
import org.jboss.test.cluster.ejb3.stateful.nested.base.std.NestedStateful;
import org.jboss.test.cluster.ejb3.stateful.nested.base.std.ParentStatefulRemote;

/**
 * Encapsulates the functions needed for tests of nested SFSBs in
 * a class that can be used either by a JBossTestCase subclass or
 * a JBossClusteredTestCase subclass.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 108925 $
 */
public class NestedBeanTestRunner extends BaseTestRunner
{
   public NestedBeanTestRunner(InitialContext context, Logger log)
   {
      super(context, log);
   }
   
   
   public void testBasic()
   throws Exception
   {
      getLog().debug("Running testBasic()");
      getLog().debug("==================================");
      
      NestedBeanSet beanSet = getNestedBeanSet();
      ParentStatefulRemote stateful = beanSet.parent;
      
      Assert.assertEquals("Remote counter: ", 1, stateful.increment());
      Assert.assertEquals("Remote counter: ", 2, stateful.increment());
      Assert.assertEquals("Local counter: ", 1, stateful.incrementLocal());
      Assert.assertEquals("Local counter: ", 2, stateful.incrementLocal());
      
      removeBean(stateful);
      
      getLog().debug("ok");
   }
   
   public void testDependentLifecycle()
   throws Exception
   {
      getLog().debug("Running testDependentLifecycle()");
      getLog().debug("==================================");
      
      NestedBeanSet beanSet = getNestedBeanSet();
      ParentStatefulRemote parent = beanSet.parent;
      NestedStateful nested = beanSet.nested;
      NestedBeanMonitor monitor = beanSet.monitor;
      
      Assert.assertEquals("Remote counter (via parent): ", 1, parent.increment());  
      
      String deepId = monitor.getDeepNestedId();
      Assert.assertNotNull("Got a deepId", deepId);
      Assert.assertFalse("Got a non-ERROR deepId", "ERROR".equals(deepId));
      String localDeepId = monitor.getLocalDeepNestedId();
      Assert.assertNotNull("Got a localDeepId", localDeepId);
      Assert.assertFalse("Got a non-ERROR localDeepId", "ERROR".equals(localDeepId));
       
      removeBean(parent);
       
      // Confirm parent is dead
      Assert.assertEquals("Remote counter (via dead parent): ", -1, monitor.incrementParent());
      Assert.assertFalse("parent.remove() fails", monitor.removeParent());
      
      // Confirm nested beans still work following parent remove
      Assert.assertEquals("Remote counter (direct):", 2 ,nested.increment());      
      Assert.assertEquals("Local counter (monitor):", 1 , monitor.incrementLocalNested());
      Assert.assertEquals("Deep nested id", deepId, monitor.getDeepNestedId());
      Assert.assertEquals("Local deep nested id", localDeepId, monitor.getLocalDeepNestedId());
      
      // Remove the bottom tier
      Assert.assertTrue("Local deep nested removed", monitor.removeLocalDeepNested());
      
      // Confirm it is dead
      Assert.assertEquals("Local deep nested removed", "ERROR", monitor.getLocalDeepNestedId());
      Assert.assertFalse("localDeepNested.remove() fails", monitor.removeLocalDeepNested());
      
      // Confirm other beans still work following parent remove
      Assert.assertEquals("Remote counter (direct):", 3 ,nested.increment());      
      Assert.assertEquals("Local counter (monitor):", 2 , monitor.incrementLocalNested());
      Assert.assertEquals("Deep nested id", deepId, monitor.getDeepNestedId());
      
      // Remove the nested bean
      removeBean(nested);
      
      Assert.assertEquals("Remote counter (via monitor): ", -1, monitor.incrementNested());
      Assert.assertFalse("nested.remove() fails", monitor.removeNested());
      
      // Confirm other beans still work
      Assert.assertEquals("Local counter (monitor):", 3 , monitor.incrementLocalNested());
      Assert.assertEquals("Deep nested id", deepId, monitor.getDeepNestedId());
      
      // Remove the local nested bean
      Assert.assertTrue("Local nested removed", monitor.removeLocalNested());
      
      // Confirm it is dead
      Assert.assertEquals("Local nested removed", -1, monitor.incrementLocalNested());
      Assert.assertFalse("localNested.remove() fails", monitor.removeLocalNested());
      
      // Confirm other beans still work following parent remove
      Assert.assertEquals("Deep nested id", deepId, monitor.getDeepNestedId());
      
      // Remove the deep nested bean
      Assert.assertTrue("Deep removed", monitor.removeDeepNested());
      
      // Confirm it is dead
      Assert.assertEquals("Local nested removed", "ERROR", monitor.getDeepNestedId());
      Assert.assertFalse("deepNested.remove() fails", monitor.removeDeepNested());
      
      removeBean(monitor);
      
      getLog().debug("ok");
      
   }

   public void testStatefulPassivation()
   throws Exception
   {
      getLog().debug("Running testStatefulPassivation()");
      getLog().debug("==================================");
      
      NestedBeanSet beanSet = getNestedBeanSet();
      NestedBeanMonitor monitor = beanSet.monitor;
      ParentStatefulRemote parent = beanSet.parent;
      NestedStateful nested = beanSet.nested;
      
      int parentInv = beanSet.parentInvocations; // 2
      int nestedInv = beanSet.nestedInvocations; // 1

//    Assert.assertEquals("Remote counter: ", 1, parent.increment());
      Assert.assertEquals("Remote counter: ", 1, monitor.incrementParent());
      parentInv++; //3
      nestedInv++; //2

//    Assert.assertEquals("Remote counter: ", 2, parent.increment());
      Assert.assertEquals("Remote counter: ", 2, monitor.incrementParent());
      parentInv++; //4
      nestedInv++; //3
//    Assert.assertEquals("Local counter: ", 1, parent.incrementLocal());
      Assert.assertEquals("Local counter: ", 1, monitor.incrementLocalNested());
      parentInv++; //5
//    Assert.assertEquals("Local counter: ", 2, parent.incrementLocal());
      Assert.assertEquals("Local counter: ", 2, monitor.incrementLocalNested());
      parentInv++; //6
      
      // How the nested bean behaves depends on whether its context is
      // contained by the parent bean context, which varies. Here we use
      // the passivation counts to determine what the situation is and
      // then use that behavior later to establish correct assertions.
      int parentPre = monitor.getParentPassivations();
      Assert.assertEquals("Parent passivate count: ",
                          getExpectedPassivations(0, parentInv), 
                          parentPre);
      parentInv++; //7
      boolean nestedIsContained = (monitor.getNestedPassivations() == parentPre + 1); // add one because parent.getPrePassivate() added one
      System.err.println("nestedIsContained = " + nestedIsContained);
      if (nestedIsContained)
      {
         // Above parentInv++ should also have been nestedInv++
         // plus one more from getNestedBeanSet()
         nestedInv += 4; //3-7
         // nested.getPrePassivate() activates the parent, which means
         // the passivation below will increment the passivate count.
         // So record that
         parentInv++; //6-7
      }
      // nested.getPrePassivate() activates nested, which means
      // the passivation below will increment the passivate count.
      // So record that 
      nestedInv++; //4-8
      
      sleep(getSleepTime());  // should passivate

      Assert.assertEquals("Parent passivate count: ",
                          getExpectedPassivations(1, parentInv),  
//                          parent.getPrePassivate());
                          monitor.getParentPassivations());
      parentInv++; //7-8
      if (nestedIsContained)
      {
         nestedInv++; //4-9
      }
      Assert.assertEquals("Parent activate count: ",
                          getExpectedPassivations(1, parentInv),
//                          parent.getPostActivate());
                          monitor.getParentActivations());
      parentInv++; //8-9
      if (nestedIsContained)
      {
         nestedInv++; //4-10
      }
      Assert.assertEquals("Remote nested passivate count: ",
                          getExpectedPassivations(1, nestedInv),  
//                          nested.getPrePassivate());
                          monitor.getNestedPassivations());
      Assert.assertEquals("Remote nested activate count: ",
                          getExpectedPassivations(1, nestedInv),
//                          nested.getPostActivate());
                          monitor.getNestedActivations());
      Assert.assertEquals("Local nested passivate count: ",
                          getExpectedPassivations(1, parentInv),
//                          parent.getLocalNestedPrePassivate());
                          monitor.getLocalNestedPassivations());
      parentInv++; //9-10
      if (nestedIsContained)
      {
         nestedInv++; //4-11
      }
      Assert.assertEquals("Local nested activate count: ",
                          getExpectedPassivations(1, parentInv),
//                          parent.getLocalNestedPostActivate());
                          monitor.getLocalNestedActivations());
      parentInv++; //10-11
      if (nestedIsContained)
      {
         nestedInv++; //4-12
      }
      
      // Use the monitor to check the deep nested beans.  In a cluster these
      // are marked not to treat replication as passivation, so we ignore 
      // the number of invocations
      Assert.assertEquals("Deep nested passivate count: ", 1, monitor.getDeepNestedPassivations());
      nestedInv++; //5-13
      if (nestedIsContained)
      {
         parentInv++; //10-12
      }
      Assert.assertEquals("Deep nested activate count: ", 1, monitor.getDeepNestedActivations());
      nestedInv++; //6-14
      if (nestedIsContained)
      {
         parentInv++; //10-13
      }
      Assert.assertEquals("Local deep nested passivate count: ", 1, monitor.getLocalDeepNestedPassivations());
      parentInv++; //11-14
      if (nestedIsContained)
      {
         nestedInv++; //6-15
      }
      Assert.assertEquals("Local deep nested activate count: ", 1, monitor.getLocalDeepNestedActivations());
      parentInv++; //12-15
      if (nestedIsContained)
      {
         nestedInv++; //6-16
      }
      
//    Assert.assertEquals("Remote counter: ", 3, parent.increment());
      Assert.assertEquals("Remote counter: ", 3, monitor.incrementParent());
      parentInv++; //13-16
      nestedInv++; //7-17
//    Assert.assertEquals("Remote counter: ", 4, parent.increment());
      Assert.assertEquals("Remote counter: ", 4, monitor.incrementParent());
      parentInv++; //14-17
      nestedInv++; //8-18
//    Assert.assertEquals("Local counter: ", 3, parent.incrementLocal());
      Assert.assertEquals("Local counter: ", 3, monitor.incrementLocalNested());
      parentInv++; //15-18
      if (nestedIsContained)
      {
         nestedInv++; //8-19
      }
//    Assert.assertEquals("Local counter: ", 4, parent.incrementLocal());
      Assert.assertEquals("Local counter: ", 4, monitor.incrementLocalNested()); 
      parentInv++;  //16-19
      if (nestedIsContained)
      {
         nestedInv++; //8-20
      }   
      
      sleep(getSleepTime());  // should passivate

      Assert.assertEquals("Parent passivate count: ",
                          getExpectedPassivations(2, parentInv),  
//                          parent.getPrePassivate());
                          monitor.getParentPassivations());
      parentInv++; //17-20
      if (nestedIsContained)
      {
         nestedInv++; //8-21
      }
      Assert.assertEquals("Parent activate count: ",
                          getExpectedPassivations(2, parentInv),
//                          parent.getPostActivate());
                          monitor.getParentActivations());
      parentInv++; //18-21
      if (nestedIsContained)
      {
         nestedInv++; //8-22
      }
      Assert.assertEquals("Remote nested passivate count: ",
                          getExpectedPassivations(2, nestedInv),  
//                          nested.getPrePassivate());
                          monitor.getNestedPassivations());
      Assert.assertEquals("Remote nested activate count: ",
                          getExpectedPassivations(2, nestedInv),  
//                          nested.getPostActivate());
                          monitor.getNestedActivations());
      Assert.assertEquals("Local nested passivate count: ",
                          getExpectedPassivations(2, parentInv), 
//                          parent.getLocalNestedPrePassivate());
                          monitor.getLocalNestedPassivations());
      parentInv++; //19-22
      if (nestedIsContained)
      {
         nestedInv++; //8-23
      }
      Assert.assertEquals("Local nested activate count: ",
                          getExpectedPassivations(2, parentInv), 
//                          parent.getLocalNestedPostActivate());
                          monitor.getLocalNestedActivations());
      parentInv++; //20-23
      if (nestedIsContained)
      {
         nestedInv++; //8-24
      }
      
      // Use the monitor to check the deep nested beans.
      Assert.assertEquals("Deep nested passivate count: ", 2, monitor.getDeepNestedPassivations());
      nestedInv++; //9-25
      if (nestedIsContained)
      {
         parentInv++; //20-24
      }
      Assert.assertEquals("Deep nested activate count: ", 2, monitor.getDeepNestedActivations());
      nestedInv++; //10-26
      if (nestedIsContained)
      {
         parentInv++; //20-25
      }
      Assert.assertEquals("Local deep nested passivate count: ", 2, monitor.getLocalDeepNestedPassivations());
      parentInv++; //21-26
      if (nestedIsContained)
      {
         nestedInv++; //10-27
      }
      Assert.assertEquals("Local deep nested activate count: ", 2, monitor.getLocalDeepNestedActivations());
      parentInv++; //22-27
      if (nestedIsContained)
      {
         nestedInv++; //10-28
      }
      
//    Assert.assertEquals("Remote counter: ", 5, parent.increment());
      Assert.assertEquals("Remote counter: ", 5, monitor.incrementParent());
      parentInv++; //23-28
      nestedInv++; //11-29
//    Assert.assertEquals("Remote counter: ", 6, parent.increment());
      Assert.assertEquals("Remote counter: ", 6, monitor.incrementParent());
      parentInv++; // 24-29
      nestedInv++; //12-30
//    Assert.assertEquals("Local counter: ", 5, parent.incrementLocal());
      Assert.assertEquals("Local counter: ", 5, monitor.incrementLocalNested());
      parentInv++;  //25-30
      if (nestedIsContained)
      {
         nestedInv++; //12-31
      }
//    Assert.assertEquals("Local counter: ", 6, parent.incrementLocal());
      Assert.assertEquals("Local counter: ", 6, monitor.incrementLocalNested());
      parentInv++; //26-31
      if (nestedIsContained)
      {
         nestedInv++; //12-32
      }
      
      removeBean(parent);
      if (nestedIsContained)
      {
         nestedInv++; //12-33
      }
      
      // Force the nested bean to go through another passivation
      // to check the activation process can survive the removal 
      // of its parent
      sleep(getSleepTime());
      
      // Confirm nested still works following parent remove
//    Assert.assertEquals("Remote counter (direct): ", 7, nested.increment());
      Assert.assertEquals("Remote counter (direct): ", 7, monitor.incrementNested());
      nestedInv++; //13-34
      Assert.assertEquals("Remote nested passivate count: ",
                          getExpectedPassivations(3, nestedInv), 
//                          nested.getPrePassivate());
                          monitor.getNestedPassivations());
      Assert.assertEquals("Remote nested activate count: ",
                          getExpectedPassivations(3, nestedInv), 
//                          nested.getPostActivate());
                          monitor.getNestedActivations());
      
      removeBean(nested);
      
      getLog().debug("ok");
   }
   
   public NestedBeanSet getNestedBeanSet()
   throws Exception
   {
      // We want a colocated monitor and SFSB to test passivation/destruction
      // StatelessRemoteBean is not clustered, so once we have it's VMID
      // we can keep creating SFSBs until we get one we want
      getLog().debug("Looking up NestedBeanMonitorBean...");
      NestedBeanMonitor monitor = (NestedBeanMonitor) getInitialContext().lookup("NestedBeanMonitorBean/remote");
      VMID monitorVM = monitor.getVMID();
      addRemovable(monitor);
      
      int parentInv = 0;
      int nestedInv = 0;
      
      ParentStatefulRemote parent = null;      
      boolean vmMatch = false;
      for (int i = 0; i < 20 && !vmMatch; i++)
      {
         getLog().debug("Looking up testParentStateful... Attempt " + (i + 1));
         parent = (ParentStatefulRemote) initialContext.lookup("testParentStateful/remote");
         
         vmMatch = monitorVM.equals(parent.getVMID());
         addRemovable(parent);
         if (!vmMatch)
         {
            removeBean(parent);
         }
      }
      
      Assert.assertTrue("Monitor and SFSB in same VM", vmMatch);
      
      // Reset the parent to initialize all the beans
      parent.reset();
      
      // Monitor the parent
      monitor.monitor(parent);
      
      // Reset the parent to clean things up
      parent.reset();
      parentInv++;
      nestedInv++;
      
      NestedStateful nested = (NestedStateful) parent.getNested();
      parentInv++;
      addRemovable(nested);
      
      NestedBeanSet result = new NestedBeanSet();
      result.monitor = monitor;
      result.parent = parent;
      result.nested = nested;
      result.parentInvocations = parentInv;
      result.nestedInvocations = nestedInv;
      
      return result;
   }
   
   

   public static class NestedBeanSet
   {
      public NestedBeanMonitor monitor;
      public ParentStatefulRemote parent;
      public NestedStateful nested;
      public int parentInvocations = 0;
      public int nestedInvocations = 0;
   }
   
}
