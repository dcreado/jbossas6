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
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.test.deployers.support.jar.PlainJavaBean;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.deployer.DeployersUtils;

/**
 * Smoke tests.
 * Test non WB deployments work OK.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class SmokeTestCase extends AbstractWeldTest
{
   public SmokeTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(SmokeTestCase.class);
   }

   protected void testDeployment(VirtualFile app) throws Exception
   {
      Deployment deployment = createVFSDeployment(app);

      DeployerClient mainDeployer = getDeployerClient();
      mainDeployer.addDeployment(deployment);
      mainDeployer.process();
      try
      {
         DeploymentUnit du = getMainDeployerStructure().getDeploymentUnit(deployment.getName());
         ControllerContext context = du.getAttachment(ControllerContext.class);
         assertNotNull(context);
         assertEquals(ControllerState.INSTALLED, context.getState());

         // There should be no WB bootstrap bean
         assertNoBean(DeployersUtils.getBootstrapBeanName(du), null);
      }
      finally
      {
         mainDeployer.removeDeployment(deployment);
         mainDeployer.process();
      }
   }

   public void testEar() throws Exception
   {
      VirtualFile ear = VFS.getChild("jar-in-ear.ear");
      createAssembledDirectory(ear)
         .addPath("/weld/jarwarinear")
         .addPackage("simple.jar", PlainJavaBean.class)
         .addPackage("simple.war/WEB-INF/classes", PlainJavaBean.class)
         .addPath("simple.war", "/weld/warwowb/web");

      testDeployment(ear);
   }

   public void testWar() throws Exception
   {
      VirtualFile war = VFS.getChild("w1.war");
      createAssembledDirectory(war)
         .addPackage("WEB-INF/classes", PlainJavaBean.class)
         .addPath("/weld/warwowb/web");
      testDeployment(war);
   }

   public void testJar() throws Exception
   {
      VirtualFile jar = VFS.getChild("j1.jar");
      createAssembledDirectory(jar)
         .addPackage(PlainJavaBean.class);

      testDeployment(jar);
   }
}