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

import java.net.URL;

import org.jboss.test.deployers.as.support.Blue;
import org.jboss.test.deployers.as.support.Red;
import org.jboss.test.deployers.as.support.weld.simple.BlueWeldDependency;
import org.jboss.test.deployers.as.support.weld.simple.RedWeldDependency;
import org.jboss.test.deployers.as.support.weld.simple.SimpleLocalInterface;
import org.jboss.test.deployers.as.support.weld.simple.SimpleWeldBeanNoInjection;
import org.jboss.test.deployers.as.support.weld.simple.SimpleWeldBeanWeldInjection;
import org.jboss.test.deployers.as.support.weld.simple.SimpleWeldEjbNoInjection;
import org.jboss.test.deployers.as.support.weld.simple.SimpleWeldEjbWeldInjection;
import org.jboss.test.deployers.as.support.weld.simple.WeldDependency;

/**
 * A few tests to make sure that basic weld is working as expected.
 * No MC<->Weld injection here.
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class SanityApplicationServerTestCase extends AbstractWeldInAsTest
{

   public SanityApplicationServerTestCase(String name)
   {
      super(name);
   }

   public void testSimpleWeldBeanNoInjectionWar() throws Throwable
   {
      URL url = createAndDeployWar("simpleWeldBeanNoInjection", SimpleWeldBeanNoInjection.class);
      try
      {
         accessWebApp("test-weld/simpleWeldBeanNoInjection.jsf", "SimpleWeldBeanNoInjection#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
   }

   public void testSimpleWeldBeanNoInjectionEar() throws Throwable
   {
      URL url = createAndDeployEar(
            createWebArchive("simpleWeldBeanNoInjection"), 
            createWeldArchive(SimpleWeldBeanNoInjection.class));
      try
      {
         accessWebApp("test-weld/simpleWeldBeanNoInjection.jsf", "SimpleWeldBeanNoInjection#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
   }

   public void testSimpleWeldEjbNoInjectionWar() throws Throwable
   {
      URL url = createAndDeployWar("simpleWeldEjbNoInjection", 
            SimpleWeldEjbNoInjection.class, 
            SimpleLocalInterface.class);
      try
      {
         accessWebApp("test-weld/simpleWeldEjbNoInjection.jsf", "SimpleWeldEjbNoInjection#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
   }

   public void testSimpleWeldEjbNoInjectionEar() throws Throwable
   {
      URL url = createAndDeployEar(
            createWebArchive("simpleWeldEjbNoInjection"), 
            createWeldArchive(SimpleWeldEjbNoInjection.class, SimpleLocalInterface.class));
      try
      {
         accessWebApp("test-weld/simpleWeldEjbNoInjection.jsf", "SimpleWeldEjbNoInjection#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
   }

   public void testSimpleWeldBeanWeldInjectionWar() throws Throwable
   {
      URL url = createAndDeployWar("simpleWeldBeanWeldInjection", 
            SimpleWeldBeanWeldInjection.class, 
            WeldDependency.class, 
            RedWeldDependency.class, 
            BlueWeldDependency.class, 
            Red.class, 
            Blue.class);
      try
      {
         accessWebApp("test-weld/simpleWeldBeanWeldInjection.jsf", "SimpleWeldBeanWeldInjection#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
   }

   public void testSimpleWeldBeanWeldInjectionEar() throws Throwable
   {
      URL url = createAndDeployEar(
            createWebArchive("simpleWeldBeanWeldInjection"), 
            createWeldArchive(SimpleWeldBeanWeldInjection.class, WeldDependency.class, RedWeldDependency.class, BlueWeldDependency.class, Red.class, Blue.class));
      try
      {
         accessWebApp("test-weld/simpleWeldBeanWeldInjection.jsf", "SimpleWeldBeanWeldInjection#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
   }

   public void testSimpleWeldEjbWeldInjectionWar() throws Throwable
   {
      URL url = createAndDeployWar("simpleWeldEjbWeldInjection", 
            SimpleWeldEjbWeldInjection.class,
            SimpleLocalInterface.class,
            WeldDependency.class, 
            RedWeldDependency.class, 
            BlueWeldDependency.class, 
            Red.class, 
            Blue.class);
      try
      {
         accessWebApp("test-weld/simpleWeldEjbWeldInjection.jsf", "SimpleWeldBeanEjbInjection#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
   }

   public void testSimpleWeldEjbWeldInjectionEar() throws Throwable
   {
      URL url = createAndDeployEar(
            createWebArchive("simpleWeldEjbWeldInjection"), 
            createWeldArchive(SimpleWeldEjbWeldInjection.class,
                  SimpleLocalInterface.class, 
                  WeldDependency.class, 
                  RedWeldDependency.class, 
                  BlueWeldDependency.class, 
                  Red.class, 
                  Blue.class));
      try
      {
         accessWebApp("test-weld/simpleWeldEjbWeldInjection.jsf", "SimpleWeldBeanEjbInjection#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
   }

}
