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
import org.jboss.test.deployers.as.support.mctoweld.mc.BlueMcBeanNoDependencies;
import org.jboss.test.deployers.as.support.mctoweld.mc.McBeanNoDependencies;
import org.jboss.test.deployers.as.support.mctoweld.mc.RedMcBeanNoDependencies;
import org.jboss.test.deployers.as.support.mctoweld.weld.McToWeldLocalInterface;
import org.jboss.test.deployers.as.support.mctoweld.weld.WeldWithMcBean;
import org.jboss.test.deployers.as.support.mctoweld.weld.WeldWithMcEjb;
import org.jboss.test.deployers.as.support.weldtomctoweld.mc.BlueMcBeanWeldDependencies;
import org.jboss.test.deployers.as.support.weldtomctoweld.mc.McBeanWeldDependencies;
import org.jboss.test.deployers.as.support.weldtomctoweld.mc.RedMcBeanWeldDependencies;
import org.jboss.test.deployers.as.support.weldtomctoweld.weld.BlueWeldMcDependency;
import org.jboss.test.deployers.as.support.weldtomctoweld.weld.RedWeldMcDependency;
import org.jboss.test.deployers.as.support.weldtomctoweld.weld.WeldMcDependency;
import org.jboss.test.deployers.as.support.weldtomctoweld.weld.WeldToMcToWeldLocalInterface;
import org.jboss.test.deployers.as.support.weldtomctoweld.weld.WeldWithMcWithWeldBean;
import org.jboss.test.deployers.as.support.weldtomctoweld.weld.WeldWithMcWithWeldEjb;


