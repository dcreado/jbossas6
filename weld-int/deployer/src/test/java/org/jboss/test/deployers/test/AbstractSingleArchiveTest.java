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

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.deployers.support.MockArchiveManifest;
import org.jboss.test.deployers.support.crm.CrmWebBean;
import org.jboss.test.deployers.support.ejb.BusinessInterface;
import org.jboss.test.deployers.support.ejb.MySLSBean;
import org.jboss.test.deployers.support.ext.ExternalWebBean;
import org.jboss.test.deployers.support.jar.PlainJavaBean;
import org.jboss.test.deployers.support.jsf.NotWBJsfBean;
import org.jboss.test.deployers.support.ui.UIWebBean;
import org.jboss.test.deployers.support.util.SomeUtil;
import org.jboss.test.deployers.support.web.ServletWebBean;
import org.jboss.vfs.VirtualFile;

/**
 * Abstract test case that contains scenarios involving the deployment of a single archive.
 * This test does not contain test scenarios that assert the behaviour when more than one
 * archive is deployed at the same time.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @param E the type of environment this test applies to
 */
public abstract class AbstractSingleArchiveTest<E> extends AbstractEnvironmentTest<E>
{
   public AbstractSingleArchiveTest(String name)
   {
      super(name);
   }

   protected DeploymentUnit unit;

   public void testEjbJar() throws Exception
   {
      JavaArchive ejbJar = createEjbJar(true);
      unit = assertDeploy(ejbJar);

      E environment = assertSingleEnvironment(EJB_JAR_NAME);
      assertExpectedClasses(environment, PlainJavaBean.class);
      assertExpectedResources(environment, "ejb.jar");
   }

   public void testEjbJarWithoutXml() throws Exception
   {
      JavaArchive ejbJar = createEjbJar(false);
      unit = assertDeploy(ejbJar);
      
      assertEmptyEnvironment();
   }

