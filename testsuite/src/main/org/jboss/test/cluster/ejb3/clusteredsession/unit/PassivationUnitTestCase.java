/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.test.cluster.ejb3.clusteredsession.unit;

import java.util.Date;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.Test;

import org.jboss.test.JBossClusteredTestCase;
import org.jboss.test.cluster.ejb3.clusteredsession.NodeAnswer;
import org.jboss.test.cluster.ejb3.clusteredsession.StatefulRemote;

/**
 * Test SFSB for load-balancing and failover behaviour
 *
 * TODO add a test with a bean configured to not treat replication as
 * passivation
 * 
 * @author  Ben.Wang@jboss.org
 * @version $Revision: 108925 $
 */
public class PassivationUnitTestCase extends JBossClusteredTestCase
{
   static boolean deployed = false;
   public static int test = 0;
   static Date startDate = new Date();

   protected final String namingFactory =
   System.getProperty(Context.INITIAL_CONTEXT_FACTORY);

   protected final String providerURL =
   System.getProperty(Context.PROVIDER_URL);

   public PassivationUnitTestCase (String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      final String jarName = "clusteredsession-test.jar";
      Test t1 = JBossClusteredTestCase.getDeploySetup(PassivationUnitTestCase.class,
              jarName);
      return t1;
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

   /**
    */
   public void testStatefulPassivation()
      throws Exception
   {
      log.info("+++ testStatefulPassivation");

      // Connect to the server0 JNDI
      InitialContext ctx = getInitialContext(0);

      getLog().debug("==================================");
      getLog().debug(++PassivationUnitTestCase.test +"- "
              +"Looking up testStateful...");
      StatefulRemote remote = (StatefulRemote) ctx.lookup("testStateful/remote");

      int invCount = 0;
      
      remote.reset();
      invCount++;
      remote.setState("hello");
      invCount++;
      
      sleep_(11100);
      
      assertEquals("hello", remote.getState());
      invCount++;
      
      assertEquals(invCount, remote.getPrePassivate());
      invCount++;
      
      assertEquals(invCount, remote.getPostActivate());

      remote.remove();

   }

   /**
    * Tests that @CacheConfig.replicationIsPassivation=false works.
    */
   public void testIgnoreReplicationStatefulPassivation()
      throws Exception
   {
      log.info("+++ testIgnoreReplicationStatefulPassivation");

      // Connect to the server0 JNDI
      InitialContext ctx = getInitialContext(0);

      getLog().debug("==================================");
      getLog().debug(++PassivationUnitTestCase.test +"- "
              +"Looking up testIgnoreReplicationStateful...");
      StatefulRemote remote = (StatefulRemote) ctx.lookup("testIgnoreReplicationStateful/remote");

      remote.reset();
      remote.setState("hello");
      
      sleep_(11100);
      
      assertEquals("hello", remote.getState());      
      assertEquals("@PrePassivate count correct", 1, remote.getPrePassivate());
      assertEquals("@PostActivate count correct", 1, remote.getPostActivate());

      remote.remove();
   }
   
   /**
    * Tests that disabling replication via implementing Optimized works.
    */
   public void testOptimizedDisablesReplication()
      throws Exception
   {
      log.info("+++ testOptimizedDisablesReplication");

      // Connect to the server0 JNDI
      InitialContext ctx = getInitialContext(0);

      getLog().debug("==================================");
      getLog().debug(++PassivationUnitTestCase.test +"- "
              +"Looking up testOptimizedStateful...");
      StatefulRemote remote = (StatefulRemote) ctx.lookup("testOptimizedStateful/remote");

      int invCount = 0;
      remote.reset();
      invCount++;
      remote.setState("hello");
      invCount++;
      
      sleep_(10100);
      
      assertEquals("hello", remote.getState());
      invCount++;
      
      // Don't include the regular passivation in the count
      // since there won't be an event following replication
      assertEquals(invCount, remote.getPrePassivate());
      // don't increment count
      assertEquals(invCount, remote.getPostActivate());
      // don't increment count
      // Check that getPostActivate() didn't cause replication
      assertEquals(invCount, remote.getPrePassivate());
      
      remote.remove();
   }

   /** This is to test failover with passivation
    */
   public void testFailoverStatefulPassivation()
      throws Exception
   {
      log.info("+++ testFiloverStatefulPassivation");

      // Connect to the server0 JNDI
      InitialContext ctx = getInitialContext(0);

      getLog().debug("==================================");
      getLog().debug(++PassivationUnitTestCase.test +"- "
              +"Looking up testStateful...");
      StatefulRemote stateful = (StatefulRemote) ctx.lookup("testStateful/remote");

      stateful.reset();
      
      stateful.setName("The Code");
      NodeAnswer node1 = stateful.getNodeState();
      getLog ().debug ("Node 1 ID: " +node1);

      assertEquals("Counter: ", 1, stateful.increment());
      assertEquals("Counter: ", 2, stateful.increment());
      
      sleep_(10100);

      // Now we switch to the other node, simulating a failure on node 1
      stateful.setUpFailover("once");
      NodeAnswer node2 = stateful.getNodeState();
      assertNotNull("State node: ", node2);
      getLog ().debug ("Node 2 ID : " +node2);

      assertNotSame ("No failover has occured!", node1.nodeId, node2.nodeId);

      assertEquals ("Node 1: ", "The Code", node1.answer);
      assertEquals ("Node 2: ", "The Code", node2.answer);

      stateful.resetActivationCounter(); // This will activate the bean.
      int invCount = 1;
      
      sleep_(10100); // let it get passivated again.
      
      assertEquals("Counter: ", 3, stateful.increment());
      invCount++;
      assertEquals("Counter: ", 4, stateful.increment());
      invCount++;

      assertEquals(invCount, stateful.getPrePassivate());
      invCount++;
      assertEquals(invCount, stateful.getPostActivate());

      stateful.remove();
   }

   protected void sleep_(long msec)
   {
      try {
         Thread.sleep(msec);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }
}
