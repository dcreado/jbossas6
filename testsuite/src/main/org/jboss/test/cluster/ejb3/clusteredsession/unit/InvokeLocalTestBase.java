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
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.test.JBossClusteredTestCase;
import org.jboss.test.cluster.ejb3.clusteredsession.islocal.VMTester;

/**
 * @author Brian Stansberry
 */
public abstract class InvokeLocalTestBase extends JBossClusteredTestCase
{
   public static final String TESTER_JNDI_NAME = "NonClusteredStatelessRemote";
   private static final String PROPERTIES_SERVICE = "jboss:type=Service,name=SystemProperties";
   private static final String PARTITION_NAME_PROPERTY = "clusteredsession.islocal.partition";
   
   
   public InvokeLocalTestBase(String name)
   {
      super(name);
   }

   
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      MBeanServerConnection[] adaptors = getAdaptors();
      setPartitionName(adaptors[0], "DefaultPartition");
      setPartitionName(adaptors[1], getPartitionName());
   }


   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      
      MBeanServerConnection[] adaptors = getAdaptors();
      clearPartitionName(adaptors[0]);
      clearPartitionName(adaptors[1]);
   }


   protected void stayLocalTest(String jndiName, boolean expectLocal, boolean expectLocalViaLookup)
      throws Exception
   {
      String[] jndiURLs = getNamingURLs();
      
      Properties env = new Properties();
      env.setProperty(Context.PROVIDER_URL, jndiURLs[0]);
      env.setProperty("jnp.disableDiscovery", "true");
      InitialContext ctx = new InitialContext(env);
      VMTester tester = (VMTester) ctx.lookup(TESTER_JNDI_NAME);
      
      VMID local = tester.getVMID();
      assertNotNull("Got the local VMID", local);
      
      Properties env1 = new Properties();
      env1.setProperty(Context.PROVIDER_URL, jndiURLs[1]);
      env1.setProperty("jnp.disableDiscovery", "true");
      ctx = new InitialContext(env1);
      VMTester remote = (VMTester) ctx.lookup(jndiName);
      
      // This call instantiates the SFSB if needed
      VMID remoteID = remote.getVMID();
      assertNotNull("Got the remote VMID", remoteID);
      
      // Pass the proxy back to the server and invoke getVMID() on it
      VMID passThroughID = tester.getVMIDFromRemote(remote);
      assertNotNull("Got the remote VMID", passThroughID);
      
      if (expectLocal)
         assertEquals("Call stayed local", local, passThroughID);
      else
         assertFalse("Call went remote", local.equals(passThroughID));
      
      // Tell the server to look up a proxy from node1 and invoke getVMID() on it
      passThroughID = tester.getVMIDFromRemoteLookup(jndiURLs[1], jndiName);
      assertNotNull("Got the remote VMID", passThroughID);
      
      if (expectLocalViaLookup)
         assertEquals("Call stayed local", local, passThroughID);
      else
         assertFalse("Call went remote", local.equals(passThroughID));
   }
   
   protected abstract String getPartitionName();
   
   private void setPartitionName(MBeanServerConnection adaptor, String partitionName) throws Exception
   {
      Object[] args = { PARTITION_NAME_PROPERTY, partitionName };
      String[] sig = { String.class.getName(), String.class.getName() };
      adaptor.invoke(new ObjectName(PROPERTIES_SERVICE), "set", args, sig);
   }
   
   private void clearPartitionName(MBeanServerConnection adaptor) throws Exception
   {
      setPartitionName(adaptor, "DefaultPartition");
      Object[] args = { PARTITION_NAME_PROPERTY };
      String[] sig = { String.class.getName() };
      adaptor.invoke(new ObjectName(PROPERTIES_SERVICE), "remove", args, sig);
   }
}
