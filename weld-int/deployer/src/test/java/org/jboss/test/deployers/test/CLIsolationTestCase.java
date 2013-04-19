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
import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.test.deployers.support.jar.PlainJavaBean;
import org.jboss.test.deployers.support.jsf.NotWBJsfBean;
import org.jboss.test.deployers.support.web.ServletWebBean;
import org.jboss.vfs.VirtualFile;

/**
 * CL isolation tests.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CLIsolationTestCase extends AbstractWeldTest
{
   public CLIsolationTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(CLIsolationTestCase.class);
   }

   protected void assertClassNotFound(String className, DeploymentUnit unit) throws Exception
   {
      ClassLoader cl = unit.getClassLoader();
      try
      {
         Class<?> clazz = cl.loadClass(className);
         fail("Should not be here: " + clazz.getClassLoader());
      }
      catch (ClassNotFoundException ignore)
      {
      }
   }

   protected void testIsolation(VirtualFile fst, String inFst, VirtualFile snd, String inSnd) throws Exception
   {
      Deployment deployment1 = createVFSDeployment(fst);
      Deployment deployment2 = createVFSDeployment(snd);

      DeployerClient mainDeployer = getDeployerClient();
      mainDeployer.addDeployment(deployment1);
      mainDeployer.addDeployment(deployment2);
      mainDeployer.process();
      try
      {
         DeploymentUnit du1 = getMainDeployerStructure().getDeploymentUnit(deployment1.getName());
         assertLoadClass(inFst, du1.getClassLoader());
         assertClassNotFound(inSnd, du1);

         DeploymentUnit du2 = getMainDeployerStructure().getDeploymentUnit(deployment2.getName());
         assertLoadClass(inSnd, du2.getClassLoader());
         assertClassNotFound(inFst, du2);
      }
      finally
      {
         mainDeployer.removeDeployment(deployment2);
         mainDeployer.removeDeployment(deployment1);
         mainDeployer.process();
      }
   }

   public void testTwoEars() throws Exception
   {
      VirtualFile ear1 = createTopLevelWithUtil("/weld/earwithutil");
      VirtualFile ear2 = createJarInEar();
      testIsolation(ear1, "org.jboss.test.deployers.support.util.SomeUtil", ear2, "org.jboss.test.deployers.support.jar.PlainJavaBean");
   }

   public void testTwoWars() throws Exception
   {
      VirtualFile war1 = createWar("w1.war", ServletWebBean.class);
      VirtualFile war2 = createWar("w2.war", NotWBJsfBean.class);
      testIsolation(war1, ServletWebBean.class.getName(), war2, NotWBJsfBean.class.getName());
   }

   public void testTwoJars() throws Exception
   {
      VirtualFile jar1 = createEjbJar("j1.jar", ServletWebBean.class);
      VirtualFile jar2 = createEjbJar("j2.jar", NotWBJsfBean.class);
      testIsolation(jar1, ServletWebBean.class.getName(), jar2, NotWBJsfBean.class.getName());
   }

   public void testWarEar() throws Exception
   {
      VirtualFile ear = createJarInEar();
      VirtualFile war = createWar("w1.war", ServletWebBean.class);
      testIsolation(ear, "org.jboss.test.deployers.support.jar.PlainJavaBean", war, ServletWebBean.class.getName());
   }

   public void testJarEar() throws Exception
   {
      VirtualFile ear = createJarInEar();
      VirtualFile jar = createEjbJar("j1.jar", ServletWebBean.class);
      testIsolation(ear, "org.jboss.test.deployers.support.jar.PlainJavaBean", jar, ServletWebBean.class.getName());
   }

   public void testJarWar() throws Exception
   {
      VirtualFile jar = createEjbJar("j1.jar", PlainJavaBean.class);
      VirtualFile war = createWar("w1.war", ServletWebBean.class);
      testIsolation(jar, PlainJavaBean.class.getName(), war, ServletWebBean.class.getName());
   }
}