/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.test.deployers.seam.test;

import java.io.IOException;
import java.io.Closeable;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import junit.framework.Test;

import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloading.spi.vfs.policy.VFSClassLoaderPolicy;
import org.jboss.mx.loading.UnifiedLoaderRepository3;
import org.jboss.test.JBossTestCase;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VFSUtils;

/**
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 * @author Scott.Stark@jboss.org
 * @version $Revision: 101688 $
 */
public class SeamVFSClassloadingUnitTestCase extends JBossTestCase
{
   private TempFileProvider provider;
   private List<Closeable> handles;

   public SeamVFSClassloadingUnitTestCase(String string)
   {
      super(string);
   }

   public static Test suite()
   {
      return suite(SeamVFSClassloadingUnitTestCase.class);
   }

   protected void setUp() throws Exception
   {
      super.setUp();

      provider = TempFileProvider.create("test", new ScheduledThreadPoolExecutor(2));
   }

   public void tearDown() throws Exception 
   {
       VFSUtils.safeClose(handles);
   }

   /*
   jboss-seam-booking.ear contents
   META-INF/application.xml
   META-INF/jboss-app.xml
   lib/commons-beanutils.jar
   lib/commons-digester.jar
   lib/jboss-el.jar
   lib/richfaces-api.jar
   jboss-seam.jar
   jboss-seam.jar/META-INF/MANIFEST.MF
   jboss-seam.jar/META-INF/components.xml
   jboss-seam.jar/META-INF/ejb-jar.xml
   jboss-seam.jar/META-INF/faces-config.xml
   jboss-seam.jar/META-INF/javamail.providers
   jboss-seam-booking.jar
   jboss-seam-booking.jar/META-INF/ejb-jar.xml
   jboss-seam-booking.jar/META-INF/persistence.xml
   jboss-seam-booking.war
   jboss-seam-booking.war/WEB-INF/classes/
   jboss-seam-booking.war/WEB-INF/components.xml
   jboss-seam-booking.war/WEB-INF/faces-config.xml
   jboss-seam-booking.war/WEB-INF/lib/jboss-seam-debug.jar
   jboss-seam-booking.war/WEB-INF/lib/jboss-seam-ui.jar
   jboss-seam-booking.war/WEB-INF/lib/jsf-facelets.jar
   jboss-seam-booking.war/WEB-INF/lib/richfaces-impl.jar
   jboss-seam-booking.war/WEB-INF/lib/richfaces-ui.jar
   jboss-seam-booking.war/WEB-INF/pages.xml
   jboss-seam-booking.war/WEB-INF/web.xml
   */
   protected VirtualFile getRoot() throws IOException, URISyntaxException
   {
      URL url = getDeployURL("jboss-seam-booking.ear");
      assertNotNull(url);
      VirtualFile vf = VFS.getChild(url);
	
      handles = recursiveMount(vf);
	
      assertTrue(vf.exists());
      return vf;
   }
   protected URL[] getEarClassPath(VirtualFile ear)
      throws Exception
   {
      URL[] cp = {
         // ear
         ear.toURL(),
         ear.getChild("lib/commons-beanutils.jar").getPhysicalFile().toURI().toURL(),
         ear.getChild("lib/commons-digester.jar").getPhysicalFile().toURI().toURL(),
         ear.getChild("lib/commons-digester.jar").getPhysicalFile().toURI().toURL(),
         ear.getChild("lib/jboss-el.jar").getPhysicalFile().toURI().toURL(),
         ear.getChild("lib/richfaces-api.jar").getPhysicalFile().toURI().toURL(),
         ear.getChild("jboss-seam.jar").getPhysicalFile().toURI().toURL(),
         ear.getChild("jboss-seam-booking.jar").getPhysicalFile().toURI().toURL(),
         ear.getChild("jboss-seam-booking.war/WEB-INF/classes/").getPhysicalFile().toURI().toURL(),
         ear.getChild("jboss-seam-booking.war/WEB-INF/lib/jboss-seam-debug.jar").getPhysicalFile().toURI().toURL(),
         ear.getChild("jboss-seam-booking.war/WEB-INF/lib/jboss-seam-ui.jar").getPhysicalFile().toURI().toURL(),
         ear.getChild("jboss-seam-booking.war/WEB-INF/lib/jsf-facelets.jar").getPhysicalFile().toURI().toURL(),
         ear.getChild("jboss-seam-booking.war/WEB-INF/lib/richfaces-impl.jar").getPhysicalFile().toURI().toURL(),
         ear.getChild("jboss-seam-booking.war/WEB-INF/lib/richfaces-ui.jar").getPhysicalFile().toURI().toURL(),
      };
      return cp;
   }

   public void testURLClassLoader() throws Exception
   {
      VirtualFile ear = getRoot();
      testURLClassLoader(ear);
   }

   protected void testURLClassLoader(VirtualFile ear) throws Exception
   {
      URL[] cp = getEarClassPath(ear);
      log.error("ear classpath: "+Arrays.asList(cp));
      URLClassLoader loader = new URLClassLoader(cp);
      loader.loadClass("org.jboss.seam.example.booking.Hotel");
      loader.loadClass("org.jboss.seam.debug.Contexts");      
   }

   public void testULRClassloading() throws Exception
   {
      VirtualFile ear = getRoot();
      testULRClassloading(ear);
   }

   public void testULRClassloading(VirtualFile ear) throws Exception
   {
      URL[] cp = getEarClassPath(ear);
      UnifiedLoaderRepository3 repository = new UnifiedLoaderRepository3();
      for(URL url : cp)
         repository.newClassLoader(url, true);
      log.debug("ear classpath: "+Arrays.asList(cp));
      repository.loadClass("org.jboss.seam.example.booking.Hotel");
      repository.loadClass("org.jboss.seam.debug.Contexts");
   }

   public void testVFSPolicy() throws Exception
   {
      VirtualFile vf = getRoot();
      VirtualFile childWar = vf.getChild("jboss-seam-booking.war");
      VirtualFile child = childWar.getChild("/WEB-INF/lib/jboss-seam-debug.jar");
      assertTrue(child.exists());
      VirtualFile[] roots = {child};
      ClassLoaderPolicy policy = new VFSClassLoaderPolicy(roots);
      URL url = policy.getResource("org/jboss/seam/debug/Contexts.class");
      log.info(url);
      assertNotNull(url);
   }

   public List<Closeable> recursiveMount(VirtualFile file) throws IOException
   {
      ArrayList<Closeable> mounts = new ArrayList<Closeable>();

      if (!file.isDirectory() && file.getName().matches("^.*\\.([EeWwJj][Aa][Rr]|[Zz][Ii][Pp])$"))
         mounts.add(VFS.mountZipExpanded(file, file, provider));

      if (file.isDirectory())
         for (VirtualFile child : file.getChildren())
            mounts.addAll(recursiveMount(child));

      return mounts;
   }
}
