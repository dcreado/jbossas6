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

import java.lang.reflect.Field;
import java.util.Map;

import javax.naming.Context;

import junit.framework.Test;
import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.beans.metadata.plugins.AbstractBeanMetaData;
import org.jboss.dependency.plugins.AbstractController;
import org.jboss.dependency.plugins.tracker.AbstractContextRegistry;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.kernel.plugins.bootstrap.basic.KernelConstants;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.deployers.support.deployer.CheckableJndiBinder;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.util.JndiUtils;

/**
 * Test boot deployer.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class BootDeployerTestCase extends AbstractWeldTest
{
   private KernelController controller;

   public BootDeployerTestCase(String name)
   {
      super(name);
      setAutowireCandidate(true);
   }

   public static Test suite()
   {
      return suite(BootDeployerTestCase.class);
   }

   @Inject(bean = KernelConstants.KERNEL_CONTROLLER_NAME)
   public void setController(KernelController controller)
   {
      this.controller = controller;
   }

   protected void testBootstrap(VirtualFile ear) throws Exception
   {
      // should already be on the deployer
      Context bmContext = assertInstanceOf(
            CheckableJndiBinder.ROOT.lookup(JndiUtils.BEAN_MANAGER_GLOBAL_SUBCONTEXT),
            Context.class,
            false
      );

      Object bootstrap;
      String duSimpleName = null;
      Deployment deployment = createVFSDeployment(ear);
      DeployerClient mainDeployer = getDeployerClient();
      mainDeployer.addDeployment(deployment);
      mainDeployer.process();
      ControllerContext wbContext = null;
      try
      {
         DeploymentUnit earDU = getMainDeployerStructure().getDeploymentUnit(deployment.getName());
         String bootName = DeployersUtils.getBootstrapBeanName(earDU);
         bootstrap = getBean(bootName, null);
         assertInstanceOf(bootstrap, "org.jboss.test.deployers.support.CheckableBootstrap", earDU.getClassLoader());
         // waiting on ejb
         assertTrue(invoke(bootstrap, "Create"));
         assertFalse(invoke(bootstrap, "Boot")); // not yet booted
         assertFalse(invoke(bootstrap, "Shutdown"));
         // install ejb
         KernelControllerContext ejb = deploy(new AbstractBeanMetaData("EjbContainer#1", Object.class.getName()));
         try
         {
            assertTrue(ejb.getState().equals(ControllerState.INSTALLED));
            /// check boot
            assertTrue(invoke(bootstrap, "Create"));
            assertTrue(invoke(bootstrap, "Boot"));
            assertFalse(invoke(bootstrap, "Shutdown"));

            // test jndi binding
            duSimpleName = earDU.getSimpleName();
            Context context = assertInstanceOf(bmContext.lookup(duSimpleName), Context.class, false);
            assertSame("Bootstrap Dummy", context.lookup("bootstrap"));

            Class<?> wbClass = earDU.getClassLoader().loadClass("org.jboss.test.deployers.support.CheckableBootstrap");
            wbContext = controller.getContextByClass(wbClass);
            assertNotNull(wbContext);
            assertSame(bootstrap, wbContext.getTarget());
         }
         finally
         {
            undeploy(ejb);
         }
      }
      finally
      {
         mainDeployer.removeDeployment(deployment);
         mainDeployer.process();

         // clear binding
         assertNull(bmContext.lookup(duSimpleName));

         // clear context 2 class mapping
         if (wbContext != null)
            assertNull(wbContext.getTarget());

         Field registryField = AbstractController.class.getDeclaredField("registry");
         registryField.setAccessible(true);
         Object registry = registryField.get(controller);
         Field mapField = AbstractContextRegistry.class.getDeclaredField("contextsByClass");
         mapField.setAccessible(true);
         @SuppressWarnings({"unchecked"})
         Map<Class<?>, ?> map = (Map) mapField.get(registry);
         for (Class<?> clazz : map.keySet())
         {
            String className = clazz.getName();
            // should be the only one -- no other service/bean should use it -- except if we leak
            if ("org.jboss.test.deployers.support.CheckableBootstrap".equals(className))
            {

               Object value = map.get(clazz);
               fail("" + value);
            }
         }
      }
      assertTrue(invoke(bootstrap, "Shutdown"));
   }

   public void testEar() throws Exception
   {
      testBootstrap(createBasicEar());
   }

   public void testWarInEar() throws Exception
   {
      testBootstrap(createWarInEar());
   }
}
