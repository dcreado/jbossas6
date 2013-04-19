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

import java.util.Collection;

import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.bootstrap.spi.Deployment;

/**
 * Flat Deployment Deployer.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class FlatDeploymentDeployer extends AbstractDeploymentDeployer
{
   public FlatDeploymentDeployer()
   {
      super();
      addInput(WeldDiscoveryEnvironment.class);
   }

   @Override
   protected boolean isRelevant(DeploymentUnit unit)
   {
      return unit.isAttachmentPresent(WeldDiscoveryEnvironment.class);
   }

   protected Class<? extends Deployment> getDeploymentClass()
   {
      return FlatDeployment.class;
   }

   protected void buildDeployment(DeploymentUnit unit, BootstrapInfo info, BeanMetaDataBuilder builder)
   {
      WeldDiscoveryEnvironment env = unit.getAttachment(WeldDiscoveryEnvironment.class);
      builder.addConstructorParameter(WeldDiscoveryEnvironment.class.getName(), env);
      builder.addConstructorParameter(Collection.class.getName(), builder.createInject(info.getEjbServices().getUnderlyingValue(), "ejbs"));
   }
}