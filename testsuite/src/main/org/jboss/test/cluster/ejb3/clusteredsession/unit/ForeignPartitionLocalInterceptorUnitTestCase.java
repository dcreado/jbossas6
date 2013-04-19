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

import javax.management.MBeanServerConnection;

import junit.framework.Test;

/**
 * @author Brian Stansberry
 */
public class ForeignPartitionLocalInterceptorUnitTestCase extends InvokeLocalTestBase
{
   private static final String PARTITION_NAME_VALUE = "Ejb3IsLocalTestPartition";
   
   private static boolean deployed0 = false;
   private static boolean deployed1 = false;
   
   /**
    * @param name
    */
   public ForeignPartitionLocalInterceptorUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(ForeignPartitionLocalInterceptorUnitTestCase.class,
                           "testlocal-jboss-beans.xml");
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      cleanDeployments();
      
      deployJars();
   }

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      
      cleanDeployments();      
   }
   
   @Override
   protected String getPartitionName()
   {
      return PARTITION_NAME_VALUE;
   }
   
   public void testClusteredStatefulGoesRemote() throws Exception
   {
      stayLocalTest("ClusteredStatefulRemote", false, false);
   }
   
   public void testClusteredStatelessGoesRemote() throws Exception
   {
      stayLocalTest("ClusteredStatelessRemote", false, false);      
   }
   
   public void testNonClusteredStatefulGoesRemote() throws Exception
   {
      stayLocalTest("NonClusteredStatefulRemote", false, false);      
   }
   
   public void testNonClusteredStatelessGoesRemote() throws Exception
   {
      stayLocalTest("NonClusteredStatelessRemote", false, false);    
   }

   private void cleanDeployments() throws Exception
   {
      MBeanServerConnection[] adaptors = getAdaptors();
      
      if (deployed0)
      {
         undeploy(adaptors[0], "clusteredsession-local.jar");
         deployed0 = false;
      }
      
      if (deployed1)
      {
         undeploy(adaptors[1], "clusteredsession-local.jar");
         deployed1 = false;
      }
   }
   
   private void deployJars() throws Exception
   {
      MBeanServerConnection[] adaptors = getAdaptors();
      
      deploy(adaptors[0], "clusteredsession-local.jar");
      deployed0 = true;
      
      
      deploy(adaptors[1], "clusteredsession-local.jar");
      deployed1 = true;      
   }
   
   

}
