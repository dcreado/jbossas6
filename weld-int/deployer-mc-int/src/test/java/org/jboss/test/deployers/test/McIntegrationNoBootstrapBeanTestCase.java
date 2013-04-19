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
package org.jboss.test.deployers.test;

import junit.framework.Test;

import org.jboss.dependency.spi.ControllerContext;
import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.test.deployers.support.nobootstrap.mc.SimpleBeanWithSimpleWebBean;
import org.jboss.test.deployers.support.nobootstrap.weld.SimpleWebBean;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.deployer.env.FlatDeployment;

/**
 * Test boot deployer.
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class McIntegrationNoBootstrapBeanTestCase extends AbstractWeldTest
{
   public McIntegrationNoBootstrapBeanTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(McIntegrationNoBootstrapBeanTestCase.class);
   }
   
   public void testUninstallOfIntermediateBeanIfBeanManagerWasNotInstalled() throws Throwable
   {
      DeployerClient mainDeployer = getDeployerClient();
      VirtualFile ear = VFS.getChild("top-level.ear");
      createAssembledDirectory(ear)
         .addPath("/weld/mcandweld/ear/weldandmc");
      createMcLib(ear, "/weld/mcandweld/mc/nobootstrap", SimpleBeanWithSimpleWebBean.class);
      createWeldLib(ear, "/weld/mcandweld/weld", SimpleWebBean.class);
      
      Deployment deployment = createVFSDeployment(ear);
      String installerName = deployment.getName() + "BootstrapBeanInstaller=SimpleBean";

      mainDeployer.addDeployment(deployment);
      mainDeployer.process();
      try
      {
         DeploymentUnit earDU = getMainDeployerStructure().getDeploymentUnit(deployment.getName());

         //Check that the flat deployment bean has been started
         FlatDeployment flatDeployment = (FlatDeployment)getBean(DeployersUtils.getDeploymentBeanName(earDU));
         assertNotNull(flatDeployment);

         //Check that the simple bean installer has been created
         ControllerContext ctx = getControllerContext(installerName,  null);
         assertNotNull(ctx);
         //Check the simple bean itself has not been created
         assertNoControllerContext("SimpleBean", null);
      }
      finally
      {
         getLog().debug("============== Undeploying everything");
         mainDeployer.removeDeployment(deployment);
         mainDeployer.process();
      }
      assertNoControllerContext(installerName, null);
      assertNoControllerContext("SimpleBean", null);
   }
   


}