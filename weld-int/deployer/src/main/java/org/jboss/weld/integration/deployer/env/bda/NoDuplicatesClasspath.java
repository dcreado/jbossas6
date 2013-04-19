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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;

/**
 * Classpath implementation that avoids duplication and keeps an updated list of
 * BeanDeploymentArchives, thus avoiding the creation of this list every time it is
 * retrieved.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @version $Revision: 108099 $
 */
class NoDuplicatesClasspath extends ArchiveCollection implements Classpath 
{
   // the classpath name
   private final String name;

   // A reference to this classpath. This reference BDA is returned as the single element
   // in the BDA collection every time it is requested by the non-reference BDAs
   private BeanDeploymentArchive reference = null;

   // A collection containing the reference BDA
   private Collection<BeanDeploymentArchive> referenceCollection = new ArrayList<BeanDeploymentArchive>();

   // contains archives and BDAs reachable from this Classpath
   private Classpath classpath;

   /**
    * Constructor.
    * 
    * @param name     a name that identifies this classpath
    * @param archives the list of archives contained in this classpath
    */
   public NoDuplicatesClasspath (String name, Archive... archives)
   {
      this.name = name;
      super.archives = new HashSet<Archive>();
      for (Archive archive: archives)
      {
         add(archive);
      }
   }

   /**
    * Constructor.
    * 
    * @param name     a name that identifies this classpath
    * @param archives the list of archives contained in this classpath
    */
   public NoDuplicatesClasspath (String name, Classpath classpath, Archive... archives)
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
      add(archive);
   }

   public Classpath getClasspath()
   {
      return this.classpath;
   }

   public Collection<BeanDeploymentArchive> getBDAs(BeanDeploymentArchive bda)
   {
      // if reference is null, it means that no BDA is contained in this classpath
      if (reference == null && classpath != null)
      {
         return classpath.getBDAs(bda);
      }
      // only the reference BDA points to the other BDAs in this classpath
      if (bda == reference)
      {
         Collection<BeanDeploymentArchive> otherBDAs= getBDAs();
      
         if (otherBDAs.isEmpty())
         {
            if (classpath != null)
               return classpath.getBDAs(bda);
            return Collections.emptyList();
         }
         else
         {
            Collection<BeanDeploymentArchive> allBDAs = new ArrayList<BeanDeploymentArchive>();
            allBDAs.addAll(otherBDAs);
            allBDAs.addAll(classpath.getBDAs(bda));
            return allBDAs;
         }
      }
      else
      {
         // all other BDAs point only to reference BDA
         return referenceCollection;
      }
   }

   @Override
   public void archiveVisible(Archive archive, BeanDeploymentArchive bda)
   {
      synchronized(bdas)
      {
         if (reference == null)
         {
            reference = bda;
            referenceCollection.add(bda);
         }
         else
         {
            //only add to BDAs collection the BDAs that are not reference
            super.archiveVisible(archive, bda);
         }
      }
   }
   
   @Override
   public void archiveDestroyed(Archive archive)
   {
      BeanDeploymentArchive bda = archive.getBeanDeploymentArchive();
      if (bda != null)
      {
         synchronized(bdas)
         {
            if (reference == bda)
            {
               reference = null;
               referenceCollection.clear();
               if (!bdas.isEmpty())
               {
                  Iterator<BeanDeploymentArchive> iterator = bdas.iterator();
                  reference = iterator.next();
                  iterator.remove();
                  referenceCollection.add(reference);
               }
               synchronized(this)
               {
                  archives.remove(archive);
               }
            }
            else
            {
               super.archiveDestroyed(archive);
            }
         }
      }
   }

   public String toString()
   {
      return "Classpath[" + this.name + "]";
   }
}