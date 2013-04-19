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
package org.jboss.system.deployers;

import java.util.Set;

import org.jboss.deployers.vfs.spi.deployer.ArchiveMatcher;
import org.jboss.vfs.VirtualFile;

/**
 * Exclude non-archives based on suffix or prefix.
 *
 * @author ales.justin@jboss.org
 */
public class ExcludeArchiveMatcher implements ArchiveMatcher
{
   private Set<String> suffixes;
   private Set<String> prefixes;

   public boolean isArchive(VirtualFile file)
   {
      return hasArchiveSuffix(file);
   }

   public boolean hasArchiveSuffix(VirtualFile file)
   {
      return hasArchiveSuffix(file.getName());
   }

   public boolean hasArchiveSuffix(String fileName)
   {
      if (suffixes != null)
      {
         for (String suffix : suffixes)
            if (fileName.startsWith(suffix))
               return false;
      }
      if (prefixes != null)
      {
         for (String prefix : prefixes)
            if (fileName.endsWith(prefix))
               return false;
      }
      return true;
   }

   public void setSuffixes(Set<String> suffixes)
   {
      this.suffixes = suffixes;
   }

   public void setPrefixes(Set<String> prefixes)
   {
      this.prefixes = prefixes;
   }
}