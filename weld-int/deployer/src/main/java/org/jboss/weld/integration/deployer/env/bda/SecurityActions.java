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
package org.jboss.weld.integration.deployer.env.bda;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.classloader.spi.base.BaseClassLoaderSource;
import org.jboss.classloading.spi.dependency.ClassLoading;
import org.jboss.classloading.spi.dependency.Module;

/**
 * Executes privileged actions.
 * 
 * @author <a href="flavia.rainone@jboss.com">Flavia Rainone</a>
 * @version $Revision: 108102 $
 */
class SecurityActions
{
   private static final Method getClassLoader;

   static
   {
      getClassLoader = AccessController.doPrivileged(new PrivilegedAction<Method>()
      {
         public Method run()
         {
            try
            {
               Method method = BaseClassLoaderSource.class.getDeclaredMethod("getClassLoader");
               method.setAccessible(true);
               return method;
            }
            catch (NoSuchMethodException e)
            {
               throw new RuntimeException("Cannot get classloader from " + BaseClassLoaderSource.class.getName(), e);
            }
         }
      });
   }

   static ClassLoader getClassLoader(BaseClassLoaderSource clSource)
   {
      try
      {
         return (ClassLoader)getClassLoader.invoke(clSource);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public static ClassLoader getClassLoader(final Class<?> clazz)
   {
      return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
      {
         public ClassLoader run()
         {
            return clazz.getClassLoader();
         }
      });
   }
   
   public static Module getModuleForClassLoader(final ClassLoader classLoader)
   {
      return AccessController.doPrivileged(new PrivilegedAction<Module>()
      {
         public Module run()
         {
            return ClassLoading.getModuleForClassLoader(classLoader);
         }
      });
   }

   public static ClassLoader getClassLoaderForModule(final Module module)
   {
      return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
      {
         public ClassLoader run()
         {
            return ClassLoading.getClassLoaderForModule(module);
         }
      });
   }
}
