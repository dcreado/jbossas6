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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.jboss.weld.ejb.spi.EjbDescriptor;

/**
 * An archive can either be loaded or deployed.
 * Deployed archives are part of a DeploymentImpl, and will be automatically
 * undeployed when the corresponding DeploymentImpl is removed from the
 * system.
 * Loaded archives are archives that are created for the sole purpose of
 * fulfilling {code {@link DeploymentImpl#loadBeanDeploymentArchive(Class)}
 * calls.
 * 
 * The ArchiveLoader loads those archives and keeps track of which
 * DeploymentImpl instances required them being loaded, so that it can be
 * determined when a loaded archive is no longer in use and can be safely
 * undeployed.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @version $Revision: 108369 $
 * @see DeploymentImpl#loadBeanDeploymentArchive(Class)
 */
class ArchiveLoader
{
   private Map<Archive, Collection<DeploymentImpl>> loadedArchives;

   /**
    * Constructor.
    */
   public ArchiveLoader()
   {
      loadedArchives = new HashMap<Archive, Collection<DeploymentImpl>>();
   }

   /**
    * Loads an archive that contains {@code beanClass}.
    * If such archive already exists, no archive is created and the
    * preexistent one is returned.
    * 
    * @param beanClass  the class the loaded archive should contain
    * @param deployment the deployment making this request
    * @return  the requested Archive
    */
   public Archive load(Class<?> beanClass, DeploymentImpl deployment)
   {
      ClassLoader beanClassLoader = SecurityActions.getClassLoader(beanClass);
      if (beanClassLoader == null)
      {
         beanClassLoader = ClassLoader.getSystemClassLoader();
      }
      synchronized(beanClassLoader)
      {
         Archive archive = Archive.getInstance(beanClassLoader);
         if (archive == null)
         {
            ArchiveInfo archiveInfo = new ArchiveInfo(beanClassLoader, Collections.<java.lang.String>emptyList());
            Collection<EjbDescriptor<?>> ejbs = Collections.emptyList();
            archive = ArchiveFactory.createArchive(archiveInfo, ejbs);
            registerArchiveLoadedByDeployment(archive, deployment);
         }
         else if (isLoaded(archive))
         {
            registerArchiveReloadedByDeployment(archive, deployment);
         }
         return archive;
      }
   }

   /**
    * Notifies this ArchiveLoader that the loaded {@code archive} is no longer
    * being used by {@code deployment}, which should occur when {@code deployment}
    * is being undeployed.
    * 
    * @param archive    an archive
    * @param deployment the deployment being undeployed 
    * @return  {@code true} if this archive is free to be released, i.e.,
    *                       if it is not in use by any other deployment.
    * @see DeploymentImpl#undeploy()
    */
   public boolean unload(Archive archive, DeploymentImpl deployment)
   {
      // this archive is not a loaded archive and hence cannot be undeployed
      // as a loaded archive
      if (!loadedArchives.containsKey(archive))
      {
         return false;
      }
      synchronized(archive.getClassLoader())
      {
         Collection<DeploymentImpl> deployments = loadedArchives.get(archive);
         deployments.remove(deployment);
         if (deployments.isEmpty())
         {
            loadedArchives.remove(archive);
            return true;
         }
         return false;
      }
   }

   /**
    * Indicates whether {@code archive} is loaded by this ArchiveLoader.
    */
   private boolean isLoaded(Archive archive)
   {
      return loadedArchives.containsKey(archive);
   }

   /**
    * Record that the loaded {@code archive} is being requested by
    * {@code deployment}
    */
   private void registerArchiveReloadedByDeployment(Archive archive, DeploymentImpl deployment)
   {
      Collection<DeploymentImpl> deployments = loadedArchives.get(archive);
      deployments.add(deployment);
   }

   /**
    * Record that the newly created {@code archive} was requested by
    * {@code deployment}.
    */
   private void registerArchiveLoadedByDeployment(Archive archive, DeploymentImpl deployment)
   {
      Collection<DeploymentImpl> deployments = new HashSet<DeploymentImpl>();
      deployments.add(deployment);
      loadedArchives.put(archive, deployments);
   }
}