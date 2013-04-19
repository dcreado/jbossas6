/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
import org.jboss.test.cluster.ejb3.clusteredsession.islocal.VMTester;

/**
 * @author Brian Stansberry
 *
 */
public class StatefulProxyFactoryLoadBalancingTestCase extends JBossClusteredTestCase
{
   public static final String TESTER_JNDI_NAME = "ClusteredStatefulRemote";
   
   /**
    * Create a new StatefulProxyFactoryLoadBalancingTestCase.
    * 
    * @param name
    */
   public StatefulProxyFactoryLoadBalancingTestCase(String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(StatefulProxyFactoryLoadBalancingTestCase.class,
                           "clusteredsession-local.jar");
   }
   
   public void testFactoryProxyLoadBalancing() throws Exception
   {
      String[] jndiURLs = getNamingURLs();
      
      Properties env1 = new Properties();
      env1.setProperty(Context.PROVIDER_URL, jndiURLs[0]);
      env1.setProperty("jnp.disableDiscovery", "true");
      InitialContext ctx1 = new InitialContext(env1);
      VMTester tester1 = (VMTester) ctx1.lookup(TESTER_JNDI_NAME);
      
      VMID create1 = tester1.getCreatorVMID();
      // confirm that the bean didn't have to migrate
      assertEquals("bean targeted where created", create1, tester1.getVMID());
      
      // Get another bean from the same server; should target
      // a different server from the first
      VMTester tester2 = (VMTester) ctx1.lookup(TESTER_JNDI_NAME);
      
      VMID create2 = tester2.getCreatorVMID();
      assertEquals("2nd bean targeted where created", create2, tester2.getVMID());      
      assertFalse("creation of 2 beans load balanced", create1.equals(create2));
      
      // A third bean should come from the same as the first
      VMTester tester3 = (VMTester) ctx1.lookup(TESTER_JNDI_NAME);
      
      VMID create3 = tester3.getCreatorVMID();
      assertEquals("3rd bean targeted where created", create3, tester3.getVMID());
      assertEquals("creation of 3 beans load balanced", create1, create3);
      
      // Get the next proxy from the other server, but the overall 
      // bean creation should still be round robin
      
      Properties env2 = new Properties();
      env2.setProperty(Context.PROVIDER_URL, jndiURLs[1]);
      env2.setProperty("jnp.disableDiscovery", "true");
      InitialContext ctx2 = new InitialContext(env2);
      VMTester tester4 = (VMTester) ctx2.lookup(TESTER_JNDI_NAME);
      
      VMID create4 = tester4.getCreatorVMID();
      assertEquals("4th bean targeted where created", create4, tester4.getVMID());
      assertEquals("creation of 4 beans load balanced", create2, create4);
      
      // One last time
      VMTester tester5 = (VMTester) ctx2.lookup(TESTER_JNDI_NAME);
      
      VMID create5 = tester5.getCreatorVMID();
      assertEquals("5th bean targeted where created", create5, tester5.getVMID());
      assertEquals("creation of 5 beans load balanced", create1, create5);
   }

}
