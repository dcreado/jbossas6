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

import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.Test;

import org.jboss.ha.framework.interfaces.ClusteringTargetsRepository;
import org.jboss.ha.framework.interfaces.FamilyClusterInfo;
import org.jboss.test.JBossClusteredTestCase;
import org.jboss.test.cluster.ejb3.clusteredsession.ClusteredStatelessRemote;
import org.jboss.test.cluster.ejb3.clusteredsession.StatefulRemote;

/**
 * Tests that the list of remote targets in downloaded clustered bean
 * proxies are correct.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 108925 $
 */
public class ProxyTargetsUnitTestCase extends JBossClusteredTestCase
{
   private static boolean deployed0 = true;
   private static boolean deployed1 = true;
   
   private static final String NAME_BASE= "jboss.j2ee:jar=";   
   private static final String deployment = "clusteredsession-test.jar";
   private static final String BEAN_PREFIX = ",name=";
   private static final String SLSB = "clusteredStateless";
   private static final String SFSB = "testStateful";
   private static final String NAME_QUALIFIER = ",service=EJB3socketDefaultPartition";
   
   /**
    * Create a new RedeployUnitTestCase.
    * 
    * @param name
    */
   public ProxyTargetsUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(ProxyTargetsUnitTestCase.class, deployment);
   }
   
   private static String getFamilyName(String beanName)
   {
      return NAME_BASE + deployment + BEAN_PREFIX + beanName + NAME_QUALIFIER;
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

   private InitialContext getInitialContext(int node) throws Exception {
      // Connect to the serverX JNDI
      String[] urls = getNamingURLs();
      Properties env1 = new Properties();
      env1.setProperty(Context.INITIAL_CONTEXT_FACTORY,
         "org.jnp.interfaces.NamingContextFactory");
      env1.setProperty(Context.PROVIDER_URL, urls[node]);
      return new InitialContext(env1);
   }
   
   public void testBeanProxyTargets() 
   throws Exception
   {  
      getLog().debug("testBeanProxyTargets");
      getLog().debug("================================================");
      
      getLog().debug("Checking ClusteredStatelessRemote on node 0");
      ClusteredStatelessRemote csr = (ClusteredStatelessRemote) getInitialContext(0).lookup("clusteredStateless/remote");
      confirmTargetCount(getFamilyName(SLSB), 2);
      
      getLog().debug("Checking ClusteredStatelessRemote on node 1");
      csr = (ClusteredStatelessRemote) getInitialContext(1).lookup("clusteredStateless/remote");
      confirmTargetCount(getFamilyName(SLSB), 2);
      
      getLog().debug("Checking StatefulRemote on node 0");
      StatefulRemote sr = (StatefulRemote) getInitialContext(0).lookup("testStateful/remote");
      confirmTargetCount(getFamilyName(SFSB), 2);
      
      getLog().debug("Checking StatefulRemote on node 1");
      sr = (StatefulRemote) getInitialContext(1).lookup("testStateful/remote");
      confirmTargetCount(getFamilyName(SFSB), 2);

      // Undeploy on node 1
      undeploy();

      getLog().debug("Checking ClusteredStatelessRemote on node 0 after undeploy");
      csr = (ClusteredStatelessRemote) getInitialContext(0).lookup("clusteredStateless/remote");
      confirmTargetCount(getFamilyName(SLSB), 1);
      
      getLog().debug("Checking StatefulRemote on node 0 after undeploy");
      sr = (StatefulRemote) getInitialContext(0).lookup("testStateful/remote");
      confirmTargetCount(getFamilyName(SFSB), 1);
      
      // Redeploy on node1
      deploy();
      
      getLog().debug("Checking ClusteredStatelessRemote on node 0 after redeploy");
      csr = (ClusteredStatelessRemote) getInitialContext(0).lookup("clusteredStateless/remote");
      confirmTargetCount(getFamilyName(SLSB), 2);
      
      getLog().debug("Checking ClusteredStatelessRemote on node 1 after redeploy");
      csr = (ClusteredStatelessRemote) getInitialContext(1).lookup("clusteredStateless/remote");
      confirmTargetCount(getFamilyName(SLSB), 2);
      
      getLog().debug("Checking StatefulRemote on node 0 after redeploy");
      sr = (StatefulRemote) getInitialContext(0).lookup("testStateful/remote");
      confirmTargetCount(getFamilyName(SFSB), 2);
      
      getLog().debug("Checking StatefulRemote on node 1 after redeploy");
      sr = (StatefulRemote) getInitialContext(1).lookup("testStateful/remote");
      confirmTargetCount(getFamilyName(SFSB), 2);
      
      getLog().debug("ok");
   }
   
   private void confirmTargetCount(String familyName, int expectedCount)
   {
      FamilyClusterInfo info = ClusteringTargetsRepository.getFamilyClusterInfo(familyName);
      assertNotNull("FamilyClusterInfo exists for " + familyName, info);
      assertEquals("FamilyClusterInfo for " + familyName + " has correct target count",
                   expectedCount, info.getTargets().size());
   }
   
   private void undeploy() throws Exception
   {
      MBeanServerConnection[] adaptors = getAdaptors();
      
      undeploy(adaptors[1], deployment);
      getLog().debug("Undeployed " + deployment + " on server1");
      deployed1 = false;
      
      sleep(5000);
   }
   
   private void deploy() throws Exception
   {
      MBeanServerConnection[] adaptors = getAdaptors();
      
      deploy(adaptors[1], deployment);
      getLog().debug("Deployed " + deployment + " on server1");
      deployed1 = true;
      
      sleep(5000);
   }
   
   

}
