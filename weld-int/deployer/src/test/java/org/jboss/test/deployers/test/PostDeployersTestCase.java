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

import java.util.List;

import junit.framework.Test;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.ExportAll;
import org.jboss.classloading.spi.version.Version;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.common.ejb.IEjbJarMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.spec.InterceptorMetaData;
import org.jboss.metadata.ejb.spec.InterceptorsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.deployer.metadata.WeldEjbInterceptorMetadataDeployer;

/**
 * Test post deployers.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class PostDeployersTestCase extends AbstractWeldTest
{
   public PostDeployersTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(PostDeployersTestCase.class);
   }

   public void testSimpleJar() throws Exception
   {
      testJar("simple", false);
   }

   public void testPostJar() throws Exception
   {
      testJar("post", true);
   }

   public void testSimpleWar() throws Exception
   {
      testWar("simple", false, true);
   }

   public void testPostWar() throws Exception
   {
      testWar("post", true, false);
   }

   public void testEar() throws Exception
   {
      VirtualFile ear = createBasicEar();
      DeploymentUnit earDU = assertDeploy(ear);
      try
      {
         assertClassLoading(true, earDU);

         DeploymentUnit jarDU = assertChild(earDU, "ejbs.jar");
         assertWBInterceptor(jarDU);

         DeploymentUnit webDU = assertChild(earDU, "simple.war");
         assertWBListener(webDU);
      }
      finally
      {
         undeploy(earDU);
      }
   }

   protected void testJar(String type, boolean testCL) throws Exception
   {
      VirtualFile topLevel = VFS.getChild("ejbs.jar");
      createAssembledDirectory(topLevel)
         .addPath("/weld/" + type + "/ejb");

      DeploymentUnit topDU = assertDeploy(topLevel);
      try
      {
         assertClassLoading(testCL, topDU);
         assertWBInterceptor(topDU);
      }
      finally
      {
         undeploy(topDU);
      }
   }

   protected void testWar(String type, boolean testCL, boolean testListner) throws Exception
   {
      VirtualFile topLevel = VFS.getChild("web.war");
      createAssembledDirectory(topLevel)
         .addPath("/weld/" + type + "/web");

      DeploymentUnit topDU = assertDeploy(topLevel);
      try
      {
         if (testCL)
            assertClassLoading(testCL, topDU);
         if (testListner)
            assertWBListener(topDU);
      }
      finally
      {
         undeploy(topDU);
      }
   }

   protected void assertClassLoading(boolean equals, DeploymentUnit unit)
   {
      ClassLoadingMetaData clmd = unit.getAttachment(ClassLoadingMetaData.class);
      assertNotNull(clmd);

      ClassLoadingMetaData classLoadingMetaData = new ClassLoadingMetaData();
      classLoadingMetaData.setName(unit.getName());
      classLoadingMetaData.setDomain(clmd.getDomain()); // hack, get domain from clmd
      classLoadingMetaData.setExportAll(ExportAll.NON_EMPTY);
      classLoadingMetaData.setImportAll(true);
      classLoadingMetaData.setVersion(Version.DEFAULT_VERSION);
      classLoadingMetaData.setJ2seClassLoadingCompliance(false);

      assertEquals(equals, clmd.equals(classLoadingMetaData));
   }

   protected void assertWBInterceptor(DeploymentUnit unit)
   {
      IEjbJarMetaData ejbmd = unit.getAttachment(JBossMetaData.class);
      assertNotNull(ejbmd);
      InterceptorsMetaData interceptors = ejbmd.getInterceptors();
      assertNotNull(interceptors);
      InterceptorMetaData imd = interceptors.get(WeldEjbInterceptorMetadataDeployer.INJECTION_INTERCEPTOR_CLASS_NAME);
      assertNotNull(imd);
   }

   protected void assertWBListener(DeploymentUnit unit)
   {
      JBossWebMetaData jbwmd = unit.getAttachment(JBossWebMetaData.class);
      assertNotNull(jbwmd);
      List<ListenerMetaData> listeners = jbwmd.getListeners();
      assertNotNull(listeners);
      assertEquals(2, listeners.size());
      ListenerMetaData lmd = listeners.get(0);
      assertNotNull(lmd);
      assertEquals("org.jboss.weld.servlet.WeldListener", lmd.getListenerClass());
   }
}