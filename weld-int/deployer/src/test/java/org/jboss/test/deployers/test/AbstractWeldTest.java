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

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.jboss.classloader.plugins.jdk.AbstractJDKChecker;
import org.jboss.deployers.vfs.deployer.kernel.BeanMetaDataFactoryVisitor;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.vfs3.ArchiveFileSystem;
import org.jboss.test.deployers.BootstrapDeployersTest;
import org.jboss.test.deployers.support.crm.CrmWebBean;
import org.jboss.test.deployers.support.ejb.MySLSBean;
import org.jboss.test.deployers.support.ext.ExternalWebBean;
import org.jboss.test.deployers.support.jar.PlainJavaBean;
import org.jboss.test.deployers.support.jsf.NotWBJsfBean;
import org.jboss.test.deployers.support.ui.UIWebBean;
import org.jboss.test.deployers.support.util.SomeUtil;
import org.jboss.test.deployers.support.web.ServletWebBean;
import org.jboss.vfs.TempDir;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

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
            tempFileProvider = TempFileProvider.create("shrinkwrap-", Executors.newSingleThreadScheduledExecutor());
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

   protected VirtualFile createBasicEar() throws Exception
   {
      return createBasicEar(SomeUtil.class);
   }

   protected VirtualFile createBasicEar(Class<?> utilClass) throws Exception
   {
      VirtualFile ear = createTopLevelWithUtil(utilClass);

      VirtualFile jar = ear.getChild("simple.jar");
      createAssembledDirectory(jar)
         .addPackage(PlainJavaBean.class)
         .addPath("/weld/simple/jar");

      VirtualFile ejbs = ear.getChild("ejbs.jar");
      createAssembledDirectory(ejbs)
         .addPackage(MySLSBean.class)
         .addPath("/weld/simple/ejb");

      VirtualFile war = ear.getChild("simple.war");
      createAssembledDirectory(war)
         .addPackage("WEB-INF/classes", ServletWebBean.class)
         .addPath("/weld/simple/web")
         .addPackage("WEB-INF/lib/ui.jar", UIWebBean.class)
         .addPath("WEB-INF/lib/ui.jar", "/weld/simple/ui");

      // war w/o beans.xml

      war = ear.getChild("crm.war");
      createAssembledDirectory(war)
         .addPackage("WEB-INF/classes", NotWBJsfBean.class)
         .addPackage("WEB-INF/lib/crm.jar", CrmWebBean.class)
         .addPath("WEB-INF/lib/crm.jar", "/weld/simple/crm");

      enableTrace("org.jboss.deployers");

      return ear;
   }

   protected VirtualFile createTopLevelWithUtil() throws Exception
   {
      return createTopLevelWithUtil("/weld/simple");
   }

   protected VirtualFile createTopLevelWithUtil(Class<?> utilClass) throws Exception
   {
      if (utilClass != null)
      return createTopLevelWithUtil("/weld/simple", utilClass);
      else
      return createTopLevelWithUtil();
   }

   protected VirtualFile createTopLevelWithUtil(String path) throws Exception
   {
      return createTopLevelWithUtil(path, SomeUtil.class);
   }

   protected VirtualFile createTopLevelWithUtil(String path, Class<?> utilClass) throws Exception
   {
      VirtualFile earFile = VFS.getChild("top-level.ear");
      createAssembledDirectory(earFile)
         .addPath(path)
         .addPackage("lib/util.jar", utilClass)
         .addPackage("lib/ext.jar", ExternalWebBean.class)
         .addPath("lib/ext.jar", "/weld/simple/ext");
      return earFile;
   }

   protected VirtualFile createWarInEar() throws Exception
   {
      VirtualFile earFile = VFS.getChild("war-in-ear.ear");
      createAssembledDirectory(earFile)
         .addPath("/weld/warinear")
         .addPackage("simple.war/WEB-INF/classes", ServletWebBean.class)
         .addPath("simple.war", "/weld/simple/web");
      return earFile;
   }

   protected VirtualFile createJarInEar() throws Exception
   {
      VirtualFile earFile = VFS.getChild("jar-in-ear.ear");
      createAssembledDirectory(earFile)
         .addPath("/weld/jarinear")
         .addPackage("simple.jar", PlainJavaBean.class)
         .addPath("simple.jar", "/weld/simple/jar");
      return earFile;
   }

   protected VirtualFile createWar(String warName, Class<?> reference) throws Exception
   {
      VirtualFile warFile = VFS.getChild(warName);
      createAssembledDirectory(warFile)
         .addPackage("WEB-INF/classes", reference)
         .addPath("/weld/simple/web");
      return warFile;
   }

   protected VirtualFile createEjbJar(String jarName, Class<?> reference) throws Exception
   {
      VirtualFile jarFile = VFS.getChild(jarName);
      createAssembledDirectory(jarFile)
         .addPackage(reference)
         .addPath("/weld/simple/ejb");
      return jarFile;
   }
   
      
   private static TempFileProvider tempFileProvider;
   private final List<Closeable> vfsHandles = new ArrayList<Closeable>();
   
   protected VFSDeploymentUnit assertDeploy(Archive<?> archive) throws Exception
   {
      VirtualFile virtualFile = mount(archive);
      VFSDeploymentUnit unit = assertDeploy(virtualFile);
      return unit;
   }
   
   @Override
   protected VFSDeploymentUnit assertDeploy(VirtualFile virtualFile) throws Exception
   {
      VFSDeploymentUnit unit = super.assertDeploy(virtualFile);
      units.add(unit);
      return unit;
   }
   
   private Collection<VFSDeploymentUnit> units = new ArrayList<VFSDeploymentUnit>();
   
   private VirtualFile mount(Archive<?> archive) throws IOException
   {
      final TempDir tempDir = tempFileProvider.createTempDir(archive.getName());
      VirtualFile virtualFile = VFS.getChild(UUID.randomUUID().toString()).getChild(archive.getName());
      vfsHandles.add(VFS.mount(virtualFile, new ArchiveFileSystem(archive, tempDir)));
      mountZipFiles(virtualFile);
      return virtualFile;
   }
   
   private void mountZipFiles(VirtualFile file) throws IOException
   {
      if (!file.isDirectory() && file.getName().matches("^.*\\.([EeWwJj][Aa][Rr]|[Zz][Ii][Pp])$"))
         vfsHandles.add(VFS.mountZip(file, file, tempFileProvider));

      if (file.isDirectory())
         for (VirtualFile child : file.getChildren())
            mountZipFiles(child);
   }
   
   protected void tearDown() throws Exception
   {
      for (VFSDeploymentUnit unit: units)
      {
         undeploy(unit);
      }
      units.clear();
      for (Closeable vfsHandle: vfsHandles)
      {
            vfsHandle.close();
      }
      vfsHandles.clear();
      super.tearDown();
   }
}