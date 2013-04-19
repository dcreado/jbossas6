/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.deployers.as.test;

import java.io.File;
import java.net.URL;

import org.apache.commons.httpclient.HttpMethodBase;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.AbstractTestDelegate;
import org.jboss.test.JBossTestCase;
import org.jboss.test.JBossTestServices;
import org.jboss.test.deployers.as.support.HttpUtils;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractWeldInAsTest extends JBossTestCase
{
   private static final ArchivePath META_INF = ArchivePaths.create("META-INF");
   private static final ArchivePath WEB_INF = ArchivePaths.create("WEB-INF");
   private static final ArchivePath FACES_CONFIG = ArchivePaths.create(WEB_INF, "faces-config.xml");
   private static final ArchivePath WEB_XML = ArchivePaths.create(WEB_INF, "web.xml");
   private static final ArchivePath META_BEANS_XML = ArchivePaths.create(META_INF, "beans.xml");
   private static final ArchivePath WEB_BEANS_XML = ArchivePaths.create(WEB_INF, "beans.xml");
   private static final ArchivePath JBOSS_BEANS_XML = ArchivePaths.create(META_INF, "jboss-beans.xml");

   public static AbstractTestDelegate getDelegate(Class<?> clazz)
   {
      return new JBossTestServices(clazz);
   }

   public AbstractWeldInAsTest(String name)
   {
      super(name);
   }

   protected WebArchive createWebArchive(String name)
   {
      WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "test-weld.war");
      webArchive
         .addResource("org/jboss/test/deployers/as/support/webapp/" + name + ".xhtml", "/" + name + ".xhtml")
         .addResource("org/jboss/test/deployers/as/support/webapp/WEB-INF/web.xml", WEB_XML)
         .addResource("org/jboss/test/deployers/as/support/webapp/WEB-INF/faces-config.xml", FACES_CONFIG);
      
      
      
      getLog().debug(webArchive.toString(true));
      
      return webArchive;
   }

   protected JavaArchive createWeldArchive(Class<?>...classes)
   {
      JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class, "weld.jar")
         .addClasses(classes)
         .addResource("org/jboss/test/deployers/as/support/weld/META-INF/beans.xml", META_BEANS_XML);
      
      getLog().debug(javaArchive.toString(true));
      return javaArchive;
   }

   protected JavaArchive createMcArchive(String packageFragment, Class<?>...classes)
   {
      
      JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class, "mc.jar")
         .addClasses(classes)
         .addResource("org/jboss/test/deployers/as/support/" + packageFragment + "/META-INF/jboss-beans.xml", JBOSS_BEANS_XML);
      
      getLog().debug(javaArchive.toString(true));
      return javaArchive;
   }

   protected URL createAndDeployWar(String name, Class<?>...classes) throws Exception
   {
      WebArchive webArchive = createWebArchive(name)
         .addResource("org/jboss/test/deployers/as/support/weld/META-INF/beans.xml", WEB_BEANS_XML)
         .addClasses(classes);
   
      return explodeArchiveAndDeploy(webArchive);
   }

   protected URL createAndDeployWar(String name, String mcDescriptorPackageFragment, Class<?>...classes) throws Exception
   {
      WebArchive webArchive = createWebArchive(name)
      .addResource("org/jboss/test/deployers/as/support/weld/META-INF/beans.xml", WEB_BEANS_XML)
      .addResource("org/jboss/test/deployers/as/support/" + mcDescriptorPackageFragment + "/META-INF/jboss-beans.xml", JBOSS_BEANS_XML)
      .addClasses(classes);
   
      return explodeArchiveAndDeploy(webArchive);
   }

   protected URL createAndDeployEar(Archive<?>...archives) throws Exception
   {
      EnterpriseArchive enterpriseArchive = ShrinkWrap.create(EnterpriseArchive.class, "simple.ear");
      for (Archive<?> archive : archives)
         enterpriseArchive.add(archive, "/");
   
       return explodeArchiveAndDeploy(enterpriseArchive);
   }

   private URL explodeArchiveAndDeploy(Archive<?> archive) throws Exception
   {
      URL url = createExplodedArchive(archive);
   
      getLog().debug(archive.toString(true));
   
      undeploy(url.toString());
      try
      {
         deploy(url.toString());
      }
      catch(Exception e)
      {
         undeploy(url.toString());
         throw(e);
      }
      
      return url;
   }

   protected void accessWebApp(String urlPath, String...tokens) throws Exception
   {
      HttpMethodBase request = HttpUtils.accessURL(new URL(HttpUtils.getBaseURL() + urlPath));
      String body = request.getResponseBodyAsString();
      
      assertTrue("Body was not transformed to html:\n" + body, body.indexOf("<h:") < 0);
      
      System.out.println(body);
      
      
      for (String token : tokens)
         assertTrue("Could not find '" + token + "' in body:\n" + body, body.indexOf(token) >= 0);
   }

   private URL createExplodedArchive(Archive<?> archive) throws Exception
   {
      File file = new File("target/lib");
      if (file.exists())
         deleteFile(file);
      file.mkdir();
      File ear = new File(file, archive.getName()); 
      if (ear.exists())
         deleteFile(file);
      file = archive.as(ExplodedExporter.class).exportExploded(file);
      
      return file.toURI().toURL();
   }

   private void deleteFile(File file)
   {
      if (file.isDirectory())
      {
         for (String name : file.list())
            deleteFile(new File(file, name));
      }
   
      file.delete();
   }

}
