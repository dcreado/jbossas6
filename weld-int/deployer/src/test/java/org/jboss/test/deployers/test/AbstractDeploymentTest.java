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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.test.deployers.support.MockEmptyEjbServices;
import org.jboss.test.deployers.support.crm.CrmWebBean;
import org.jboss.test.deployers.support.ejb.BusinessInterface;
import org.jboss.test.deployers.support.ejb.MySLSBean;
import org.jboss.test.deployers.support.ext.ExternalWebBean;
import org.jboss.test.deployers.support.jar.PlainJavaBean;
import org.jboss.test.deployers.support.ui.UIWebBean;
import org.jboss.test.deployers.support.web.ServletWebBean;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.integration.deployer.DeployersUtils;

/**
 * Abstract Deployment test case.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractDeploymentTest extends AbstractWeldTest
{
   protected AbstractDeploymentTest(String name)
   {
      super(name);
   }

   protected void getArchives(List<BeanDeploymentArchive> result, Collection<BeanDeploymentArchive> archives)
   {
      for (BeanDeploymentArchive bda : archives)
      {
         if (result.contains(bda))
         {
            continue;
         }
         result.add(bda);
         getArchives(result, bda.getBeanDeploymentArchives());
      }
   }

   protected abstract int getExpectedArchives();

   public void testSimpleUsage() throws Exception
   {
      VirtualFile ear = createBasicEar(MockEmptyEjbServices.class);
      VFSDeploymentUnit topUnit = assertDeploy(ear);
      try
      {
         assertBean(DeployersUtils.getBootstrapBeanName(topUnit), null, Object.class);

         Object bean = getBean(Deployment.class);
         Deployment deployment = assertInstanceOf(bean, Deployment.class, false);
         initializeDeployment(deployment);
         
         List<BeanDeploymentArchive> archives = new ArrayList<BeanDeploymentArchive>();
         getArchives(archives, deployment.getBeanDeploymentArchives());
         assertEquals(getExpectedArchives(), archives.size());

         //List<BeansXml> urls = new ArrayList<BeansXml>();
         List<String> classes = new ArrayList<String>();
         for (BeanDeploymentArchive bad : archives)
         {
            //urls.add(bad.getBeansXml());
            for (String clazz : bad.getBeanClasses())
               classes.add(clazz);
         }

         Set<String> expected = new HashSet<String>();
//         addExpectedResource(expected, "ejbs.jar");
//         addExpectedResource(expected, "ext.jar");
//         addExpectedResource(expected, "simple.jar");
//         addExpectedResource(expected, "ui.jar");
//         addExpectedResource(expected, "crm.jar");
//         addExpectedResource(expected, "simple.war", "/WEB-INF/beans.xml");
//
//         assertEquals("Illegal size or urls.", expected.size(), urls.size());

//         for (URL url : urls)
//         {
//            boolean found = false;
//            Iterator<String> iter = expected.iterator();
//            while (iter.hasNext())
//            {
//               String expectedURL = iter.next();
//               if (url.toExternalForm().contains(expectedURL))
//               {
//                  iter.remove();
//                  found = true;
//                  break;
//               }
//            }
//            assertTrue("Unexpected wb url: " + url, found);
//         }

         addExpectedClass(expected, BusinessInterface.class);
         addExpectedClass(expected, MySLSBean.class);
         addExpectedClass(expected, ExternalWebBean.class);
         addExpectedClass(expected, PlainJavaBean.class);
         addExpectedClass(expected, UIWebBean.class);
         addExpectedClass(expected, ServletWebBean.class);
         addExpectedClass(expected, CrmWebBean.class);

         assertEquals("Illegal size or classes.", classes.size(), expected.size());

         for (String className : classes)
            assertTrue(expected.remove(className));

         assertEmpty("Should be emtpy, missing " + expected, expected);

         Class<?> newBeanClass = topUnit.getClassLoader().loadClass("org.jboss.test.deployers.support.MockTransactionServices");
         BeanDeploymentArchive newBDA = deployment.loadBeanDeploymentArchive(newBeanClass);
         assertNewBeanDeploymentArchive(archives, newBDA);
      }
      finally
      {
         undeploy(topUnit);
      }
   }

   protected abstract void assertNewBeanDeploymentArchive(List<BeanDeploymentArchive> archives, BeanDeploymentArchive newBDA);
   
   protected abstract void initializeDeployment(Deployment deployment);

   private static void addExpectedResource(Set<String> expected, String unit)
   {
      addExpectedResource(expected, unit, "/META-INF/beans.xml");
   }

   private static void addExpectedResource(Set<String> expected, String unit, String suffix)
   {
      expected.add(unit + suffix);
   }

   private static void addExpectedClass(Set<String> expected, Class<?> clazz)
   {
      expected.add(clazz.getName());
   }
}