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

import java.lang.reflect.Method;
import java.util.Set;

import javax.enterprise.inject.spi.BeanManager;

import junit.framework.Test;

import org.jboss.beans.metadata.plugins.AbstractBeanMetaData;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.dependency.spi.DependencyItem;
import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.kernel.weld.plugins.dependency.WeldKernelControllerContext;
import org.jboss.test.deployers.support.mcandweld.mc.fromweld.McBeanWithInjectedWeldBean;
import org.jboss.test.deployers.support.mcandweld.mc.simple.SimpleBean;
import org.jboss.test.deployers.support.mcandweld.weld.frommc.WeldBeanWithInjectedMcBean;
import org.jboss.test.deployers.support.mcandweld.weld.simple.SimpleWebBean;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.deployer.env.FlatDeployment;

/**
 * Test weld and mc injection with the weld bootstrap bean set up properly 
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class McIntegrationTestCase extends AbstractWeldTest
{
   public McIntegrationTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(McIntegrationTestCase.class);
   }

   public void testMcDeployment() throws Exception
   {
      VirtualFile top = VFS.getChild("top-level.ear"); 
      createAssembledDirectory(top)
         .addPath("/weld/mcandweld/ear/mconly");
      createMcLib(top, "/weld/mcandweld/mc/simple", SimpleBean.class);
      
      Deployment deployment = createVFSDeployment(top);
      DeployerClient mainDeployer = getDeployerClient();
      mainDeployer.addDeployment(deployment);
      mainDeployer.process();
      try
      {
         Object o = getBean("SimpleBean");
         assertNotNull(o);
      }
      finally
      {
         mainDeployer.removeDeployment(deployment);
         mainDeployer.process();
      }
   }
   
   public void testWeldDeployment() throws Exception
   {
      VirtualFile top = VFS.getChild("top-level.ear"); 
      createAssembledDirectory(top)
         .addPath("/weld/mcandweld/ear/weldonly");
      createWeldLib(top, "/weld/mcandweld/weld", SimpleWebBean.class);
      
      testBootstrap(top, new RunSpecificTest()
      {
         public void runTest(BeanManager manager, DeploymentUnit unit) throws Exception
         {
            assertWebBean(manager, unit, SimpleWebBean.class.getName());
         }
      });
   }

   public void testWeldAndMcDeployment() throws Exception
   {
      VirtualFile top = VFS.getChild("top-level.ear"); 
      createAssembledDirectory(top)
         .addPath("/weld/mcandweld/ear/weldandmc");
      createMcLib(top, "/weld/mcandweld/mc/simple", SimpleBean.class);
      createWeldLib(top, "/weld/mcandweld/weld", SimpleWebBean.class);

      testBootstrap(top, new RunSpecificTest()
      {
         public void runTest(BeanManager manager, DeploymentUnit unit) throws Exception
         {
            assertNotNull(getControllerContext("SimpleBean"));
            Object o = getBean("SimpleBean");
            assertNotNull(o);
            assertWebBean(manager, unit, SimpleWebBean.class.getName());
            assertNoWebBean(manager, unit, SimpleBean.class.getName());
         }
      });
      assertNoControllerContext("SimpleBean", null);
   }
   
   public void testMcInjectedByWeld() throws Exception
   {
      VirtualFile top = VFS.getChild("top-level.ear"); 
      createAssembledDirectory(top)
         .addPath("/weld/mcandweld/ear/weldandmc");
      createMcLib(top, "/weld/mcandweld/mc/fromweld", McBeanWithInjectedWeldBean.class);
      createWeldLib(top, "/weld/mcandweld/weld", SimpleWebBean.class);

      testBootstrap(top, new RunSpecificTest()
      {
         public void runTest(BeanManager manager, DeploymentUnit unit) throws Exception
         {
            Object mc = getBean("McBean");
            assertNotNull(mc);
            Object weldBean = assertWebBean(manager, unit, SimpleWebBean.class.getName());
            assertNoWebBean(manager, unit, McBeanWithInjectedWeldBean.class.getName());
            
            Method m = mc.getClass().getMethod("getSimpleWebBean");
            Object injectedWeldBean = m.invoke(mc);
            assertNotNull(injectedWeldBean);
            assertSame(weldBean.getClass(), injectedWeldBean.getClass());
         }
      });
   }

   public void testWeldInjectedByMc() throws Exception
   {
      VirtualFile top = VFS.getChild("top-level.ear"); 
      createAssembledDirectory(top)
         .addPath("/weld/mcandweld/ear/weldandmc");
      createMcLib(top, "/weld/mcandweld/mc/simple", SimpleBean.class);
      createWeldLib(top, "/weld/mcandweld/weld", WeldBeanWithInjectedMcBean.class);
      
      testBootstrap(top, new RunSpecificTest()
      {
         public void runTest(BeanManager manager, DeploymentUnit unit) throws Exception
         {
            Object mc = getBean("SimpleBean");
            assertNotNull(mc);
            Object weldBean = assertWebBean(manager, unit, WeldBeanWithInjectedMcBean.class.getName());
            assertSame(mc, assertWebBean(manager, unit, mc));

            Method m = weldBean.getClass().getMethod("getSimpleBean");
            Object injectedBean = m.invoke(weldBean);
            assertNotNull(injectedBean);
            assertSame(mc, injectedBean);
         }
      });
   }

   public void testMcInjectedIntoWeldInjectedIntoMc() throws Exception
   {
      VirtualFile top = VFS.getChild("top-level.ear"); 
      createAssembledDirectory(top)
         .addPath("/weld/mcandweld/bidirectional");
      createMcLib(top, "/weld/mcandweld/bidirectional/mc/end", org.jboss.test.deployers.support.mcandweld.bidirectional.mcend.end.First.class);
      createWeldLib(top, "/weld/mcandweld/weld", org.jboss.test.deployers.support.mcandweld.bidirectional.mcend.middle.Middle.class);
      
      testBootstrap(top, new RunSpecificTest()
      {
         public void runTest(BeanManager manager, DeploymentUnit unit) throws Exception
         {
            Object first = getBean("First");
            assertNotNull(first);
            Object middle = assertWebBean(manager, unit, org.jboss.test.deployers.support.mcandweld.bidirectional.mcend.middle.Middle.class.getName());
            Object last = getBean("Last");

            Method getLast = middle.getClass().getMethod("getLast");
            Object injectedLast = getLast.invoke(middle);
            assertNotNull(injectedLast);
            assertSame(last, injectedLast);
            assertSame(last, assertWebBean(manager, unit, last));
            
            Method getMiddle = first.getClass().getMethod("getMiddle");
            Object injectedMiddle = getMiddle.invoke(first);
            assertNotNull(injectedMiddle);

            assertSame(first, assertWebBean(manager, unit, first));
         }
      });
   }
   
   public void testWeldInjectedIntoMcInjectedIntoWeld() throws Exception
   {
      VirtualFile top = VFS.getChild("top-level.ear"); 
      createAssembledDirectory(top)
         .addPath("/weld/mcandweld/bidirectional");
      createMcLib(top, "/weld/mcandweld/bidirectional/mc/middle", org.jboss.test.deployers.support.mcandweld.bidirectional.mcmiddle.middle.Middle.class);
      createWeldLib(top, "/weld/mcandweld/weld", org.jboss.test.deployers.support.mcandweld.bidirectional.mcmiddle.end.First.class);
      
      testBootstrap(top, new RunSpecificTest()
      {
         public void runTest(BeanManager manager, DeploymentUnit unit) throws Exception
         {
            Object first = assertWebBean(manager, unit, org.jboss.test.deployers.support.mcandweld.bidirectional.mcmiddle.end.First.class.getName());
            assertNotNull(first);
            Object middle = getBean("Middle");
            assertNotNull(middle);
            assertWebBean(manager, unit, org.jboss.test.deployers.support.mcandweld.bidirectional.mcmiddle.end.Last.class.getName());

            Method getLast = middle.getClass().getMethod("getLast");
            Object injectedLast = getLast.invoke(middle);
            assertNotNull(injectedLast);
            
            Method getMiddle = first.getClass().getMethod("getMiddle");
            Object injectedMiddle = getMiddle.invoke(first);
            assertNotNull(injectedMiddle);
            assertSame(middle, injectedMiddle);
            assertSame(middle, assertWebBean(manager, unit, middle));
         }
      });
   }
 
   public void testExternalMcBeanInjectedIntoWeld() throws Exception
   {
      VirtualFile mcEar = VFS.getChild("mc.ear");
      createAssembledDirectory(mcEar)
         .addPath("/weld/mcandweld/ear/mconly");
      createMcLib(mcEar, "/weld/mcandweld/mc/simple", SimpleBean.class);
      Deployment mc = deploy(mcEar);
      
      try
      {
         VirtualFile weldEar = VFS.getChild("weld.ear");
         createAssembledDirectory(weldEar)
            .addPath("/weld/mcandweld/ear/weldonly");
         createWeldLib(weldEar, "/weld/mcandweld/weld", WeldBeanWithInjectedMcBean.class);
         
         testBootstrap(weldEar, new RunSpecificTest()
         {
            public void runTest(BeanManager manager, DeploymentUnit unit) throws Exception
            {
               Object mc = getBean("SimpleBean");
               assertNotNull(mc);
               Object weldBean = assertWebBean(manager, unit, WeldBeanWithInjectedMcBean.class.getName());
               assertSame(mc, assertWebBean(manager, unit, mc));

               Method m = weldBean.getClass().getMethod("getSimpleBean");
               Object injectedBean = m.invoke(weldBean);
               assertNotNull(injectedBean);
               assertSame(mc, injectedBean);
            }
         });
      }
      finally
      {
         undeploy(mc);
      }
   }
   
   public void testNonExistantMcBeanInjectedIntoWeldFails() throws Exception
   {
      VirtualFile weldEar = VFS.getChild("weld.ear");
      createAssembledDirectory(weldEar)
         .addPath("/weld/mcandweld/ear/weldonly");
      createWeldLib(weldEar, "/weld/mcandweld/weld", WeldBeanWithInjectedMcBean.class);
      
      try
      {
         testBootstrap(weldEar, null);
         fail("Weld deployment should not have worked without mc deployment");
      }
      catch(Exception expected)
      {
      }
   }
   
   public void testUndeployedMcBeanInjectedIntoWeldFails() throws Exception
   {
      VirtualFile mcEar = VFS.getChild("mc.ear");
      createAssembledDirectory(mcEar)
         .addPath("/weld/mcandweld/ear/mconly");
      createMcLib(mcEar, "/weld/mcandweld/mc/simple", SimpleBean.class);
      Deployment mc = deploy(mcEar);
      undeploy(mc);
      
      VirtualFile weldEar = VFS.getChild("weld.ear");
      createAssembledDirectory(weldEar)
         .addPath("/weld/mcandweld/ear/weldonly");
      createWeldLib(weldEar, "/weld/mcandweld/weld", WeldBeanWithInjectedMcBean.class);
      
      try
      {
         testBootstrap(weldEar, null);
         fail("Weld deployment should not have worked without mc deployment");
      }
      catch(Exception expected)
      {
      }
   }

   public void testExternalMcBeanInjectedIntoWeldInitiallyNotPresentThenDeployed() throws Exception
   {
      VirtualFile weldEar = VFS.getChild("weld.ear");
      createAssembledDirectory(weldEar)
         .addPath("/weld/mcandweld/ear/weldonly");
      createWeldLib(weldEar, "/weld/mcandweld/weld", WeldBeanWithInjectedMcBean.class);
      
      try
      {
         testBootstrap(weldEar, null);
         fail("Weld deployment should not have worked without mc deployment");
      }
      catch(Exception expected)
      {
      }
      
      VirtualFile mcEar = VFS.getChild("mc.ear");
      createAssembledDirectory(mcEar)
         .addPath("/weld/mcandweld/ear/mconly");
      createMcLib(mcEar, "/weld/mcandweld/mc/simple", SimpleBean.class);
      Deployment mc = deploy(mcEar);
      try
      {
         testBootstrap(weldEar, new RunSpecificTest()
         {
            public void runTest(BeanManager manager, DeploymentUnit unit) throws Exception
            {
               Object mc = getBean("SimpleBean");
               assertNotNull(mc);
               Object weldBean = assertWebBean(manager, unit, WeldBeanWithInjectedMcBean.class.getName());
               assertSame(mc, assertWebBean(manager, unit, mc));

               Method m = weldBean.getClass().getMethod("getSimpleBean");
               Object injectedBean = m.invoke(weldBean);
               assertNotNull(injectedBean);
               assertSame(mc, injectedBean);
            }
         });
      }
      finally
      {
         undeploy(mc);
      }
   }
   
   public void testExternalMcBeanInjectedIntoWeldInitiallyUndeployedThenDeployed() throws Exception
   {
      VirtualFile mcEar = VFS.getChild("mc.ear");
      createAssembledDirectory(mcEar)
         .addPath("/weld/mcandweld/ear/mconly");
      createMcLib(mcEar, "/weld/mcandweld/mc/simple", SimpleBean.class);
      Deployment mc = deploy(mcEar);
      
      undeploy(mc);
      
      VirtualFile weldEar = VFS.getChild("weld.ear");
      createAssembledDirectory(weldEar)
         .addPath("/weld/mcandweld/ear/weldonly");
      createWeldLib(weldEar, "/weld/mcandweld/weld", WeldBeanWithInjectedMcBean.class);
      
      try
      {
         testBootstrap(weldEar, null);
         fail("Weld deployment should not have worked without mc deployment");
      }
      catch(Exception expected)
      {
      }
      
      mc = deploy(mcEar);
      try
      {
         testBootstrap(weldEar, new RunSpecificTest()
         {
            public void runTest(BeanManager manager, DeploymentUnit unit) throws Exception
            {
               Object mc = getBean("SimpleBean");
               assertNotNull(mc);
               Object weldBean = assertWebBean(manager, unit, WeldBeanWithInjectedMcBean.class.getName());
               assertSame(mc, assertWebBean(manager, unit, mc));

               Method m = weldBean.getClass().getMethod("getSimpleBean");
               Object injectedBean = m.invoke(weldBean);
               assertNotNull(injectedBean);
               assertSame(mc, injectedBean);
            }
         });
      }
      finally
      {
         undeploy(mc);
      }
   }
   
   protected void testBootstrap(VirtualFile ear, RunSpecificTest test) throws Exception
   {
      enableTrace("org.jboss.dependency.plugins.");
      Deployment deployment = deploy(ear);
      DeploymentUnit earDU = null;
      try
      {
         earDU = getMainDeployerStructure().getDeploymentUnit(deployment.getName());

         //Check that the flat deployment bean has been started
         FlatDeployment flatDeployment = (FlatDeployment)getBean(DeployersUtils.getDeploymentBeanName(earDU));
         assertNotNull(flatDeployment);

         //Check the bootstrap bean has been installed
         assertNotNull(getControllerContext(DeployersUtils.getBootstrapBeanName(earDU)));

         BeanManager manager = getBeanManager(earDU);
         
         if (test != null)
            test.runTest(manager, earDU);
      }
      finally
      {
         getDeployerClient().removeDeployment(deployment);
         getDeployerClient().process();
      }
   }

   protected Deployment deploy(VirtualFile ear) throws Exception
   {
      Deployment deployment = createVFSDeployment(ear);
      
      DeployerClient mainDeployer = getDeployerClient();
      mainDeployer.addDeployment(deployment);
      mainDeployer.process();
      return deployment;
   }
   
   protected void undeploy(Deployment deployment) throws Exception
   {
      DeployerClient mainDeployer = getDeployerClient();
      mainDeployer.removeDeployment(deployment);
      mainDeployer.process();
      
   }

   interface RunSpecificTest
   {
      void runTest(BeanManager manager, DeploymentUnit unit) throws Exception;
   }   

}
