/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.deployers;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.Ejb3Deployment;
import org.jboss.injection.manager.spi.InjectionManager;
import org.jboss.switchboard.spi.Barrier;

/**
 * Deployer responsible for "starting" a {@link Ejb3Deployment}.
 * 
 * <p>
 *  The {@link Ejb3Deployer} creates and attaches a {@link Ejb3Deployment} into
 *  the deployment unit. The attached {@link Ejb3Deployment} will have it {@link Ejb3Deployment#create()}
 *  invocation completed by the {@link Ejb3Deployer}. This {@link Ejb3DeploymentDeployer} is just responsible
 *  for invoking {@link Ejb3Deployer#start()} *after* the deployment unit has been processed by the deployers
 *  which are responsible for attaching {@link Barrier SwitchBoard barrier} and {@link InjectionManager InjectionManager}
 *  to the unit. 
 * </p>
 *
 * <p>
 *  Note: This deployer ideally shouldn't have been required and the other {@link Ejb3Deployer} should have been 
 *  sufficient for starting the {@link Ejb3Deployment}. But as soon as the {@link Barrier} is added a an input to the
 *  {@link Ejb3Deployer}, things start falling apart due to the weird circular dependencies that get introduced between the deployers,
 *  due to the various JBoss WS deployers. 
 * </p>
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class Ejb3DeploymentDeployer extends AbstractRealDeployer
{

   public Ejb3DeploymentDeployer()
   {
      this.setInput(Ejb3Deployment.class);

      // SwitchBoard
      addInput(Barrier.class);
      // InjectionManager
      addInput(InjectionManager.class);

   }

   @Override
   protected void internalDeploy(DeploymentUnit unit) throws DeploymentException
   {
      Ejb3Deployment ejb3Deployment = unit.getAttachment(Ejb3Deployment.class);
      
      try
      {
         // start the deployment
         ejb3Deployment.start();
      }
      catch (Exception e)
      {
         throw new DeploymentException("Error starting Ejb3Deployment: " + ejb3Deployment.getName(), e);
      }
   }

}
