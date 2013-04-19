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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Find manifest info from class - plain JDK.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class JDKManifestFinder extends AbstractManifestFinder
{
   protected Manifest findManifest(URL url) throws Exception
   {
      URLConnection conn = url.openConnection();
      if (conn instanceof JarURLConnection)
      {
         JarURLConnection jarConn = (JarURLConnection)conn;
         return jarConn.getManifest();
      }
      else
      {
         File parent = new File(url.toURI());
         File child = new File(parent, JarFile.MANIFEST_NAME);
         if (child.exists())
         {
            InputStream fis = new FileInputStream(child);
            try
            {
               return new Manifest(fis);
            }
            finally
            {
               close(fis);
            }
         }
         else
         {
            return null;
         }
      }
   }

   private static void close(InputStream is)
   {
      try
      {
         is.close();
      }
      catch (IOException ignored)
      {
      }
   }
}