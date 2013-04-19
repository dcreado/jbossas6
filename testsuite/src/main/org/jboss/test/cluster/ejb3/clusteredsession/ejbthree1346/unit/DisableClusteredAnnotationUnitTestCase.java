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

package org.jboss.test.cluster.ejb3.clusteredsession.ejbthree1346.unit;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

import junit.framework.Test;

import org.jboss.test.JBossClusteredTestCase;
import org.jboss.test.cluster.ejb3.clusteredsession.NodeAnswer;
import org.jboss.test.cluster.ejb3.clusteredsession.ejbthree1346.DisableClusteredAnnotationRemote;

/**
 * Tests the @Clustered beans with <clustered>false</clustered> in jboss.xml
 * do not exhibit clustering behavior.
 *
 * @author  Brian Stansberry
 * @version $Revision: 83582 $
 */
public class DisableClusteredAnnotationUnitTestCase extends JBossClusteredTestCase
{
   public DisableClusteredAnnotationUnitTestCase (String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      final String jarName = "ejbthree1346.jar";
      Test t1 = JBossClusteredTestCase.getDeploySetup(DisableClusteredAnnotationUnitTestCase.class,
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

   /** Validate the stateful bean is not clustered by having failover not work */
   public void testStatefulBean() throws Exception
   {
      // Connect to the server0 JNDI
      InitialContext ctx = getInitialContext(0);

      DisableClusteredAnnotationRemote stateful = null;      
      try
      {
         stateful = (DisableClusteredAnnotationRemote) ctx.lookup("DisableClusteredAnnotationStateful/remote");
      }
      catch (NameNotFoundException nnfe)
      {
         fail(nnfe.getMessage());
      }

      NodeAnswer node1 = stateful.getNodeState ();
      getLog ().debug ("Node 1 ID: " +node1);

      // Now we switch to the other node, simulating a failure on node 1
      //
      stateful.setUpFailover("once");
      try
      {
         stateful.getNodeState ();
         fail("GenericClusteringException did not propagate");
      }
      catch (Exception good) {}
   }

   /** Test stateless bean by demonstrating no load balancing */
   public void testStatelessBean() throws Exception
   {
      InitialContext ctx = getInitialContext(0);

      DisableClusteredAnnotationRemote stateless = null;
      
      try
      {
         stateless = (DisableClusteredAnnotationRemote) ctx.lookup("DisableClusteredAnnotationStateless/remote");
      }
      catch (NameNotFoundException nnfe)
      {
         fail(nnfe.getMessage());
      }
      
      NodeAnswer node1 = stateless.getNodeState();
      assertNotNull(node1);
      getLog ().debug ("Node 1 ID: " +node1);
      
      for (int i = 0; i < 20; i++)
      {
         assertEquals(node1, stateless.getNodeState());
      }
   }

   @Override
   public void testServerFound() throws Exception
   {      
      // The superclass throws an exception, but we want this
      // to be a failure, not an error
      try
      {
         super.testServerFound();
      }
      catch (Exception e)
      {
         // Use assertNull to get the stack trace in the test report
         assertNull("Deployment had no exceptions", e);
      }
   }
   
   
}
