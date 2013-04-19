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
import org.jboss.scanning.plugins.visitor.CachingReflectProvider;
import org.jboss.scanning.plugins.visitor.ReflectProvider;

/**
 * Cleanup scanning resource utils.
 *
 * @author ales.justin@jboss.org
 */
public class ResourceUtilCleanupDeployer extends AbstractSimpleRealDeployer<Module>
{
   public ResourceUtilCleanupDeployer()
   {
      super(Module.class);
      setStage(DeploymentStages.PRE_REAL);
   }

   public void deploy(DeploymentUnit unit, Module deployment) throws DeploymentException
   {
      ReflectProvider provider = unit.removeAttachment(ReflectProvider.class);
      if (provider != null && provider instanceof CachingReflectProvider)
      {
         CachingReflectProvider crp = (CachingReflectProvider)provider;
         crp.reset();
      }
   }
}