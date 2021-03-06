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

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.deployer.DeployersUtils;

import java.util.Collection;

/**
 * Post jboss-app.xml weld deployer.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class PostJBossAppMetadataDeployer extends WeldClassloadingDeployer<JBossAppMetaData>
{
   public static final String BASE_EAR_DEPLOYMENT_NAME = "jboss.j2ee:service=EARDeployment";

   public PostJBossAppMetadataDeployer()
   {
      super(JBossAppMetaData.class);
      setOptionalWeldXml(true);
   }

   protected boolean isClassLoadingMetadataPresent(JBossAppMetaData deployment)
   {
      return deployment.getLoaderRepository() != null;
   }

   @Override
   protected boolean isIsolated(VFSDeploymentUnit unit, Collection<VirtualFile> wbXml)
   {
      return (super.isIsolated(unit, wbXml) && (wbXml != null || DeployersUtils.checkForWeldFilesAcrossDeployment(unit)));
   }

   protected String getJMXName(JBossAppMetaData metaData, DeploymentUnit unit)
   {
      String name = metaData.getJmxName();
      if( name == null )
         name = BASE_EAR_DEPLOYMENT_NAME + ",url='" + unit.getSimpleName() + "'";

      return name;
   }
}