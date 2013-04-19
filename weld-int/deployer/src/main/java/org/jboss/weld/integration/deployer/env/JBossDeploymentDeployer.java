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

import java.util.ArrayList;
import java.util.Collection;

import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.integration.deployer.env.bda.ArchiveInfo;
import org.jboss.weld.integration.deployer.env.bda.DeploymentImpl;

/**
 * JBoss Deployment Deployer.
 * Creates metadata for creation of a Deployment.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class JBossDeploymentDeployer extends AbstractDeploymentDeployer
{
   public JBossDeploymentDeployer()
   {
      super();
      addInput(ArchiveInfo.class);
   }

   @Override
   protected boolean isRelevant(DeploymentUnit unit)
   {
      return unit.isAttachmentPresent(ArchiveInfo.class);
   }

   protected Class<? extends Deployment> getDeploymentClass()
   {
      return DeploymentImpl.class;
   }

   protected void buildDeployment(DeploymentUnit unit, BootstrapInfo info, BeanMetaDataBuilder builder)
   {
      builder.addConstructorParameter(String.class.getName(), unit.getName());
      builder.addConstructorParameter(Collection.class.getName(), getArchiveInfos(unit));
      builder.addConstructorParameter(Collection.class.getName(), builder.createInject(info.getEjbServices().getUnderlyingValue(), "ejbs"));
      builder.addConstructorParameter(EjbServices.class.getName(), info.getEjbServices());
      builder.addUninstall("undeploy");
   }
   
   private Collection<ArchiveInfo> getArchiveInfos(DeploymentUnit unit)
   {
      Collection<ArchiveInfo> archiveInfos = new ArrayList<ArchiveInfo>();
      fill(archiveInfos, unit);
      return archiveInfos;
   }
   
   private void fill(Collection<ArchiveInfo> archiveInfos, DeploymentUnit unit)
   {
      ArchiveInfo archiveInfo = unit.getAttachment(ArchiveInfo.class);
      if (archiveInfo != null)
      {
         archiveInfos.add(archiveInfo);
      }
      for (DeploymentUnit child: unit.getChildren())
      {
         fill(archiveInfos, child);
      }
   }
}