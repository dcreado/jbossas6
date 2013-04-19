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

package org.jboss.weld.integration.util;

import javax.management.ObjectName;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.bootstrap.api.as.config.JBossASBasedServerConfig;
import org.jboss.classloading.spi.RealClassLoader;

/**
 * Try creating id the best way we can.
 * 
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public final class IdFactory
{
   private static String serverHome;

   static
   {
      String home;
      SecurityManager sm = System.getSecurityManager();
      if (sm == null)
         home = System.getProperty(JBossASBasedServerConfig.PROP_KEY_JBOSSAS_SERVER_HOME_URL);
      else
      {
         home = AccessController.doPrivileged(new PrivilegedAction<String>()
         {
            public String run()
            {
               return System.getProperty(JBossASBasedServerConfig.PROP_KEY_JBOSSAS_SERVER_HOME_URL);
            }
         });
      }
      if (home != null && home.startsWith("file:"))
         home = home.substring("file:".length());

      serverHome = home;
   }

   private IdFactory()
   {
   }

   /**
    * Get the id from classloader -- let's try RealClassLoader's objectName.
    * 
    * @param cl the classloader
    * @return classloader's unique id
    */
   public static String getIdFromClassLoader(ClassLoader cl)
   {
      if (cl == null)
         throw new IllegalArgumentException("Null classloader.");

      ClassLoader current = cl;
      while (current != null && (current instanceof RealClassLoader == false))
      {
         current = current.getParent();
      }
      if (current != null)
      {
         ObjectName on = ((RealClassLoader) current).getObjectName();
         if (on != null)
         {
            String canonical = on.getCanonicalName();
            if (serverHome != null)
               return canonical.replace(serverHome, "-JBossAS-");
            else
               return canonical;
         }
      }
      return cl.toString(); // the best we can do :-(
   }
}
