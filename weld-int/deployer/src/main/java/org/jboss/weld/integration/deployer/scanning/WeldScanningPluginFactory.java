/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.weld.integration.deployer.scanning;

import java.util.Collection;

import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.visitor.ResourceContext;
import org.jboss.classloading.spi.visitor.ResourceFilter;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.scanning.plugins.DeploymentScanningPluginFactory;
import org.jboss.scanning.plugins.helpers.DelegateResourceFilter;
import org.jboss.scanning.plugins.helpers.VoidScanningHandle;
import org.jboss.scanning.spi.ScanningPlugin;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.deployer.env.WeldDiscoveryEnvironment;
import org.jboss.weld.integration.deployer.env.bda.ArchiveInfo;

/**
 * Add Weld scanning plugin.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class WeldScanningPluginFactory implements DeploymentScanningPluginFactory<VoidScanningHandle, Object>
{
   /** The filter */
   private ResourceFilter weldDeployerFilter = new WeldDeployerFilter();

   /**
    * Get Weld discovery environment.
    *
    * @param unit the current deployment unit
    * @return weld discovery env
    */
   protected WeldDiscoveryEnvironment getEnv(DeploymentUnit unit)
   {
      DeploymentUnit moduleUnit = unit;
      Module module = moduleUnit.getAttachment(Module.class);
      while (moduleUnit != null && module == null)
      {
         moduleUnit = moduleUnit.getParent();
         module = moduleUnit.getAttachment(Module.class);
      }
      if (module == null)
         throw new IllegalStateException("No module in deployment unit's hierarchy: " + unit.getName());

      ArchiveInfo archive = moduleUnit.getAttachment(ArchiveInfo.class);
      if (archive == null)
         throw new IllegalStateException("Archive attachment expected for unit " + unit);

      return archive.getEnvironment();
   }

   public boolean isRelevant(DeploymentUnit unit)
   {
      if (DeployersUtils.checkForWeldFilesAcrossDeployment(unit) == false)
         return false;

      // only create ArchiveInfo for deployments with own classloader/module
      if (unit.isAttachmentPresent(Module.class))
      {
         unit.addAttachment(ArchiveInfo.class, new ArchiveInfo(unit));
      }

      @SuppressWarnings("unchecked")
      Collection<VirtualFile> wbFiles = unit.getAttachment(DeployersUtils.WELD_FILES, Collection.class);
      boolean hasWB = (wbFiles != null && wbFiles.isEmpty() == false);

      if (hasWB)
      {
         WeldDiscoveryEnvironment environment = getEnv(unit);
         try
         {
            for (VirtualFile file : wbFiles)
               environment.addWeldXmlURL(file.toURL());
         }
         catch (Exception e)
         {
            throw new RuntimeException(e);
         }
      }

      @SuppressWarnings("unchecked")
      Collection<VirtualFile> cpFiles = unit.getAttachment(DeployersUtils.WELD_CLASSPATH, Collection.class);
      boolean hasCP = cpFiles != null && cpFiles.isEmpty() == false;

      if (hasCP)
         handleRelevantDeployment(unit);

      return hasCP;
   }

   /**
    * Handle relevant deployment.
    *
    * @param unit the deployment unit
    */
   protected void handleRelevantDeployment(DeploymentUnit unit)
   {
      final String name = ResourceFilter.class.getName() + ".recurse";
      ResourceFilter filter = unit.getAttachment(name, ResourceFilter.class);
      if (filter != null)
         unit.addAttachment(name, new DelegateResourceFilter(weldDeployerFilter, filter));
      else
         unit.addAttachment(name, weldDeployerFilter);
   }

   public String getPluginOutput()
   {
      return ArchiveInfo.class.getName(); // we depend on this attachment
   }

   public ScanningPlugin<VoidScanningHandle, Object> create(DeploymentUnit unit)
   {
      @SuppressWarnings("unchecked")
      Collection<VirtualFile> cpFiles = unit.getAttachment(DeployersUtils.WELD_CLASSPATH, Collection.class);
      WeldDiscoveryEnvironment environment = getEnv(unit);
      return new WeldScanningPlugin(environment, cpFiles);
   }

   /**
    * Set weld deployers filter.
    * This way we exclude additional libs from scanning.
    *
    * @param weldDeployerFilter the filter
    */
   public void setWeldDeployerFilter(ResourceFilter weldDeployerFilter)
   {
      this.weldDeployerFilter = weldDeployerFilter;
   }

   private static class WeldDeployerFilter implements ResourceFilter
   {
      public boolean accepts(ResourceContext resource)
      {
         return resource.getUrl().toString().contains("weld.deployer") == false;
      }
   }
}
