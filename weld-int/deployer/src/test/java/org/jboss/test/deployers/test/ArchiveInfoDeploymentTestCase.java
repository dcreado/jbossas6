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

import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.test.deployers.support.jar.PlainJavaBean;
import org.jboss.test.deployers.support.web.ServletWebBean;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.deployer.env.bda.ArchiveInfo;
import org.jboss.weld.integration.deployer.env.bda.Classpath;

/**
 * ArchiveInfo deployment test case.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @version $Revision: 107075 $
 */
public class ArchiveInfoDeploymentTestCase extends AbstractWeldTest
{
   // the name of a classpath should always coincide with the name of the corresponding domain
   private static String DEFAULT_CLASSPATH_NAME = ClassLoaderSystem.getInstance().getDefaultDomain().getName();
   
   public ArchiveInfoDeploymentTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(ArchiveInfoDeploymentTestCase.class);
   }
   
   private Classpath defaultClasspath;
   private VFSDeploymentUnit unit;
   
   public void tearDown() throws Exception
   {
      undeploy(unit);
      super.tearDown();
   }

   public void testEjbJar() throws Exception
   {
      VirtualFile ejbJar = createEjbJar("simple.jar", PlainJavaBean.class);
      unit = assertDeploy(ejbJar);
      assertArchiveInfoWithDefaultClasspath(unit);
      assertEmpty(unit.getChildren());
   }

   public void testWar() throws Exception
   {
      VirtualFile war = createWar("simple.war", ServletWebBean.class);
      unit = assertDeploy(war);
      assertArchiveInfoWithWarClasspath(unit);
   }

   public void testEjbJarinEar() throws Exception
   {
      VirtualFile ejbJar = createJarInEar();
      unit = assertDeploy(ejbJar);
      assertArchiveInfoWithDefaultClasspath(unit);
      for (DeploymentUnit childUnit: unit.getChildren())
      {
         assertNull(childUnit.getAttachment(ArchiveInfo.class));
      }
   }

   public void testWarinEar() throws Exception
   {
      VirtualFile warInEar = createWarInEar();
      unit = assertDeploy(warInEar);
      assertArchiveInfoWithDefaultClasspath(unit);
      for (DeploymentUnit childUnit: unit.getChildren())
      {
         assertArchiveInfoWithWarClasspath(childUnit);
      }
   }

   public void testEarWithUtil() throws Exception
   {
      VirtualFile ear = createTopLevelWithUtil("/weld/earwithutil");
      unit = assertDeploy(ear);
      assertArchiveInfoWithDefaultClasspath(unit);
      for (DeploymentUnit childUnit: unit.getChildren())
      {
         assertNull(childUnit.getAttachment(ArchiveInfo.class));
      }
   }

   public void testJarWarInEar() throws Exception
   {
      VirtualFile ear = VFS.getChild("jar-in-ear.ear"); 
      createAssembledDirectory(ear)
      .addPath("/weld/jarwarinear")
      .addPackage("simple.jar", PlainJavaBean.class)
      .addPackage("simple.war/WEB-INF/classes", PlainJavaBean.class)
      .addPath("simple.war", "/weld/warwowb/web");
      unit = assertDeploy(ear);
      
      assertArchiveInfoWithDefaultClasspath(unit);
      for (DeploymentUnit childUnit: unit.getChildren())
      {
         if (childUnit.getName().endsWith("simple.jar/"))
         {
            assertNull(childUnit.getAttachment(ArchiveInfo.class));
         }
         else if (childUnit.getName().endsWith("simple.war/"))
         {
            assertArchiveInfoWithWarClasspath(childUnit);
         }
         else
         {
            fail("Unexpected unit name: " + childUnit.getName());
         }
      }
   }

   public void testEar() throws Exception
   {
      VirtualFile ear = createBasicEar();
      unit = assertDeploy(ear);
      assertArchiveInfoWithDefaultClasspath(unit);
      for (DeploymentUnit childUnit: unit.getChildren())
      {
         ArchiveInfo archiveInfo = childUnit.getAttachment(ArchiveInfo.class);
         if (childUnit.getName().endsWith(".war/"))
         {
            assertNotNull("Null ArchiveInfo", archiveInfo);
            assertNonDefaultClasspath(childUnit.getName(), archiveInfo.getClasspath());
         }
         else
         {
            assertNull("ArchiveInfo for unit " + childUnit.getName() + " is not null", archiveInfo);
         }
      }
   }

   private void assertArchiveInfoWithDefaultClasspath(VFSDeploymentUnit unit)
   {
      ArchiveInfo archiveInfo = unit.getAttachment(ArchiveInfo.class);
      assertNotNull("Null ArchiveInfo", archiveInfo);
      assertDefaultClasspath(archiveInfo.getClasspath());
   }

   private void assertArchiveInfoWithWarClasspath(DeploymentUnit unit)
   {
      ArchiveInfo archiveInfo = unit.getAttachment(ArchiveInfo.class);
      assertNotNull("Null ArchiveInfo for unit " + unit, archiveInfo);
      assertClasspath(unit.getName(), archiveInfo.getClasspath());
      assertEmpty(unit.getChildren());
   }

   private void assertDefaultClasspath(Classpath classpath)
   {
      assertClasspath(DEFAULT_CLASSPATH_NAME, classpath);
      this.defaultClasspath = classpath;
   }

   private void assertNonDefaultClasspath(String name, Classpath classpath)
   {
      assertClasspath(name, classpath);
      assertNotSame(defaultClasspath, classpath);
   }

   private void assertClasspath(String name, Classpath classpath)
   {
      assertNotNull(classpath);
      assertFalse(classpath.iterator().hasNext());
      assertEquals(name, classpath.getName());
   }
}
