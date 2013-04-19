/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.test.cluster.ejb3.clusteredsession.unit;

import java.rmi.dgc.VMID;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.ha.framework.interfaces.ClusteringTargetsRepository;
import org.jboss.ha.framework.interfaces.FamilyClusterInfo;
import org.jboss.test.JBossClusteredTestCase;
import org.jboss.test.cluster.ejb3.clusteredsession.ClusteredStatelessRemote;
import org.jboss.test.cluster.ejb3.clusteredsession.NodeAnswer;

/**
 * Test slsb for load-balancing behaviour and others.
 *
 * @author  Ben.Wang@jboss.org
 * @version $Revision: 108925 $
 */
public class StatelessUnitTestCase extends JBossClusteredTestCase
{
   public static final String DEPLOYMENT =  "clusteredsession-test.jar";

   private static boolean deployed0 = true;
   private static boolean deployed1 = true;
   
   private static final String NAME_BASE= "jboss.j2ee:jar=";   
   private static final String BEAN_PREFIX = ",name=";
   private static final String SLSB = "clusteredStateless";
   private static final String NAME_QUALIFIER = ",service=EJB3socketDefaultPartition";
   
   public static int test = 0;
   static Date startDate = new Date();
   
   

   protected final String namingFactory = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);

   protected final String providerURL =
   System.getProperty(Context.PROVIDER_URL);

   public StatelessUnitTestCase (String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      TestSuite suite = new TestSuite();
      suite.addTest(TestSuite.createTest(StatelessUnitTestCase.class, "testServerFound"));
      suite.addTest(TestSuite.createTest(StatelessUnitTestCase.class, "testLoadbalance"));
      suite.addTest(TestSuite.createTest(StatelessUnitTestCase.class, "testLoadBalanceAfterTopologyChange"));
      return JBossClusteredTestCase.getDeploySetup(suite,
              DEPLOYMENT);
   }   

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      if (!deployed0)
      {
         deploy(getAdaptors()[0], DEPLOYMENT);
         deployed0 = true;
      }
      
      if (!deployed1)
      {
         deploy(getAdaptors()[1], DEPLOYMENT);
         deployed1 = true;
      }
   }

   protected InitialContext getInitialContext(int node) throws Exception {
      // Connect to the server0 JNDI
      String[] urls = getNamingURLs();
      Properties env1 = new Properties();
      env1.setProperty(Context.INITIAL_CONTEXT_FACTORY,
         "org.jnp.interfaces.NamingContextFactory");
      env1.setProperty(Context.PROVIDER_URL, urls[node]);
      return new InitialContext(env1);
   }

   public void testLoadbalance()
   throws Exception
   {
      getLog().debug(++StatelessUnitTestCase.test +"- "+"Trying the context...");

      InitialContext ctx = getInitialContext(0);

      getLog().debug("Test Stateless Bean loadbalancing");
      getLog().debug("==================================");
      getLog().debug(++StatelessUnitTestCase.test +"- "
              +"Looking up clusteredStateless/remote...");
      ClusteredStatelessRemote stateless = (ClusteredStatelessRemote) ctx.lookup("clusteredStateless/remote");
      
      confirmTargetCount(2, true);
      
      NodeAnswer node1 = stateless.getNodeState();
      getLog ().debug ("Node 1 ID: " +node1);

      confirmTargetCount(2, false);
      
      Map<VMID, Integer> callCount = new HashMap<VMID, Integer>();
      
      int allowedErr = 0;
      allowedErr = validateBalancing(stateless, callCount, 4, 2, 0);
      
      ctx = getInitialContext(1);
      stateless = (ClusteredStatelessRemote) ctx.lookup("clusteredStateless/remote");
      confirmTargetCount(2, true);
      NodeAnswer node2 = stateless.getNodeState();
      getLog ().debug ("Node 2 ID : " +node2);
      confirmTargetCount(2, true);
      
//      assertFalse("Second call went to different node " + node2.nodeId + " vs " + node1.nodeId,
//            node1.nodeId.equals(node2.nodeId));
      
      validateBalancing(stateless, callCount, 4, 2, allowedErr);
      
//      NodeAnswer call3 = stateless.getNodeState();
//      getLog ().debug ("Call 3 ID : " +call3);
//      confirmTargetCount(2, false);
//
//      assertEquals(node1.nodeId, call3.nodeId);
   }

   public void testLoadBalanceAfterTopologyChange() throws Exception
   {
      getLog().debug(++StatelessUnitTestCase.test +"- "+"Trying the context...");

      InitialContext ctx = getInitialContext(0);
   
      getLog().debug("Test Stateless Bean loadbalancing after topology change");
      getLog().debug("==================================");
      getLog().debug(++StatelessUnitTestCase.test +"- "
              +"Looking up clusteredStateless/remote...");
      ClusteredStatelessRemote stateless = (ClusteredStatelessRemote) ctx.lookup("clusteredStateless/remote");
      confirmTargetCount(2, true);
      NodeAnswer node1 = stateless.getNodeState();
      getLog ().debug ("Node 1 ID: " +node1);
      confirmTargetCount(2, true);
      NodeAnswer node2 = stateless.getNodeState();
      getLog ().debug ("Node 2 ID : " +node2);
         
      assertFalse(node1.nodeId.equals(node2.nodeId));
      confirmTargetCount(2, false);
      
      MBeanServerConnection[] adaptors = getAdaptors();
      
      deployed0 = false;
      undeploy(adaptors[0], DEPLOYMENT);
      
      NodeAnswer call3 = stateless.getNodeState();
      getLog ().debug ("Call 3 ID : " +call3);
      confirmTargetCount(1, true);
      
      assertTrue("Call 3 hit existing node", node1.nodeId.equals(call3.nodeId) || node2.nodeId.equals(call3.nodeId));
      
      NodeAnswer call4 = stateless.getNodeState();
      getLog ().debug ("Call 4 ID : " +call4);
      
      assertEquals("Call 3 and Call 4 hit the same node", call3.nodeId, call4.nodeId);
      
      deploy(adaptors[0], DEPLOYMENT);
      deployed0 = true;
      
      // Call once to get the new topology. The next call after this
      // might go to the same node if the topology change means the node
      // that handled this call moves in the target list to the next position
      NodeAnswer call5 = stateless.getNodeState();
      getLog ().debug ("Call 5 ID : " +call5);
      
      Map<VMID, Integer> callCount = new HashMap<VMID, Integer>();
      validateBalancing(stateless, callCount, 4, 2, 0);
      
      deployed1 = false;
      undeploy(adaptors[1], DEPLOYMENT);
      
      NodeAnswer call6 = stateless.getNodeState();
      getLog ().debug ("Call 6 ID : " +call6);
      confirmTargetCount(1, true);
      
      assertTrue("Call 6 hit existing node", callCount.containsKey(call6.nodeId));
      
      NodeAnswer call7 = stateless.getNodeState();
      getLog ().debug ("Call 7 ID : " +call7);
      
      assertEquals("Call 6 and Call 7 hit the same node", call6.nodeId, call7.nodeId);
      
      deploy(adaptors[1], DEPLOYMENT);
      deployed1 = true;
      
      // Call once to get the new topology. The next call after this
      // might go to the same node if the topology change means the node
      // that handled this call moves in the target list to the next position
      NodeAnswer call8 = stateless.getNodeState();
      getLog ().debug ("Call 8 ID : " +call8);
      
      callCount = new HashMap<VMID, Integer>();
      validateBalancing(stateless, callCount, 4, 2, 0);
   }
   
   private int validateBalancing(ClusteredStatelessRemote stateless, Map<VMID, Integer> callCount, int numCalls, int expectedServers, int allowedError)
   {
      for (int i = 0; i < numCalls; i++)
      {
         NodeAnswer answer = stateless.getNodeState();
         Integer count = callCount.get(answer.nodeId);
         if (count == null)
         {
            callCount.put(answer.nodeId, new Integer(1));
         }
         else
         {
            callCount.put(answer.nodeId, new Integer(count.intValue() + 1));
         }
      }
      
      assertEquals(expectedServers, callCount.size());
      
      int max = -1;
      int min = -1;
      
      for (Map.Entry<VMID, Integer> entry : callCount.entrySet())
      {
         int count = entry.getValue().intValue();
         if (max == -1)
         {
            max = min = count;
         }
         else if (count > max)
         {
            max = count;
         }
         else if (count < min)
         {
            min = count;
         }
         
         assertTrue(entry.getKey() +" count within allowed error", max - min <= allowedError);
      }
      
      return max - min;
   }
   
   private static String getFamilyName()
   {
      return NAME_BASE + DEPLOYMENT + BEAN_PREFIX + SLSB + NAME_QUALIFIER;
   }
   
   private void confirmTargetCount(int expectedCount, boolean logIt)
   {
      String familyName = getFamilyName();
      FamilyClusterInfo info = ClusteringTargetsRepository.getFamilyClusterInfo(familyName);
      assertNotNull("FamilyClusterInfo exists for " + familyName, info);
      List<?> targets = info.getTargets();
      assertEquals("FamilyClusterInfo for " + familyName + " has correct target count " + targets,
                   expectedCount, targets.size());
      if (logIt)
      {
         this.log.info("Targets are " + targets);
      }
   }
}
