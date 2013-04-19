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

import org.jboss.beans.metadata.spi.ValueMetaData;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;

/**
 * EjbServices Deployer.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class EjbServicesDeployer extends AbstractBootstrapInfoDeployer
{
   public EjbServicesDeployer()
   {
      super(true);
      addOutput(BootstrapInfo.EJB_SERVICES);
   }

   // No need for the actual bean name, we use factory anyway
   public void deployInternal(DeploymentUnit unit, BootstrapInfo info) throws DeploymentException
   {
      ValueMetaData ejbServicesValue = createServiceConnector("JBossEjbServices", null, unit);
      info.setEjbServices(ejbServicesValue);

      ValueMetaData ejbInjectionServicesValue = createServiceConnector("JBossEjbInjectionServices", null, unit);
      info.setEjbInjectionServices(ejbInjectionServicesValue);
   }
}