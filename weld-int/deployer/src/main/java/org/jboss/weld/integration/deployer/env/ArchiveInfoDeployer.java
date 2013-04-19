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

import org.jboss.classloading.spi.RealClassLoader;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.vfs.spi.deployer.AbstractOptionalVFSRealDeployer;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.util.NotImplementedException;
import org.jboss.weld.integration.deployer.env.bda.ArchiveInfo;
import org.jboss.weld.integration.deployer.ext.JBossWeldMetaData;
import org.jboss.weld.integration.util.EjbDiscoveryUtils;
import org.jboss.weld.integration.util.JBossEjb;

/**
 * ArchiveInfo deployer.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ArchiveInfoDeployer extends AbstractOptionalVFSRealDeployer<JBossWeldMetaData>
{
   public ArchiveInfoDeployer()
   {
      super(JBossWeldMetaData.class);
      addOutput(ArchiveInfo.class);
      setStage(DeploymentStages.POST_CLASSLOADER);
   }

   public void deploy(VFSDeploymentUnit unit, JBossWeldMetaData deployment) throws DeploymentException
   {
      ClassLoader classLoader = unit.getClassLoader();
      if (!unit.isTopLevel() && unit.getParent().getClassLoader() == classLoader)
      {
         return;
      }
      if (classLoader instanceof RealClassLoader)
      {
         unit.addAttachment(ArchiveInfo.class, new ArchiveInfo(classLoader, EjbDiscoveryUtils.getVisibleEJbNames(unit)));
      }
      else
         // FIXME
         throw new NotImplementedException();
   }
}