   public void testEjbJarInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME);
      JavaArchive ejbJar = createEjbJar(true);
      ear.addModule(ejbJar);
      MockArchiveManifest.addManifest(ear);
      unit = assertDeploy(ear);
      
      E environment = assertSingleEnvironment(EAR_NAME);
      assertExpectedClasses(environment, PlainJavaBean.class);
      assertExpectedResources(environment, unit.getSimpleName() + "/ejb.jar");
   }

   public void testEjbJarWithoutXmlInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME);
      JavaArchive ejbJar = createEjbJar(false);
      ear.addModule(ejbJar);
      MockArchiveManifest.addManifest(ear);
      unit = assertDeploy(ear);
      
      assertEmptyEnvironment();
   }

   public void testEjbJarsInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME);
      JavaArchive ejbJar1 = createEjbJar("ejbJar1.jar", true, PlainJavaBean.class);
      ear.addModule(ejbJar1);
      JavaArchive ejbJar2 = createEjbJar("ejbJar2.jar", true, MySLSBean.class, BusinessInterface.class);
      ear.addModule(ejbJar2);
      MockArchiveManifest.addManifest(ear);
      unit = assertDeploy(ear);
      
      E environment = assertSingleEnvironment(EAR_NAME);
      assertExpectedClasses(environment, PlainJavaBean.class, MySLSBean.class,
               BusinessInterface.class);
      assertExpectedResources(environment, EAR_NAME + "/ejbJar1.jar",
               EAR_NAME + "/ejbJar2.jar");
   }

   public void testMixedEjbJarsInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME);
      JavaArchive ejbJar1 = createEjbJar("ejbJar1.jar", false, PlainJavaBean.class);
      ear.addModule(ejbJar1);
      JavaArchive ejbJar2 = createEjbJar("ejbJar2.jar", true, MySLSBean.class, BusinessInterface.class);
      ear.addModule(ejbJar2);
      MockArchiveManifest.addManifest(ear);
      unit = assertDeploy(ear);
      
      E environment = assertSingleEnvironment(EAR_NAME);
      assertExpectedClasses(environment, MySLSBean.class, BusinessInterface.class);
      assertExpectedResources(environment, "simple.ear/ejbJar2.jar");
   }

   public void testEjbJarsWithoutXmlInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME);
      JavaArchive ejbJar1 = createEjbJar("ejbJar1.jar", false, PlainJavaBean.class);
      ear.addModule(ejbJar1);
      JavaArchive ejbJar2 = createEjbJar("ejbJar2.jar", false, MySLSBean.class, BusinessInterface.class);
      ear.addModule(ejbJar2);
      MockArchiveManifest.addManifest(ear);
      unit = assertDeploy(ear);
      
      assertEmptyEnvironment();
   }

   public void testWar() throws Exception
   {
      WebArchive war = createWar(true);
      unit = assertDeploy(war);
      
      E environment = assertSingleEnvironment(WAR_NAME);
      assertExpectedClasses(environment, ServletWebBean.class);
      assertExpectedWarResources(environment, unit.getSimpleName(), true);
   }

   public void testWarWithLib() throws Exception
   {
      WebArchive war = createWarWithLib(true, true);
      unit = assertDeploy(war);
      
      E environment = assertSingleEnvironment(WAR_NAME);
      assertExpectedClasses(environment, ServletWebBean.class, UIWebBean.class);
      assertExpectedWarResources(environment, unit.getSimpleName(), true, "lib.jar");
   }

   public void testWarWithLibs() throws Exception
   {
      WebArchive war = createWarWithLibs(true, true, true);
      unit = assertDeploy(war);
      
      E environment = assertSingleEnvironment(WAR_NAME);
      assertExpectedClasses(environment, ServletWebBean.class, UIWebBean.class, CrmWebBean.class);
      assertExpectedWarResources(environment, unit.getSimpleName(), true, "lib1.jar", "lib2.jar");
   }

   public void testWarWithLibWithoutXml() throws Exception
   {
      WebArchive war = createWarWithLib(true, false);
      unit = assertDeploy(war);
      
      E environment = assertSingleEnvironment(WAR_NAME);
      assertExpectedClasses(environment, ServletWebBean.class);
      assertExpectedWarResources(environment, unit.getSimpleName(), true);
   }

   public void testWarWithLibsWithoutXml() throws Exception
   {
      WebArchive war = createWarWithLibs(true, false, false);
      unit = assertDeploy(war);
      
      E environment = assertSingleEnvironment(WAR_NAME);
      assertExpectedClasses(environment, ServletWebBean.class);
      assertExpectedWarResources(environment, unit.getSimpleName(), true);
   }

   public void testWarWithMixedLibs() throws Exception
   {
      WebArchive war = createWarWithLibs(true, true, false);
      unit = assertDeploy(war);
      
      E environment = assertSingleEnvironment(WAR_NAME);
      assertExpectedClasses(environment, ServletWebBean.class, UIWebBean.class);
      assertExpectedWarResources(environment, unit.getSimpleName(), true, "lib1.jar");
   }

   public void testWarWithoutXmlWithLib() throws Exception
   {
      WebArchive war = createWarWithLib(false, true);
      unit = assertDeploy(war);
      
      E environment = assertSingleEnvironment(WAR_NAME);
      assertExpectedClasses(environment, UIWebBean.class);
      assertExpectedWarResources(environment, unit.getSimpleName(), false, "lib.jar");
   }

   public void testWarWithoutXmlWithLibs() throws Exception
   {
      WebArchive war = createWarWithLibs(false, true, true);
      unit = assertDeploy(war);
      
      E environment = assertSingleEnvironment(WAR_NAME);
      assertExpectedClasses(environment, UIWebBean.class, CrmWebBean.class);
      assertExpectedWarResources(environment, unit.getSimpleName(), false, "lib1.jar", "lib2.jar");
   }

   public void testWarWithoutXmlWithLibWithoutXml() throws Exception
   {
      WebArchive war = createWarWithLib(false, false);
      unit = assertDeploy(war);
      
      assertEmptyEnvironment();
   }

   public void testWarWithoutXmlWithMixedLibs() throws Exception
   {
      WebArchive war = createWarWithLibs(false, true, false);
      unit = assertDeploy(war);
      
      E environment = assertSingleEnvironment(WAR_NAME);
      assertExpectedClasses(environment, UIWebBean.class);
      assertExpectedWarResources(environment, unit.getSimpleName(), false, "lib1.jar");
   }

   public void testWarWithoutXmlWithLibsWithoutXml() throws Exception
   {
      WebArchive war = createWarWithLibs(false, false, false);
      unit = assertDeploy(war);
      
      assertEmptyEnvironment();
   }

   public void testWarWithoutXml() throws Exception
   {
      WebArchive war = createWar(false);
      unit = assertDeploy(war);
      
      assertEmptyEnvironment();
   }

   public void testWarInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "warinear.ear");
      WebArchive war = createWar(true);
      ear.addModule(war);
      MockArchiveManifest.addManifest(ear);
      unit = assertDeploy(ear);
      
      E environment = assertSingleEnvironment("warinear.ear/simple.war");
      assertExpectedClasses(environment, ServletWebBean.class);
      assertExpectedWarResources(environment, "warinear.ear/simple.war", true);
   }

   public void testWarsInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "warinear.ear");
      WebArchive war = createWar("simple1.war", true, ServletWebBean.class);
      ear.addModule(war);
      war = createWar("simple2.war", true, NotWBJsfBean.class);
      ear.addModule(war);
      MockArchiveManifest.addManifest(ear);
      unit = assertDeploy(ear);
      
      assertWarsInEar();
   }

   protected abstract void assertWarsInEar();

   public void testMixedWarsInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "warinear.ear");
      WebArchive war = createWar("simple1.war", true, ServletWebBean.class);
      ear.addModule(war);
      war = createWar("simple2.war", false, NotWBJsfBean.class);
      ear.addModule(war);
      MockArchiveManifest.addManifest(ear);
      unit = assertDeploy(ear);
      
      E environment = assertSingleEnvironment("warinear.ear/simple1.war");
      assertExpectedClasses(environment, ServletWebBean.class);
      assertExpectedWarResources(environment, "warinear.ear/simple1.war", true);
   }

   public void testWarsWithoutXmlInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "warinear.ear");
      WebArchive war = createWar("simple1.war", false, ServletWebBean.class);
      ear.addModule(war);
      war = createWar("simple2.war", false, NotWBJsfBean.class);
      ear.addModule(war);
      MockArchiveManifest.addManifest(ear);
      unit = assertDeploy(ear);
      
      assertEmptyEnvironment();
   }

   public void testWarWithLibInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "warinear.ear");
      WebArchive war = createWarWithLib(true, true);
      ear.addModule(war);
      MockArchiveManifest.addManifest(ear);
      unit = assertDeploy(ear);
      
      E environment = assertSingleEnvironment("warinear.ear/" + WAR_NAME);
      assertExpectedClasses(environment, ServletWebBean.class, UIWebBean.class);
      assertExpectedWarResources(environment, "warinear.ear/" + WAR_NAME, true, "lib.jar");
   }

   public void testWarWithLibWithoutXmlInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "warinear.ear");
      WebArchive war = createWarWithLib(true, false);
      ear.addModule(war);
      MockArchiveManifest.addManifest(ear);
      unit = assertDeploy(ear);
      
      E environment = assertSingleEnvironment("warinear.ear/" + WAR_NAME);
      assertExpectedClasses(environment, ServletWebBean.class);
      assertExpectedWarResources(environment, "warinear.ear/" + WAR_NAME, true);
   }

   public void testWarWithoutXmlWithLibWithoutXmlInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "warinear.ear");
      WebArchive war = createWarWithLib(false, false);
      ear.addModule(war);
      MockArchiveManifest.addManifest(ear);
      unit = assertDeploy(ear);
      
      assertEmptyEnvironment();
   }

   public void testWarWithoutXmlWithLibInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "warinear.ear");
      WebArchive war = createWarWithLib(false, true);
      ear.addModule(war);
      MockArchiveManifest.addManifest(ear);
      unit = assertDeploy(ear);
      
      E environment = assertSingleEnvironment("warinear.ear/" + WAR_NAME);
      assertExpectedClasses(environment, UIWebBean.class);
      assertExpectedWarResources(environment, "warinear.ear/" + WAR_NAME, false, "lib.jar");
   }

   public void testBasicEar() throws Exception
   {
      VirtualFile ear = createBasicEar();
      unit = assertDeploy(ear);

      assertBasicEar();
   }

   protected abstract void assertBasicEar();

   public void testBasicEarFullCDI() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "top-level.ear");
      createLib(ear, "util.jar", true, SomeUtil.class);
      createLib(ear, "ext.jar", true, ExternalWebBean.class);
      JavaArchive ejbJar = createEjbJar("simple.jar", true, PlainJavaBean.class);
      ear.addModule(ejbJar);
      ejbJar = createEjbJar("ejbs.jar", true, MySLSBean.class, BusinessInterface.class);
      ear.addModule(ejbJar);
      WebArchive war = createWar("simple.war", true, ServletWebBean.class);
      createLib(war, "ui.jar", true, UIWebBean.class);
      ear.addModule(war);
      war = createWar("crm.war", true, NotWBJsfBean.class);
      createLib(war, "crm.jar", true, CrmWebBean.class);
      ear.addModule(war);
      unit = assertDeploy(ear);
      
      assertBasicEarFullCDI();
   }

   protected abstract void assertBasicEarFullCDI();

   public void testBasicEarWithoutXml() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "simple.ear");
      createLib(ear, "util.jar", false, SomeUtil.class);
      createLib(ear, "ext.jar", false, ExternalWebBean.class);
      JavaArchive ejbJar = createEjbJar("simple.jar", false, PlainJavaBean.class);
      ear.addModule(ejbJar);
      ejbJar = createEjbJar("ejbs.jar", false, MySLSBean.class, BusinessInterface.class);
      ear.addModule(ejbJar);
      WebArchive war = createWar("simple.war", false, ServletWebBean.class);
      createLib(war, "ui.jar", false, UIWebBean.class);
      ear.addModule(war);
      war = createWar("crm.war", false, NotWBJsfBean.class);
      createLib(war, "crm.jar", false, CrmWebBean.class);
      ear.addModule(war);
      unit = assertDeploy(ear);

      assertBasicEarWithoutXml();
   }

   private void addExpectedResource(Set<String> expected, String unit)
   {
      addExpectedResource(expected, unit, "/META-INF/beans.xml");
   }

   private void addExpectedResource(Set<String> expected, String unit, String suffix)
   {
      expected.add(unit + suffix);
   }
   
   private void assertExpectedResources(E environment, Set<String> expected)
   {
      Collection<URL> weldXml = getResources(environment);
      assertNotNull(weldXml);
      assertTrue("Unexpected empty weld XML collection", expected.isEmpty() || !weldXml.isEmpty());
      for (URL url : weldXml)
      {
         boolean found = false;
         Iterator<String> iter = expected.iterator();
         while (iter.hasNext())
         {
            String expectedURL = iter.next();
            if (url.toExternalForm().endsWith(expectedURL))
            {
               iter.remove();
               found = true;
               break;
            }
         }
         assertTrue("Unexpected wb url: " + url, found);
      }
      assertEmpty("Should be emtpy, missing " + expected, expected);
   }

   protected void assertExpectedResources(E environment, String... units)
   {
      Set<String> expected = new HashSet<String>();
      for (String unit: units)
      {
         addExpectedResource(expected, unit);
      }
      assertExpectedResources(environment, expected);
   }

   protected void assertExpectedWarResources(E environment, String warUnit, boolean warResourceExpected, String... units)
   {
      Set<String> expected = new HashSet<String>();
      if (warResourceExpected)
         addExpectedResource(expected, warUnit, "/WEB-INF/beans.xml");
      for (String unit: units)
      {
         addExpectedResource(expected, warUnit, "/WEB-INF/lib/" + unit + "/META-INF/beans.xml");
      }
      assertExpectedResources(environment, expected);
   }

   /**
    * Returns the Weld XML resources recorded in the environment.
    * 
    * @param environment the environment
    * @return the list of URLs pointing to the Weld XML files that have been found in the
    *         environment
    */
   protected abstract Collection<URL> getResources(E environment);

   protected abstract void assertBasicEarWithoutXml();

   /**
    * Asserts that there is no environment resultant from the deployment.
    */
   protected abstract void assertEmptyEnvironment();

   /**
    * Asserts that there is only one environment resultant from the deployment.
    * 
    * @param name the name of the environment
    * @return     the environment
    */
   protected abstract E assertSingleEnvironment(String name);
}
