/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.webservices.integration.weld;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.wsf.common.integration.AbstractDeploymentAspect;
import org.jboss.wsf.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * Weld deployment aspect that associates Weld Invocation handler
 * if WS CDI integration is detected.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WeldDeploymentAspect extends AbstractDeploymentAspect
{

   public WeldDeploymentAspect()
   {
      // does nothing
   }

   @Override
   public void start(final Deployment dep)
   {
      if (!WSHelper.isJaxwsJseDeployment(dep))
      {
         // we support weld integration for JAXWS JSE endpoints only
         return;
      }

      final DeploymentUnit deploymentUnit = WSHelper.getRequiredAttachment(dep, DeploymentUnit.class);
      if (this.isWeldDeployment(deploymentUnit))
      {
         for (final Endpoint endpoint : dep.getService().getEndpoints())
         {
            endpoint.setInvocationHandler(new WeldInvocationHandler(endpoint.getInvocationHandler()));
         }
      }
   }

   private boolean isWeldDeployment(final DeploymentUnit unit)
   {
      return unit.getAttachment(DeployersUtils.WELD_FILES) != null;
   }

}
