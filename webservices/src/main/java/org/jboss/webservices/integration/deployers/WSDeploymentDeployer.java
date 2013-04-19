/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.webservices.integration.deployers;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.webservices.integration.deployers.deployment.WSDeploymentBuilder;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;

/**
 * This deployer initializes JBossWS deployment meta data.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSDeploymentDeployer extends AbstractRealDeployer
{
   /**
    * Constructor.
    */
   public WSDeploymentDeployer()
   {
      super();

      // inputs
      this.addInput(JBossWebMetaData.class);
      this.addInput(DeploymentType.class);

      // outputs
      this.addOutput(JBossWebMetaData.class);
      this.addOutput(Deployment.class);
   }

   /**
    * Creates new Web Service deployment and registers it with deployment unit.
    *
    * @param unit deployment unit
    * @throws DeploymentException if any error occurs
    */
   @Override
   protected void internalDeploy(final DeploymentUnit unit) throws DeploymentException
   {
      this.log.trace("Building JBoss agnostic webservices meta data model");
      WSDeploymentBuilder.getInstance().build(unit);
   }
}
