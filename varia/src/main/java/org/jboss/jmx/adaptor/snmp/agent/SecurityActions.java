/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.jmx.adaptor.snmp.agent;

import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * SecurityActions
 * 
 * Package-private class to encapsulate secure methods.
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
class SecurityActions
{

   //-------------------------------------------------------------------------------------||
   // Constructor ------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * No instantiation
    */
   private SecurityActions()
   {
      throw new UnsupportedOperationException("No instances permitted");
   }

   //-------------------------------------------------------------------------------------||
   // Functional Methods -----------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Returns an {@link InputStream} to the ClassLoadder resource
    * (using the TCCL) denoted by the specified name
    * 
    * @param resourceName
    * @throws IllegalArgumentException If the specified name could not be found/loaded by the TCCL
    */
   static InputStream getThreadContextClassLoaderResource(final String resourceName) throws IllegalArgumentException
   {
      final ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
      {
         public ClassLoader run()
         {
            return Thread.currentThread().getContextClassLoader();
         }
      });
      final InputStream in = cl.getResourceAsStream(resourceName);
      if (in == null)
      {
         throw new IllegalArgumentException("Cannot locate classloader resource \"" + resourceName + "\" using " + cl);
      }
      return in;
   }

}
