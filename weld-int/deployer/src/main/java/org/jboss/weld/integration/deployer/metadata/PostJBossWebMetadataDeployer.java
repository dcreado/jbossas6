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
import org.jboss.metadata.web.jboss.JBossWebMetaData;

/**
 * Post jboss-web.xml weld deployer.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class PostJBossWebMetadataDeployer extends WeldClassloadingDeployer<JBossWebMetaData>
{
   public static final String BASE_WAR_DEPLOYMENT_NAME = "jboss.j2ee:service=WARDeployment";

   public PostJBossWebMetadataDeployer()
   {
      super(JBossWebMetaData.class);
      setTopLevelOnly(true); // only check for top .war files
   }

   protected boolean isClassLoadingMetadataPresent(JBossWebMetaData deployment)
   {
      return deployment.getClassLoading() != null;
   }

   protected String getJMXName(JBossWebMetaData metaData, DeploymentUnit unit)
   {
      return BASE_WAR_DEPLOYMENT_NAME + ",url='" + unit.getSimpleName() + "'";
   }
}