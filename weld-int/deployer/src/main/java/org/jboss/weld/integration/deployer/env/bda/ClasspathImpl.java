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
 * Naïve implementation of Classpath. This implementation retrieves the
 * BeanDeploymentArchives from the contained archives whenever the BDA collection is
 * requested, and allows for duplicates in the collection.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @version $Revision: 107075 $
 */
class ClasspathImpl implements Classpath, ArchiveLifecycleListener
{
   private Collection<Archive> archives = new ArrayList<Archive>();
   private Classpath classpath;
   private final String name;
   
   /**
    * Constructor.
    * 
    * @param name     the name of this classpath
    * @param archives the list of archives contained in this classpath
    */
   public ClasspathImpl (String name, Archive... archives)
   {
      this.name = name;
      for (Archive archive: archives)
      {
         this.archives.add(archive);
      }
   }
   
   /**
    * Constructor.
    * 
    * @param name      the name of this classpath
    * @param classpath contain archives that are reachable from this classpath
    * @param archives the list of archives contained in this classpath
    */
   public ClasspathImpl (String name, Classpath classpath, Archive... archives)
   {
      this(name, archives);
      this.classpath = classpath;
   }
   
   public String getName()
   {
      return this.name;
   }

   public void addArchive(Archive archive)
   {
      archives.add(archive);
      archive.addLifecycleListener(this);
   }

   public Iterator<Archive> iterator()
   {
      return archives.iterator();
   }
   
   public Classpath getClasspath()
   {
      return this.classpath;
   }

   public Collection<BeanDeploymentArchive> getBDAs(BeanDeploymentArchive bda)
   {
      if (archives.isEmpty() && classpath != null)
      {
         return classpath.getBDAs(bda);
      }
      Collection<BeanDeploymentArchive> bdas = getBDAsFromArchives();
      if (bdas.isEmpty() && classpath != null)
      {
         return classpath.getBDAs(bda);
      }
      if (classpath != null)
      {
         bdas.addAll(classpath.getBDAs(bda));
      }
      return bdas;
   }

   public String toString()
   {
      return "Classpath[" + name + "]";
   }

   private Collection<BeanDeploymentArchive> getBDAsFromArchives()
   {
      Collection<BeanDeploymentArchive> bdas = new ArrayList<BeanDeploymentArchive>();
      for (Archive archive: archives)
      {
         BeanDeploymentArchive bda = archive.getBeanDeploymentArchive();
         if (bda != null)
         {
            bdas.add(bda);
         }
      }
      return bdas;
   }

   public void archiveVisible(Archive archive, BeanDeploymentArchive bda)
   {
      // do nothing, as this classpath impl doesn't keep track of bdas created
   }

   public void archiveDestroyed(Archive archive)
   {
      archives.remove(archive);
   }
}
