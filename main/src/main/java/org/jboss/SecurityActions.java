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
package org.jboss;

import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * SecurityActions
 * 
 * Collection of tasks to be run under a security manager, 
 * package-private so we don't leak out.
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
class SecurityActions
{

   //-------------------------------------------------------------------------------||
   // Constructor ------------------------------------------------------------------||
   //-------------------------------------------------------------------------------||

   /**
    * No external instanciation
    */
   private SecurityActions()
   {

   }

   //-------------------------------------------------------------------------------||
   // Utility Methods --------------------------------------------------------------||
   //-------------------------------------------------------------------------------||

   /**
    * Sets the specified property with key and value
    */
   static void setSystemProperty(final String key, final String value)
   {
      AccessController.doPrivileged(new PrivilegedAction<Void>()
      {
         public Void run()
         {
            System.setProperty(key, value);
            return null;
         }
      });
   }

   /**
    * Obtains the system property with the specified key
    * 
    * @param key the key
    * @return system property for key
    * @throws IllegalArgumentException If the key is null
    */
   static String getSystemProperty(final String key) throws IllegalArgumentException
   {
      // Precondition check
      if (key == null)
      {
         throw new IllegalArgumentException("key was null");
      }

      // Get sysprop
      return AccessController.doPrivileged(new PrivilegedAction<String>()
      {
         public String run()
         {
            return System.getProperty(key);
         }
      });
   }

   /**
    * Obtains the Thread Context ClassLoader
    */
   static ClassLoader getThreadContextClassLoader()
   {
      return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
      {
         public ClassLoader run()
         {
            return Thread.currentThread().getContextClassLoader();
         }
      });
   }

   /**
    * Sets the specified CL upon the current Thread's Context 
    * 
    * @param cl the classloader
    * @throws IllegalArgumentException If the CL was null
    */
   static void setThreadContextClassLoader(final ClassLoader cl) throws IllegalArgumentException
   {
      if (cl == null)
      {
         throw new IllegalArgumentException("ClassLoader was null");
      }

      AccessController.doPrivileged(new PrivilegedAction<Void>()
      {
         public Void run()
         {
            Thread.currentThread().setContextClassLoader(cl);
            return null;
         }
      });
   }

   /**
    * Adds the specified shutdown hook
    * 
    * @param shutdownHook the shutdown hook
    */
   static void addShutdownHook(final Thread shutdownHook)
   {
      AccessController.doPrivileged(new PrivilegedAction<Void>()
      {
         public Void run()
         {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            return null;
         }
      });
   }

   /**
    * Set URL stream handler factory.
    *
    * @param ushf the url stream handler factory
    */
   static void setURLStreamHandlerFactory(final URLStreamHandlerFactory ushf)
   {
      AccessController.doPrivileged(new PrivilegedAction<Void>()
      {
         public Void run()
         {
            URL.setURLStreamHandlerFactory(ushf);
            return null;
         }
      });
   }
}
