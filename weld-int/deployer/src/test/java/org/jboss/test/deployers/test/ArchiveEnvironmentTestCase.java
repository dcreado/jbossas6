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

import junit.framework.Test;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.test.deployers.support.crm.CrmWebBean;
import org.jboss.test.deployers.support.ejb.BusinessInterface;
import org.jboss.test.deployers.support.ejb.MySLSBean;
import org.jboss.test.deployers.support.ext.ExternalWebBean;
import org.jboss.test.deployers.support.jar.PlainJavaBean;
import org.jboss.test.deployers.support.jsf.NotWBJsfBean;
import org.jboss.test.deployers.support.ui.UIWebBean;
import org.jboss.test.deployers.support.util.SomeUtil;
import org.jboss.test.deployers.support.web.ServletWebBean;
import org.jboss.util.UnreachableStatementException;
import org.jboss.weld.integration.deployer.env.WeldDiscoveryEnvironment;
import org.jboss.weld.integration.deployer.env.bda.ArchiveInfo;

/**
 * ArchiveInfo environment discovery test case.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ArchiveEnvironmentTestCase extends AbstractSingleArchiveTest<WeldDiscoveryEnvironment>
{
   public ArchiveEnvironmentTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(ArchiveEnvironmentTestCase.class);
   }

   @Override
   public void testEjbJarInEar() throws Exception
   {
      super.testEjbJarInEar();

      assertSingleChildAsUnit(EJB_JAR_NAME);
      assertNoArchiveInfo(unit);
   }

   @Override
   public void testEjbJarWithoutXmlInEar() throws Exception
   {
      super.testEjbJarWithoutXmlInEar();

      assertSingleChildAsUnit(EJB_JAR_NAME);
      assertNoArchiveInfo(unit);
   }

   @Override
   protected void assertWarsInEar()
   {
      WeldDiscoveryEnvironment discovery = assertDiscoveryEnvironment();
      assertExpectedClasses(discovery);
      assertExpectedResources(discovery);
      
      for (DeploymentUnit childUnit: unit.getChildren())
      {
         unit = childUnit;
         if (unit.getSimpleName().equals("simple1.war"))
         {
            discovery = assertDiscoveryEnvironment();
            assertExpectedClasses(discovery, ServletWebBean.class);
            assertExpectedWarResources(discovery, "warinear.ear/simple1.war", true);
         }
         else if (unit.getSimpleName().equals("simple2.war"))
         {
            discovery = assertDiscoveryEnvironment();
            assertExpectedClasses(discovery, NotWBJsfBean.class);
            assertExpectedWarResources(discovery, "warinear.ear/simple2.war", true);
         }
         else
         {
            fail("Unexpected childUnit: " + unit.getSimpleName());
         }
      }
   }

   @Override
   protected void assertBasicEar()
   {
      WeldDiscoveryEnvironment discovery = assertDiscoveryEnvironment();
      assertExpectedClasses(discovery, BusinessInterface.class, MySLSBean.class,
               ExternalWebBean.class, PlainJavaBean.class);
      assertExpectedResources(discovery, "top-level.ear/ejbs.jar",
               "top-level.ear/lib/ext.jar", "top-level.ear/simple.jar");

      for (DeploymentUnit childUnit: unit.getChildren())
      {
         unit = childUnit;
         if (childUnit.getName().endsWith("simple.war/"))
         {
            discovery = assertDiscoveryEnvironment();
            assertExpectedClasses(discovery, UIWebBean.class, ServletWebBean.class);
            assertExpectedWarResources(discovery, "top-level.ear/simple.war", true, "ui.jar");
         }
         else if (childUnit.getName().endsWith("crm.war/"))
         {
            discovery = assertDiscoveryEnvironment();
            assertExpectedClasses(discovery, CrmWebBean.class);
            assertExpectedWarResources(discovery, "top-level.ear/crm.war", false, "crm.jar");
         }
         else
         {
            ArchiveInfo archiveInfo = childUnit.getAttachment(ArchiveInfo.class);
            assertNull("Unit " + unit.getName() +  " contains a not null ArchiveInfo", archiveInfo);
         }
      }
   }

   @Override
   protected void assertBasicEarFullCDI()
   {
      WeldDiscoveryEnvironment discovery = assertDiscoveryEnvironment();
      assertExpectedClasses(discovery, BusinessInterface.class, MySLSBean.class,
               ExternalWebBean.class, PlainJavaBean.class, SomeUtil.class);
      assertExpectedResources(discovery, "top-level.ear/lib/util.jar",
               "top-level.ear/lib/ext.jar", "top-level.ear/simple.jar",
               "top-level.ear/ejbs.jar");

      for (DeploymentUnit childUnit: unit.getChildren())
      {
         unit = childUnit;
         if (childUnit.getName().endsWith("simple.war/"))
         {
            discovery = assertDiscoveryEnvironment();
            assertExpectedClasses(discovery, UIWebBean.class, ServletWebBean.class);
            assertExpectedWarResources(discovery, "top-level.ear/simple.war", true, "ui.jar");
         }
         else if (childUnit.getName().endsWith("crm.war/"))
         {
            discovery = assertDiscoveryEnvironment();
            assertExpectedClasses(discovery, CrmWebBean.class, NotWBJsfBean.class);
            assertExpectedWarResources(discovery, "top-level.ear/crm.war", true, "crm.jar");
         }
         else
         {
            ArchiveInfo archiveInfo = childUnit.getAttachment(ArchiveInfo.class);
            assertNull("Unit " + unit.getName() +  " contains a not null ArchiveInfo", archiveInfo);
         }
      }
   }

   @Override
   protected void assertBasicEarWithoutXml()
   {
      WeldDiscoveryEnvironment discovery = assertDiscoveryEnvironment();
      
      assertExpectedClasses(discovery);
      assertExpectedResources(discovery);

      for (DeploymentUnit childUnit: unit.getChildren())
      {
         if (childUnit.getName().endsWith("simple.war/") ||
                  childUnit.getName().endsWith("crm.war/"))
         {
            unit = childUnit;
            discovery = assertDiscoveryEnvironment();
            assertExpectedClasses(discovery);
            assertExpectedResources(discovery);
         }
         else
         {
            ArchiveInfo archiveInfo = childUnit.getAttachment(ArchiveInfo.class);
            assertNull("Unit " + unit.getName() +  " contains a not null ArchiveInfo", archiveInfo);
         }
      }
   }

   @Override
   protected void assertEmptyEnvironment()
   {
      WeldDiscoveryEnvironment discovery = assertDiscoveryEnvironment();
      assertExpectedClasses(discovery);
      assertExpectedResources(discovery);
   }

   @Override
   protected WeldDiscoveryEnvironment assertSingleEnvironment(String name)
   {
      if (unit.getName().contains(name))
      {
         return assertDiscoveryEnvironment(name);
      }
      for (DeploymentUnit childUnit : unit.getChildren())
      {
         if (childUnit.getName().contains(name))
         {
            return assertDiscoveryEnvironment(childUnit, name);
         }
      }
      fail("Was not able to find a unit with suffix " + name);
      throw new UnreachableStatementException();
   }

   @Override
   protected Collection<String> getClasses(
            WeldDiscoveryEnvironment environment)
   {
      return environment.getWeldClasses();
   }

   @Override
   protected Collection<URL> getResources(WeldDiscoveryEnvironment environment)
   {
      return environment.getWeldXml();
   }

   private void assertSingleChildAsUnit(String name)
   {
      assertEquals(1, unit.getChildren().size());
      unit = unit.getChildren().iterator().next();
      assertEquals(unit.getSimpleName(), name);
   }

   private void assertNoArchiveInfo(DeploymentUnit unit)
   {
      assertNull(unit.getAttachment(ArchiveInfo.class));
   }

   private WeldDiscoveryEnvironment assertDiscoveryEnvironment(String name)
   {
      return assertDiscoveryEnvironment(unit, name);
   }

   private WeldDiscoveryEnvironment assertDiscoveryEnvironment(DeploymentUnit unit, String name)
   {
      ArchiveInfo archiveInfo = unit.getAttachment(ArchiveInfo.class);
      assertNotNull(archiveInfo);
      assertTrue("Unit name \"" + unit.getName() + "\" expected to end with \"" + name +
               "\" suffix", unit.getName().endsWith(name + '/'));
      WeldDiscoveryEnvironment discovery = archiveInfo.getEnvironment();
      assertNotNull(discovery);
      return discovery;
   }

   private WeldDiscoveryEnvironment assertDiscoveryEnvironment()
   {
      ArchiveInfo archiveInfo = unit.getAttachment(ArchiveInfo.class);
      assertNotNull(archiveInfo);
      WeldDiscoveryEnvironment discovery = archiveInfo.getEnvironment();
      assertNotNull(discovery);
      return discovery;
   }
}
