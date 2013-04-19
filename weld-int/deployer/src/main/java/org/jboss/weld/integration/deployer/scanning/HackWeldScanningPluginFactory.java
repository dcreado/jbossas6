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

import org.jboss.classloading.spi.visitor.ResourceFilter;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.deployer.DeployersUtils;

/**
 * Hack around the ear/lib exclude filtering.
 * See EarLibExcludeDeployer.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class HackWeldScanningPluginFactory extends WeldScanningPluginFactory
{
   protected void handleRelevantDeployment(DeploymentUnit unit)
   {
      // Only inspect and modify ear's
      if (unit.isTopLevel() && unit instanceof VFSDeploymentUnit)
      {
         JBossAppMetaData jBossAppMetaData = unit.getAttachment(JBossAppMetaData.class);
         if (jBossAppMetaData != null)
         {
            VFSDeploymentUnit vfsDU = (VFSDeploymentUnit) unit;
            VirtualFile root = vfsDU.getRoot();
            String libDir = jBossAppMetaData.getLibraryDirectory();
            if (libDir == null || libDir.length() == 0) // take 'lib' even on empty
               libDir = "lib";
            VirtualFile lib = root.getChild(libDir);
            if (lib != null && lib.exists())
            {
               @SuppressWarnings("unchecked")
               Collection<VirtualFile> cpFiles = unit.getAttachment(DeployersUtils.WELD_CLASSPATH, Collection.class);
               for (VirtualFile cp : cpFiles)
               {
                  if (lib.equals(cp.getParent()))
                  {
                     ResourceFilter rf = unit.getAttachment(ResourceFilter.class.getName() + ".recurse", ResourceFilter.class);
                     // Only remove resource filter if it's default EarLibExclude's
                     if (rf != null && rf.getClass().getName().contains("UrlExclude"))
                     {
                        unit.removeAttachment(ResourceFilter.class.getName() + ".recurse");
                     }
                     break; // we checked, so break
                  }
               }
            }
         }
      }
      super.handleRelevantDeployment(unit);
   }
}
