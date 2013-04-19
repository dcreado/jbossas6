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
package org.jboss.weld.integration.deployer.metadata;

import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.ExportAll;
import org.jboss.classloading.spi.version.Version;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.vfs.VirtualFile;

import java.util.Collection;

/**
 * Handle classloading metadata creation.
 *
 * @param <T> exact metadata type
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class WeldClassloadingDeployer<T> extends WeldAwareMetadataDeployer<T>
{
   /** Whether to isolated deployments */
   private boolean isolated = true;

   protected WeldClassloadingDeployer(Class<T> input)
   {
      super(input, false);
      addInput(ClassLoadingMetaData.class);
      addOutput(ClassLoadingMetaData.class);
   }

   protected void internalDeploy(VFSDeploymentUnit unit, T deployment, Collection<VirtualFile> wbXml) throws DeploymentException
   {
      ClassLoadingMetaData classLoadingMetaData = unit.getAttachment(ClassLoadingMetaData.class);
      if (classLoadingMetaData != null)
         return;

      if (isClassLoadingMetadataPresent(deployment))
         return;

      if (isIsolated(unit, wbXml) == false)
         return;

      String domain = getJMXName(deployment, unit) + ",extension=LoaderRepository";
      classLoadingMetaData = new ClassLoadingMetaData();
      classLoadingMetaData.setName(unit.getName());
      classLoadingMetaData.setDomain(domain);
      classLoadingMetaData.setExportAll(ExportAll.NON_EMPTY);
      classLoadingMetaData.setImportAll(true);
      classLoadingMetaData.setVersion(Version.DEFAULT_VERSION);
      classLoadingMetaData.setJ2seClassLoadingCompliance(false);

      unit.addAttachment(ClassLoadingMetaData.class, classLoadingMetaData);
   }

   /**
    * Is cl metadata already present.
    *
    * @param deployment the metadata deployment
    * @return true if cl metadata already exists
    */
   protected abstract boolean isClassLoadingMetadataPresent(T deployment);

   /**
    * Should we isolate deployment.
    *
    * @param unit the deployment unit
    * @param wbXml weld xml
    * @return true is deployment shouold be isolated, false otherwise
    */
   protected boolean isIsolated(VFSDeploymentUnit unit, Collection<VirtualFile> wbXml)
   {
      return isolated;
   }

   /**
    * Get jmx name.
    *
    * @param metaData the metadata
    * @param unit the deployment unit
    * @return deployment's jmx name
    */
   protected abstract String getJMXName(T metaData, DeploymentUnit unit);

   /**
    * @param isolated whether ear deployments should be isolated
    */
   public void setIsolated(boolean isolated)
   {
      this.isolated = isolated;
   }
}