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

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.vfs.spi.deployer.AbstractSimpleVFSRealDeployer;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.deployer.DeployersUtils;

import java.util.Collection;

/**
 * Weld aware metadata deployer.
 * It looks for weld.xml in metadata.
 *
 * @param <T> exact metadata type
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class WeldAwareMetadataDeployer<T> extends AbstractSimpleVFSRealDeployer<T>
{
   /** Do we allow null weld xml file */
   private boolean optionalWeldXml;

   protected WeldAwareMetadataDeployer(Class<T> input, boolean isInputModified)
   {
      super(input);
      setStage(DeploymentStages.POST_PARSE);
      addInput(DeployersUtils.WELD_FILES);
      if (isInputModified)
         addOutput(input); // we also modify input
   }

   public void deploy(VFSDeploymentUnit unit, T deployment) throws DeploymentException
   {
      Collection<VirtualFile> wbXml = unit.getAttachment(DeployersUtils.WELD_FILES, Collection.class);
      if (wbXml != null || optionalWeldXml)
         internalDeploy(unit, deployment, wbXml);
   }

   /**
    * Deploy.
    *
    * @param unit the deployment unit
    * @param deployment the deployment metadata
    * @param wbXml web beans xml
    * @throws org.jboss.deployers.spi.DeploymentException for any deployment error
    */
   protected abstract void internalDeploy(VFSDeploymentUnit unit, T deployment, Collection<VirtualFile> wbXml) throws DeploymentException;

   /**
    * Set optional wb xml file.
    *
    * @param optionalWeldXml the optional wb xml file flag
    */
   public void setOptionalWeldXml(boolean optionalWeldXml)
   {
      this.optionalWeldXml = optionalWeldXml;
   }
}