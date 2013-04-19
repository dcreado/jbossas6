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

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.classloader.plugins.jdk.AbstractJDKChecker;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.DependencyInfo;
import org.jboss.dependency.spi.DependencyItem;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.deployer.kernel.BeanMetaDataFactoryVisitor;
import org.jboss.test.deployers.BootstrapDeployersTest;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.deployer.env.FlatDeployment;

/**
 * AbstractWeldTest.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractWeldTest extends BootstrapDeployersTest
{
   protected AbstractWeldTest(String name)
   {
      super(name);
   }

   @Override
   protected void setUp() throws Exception
   {
      // excluding class that knows hot to load from system classloader
      Set<Class<?>> excluded = AbstractJDKChecker.getExcluded();
      excluded.add(BeanMetaDataFactoryVisitor.class);

      super.setUp();
   }

   protected void assertInstanceOf(Object target, String className, ClassLoader cl) throws Exception
   {
      Class<?> clazz = cl.loadClass(className);
      assertTrue(clazz.isInstance(target));
   }

   protected boolean invoke(Object target, String name) throws Exception
   {
      Method m = target.getClass().getMethod("is" + name);
      return (Boolean)m.invoke(target);
   }

   protected void createMcLib(VirtualFile dir, String metaInfParent, Class<?> clazz) throws Exception
   {
      VirtualFile mcDir = dir.getChild("mc.jar");
      createAssembledDirectory(mcDir) 
         .addPackage(clazz)
         .addPath(metaInfParent);
   }
   
   protected void createWeldLib(VirtualFile dir, String metaInfParent, Class<?> clazz) throws Exception
   {
      VirtualFile weldDir = dir.getChild("weld.jar");
      createAssembledDirectory(weldDir)
         .addPackage(clazz)
         .addPath(metaInfParent);
   }

   protected Class<?> findClass(DeploymentUnit unit, String name, boolean mustFind) throws ClassNotFoundException
   {
      //The class is loaded by a different classloader, so search for the correct class
      FlatDeployment flatDeployment = (FlatDeployment)getBean(DeployersUtils.getDeploymentBeanName(unit));
      assertNotNull(flatDeployment);
      Class<?> found = null;
      for (String current : flatDeployment.getFlatBeanDeploymentArchive().getBeanClasses())
      {
         if (name.equals(current))
         {
            found = unit.getClassLoader().loadClass(current);
            break;
         }
      }
      if (mustFind)
         assertNotNull(found);
      return found;
   }
   
   protected Object assertWebBean(BeanManager manager, DeploymentUnit unit, String className) throws Exception
   {
      Class<?> clazz = findClass(unit, className, true);
      Set<Bean<?>> beans = manager.getBeans(clazz);
      assertNotNull(beans);
      Bean<?> bean = manager.resolve(beans);
      assertNotNull(bean);
      
      Object ref = manager.getReference(bean, clazz, manager.createCreationalContext(bean));
      assertNotNull(ref);
      return ref;
   }
   
   protected Object assertWebBean(BeanManager manager, DeploymentUnit unit, Object o) throws Exception
   {
      Set<Bean<?>> beans = manager.getBeans(o.getClass());
      assertNotNull(beans);
      Bean<?> bean = manager.resolve(beans);
      assertNotNull(bean);
      
      Object ref = manager.getReference(bean, o.getClass(), manager.createCreationalContext(bean));
      assertNotNull(ref);
      return ref;
   }
   
   protected void assertNoWebBean(BeanManager manager, DeploymentUnit unit, String className) throws Exception
   {
      Class<?> clazz = findClass(unit, className, false);
      if (clazz != null)
      {
         Set<Bean<?>> beans = manager.getBeans(clazz);
         assertTrue(beans.isEmpty());
      }
   }
   
   protected void outputContext(ControllerContext context)
   {
      System.out.println(context.getName() + " " + context.getState());
      
      DependencyInfo di = context.getDependencyInfo();
      
      for (DependencyItem item : di.getUnresolvedDependencies(null))
      {
         System.out.println(item);
      }
   }
   
   protected BeanManager getBeanManager(DeploymentUnit unit) throws Exception
   {
      ControllerContext context = getControllerContext(DeployersUtils.getBootstrapBeanName(unit));
      assertNotNull(context);
      //Check the bootstrap bean has been installed
      assertNotNull(context.getTarget());
      
      Method m = context.getTarget().getClass().getMethod("getBootstrap"); 
      Object o = m.invoke(context.getTarget());
      assertNotNull(o);
      assertInstanceOf(o, Bootstrap.class);
      
      BeanManager manager = ((Bootstrap)o).getManager(getBeanDeploymentArchive(unit));
      assertNotNull(manager);
      return manager;
   }
   
   
   protected BeanDeploymentArchive getBeanDeploymentArchive(DeploymentUnit unit)
   {
      FlatDeployment flatDeployment = (FlatDeployment)getBean(DeployersUtils.getDeploymentBeanName(unit));
      assertNotNull(flatDeployment);
      return flatDeployment.getFlatBeanDeploymentArchive();
   }

}