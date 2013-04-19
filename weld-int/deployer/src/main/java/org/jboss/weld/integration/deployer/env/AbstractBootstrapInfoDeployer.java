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

import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.ValueMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractOptionalRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.integration.deployer.DeployersUtils;

/**
 * Abstract bootstrap info deployer.
 * Adding and getting info for Bootstrap bean.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractBootstrapInfoDeployer extends AbstractOptionalRealDeployer<BootstrapInfo>
{
   protected AbstractBootstrapInfoDeployer(boolean isPassThroughBootstrapInfo)
   {
      super(BootstrapInfo.class);
      setTopLevelOnly(true);
      setStage(DeploymentStages.PRE_REAL);
      addInput(DeployersUtils.WELD_FILES);
      if (isPassThroughBootstrapInfo)
         addOutput(BootstrapInfo.class);
   }

   @Override
   public final void deploy(DeploymentUnit unit, BootstrapInfo info) throws DeploymentException
   {
      if (info != null)
      {
         deployInternal(unit, info);
      }
      else if (DeployersUtils.checkForWeldFilesAcrossDeployment(unit))
      {
         info = new BootstrapInfo();
         unit.addAttachment(BootstrapInfo.class, info);
         deployInternal(unit, info);
      }
   }

   @Override
   public void undeploy(DeploymentUnit unit, BootstrapInfo info)
   {
      if (info != null)
      {
         undeployInternal(unit, info);
      }
   }

   protected void undeployInternal(DeploymentUnit unit, BootstrapInfo info)
   {
   }

   /**
    * Do deploy.
    *
    * @param unit the deployment unit
    * @param info non-null bootstrap info
    * @throws org.jboss.deployers.spi.DeploymentException for any error
    */
   protected abstract void deployInternal(DeploymentUnit unit, BootstrapInfo info) throws DeploymentException;

   /**
    * Create service connector.
    *
    * @param name the connector name
    * @param bean the bean to create
    * @param unit the deployment unit
    * @return new inject metadata
    */
   protected static ValueMetaData createServiceConnector(String name, String bean, DeploymentUnit unit)
   {
      String beanName = unit.getName() + "_" + name;
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(beanName, bean);
      builder.setFactory(name);
      builder.setFactoryMethod("createBean");
      builder.addPropertyMetaData("deploymentUnit", unit);
      unit.addAttachment(beanName + "_" + BeanMetaData.class.getSimpleName(), builder.getBeanMetaData());
      return builder.createInject(beanName);
   }
}