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

import java.util.HashMap;
import java.util.Map;

import org.jboss.deployers.plugins.deployers.DeployersImpl;
import org.jboss.deployers.spi.deployer.Deployer;
import org.jboss.logging.Logger;
import org.jboss.wsf.spi.deployment.DeploymentAspect;

/**
 * WSDeploymentAspectDeployer factory.
 *
 * @author <a href="ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSAspectizedDeployersFactory
{
   /** Logger. */
   private static final Logger LOGGER = Logger.getLogger(WSAspectizedDeployersFactory.class);

   /** Real deployers registry. */
   private final DeployersImpl delegee;

   /** Our deployers regitry. */
   private final Map<DeploymentAspect, Deployer> deployersRegistry = new HashMap<DeploymentAspect, Deployer>();

   /**
    * Constructor.
    *
    * @param realDeployers real deployers registry
    */
   public WSAspectizedDeployersFactory(final DeployersImpl realDeployers)
   {
      this.delegee = realDeployers;
   }

   /**
    * MC incallback method. It will be called each time DeploymentAspect bean will be installed.
    *
    * @param aspect to create real WS aspectized deployer for
    */
   public void addDeployer(final DeploymentAspect aspect)
   {
      if (WSAspectizedDeployersFactory.LOGGER.isTraceEnabled())
      {
         WSAspectizedDeployersFactory.LOGGER.trace("Adding deployer for: " + aspect);
      }
      final Deployer wsAspectizedDeployer = new WSDeploymentAspectDeployer(aspect);

      this.delegee.addDeployer(wsAspectizedDeployer);
      this.deployersRegistry.put(aspect, wsAspectizedDeployer);
   }

   /**
    * MC uncallback method. It will be called each time DeploymentAspect bean will be removed.
    *
    * @param aspect to remove real WS aspectized deployer for
    */
   public void removeDeployer(final DeploymentAspect aspect)
   {
      if (WSAspectizedDeployersFactory.LOGGER.isTraceEnabled())
      {
         WSAspectizedDeployersFactory.LOGGER.trace("Removing deployer for: " + aspect);
      }
      final Deployer wsAspectizedDeployer = this.deployersRegistry.get(aspect);

      this.deployersRegistry.remove(aspect);
      this.delegee.removeDeployer(wsAspectizedDeployer);
   }
}
