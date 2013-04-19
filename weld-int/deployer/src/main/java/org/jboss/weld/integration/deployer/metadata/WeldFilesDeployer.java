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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.vfs.spi.deployer.AbstractOptionalVFSRealDeployer;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.deployer.ext.JBossWeldMetaData;

/**
 * A deployer that collects all beans.xml files in deployment unit
 * and keeps them under WEB_BEANS_FILES constant.
 * It also collects wb's matching classpaths, under WEB_BEANS_CLASSPATH const.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class WeldFilesDeployer extends AbstractOptionalVFSRealDeployer<JBossWeldMetaData>
{
   public WeldFilesDeployer()
   {
      super(JBossWeldMetaData.class);
      addOutput(DeployersUtils.WELD_FILES);
      addOutput(DeployersUtils.WELD_CLASSPATH);
      setStage(DeploymentStages.POST_PARSE);
   }

   public void deploy(VFSDeploymentUnit unit, JBossWeldMetaData deployment) throws DeploymentException
   {
      List<VirtualFile> wbFiles = new ArrayList<VirtualFile>();
      List<VirtualFile> cpFiles = new ArrayList<VirtualFile>();
      try
      {
         if (deployment != null)
         {
            // do some custom stuff
         }

         Iterable<VirtualFile> classpaths = getClassPaths(unit);
         for (VirtualFile cp : classpaths)
         {
            VirtualFile wbXml = cp.getChild("META-INF/beans.xml");
            if (wbXml.exists())
            {
               // add url
               wbFiles.add(wbXml);
               // add classes
               cpFiles.add(cp);
            }
         }

         // handle war slightly different
         VirtualFile warWbXml = unit.getFile("WEB-INF/beans.xml");
         if (warWbXml != null)
         {
            wbFiles.add(warWbXml);

            VirtualFile classes = unit.getFile("WEB-INF/classes");
            if (classes != null)
               cpFiles.add(classes);
         }

         if (wbFiles.isEmpty() == false)
            unit.addAttachment(DeployersUtils.WELD_FILES, wbFiles, Collection.class);
         if (cpFiles.isEmpty() == false)
            unit.addAttachment(DeployersUtils.WELD_CLASSPATH, cpFiles, Collection.class);
      }
      catch (Exception e)
      {
         throw DeploymentException.rethrowAsDeploymentException("Cannot WBD files/classpath.", e);
      }
   }

   /**
    * Get the matching class paths that belong to this deployment unit.
    *
    * @param unit the deployment unit
    * @return matching class paths
    * @throws Exception for any error
    */
   protected Iterable<VirtualFile> getClassPaths(VFSDeploymentUnit unit) throws Exception
   {
      return unit.getClassPath();
   }
}