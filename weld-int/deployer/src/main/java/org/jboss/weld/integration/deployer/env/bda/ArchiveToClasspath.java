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
 * A Classpath adapter for an archive.
 * This is used when a single Archive is used as classpath for another Archive.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @version $Revision: 107075 $
 * @see Classpath
 */
class ArchiveToClasspath implements Classpath, ArchiveLifecycleListener
{
   // the archive
   private Archive archive;
   // the bda corresponding to archive, if available
   private Collection<BeanDeploymentArchive> bda;
   // an unitary collection containing archive
   private Collection<Archive> archives;

   public String getName()
   {
      return archive.toString();
   }

   /**
    * This method performs initialization of this classpath, setting the archive it
    * represents.
    * This classpath is not functional until this method is invoked.
    */
   public void addArchive(Archive archive)
   {
      this.archive = archive;
      archives = new ArrayList<Archive>();
      archives.add(archive);
   }

   public Iterator<Archive> iterator()
   {
      return archives.iterator();
   }
   
   public Classpath getClasspath()
   {
      return archive.getClasspath();
   }

   public Collection<BeanDeploymentArchive> getBDAs(BeanDeploymentArchive bda)
   {
      if (this.bda == null)
      {
         return archive.getClasspath().getBDAs(bda);
      }
      return this.bda;
   }

   public void archiveVisible(Archive archive, BeanDeploymentArchive bda)
   {
      this.bda = new ArrayList<BeanDeploymentArchive>();
      this.bda.add(bda);
   }

   public void archiveDestroyed(Archive archive)
   {
      // do nothing, as this instance is also being destroyed
   }
   
   public String toString()
   {
      return archive.toString();
   }
}