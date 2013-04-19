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
package org.jboss.weld.integration.util;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.jar.Manifest;

/**
 * Get manifest info from class.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ManifestUtils
{
   /** The manifest finder abstraction */
   private static ManifestFinder finder;

   static
   {
      try
      {
         Class<?> vfsFinderClass = Class.forName("org.jboss.weld.integration.util.VFSManifestFinder");
         finder = (ManifestFinder)vfsFinderClass.newInstance();
      }
      catch (Exception e)
      {
         finder = new JDKManifestFinder();
      }
   }

   /**
    * Get manifest for class.
    *
    * @param clazz the class to check
    * @return manifest if found, else null
    * @throws Exception for any error
    */
   public static Manifest getManifest(final Class<?> clazz) throws Exception
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
         return AccessController.doPrivileged(new PrivilegedExceptionAction<Manifest>()
         {
            public Manifest run() throws Exception
            {
               return finder.findManifest(clazz);
            }
         });
      else
         return finder.findManifest(clazz);
   }
}