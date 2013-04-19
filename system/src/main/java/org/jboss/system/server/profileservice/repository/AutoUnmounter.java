/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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
package org.jboss.system.server.profileservice.repository;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.util.automount.Automounter;

/**
 * The counterpart to {@link Automounter}, preserving a mount to the original
 * {@link VirtualFile} used by profileservice.
 * 
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision$
 */
class AutoUnmounter
{

   /** The closeables. */
   private Map<String, Closeable> mounts = new ConcurrentHashMap<String, Closeable>(); 

   /** The originals root. */
   // TODO better name: ${server.temp.dir}... 
   private VirtualFile originals = VFS.getChild("/profileservice/originals/"); 
   
   /**
    * Backup a virtual file to preserve the original location.
    * 
    * @param profileName the profile name
    * @param name the deployment name
    * @param original the original file
    * @return the backup
    * @throws IOException
    */
   VirtualFile backup(String profileName, String name, VirtualFile original) throws IOException
   {
      File realFile = original.getPhysicalFile();
      String hash = Integer.toHexString(realFile.toURI().hashCode());
      VirtualFile backup = originals.getChild(profileName).getChild(hash + realFile.getName());
      Closeable closeable = VFS.mountReal(realFile, backup);
      mounts.put(name, closeable);
      return backup;
   }
   
   /**
    * Cleanup
    * 
    * @param name
    * @throws IOException
    */
   void cleanup(String name) throws IOException
   {
      Closeable closeable = mounts.remove(name);
      if(closeable != null)
      {
         closeable.close();
      }
   }
  
}

