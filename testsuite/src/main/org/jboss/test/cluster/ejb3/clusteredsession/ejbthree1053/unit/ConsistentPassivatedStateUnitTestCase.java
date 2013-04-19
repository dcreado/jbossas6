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

package org.jboss.test.cluster.ejb3.clusteredsession.ejbthree1053.unit;

import java.rmi.dgc.VMID;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.Test;

import org.jboss.test.JBossClusteredTestCase;
import org.jboss.test.cluster.ejb3.stateful.nested.base.std.NestedBeanMonitor;
import org.jboss.test.cluster.ejb3.stateful.nested.base.std.NestedStateful;
import org.jboss.test.cluster.ejb3.stateful.nested.base.std.ParentStatefulRemote;
import org.jboss.test.cluster.ejb3.stateful.unit.NestedBeanTestRunner;
import org.jboss.test.cluster.ejb3.stateful.unit.NestedBeanTestRunner.NestedBeanSet;
import org.jnp.interfaces.NamingContext;

/**
 * FIXME This is a very weak test for EJBTHREE-1053; 
 * replace with something better when EJBTHREE-1053 is fixed 
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 68169 $
 */
public class ConsistentPassivatedStateUnitTestCase 
   extends JBossClusteredTestCase
{
   private NestedBeanTestRunner runner;
   
   
   public ConsistentPassivatedStateUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(ConsistentPassivatedStateUnitTestCase.class,
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
   
   /**
    * 
    * 
    * @throws Exception
    */
   public void testConsistentPassivatedState()
   throws Exception
   {
      getLog().debug("Running testConsistentPassivatedState()");
      getLog().debug("=======================================");
      
      int parentInv = 0;
      int nestedInv = 0;
      

      String[] namingURLS = this.getNamingURLs();
      
      Properties env = new Properties();
      env.put(NamingContext.PROVIDER_URL, namingURLS[0]);
      
      Context ctx = new InitialContext(env); 
      
      getLog().debug("Looking up NestedBeanMonitorBean...");
      NestedBeanMonitor monitor = (NestedBeanMonitor) ctx.lookup("NestedBeanMonitorBean/remote");
      VMID monitorVM = monitor.getVMID();
      runner.addRemovable(monitor);
      ParentStatefulRemote parent = null;      
      boolean vmMatch = true;
      for (int i = 0; i < 20 && vmMatch; i++)
      {
         env.put(NamingContext.PROVIDER_URL, namingURLS[i % 2]);         
         ctx = new InitialContext(env); 
         
         getLog().debug("Looking up testParentStateful... Attempt " + (i + 1));
         parent = (ParentStatefulRemote) ctx.lookup("testParentStateful/remote");

         VMID parentVM = parent.getVMID();
         vmMatch = monitorVM.equals(parentVM);
         runner.addRemovable(parent);
         if (vmMatch)
         {
            runner.removeBean(parent);
         }
      }
      
      assertFalse("Monitor and SFSB in same VM", vmMatch);
      
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
      runner.addRemovable(nested);
      
      NestedBeanSet beanSet = runner.getNestedBeanSet();
      
      int attempts = 0;
      while (beanSet.parent.getVMID().equals(beanSet.nested.getVMID()))
      {
         if (++attempts == 20)
         {
            System.err.println("Unable to obtain nested bean running on separate VM from parent");
            log.warn("Unable to obtain nested bean running on separate VM from parent");
            return;
         }
         beanSet = runner.getNestedBeanSet();
      }

      // Exercise the beans, trigger replication
      assertEquals("Remote counter: ", 1, parent.increment());
      parentInv++;
      nestedInv++;
      assertEquals("Remote counter: ", 2, parent.increment());
      parentInv++;
      nestedInv++;
      assertEquals("Local counter: ", 1, parent.incrementLocal());
      parentInv++;
      assertEquals("Local counter: ", 2, parent.incrementLocal());
      parentInv++;
      
      // This call activates the nested bean without triggering the @PrePassivate
      // callback at the end of the call because of the way the bean
      // implements Optimized
      nested.getPrePassivate();
      
      // Passivate. The nested bean will get the @PrePassivate callback since
      // the previous call activated it
      sleep(runner.getSleepTime());
      
      // NOTE: here the invocation goes through the monitor, which has a
      // different proxy.  It may pick a different target server than that
      // used by our 'nested' variable's proxy.  If so, we then test if the 
      // state is as expected on the 2nd server.
      // If by chance it picks the same target as 'nested', this test is meaningless
      // So, any failures will be transient
      // TODO if JBCACHE-1190 is fixed this test may need some re-work
      int prePass = monitor.getNestedPassivations();
      assertTrue("EJBTHREE-1053 Deep nested passivate count (" + prePass +
            ") incorrect, expected " + (nestedInv + 1) + " or " + (parentInv + 1), 
            prePass == nestedInv + 1 || prePass == parentInv + 1);
      
      getLog().debug("ok");
   }

}
