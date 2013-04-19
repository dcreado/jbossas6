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
package org.jboss.test.jbossmx.implementation.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.RuntimeErrorException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.mx.loading.UnifiedLoaderRepository3;
import org.jboss.mx.loading.UnifiedClassLoader3;
import org.jboss.test.jbossmx.implementation.TestCase;

/** Test of the mbean operation invocation thread context class loader. This
 *test case simulates the problem originally seen in Bug#516649. Reproducing the
 *steps here requires a number of contrived steps including simulated reloading
 *of the mbean from different jars and the loading of the TestData class by
 *the main MBeanClassLoader using Class.forName(). These are the actions that
 *occur when reloading a sar that contains an ejb-jar.
 *
 * @author  Scott.Stark@jboss.org
 * @version $Revision: 81036 $
 */
public class ContextCLTestCase extends TestCase 
{
   private static URL dataClassURL;
   private static File dataJar;
   private static UnifiedLoaderRepository3 repo;
   private static UnifiedClassLoader3 deployLoader;
   private static UnifiedClassLoader3 noLoader;
   static Object data0;
   private static URL jarUrl;

   public ContextCLTestCase(String name)
   {
      super(name);
   }

   public void testInvokeNeedingTCL() throws Exception
   {
      ClassLoader entryCL = Thread.currentThread().getContextClassLoader();
      /* Install the mainLoader to simulate how the MBeanServer would be
       *running under JBoss
      */
      Thread.currentThread().setContextClassLoader(deployLoader);
      MBeanServer server = MBeanServerFactory.createMBeanServer();

      // Create the ContextCL MBean using the TestClassLoader
      try
      {
         repo.newClassLoader(jarUrl, true);
         ObjectName beanName = new ObjectName("org.jboss.test.jbossmx.implementation.server.support:test=ContextCLTestCase");
         server.createMBean("org.jboss.test.jbossmx.implementation.server.support.ContextCL", beanName);
         getLog().info("Created ContextCL MBean");

         // Invoke the useTestData op to test the thread context class loader
         server.invoke(beanName, "useTestData", null, null);
         getLog().info("Invoked ContextCL.useTestData");
      }
      catch(RuntimeErrorException e)
      {
         getLog().error("NestedError", e.getTargetError());
         throw e;
      }
      finally
      {
         Thread.currentThread().setContextClassLoader(entryCL);
         
      }

      MBeanServerFactory.releaseMBeanServer(server);
   }

   /** This creates two mbean deployment jars, testdata.jar and testdata1.jar.
    *A redeployment is simulated by loading 
    *
    * @exception Exception  Description of Exception
    */
   public void createTestDataJar() throws Exception
   {
      repo = new UnifiedLoaderRepository3();
      dataJar = new File("testdata.jar");
      // Find the TestData class
      dataClassURL = getClass().getResource("/org/jboss/test/jbossmx/implementation/server/support/TestData.class");
      if( dataClassURL == null && dataJar.exists() == false )
         fail("Failed to find /org/jboss/test/jbossmx/implementation/server/support/TestData.class");
      if( dataClassURL != null )
      {
         getLog().info("Found TestData at: " + dataClassURL);
         // Build a jar file containing only the TestData.class and ContextCL mbean
         FileOutputStream fos = new FileOutputStream(dataJar);
         Manifest mf = new Manifest();
         JarOutputStream jos = new JarOutputStream(fos, mf);

         ZipEntry entry = new ZipEntry("org/jboss/test/jbossmx/implementation/server/support/TestData.class");
         jos.putNextEntry(entry);
         InputStream dataIS = dataClassURL.openStream();
         byte[] bytes = new byte[512];
         int read = 0;
         while( (read = dataIS.read(bytes)) > 0 )
            jos.write(bytes, 0, read);
         jos.closeEntry();
         dataIS.close();

         URL mbeanURL = getClass().getResource("/org/jboss/test/jbossmx/implementation/server/support/ContextCL.class");
         entry = new ZipEntry("org/jboss/test/jbossmx/implementation/server/support/ContextCL.class");
         jos.putNextEntry(entry);
         dataIS = mbeanURL.openStream();
         while( (read = dataIS.read(bytes)) > 0 )
            jos.write(bytes, 0, read);
         jos.closeEntry();
         dataIS.close();

         URL imbeanURL = getClass().getResource("/org/jboss/test/jbossmx/implementation/server/support/ContextCLMBean.class");
         entry = new ZipEntry("org/jboss/test/jbossmx/implementation/server/support/ContextCLMBean.class");
         jos.putNextEntry(entry);
         dataIS = imbeanURL.openStream();
         while( (read = dataIS.read(bytes)) > 0 )
            jos.write(bytes, 0, read);
         jos.closeEntry();
         dataIS.close();

         getLog().info("Created mbean jar at: "+dataJar.getAbsolutePath());
         jos.close();

         // Now remote the class files from this classpath
         File dataClassFile = new File(dataClassURL.getFile());
         getLog().info("Removed TestData.class File: " + dataClassFile.delete());
         File mbeanClassFile = new File(mbeanURL.getFile());
         getLog().info("Removed ContextCL.class File: " + mbeanClassFile.delete());
         File imbeanClassFile = new File(imbeanURL.getFile());
         getLog().info("Removed ContextCLMBean.class File: " + imbeanClassFile.delete());
      }

      // Create a copy of the jar
      FileInputStream fis = new FileInputStream("testdata.jar");
      FileOutputStream fos = new FileOutputStream("testdata1.jar");
      byte[] bytes = new byte[512];
      int read = 0;
      while( (read = fis.read(bytes)) > 0 )
         fos.write(bytes, 0, read);
      fis.close();
      fos.close();

      noLoader = (UnifiedClassLoader3) repo.newClassLoader(null, true);
      deployLoader = (UnifiedClassLoader3) repo.newClassLoader(dataJar.toURL(), true);
      Class c0 = deployLoader.loadClass("org.jboss.test.jbossmx.implementation.server.support.TestData");
      getLog().info("TestData #0 ProtectionDomain: "+c0.getProtectionDomain());
      Class c1 = Class.forName("org.jboss.test.jbossmx.implementation.server.support.TestData",
         false, noLoader);
      getLog().info("TestData #1 ProtectionDomain: "+c1.getProtectionDomain());
      repo.removeClassLoader(deployLoader);
      // Simulate a redeploy
      File data1Jar = new File("testdata1.jar");
      jarUrl = data1Jar.toURL();
      deployLoader = (UnifiedClassLoader3) repo.newClassLoader(jarUrl, true);
      c0 = deployLoader.loadClass("org.jboss.test.jbossmx.implementation.server.support.TestData");
      getLog().info("Reloaded TestData #0 ProtectionDomain: "+c0.getProtectionDomain());
      data0 = c0.newInstance();
   }

   /** Setup the test suite.
    */
   public static Test suite()
   {
      TestSuite suite = new TestSuite();
      /* If this is running under java5 just return as there is no guarentee
      of what the class loader of the mbean is.
      */
      try
      {
         Class annotationClass = Class.forName("java.lang.annotation.Annotation");
         // Running under java5+, use an empty suite
      }
      catch(ClassNotFoundException e)
      {
         // Running under jdk1.4.x, continue
         suite.addTest(new ContextCLTestCase("createTestDataJar"));
         suite.addTest(new ContextCLTestCase("testInvokeNeedingTCL"));
      }
      return suite;
   }

}
