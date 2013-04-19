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
package org.jboss.test.deployers.support;

import java.util.Map;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 *
 * @version $Revision: 107075 $
 */
public class MockArchiveManifest
{
   private static Filter<ArchivePath> EAR_MODULE_FILTER = new Filter<ArchivePath>(){

      public boolean include(ArchivePath pathObject)
      {
         String path = pathObject.get();
         // ear modules are not "/" and don't belong to /lib
         return path.length() > 1 && !path.startsWith("/lib");
      }
   };
   
   public static void addCDIManifest(JavaArchive archive)
   {
      archive.addManifestResource(new ByteArrayAsset("<web-beans></web-beans>".getBytes()), 
               ArchivePaths.create("beans.xml"));
   }
   
   public static void addCDIManifest(WebArchive archive)
   {
      archive.add(new ByteArrayAsset("<beans/>".getBytes()), 
               ArchivePaths.create("WEB-INF/beans.xml"));
   }
   
   public static void addManifest(JavaArchive archive)
   {
      archive.addManifestResource(new ByteArrayAsset("<ejb-jar/>".getBytes()), 
               ArchivePaths.create("ejb-jar.xml"));
   }
   
   public static void addManifest(JavaArchive archive, boolean isCDI)
   {
      addManifest(archive);
      if (isCDI)
      {
         addCDIManifest(archive);
      }
   }
   
   public static void addManifest(WebArchive archive)
   {
      archive.add(new ByteArrayAsset("<web/>".getBytes()), ArchivePaths.create("WEB-INF/web.xml"));
   }

   public static void addManifest(WebArchive archive, boolean isCDI)
   {
      addManifest(archive);
      if (isCDI)
      {
         addCDIManifest(archive);
      }
   }
   
   public static void addManifest(EnterpriseArchive archive)
   {
      Map<ArchivePath, Node> modules = archive.getContent(EAR_MODULE_FILTER);
      StringBuffer appProperties = new StringBuffer();
      for(ArchivePath archivePath: modules.keySet())
      {
         // remove leading '/' char
         String path = archivePath.get().substring(1);
         appProperties.append(path.replace('.', '_')).append("-module=").append(path);
         appProperties.append('\n');
      }
      archive.addManifestResource(new ByteArrayAsset(appProperties.toString().getBytes()),
               "application.properties");
   }
}
