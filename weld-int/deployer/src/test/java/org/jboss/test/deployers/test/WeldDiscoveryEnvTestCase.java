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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.Test;

import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.test.deployers.support.crm.CrmWebBean;
import org.jboss.test.deployers.support.ejb.BusinessInterface;
import org.jboss.test.deployers.support.ejb.MySLSBean;
import org.jboss.test.deployers.support.ext.ExternalWebBean;
import org.jboss.test.deployers.support.jar.PlainJavaBean;
import org.jboss.test.deployers.support.ui.UIWebBean;
import org.jboss.test.deployers.support.web.ServletWebBean;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.deployer.env.WeldDiscoveryEnvironment;

/**
 * WeldDiscovery env test case.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class WeldDiscoveryEnvTestCase extends AbstractWeldTest
{
   public WeldDiscoveryEnvTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(WeldDiscoveryEnvTestCase.class);
   }

   public void testSimpleUsage() throws Exception
   {
      VirtualFile ear = createBasicEar();
      VFSDeploymentUnit topUnit = assertDeploy(ear);
      try
      {
         WeldDiscoveryEnvironment wbDiscovery = topUnit.getAttachment(WeldDiscoveryEnvironment.class);
         assertNotNull("Null WBDiscoveryEnv.", wbDiscovery);

         Set<String> expected = new HashSet<String>();
         addExpectedResource(expected, "ejbs.jar");
         addExpectedResource(expected, "ext.jar");
         addExpectedResource(expected, "simple.jar");
         addExpectedResource(expected, "ui.jar");
         addExpectedResource(expected, "crm.jar");
         addExpectedResource(expected, "simple.war", "/WEB-INF/beans.xml");

         assertNotNull(wbDiscovery);

         for (URL url : wbDiscovery.getWeldXml())
         {
            boolean found = false;
            Iterator<String> iter = expected.iterator();
            while (iter.hasNext())
            {
               String expectedURL = iter.next();
               if (url.toExternalForm().contains(expectedURL))
               {
                  iter.remove();
                  found = true;
                  break;
               }
            }
            assertTrue("Unexpected wb url: " + url, found);
         }

         assertEmpty("Should be emtpy, missing " + expected, expected);

         addExpectedClass(expected, BusinessInterface.class);
         addExpectedClass(expected, MySLSBean.class);
         addExpectedClass(expected, ExternalWebBean.class);
         addExpectedClass(expected, PlainJavaBean.class);
         addExpectedClass(expected, UIWebBean.class);
         addExpectedClass(expected, ServletWebBean.class);
         addExpectedClass(expected, CrmWebBean.class);

         for (String className : wbDiscovery.getWeldClasses())
            assertTrue(expected.remove(className));

         assertEmpty("Should be emtpy, missing " + expected, expected);
      }
      finally
      {
         undeploy(topUnit);
      }
   }

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