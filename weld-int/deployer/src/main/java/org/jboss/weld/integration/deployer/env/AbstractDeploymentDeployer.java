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
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.integration.deployer.DeployersUtils;

/**
 * Abstract Deployment Deployer.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractDeploymentDeployer extends AbstractBootstrapInfoDeployer
{
   public AbstractDeploymentDeployer()
   {
      super(true);
      addInput(BootstrapInfo.EJB_SERVICES);
      addOutput(BootstrapInfo.DEPLOYMENT);
      addOutput(BeanMetaData.class);
   }

   public void deployInternal(DeploymentUnit unit, BootstrapInfo info) throws DeploymentException
   {
      if (info.getEjbServices() == null)
         throw new DeploymentException("Missing ejb services value: " + unit);

      if (isRelevant(unit) == false)
         return;

      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(DeployersUtils.getDeploymentBeanName(unit), getDeploymentClass().getName());
      buildDeployment(unit, info, builder);
      info.setDeployment(builder.createInject(DeployersUtils.getDeploymentBeanName(unit)));
      unit.addAttachment(DeployersUtils.getDeploymentAttachmentName(unit), builder.getBeanMetaData(), BeanMetaData.class);
   }

   /**
    * Is this deployer relevant.
    *
    * @param unit the deployment unit
    * @return true if relevant, false otherwise
    */
   protected boolean isRelevant(DeploymentUnit unit)
   {
      return true;
   }

   /**
    * Get deployment impl class.
    *
    * @return the deployment class
    */
   protected abstract Class<? extends Deployment> getDeploymentClass();

   /**
    * Build deployment
    *
    * @param unit the deployment unit
    * @param info the bootstrap info
    * @param builder the bean metadata builder
    */
   protected abstract void buildDeployment(DeploymentUnit unit, BootstrapInfo info, BeanMetaDataBuilder builder);}