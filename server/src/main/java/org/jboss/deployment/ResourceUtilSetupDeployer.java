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
package org.jboss.deployment;

import org.jboss.classloading.spi.dependency.Module;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.kernel.Kernel;
import org.jboss.scanning.plugins.helpers.ResourceOwnerFinder;
import org.jboss.scanning.plugins.visitor.CachingReflectProvider;
import org.jboss.scanning.plugins.visitor.ConfiguratorReflectProvider;
import org.jboss.scanning.plugins.visitor.ErrorHandler;
import org.jboss.scanning.plugins.visitor.ReflectProvider;

/**
 * Setup scanning resource utils.
 *
 * @author ales.justin@jboss.org
 */
public class ResourceUtilSetupDeployer extends AbstractSimpleRealDeployer<Module>
{
   private ReflectProvider provider;
   private ErrorHandler handler;
   private ResourceOwnerFinder finder;

   public ResourceUtilSetupDeployer(Kernel kernel)
   {
      super(Module.class);
      if (kernel == null)
         throw new IllegalArgumentException("Null kernel");

      setStage(DeploymentStages.CLASSLOADER);
      provider = new ConfiguratorReflectProvider(kernel.getConfigurator());
   }

   @Override
   public void deploy(DeploymentUnit unit, Module deployment) throws DeploymentException
   {
      unit.addAttachment(ReflectProvider.class, new CachingReflectProvider(provider)); // wrap with cache
      if (handler != null)
         unit.addAttachment(ErrorHandler.class, handler);
      if (finder != null)
         unit.addAttachment(ResourceOwnerFinder.class, finder);
   }

   @Override
   public void undeploy(DeploymentUnit unit, Module deployment)
   {
      if (handler != null)
         unit.removeAttachment(ErrorHandler.class);
      if (finder != null)
         unit.removeAttachment(ResourceOwnerFinder.class);
      unit.removeAttachment(ReflectProvider.class);
   }

   public void setHandler(ErrorHandler handler)
   {
      this.handler = handler;
   }

   public void setFinder(ResourceOwnerFinder finder)
   {
      this.finder = finder;
   }
}