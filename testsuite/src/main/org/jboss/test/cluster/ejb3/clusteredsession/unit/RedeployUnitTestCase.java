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

import javax.management.MBeanServerConnection;
import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.Assert;
import junit.framework.Test;

import org.jboss.test.JBossClusteredTestCase;
import org.jboss.test.cluster.ejb3.clusteredsession.NodeAnswer;
import org.jboss.test.cluster.ejb3.clusteredsession.StatefulRemote;
import org.jboss.test.cluster.ejb3.stateful.nested.base.VMTracker;

/**
 * Tests redeployment and state transfer.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 108940 $
 */
public class RedeployUnitTestCase extends JBossClusteredTestCase
{
   private static boolean deployed0 = true;
   private static boolean deployed1 = true;
   
   private static final String deployment = "clusteredsession-test.jar";
   
   private StatefulRemote stateful = null;  
   
   /**
    * Create a new RedeployUnitTestCase.
    * 
    * @param name
    */
   public RedeployUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(RedeployUnitTestCase.class, deployment);
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      MBeanServerConnection[] adaptors = getAdaptors();
      if (!deployed0)
      {
         deploy(adaptors[0], deployment);
         getLog().debug("Deployed " + deployment + " on server0");
         deployed0 = true;         
      }
      
      if (!deployed1)
      {
         deploy(adaptors[1], deployment);
         getLog().debug("Deployed " + deployment + " on server1");
         deployed1 = true;         
      }
   }


   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      
      if (stateful != null)
      {
         try
         {
            stateful.remove();
         }
         catch (Exception e)
         {
            log.error("Problem removing stateful", e);
         }
         finally
         {
            stateful = null;
         }
      }
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
   
   public void testStatefulBeanRedeploy() 
   throws Exception
   {  
      getLog().debug("Test Stateful Bean Redeploy");
      getLog().debug("================================================");
      
      getLog().debug("Trying the context...");

      // Connect to the server0 JNDI
      InitialContext ctx = getInitialContext(0);
      
      // We want a colocated SLSB and SFSB to test passivation/destruction
      // VMIDTrackerBean is not clustered, so once we have it's VMID
      // we can keep creating SFSBs until we get one we want
      getLog().debug("Looking up VMTrackerBean...");
      VMTracker tracker = (VMTracker) getInitialContext(0).lookup("VMTrackerBean/remote");
      VMID monitorVM = tracker.getVMID();
      
      NodeAnswer node0 = null;
      boolean vmMatch = false;
      for (int i = 0; i < 10 && !vmMatch; i++)
      {
         getLog().debug("Looking up testStateful/remote... Attempt " + i +1);
         stateful = (StatefulRemote) ctx.lookup("testStateful/remote");
         
         node0 = stateful.getNodeState();
         vmMatch = monitorVM.equals(node0.getNodeId());
         if (!vmMatch)
         {
            stateful.remove();
         }
      }
      
      Assert.assertTrue("Tracker and SFSB in same VM", vmMatch);

      stateful.setName("Bupple-Dupple");

      node0 = stateful.getNodeState();
      getLog().debug ("Node 0 ID: " +node0);
      
      sleep(500);

      // Redeploy on node 1
      redeploy();
      
      // Now we switch to the other node, simulating a failure on node 0
      stateful.setUpFailover("once");
      NodeAnswer node1 = stateful.getNodeState ();
      assertNotNull("State node: ", node1);
      getLog ().debug ("Node 1 ID : " +node1);

      assertFalse("Failover has occured", node0.nodeId.equals(node1.nodeId));

      assertEquals ("Node 0: ", "Bupple-Dupple", node0.answer);
      assertEquals ("Node 1: ", "Bupple-Dupple", node1.answer);
      
      getLog().debug("ok");
   }
   
   private void redeploy() throws Exception
   {
      MBeanServerConnection[] adaptors = getAdaptors();
      
      undeploy(adaptors[1], deployment);
      getLog().debug("Undeployed " + deployment + " on server1");
      deployed1 = false;
      
      sleep(2000);
      deploy(adaptors[1], deployment);
      getLog().debug("Deployed " + deployment + " on server1");
      deployed1 = true;
      
      sleep(5000);
   }
   
   

}
