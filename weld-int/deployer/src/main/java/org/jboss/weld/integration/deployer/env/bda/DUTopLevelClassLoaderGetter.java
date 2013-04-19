/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.weld.integration.deployer.env.bda;

import org.jboss.classloading.spi.RealClassLoader;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.deployers.plugins.classloading.AbstractDeploymentClassLoaderPolicyModule;
import org.jboss.deployers.structure.spi.DeploymentUnit;

/**
 * Use deployment unit to get top level classloader.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class DUTopLevelClassLoaderGetter implements TopLevelClassLoaderGetter
{
   /** The singleton instance */
   public static final TopLevelClassLoaderGetter INSTANCE = new DUTopLevelClassLoaderGetter();

   /**
    * Get instance.
    *
    * @return the instance
    */
   public static TopLevelClassLoaderGetter getInstance()
   {
      return INSTANCE;
   }

   public ClassLoader getTopLevelClassLoader(ClassLoader cl)
   {
      if (cl == null)
         throw new IllegalArgumentException("Null classloader");

      // start at the first BaseClassLoader parent,
      ClassLoader firstBaseClassLoader = cl;
      while (firstBaseClassLoader != null && !(firstBaseClassLoader instanceof RealClassLoader))
      {
         firstBaseClassLoader = firstBaseClassLoader.getParent();
      }

      if (firstBaseClassLoader == null)
      {
         // no BaseClassLoader found, perhaps this is top-level
         return cl;
      }

      Module module = SecurityActions.getModuleForClassLoader(firstBaseClassLoader);
      if (module == null || (module instanceof AbstractDeploymentClassLoaderPolicyModule == false))
      {
         return cl;
      }

      AbstractDeploymentClassLoaderPolicyModule abclm = (AbstractDeploymentClassLoaderPolicyModule) module;
      DeploymentUnit du = abclm.getDeploymentUnit();
      return du.getTopLevel().getClassLoader();
   }
}