/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class McWeldInjectionApplicationServerTestCase extends AbstractWeldInAsTest
{
   public McWeldInjectionApplicationServerTestCase(String name)
   {
      super(name);
   }

   public void testMcNoDependenciesInjectedIntoWeldBeanWar() throws Throwable
   {
      URL url = createAndDeployWar("mcNoDependenciesInjectedIntoWeldBean",
            "mctoweld/nodependencies",
            WeldWithMcBean.class, 
            Red.class, 
            Blue.class,
            McBeanNoDependencies.class, 
            RedMcBeanNoDependencies.class, 
            BlueMcBeanNoDependencies.class);

      try
      {
         accessWebApp("test-weld/mcNoDependenciesInjectedIntoWeldBean.jsf", "WeldWithMcBean#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
   }
   
   public void testMcNoDependenciesInjectedIntoWeldBeanEar() throws Throwable
   {
      URL url = createAndDeployEar(
            createWebArchive("mcNoDependenciesInjectedIntoWeldBean"),
            createWeldArchive(WeldWithMcBean.class),
            createMcArchive("mctoweld/nodependencies", Red.class, Blue.class, McBeanNoDependencies.class, RedMcBeanNoDependencies.class, BlueMcBeanNoDependencies.class));

      try
      {
         accessWebApp("test-weld/mcNoDependenciesInjectedIntoWeldBean.jsf", "WeldWithMcBean#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
   }

   public void testMcNoDependenciesInjectedIntoWeldEjbWar() throws Throwable
   {
      URL url = createAndDeployWar("mcNoDependenciesInjectedIntoWeldEjb",
            "mctoweld/nodependencies",
            WeldWithMcEjb.class, 
            McToWeldLocalInterface.class, 
            Red.class, 
            Blue.class,
            McBeanNoDependencies.class, 
            RedMcBeanNoDependencies.class, 
            BlueMcBeanNoDependencies.class);

      try
      {
         accessWebApp("test-weld/mcNoDependenciesInjectedIntoWeldEjb.jsf", "WeldWithMcEjb#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
   }
   
   public void testMcNoDependenciesInjectedIntoWeldEjbEar() throws Throwable
   {
      URL url = createAndDeployEar(
            createWebArchive("mcNoDependenciesInjectedIntoWeldEjb"),
            createWeldArchive(WeldWithMcEjb.class, McToWeldLocalInterface.class, Red.class, Blue.class),
            createMcArchive("mctoweld/nodependencies", McBeanNoDependencies.class, RedMcBeanNoDependencies.class, BlueMcBeanNoDependencies.class));

      try
      {
         accessWebApp("test-weld/mcNoDependenciesInjectedIntoWeldEjb.jsf", "WeldWithMcEjb#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
   }

   public void testWeldInjectedIntoMcInjectedIntoWeldBeanWar() throws Throwable
   {
      URL url = createAndDeployWar("weldInjectedIntoMcInjectedIntoWeldBean",
            "weldtomctoweld",
            WeldWithMcWithWeldBean.class,
            WeldMcDependency.class,
            RedWeldMcDependency.class,
            BlueWeldMcDependency.class,
            Red.class, 
            Blue.class,
            McBeanWeldDependencies.class, 
            RedMcBeanWeldDependencies.class, 
            BlueMcBeanWeldDependencies.class);

      try
      {
         accessWebApp("test-weld/weldInjectedIntoMcInjectedIntoWeldBean.jsf", "WeldWithMcWithWeldBean#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
   }
   
   public void testWeldInjectedIntoMcInjectedIntoWeldBeanEar() throws Throwable
   {
      URL url = createAndDeployEar(
            createWebArchive("weldInjectedIntoMcInjectedIntoWeldBean"),
            createWeldArchive(WeldWithMcWithWeldBean.class,
                  WeldMcDependency.class,
                  RedWeldMcDependency.class,
                  BlueWeldMcDependency.class), 
            createMcArchive("weldtomctoweld", 
                  Red.class, 
                  Blue.class, 
                  McBeanWeldDependencies.class, 
                  RedMcBeanWeldDependencies.class, 
                  BlueMcBeanWeldDependencies.class));

      try
      {
         accessWebApp("test-weld/weldInjectedIntoMcInjectedIntoWeldBean.jsf", "WeldWithMcWithWeldBean#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
   }

      
   public void testWeldInjectedIntoMcInjectedIntoWeldEjbWar() throws Throwable
   {
      URL url = createAndDeployWar("weldInjectedIntoMcInjectedIntoWeldEjb",
            "weldtomctoweld",
            WeldWithMcWithWeldEjb.class,
            WeldToMcToWeldLocalInterface.class,
            WeldMcDependency.class,
            RedWeldMcDependency.class,
            BlueWeldMcDependency.class,
            Red.class, 
            Blue.class,
            McBeanWeldDependencies.class, 
            RedMcBeanWeldDependencies.class, 
            BlueMcBeanWeldDependencies.class);

      try
      {
         accessWebApp("test-weld/weldInjectedIntoMcInjectedIntoWeldEjb.jsf", "WeldWithMcWithWeldEjb#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
      
   }
   
   public void testWeldInjectedIntoMcInjectedIntoWeldEjbEar() throws Throwable
   {
      URL url = createAndDeployEar(
            createWebArchive("weldInjectedIntoMcInjectedIntoWeldEjb"),
            createWeldArchive(WeldWithMcWithWeldEjb.class,
                  WeldToMcToWeldLocalInterface.class,
                  WeldMcDependency.class,
                  RedWeldMcDependency.class,
                  BlueWeldMcDependency.class), 
            createMcArchive("weldtomctoweld", 
                  Red.class, 
                  Blue.class, 
                  McBeanWeldDependencies.class, 
                  RedMcBeanWeldDependencies.class, 
                  BlueMcBeanWeldDependencies.class));

      try
      {
         accessWebApp("test-weld/weldInjectedIntoMcInjectedIntoWeldEjb.jsf", "WeldWithMcWithWeldEjb#ok#");
      }
      finally
      {
         if (url != null)
            undeploy(url.toString());
      }
      
   }
}
