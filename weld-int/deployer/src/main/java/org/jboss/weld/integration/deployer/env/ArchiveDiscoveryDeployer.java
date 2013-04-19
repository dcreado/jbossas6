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
package org.jboss.weld.integration.deployer.env;

import java.net.URL;
import java.util.Collection;

import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.visitor.ClassFilter;
import org.jboss.classloading.spi.visitor.ResourceContext;
import org.jboss.classloading.spi.visitor.ResourceFilter;
import org.jboss.classloading.spi.visitor.ResourceVisitor;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.vfs.spi.deployer.AbstractOptionalVFSRealDeployer;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.deployer.env.bda.ArchiveInfo;
import org.jboss.weld.integration.deployer.ext.JBossWeldMetaData;

/**
 * ArchiveDiscovery deployer.
 * Deploys information into ArchiveInfo's environment.
 * 
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ArchiveDiscoveryDeployer extends AbstractOptionalVFSRealDeployer<JBossWeldMetaData>
{
   public ArchiveDiscoveryDeployer()
   {
      super(JBossWeldMetaData.class);
      addInput(DeployersUtils.WELD_FILES);
      addInput(DeployersUtils.WELD_CLASSPATH);
      addInput(ArchiveInfo.class);
      addOutput(ArchiveInfo.class);
      setStage(DeploymentStages.PRE_REAL);
   }

   @Override
   public void deploy(VFSDeploymentUnit unit, JBossWeldMetaData deployment) throws DeploymentException
   {
      @SuppressWarnings("unchecked")
      Collection<VirtualFile> wbFiles = unit.getAttachment(DeployersUtils.WELD_FILES, Collection.class);
      boolean hasWB = (wbFiles != null && wbFiles.isEmpty() == false);

      @SuppressWarnings("unchecked")
      Collection<VirtualFile> cpFiles = unit.getAttachment(DeployersUtils.WELD_CLASSPATH, Collection.class);
      boolean hasCp = (cpFiles != null && cpFiles.isEmpty() == false);

      if (!hasWB && !hasCp)
      {
         return;
      }
      
      try
      {
         if (hasCp || hasWB)
         {
            VFSDeploymentUnit moduleUnit = unit;
            Module module = moduleUnit.getAttachment(Module.class);
            while (moduleUnit != null && module == null)
            {
               moduleUnit = moduleUnit.getParent();
               module = moduleUnit.getAttachment(Module.class);
            }
            if (module == null)
               throw new DeploymentException("No module in deployment unit's hierarchy: " + unit.getName());

            ArchiveInfo archive = moduleUnit.getAttachment(ArchiveInfo.class);
            if (archive == null)
               throw new IllegalStateException("Archive attachment expected for unit " + unit);

            WeldDiscoveryEnvironment environment = archive.getEnvironment();
            if (hasCp)
            {
               URL[] urls = new URL[cpFiles.size()];
               int i = 0;
               for (VirtualFile file : cpFiles)
               {
                  urls[i++] = file.toURL();
               }
               WBDiscoveryVisitor visitor = new WBDiscoveryVisitor(environment);
               module.visit(visitor, ClassFilter.INSTANCE, null, urls);
            }
            if (hasWB)
            {
               for (VirtualFile file : wbFiles)
                  environment.addWeldXmlURL(file.toURL());
            }
         }
      }
      catch (Exception e)
      {
         throw DeploymentException.rethrowAsDeploymentException("Cannot build WB env.", e);
      }
   }

   
   static class WBDiscoveryVisitor implements ResourceVisitor
   {
      private WeldDiscoveryEnvironment wbdi;

      WBDiscoveryVisitor(WeldDiscoveryEnvironment wbdi)
      {
         this.wbdi = wbdi;
      }

      public ResourceFilter getFilter()
      {
         return ClassFilter.INSTANCE;
      }

      public void visit(ResourceContext resource)
      {
         wbdi.addWeldClass(resource.getClassName());
      }
   }
}