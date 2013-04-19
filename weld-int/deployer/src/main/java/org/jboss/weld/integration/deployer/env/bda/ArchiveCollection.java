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
package org.jboss.weld.integration.deployer.env.bda;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;

/**
 * A collection of archives.
 * This collection contains not only the archives added to it, as it also contains
 * an updated list of the bdas corresponding to those archives when available.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @version $Revision: 107075 $
 */
class ArchiveCollection implements ArchiveLifecycleListener, Iterable<Archive>
{
   // the archives collection
   protected Collection<Archive> archives = new ArrayList<Archive>();
   
   // the bdas collection
   protected Collection<BeanDeploymentArchive> bdas = new ArrayList<BeanDeploymentArchive>();

   /**
    * Adds an archive to this collection.
    * 
    * @param archive the archive to be added
    */
   public synchronized void add(Archive archive)
   {
      archives.add(archive);
      archive.addLifecycleListener(this);
   }

   /**
    * For concurrent safe iteration, synchronize the iteration block using this collection
    * as a synchronization lock.
    */
   public Iterator<Archive> iterator()
   {
      return archives.iterator();
   }

   /**
    * Returns the BeanDeploymentArchive collection corresponding to this archive collection
    *  
    * @return a collection of all previously created BeanDeploymentArchives that represent
    *         the archives in this collection
    */
   public Collection<BeanDeploymentArchive> getBDAs()
   {
      return this.bdas;
   }

   public synchronized void archiveVisible(Archive archive, BeanDeploymentArchive bda)
   {
      synchronized(bdas)
      {
         bdas.add(bda);
      }
   }

   public synchronized void archiveDestroyed(Archive archive)
   {
      synchronized(bdas)
      {
         archives.remove(archive);
         BeanDeploymentArchive bda = archive.getBeanDeploymentArchive();
         if (bda != null)
         {
            bdas.remove(bda);
         }
      }
   }
}
