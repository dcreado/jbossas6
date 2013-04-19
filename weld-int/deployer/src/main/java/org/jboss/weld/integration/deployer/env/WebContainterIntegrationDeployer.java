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

package org.jboss.weld.integration.deployer.env;

import java.util.ArrayList;
import java.util.List;

import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.deployer.env.helpers.BootstrapBean;
import org.jboss.weld.integration.injection.WeldInjector;
import org.jboss.weld.integration.util.IdFactory;

/**
 * Ensures that Weld is bootstrapped before the Web Application is started
 * (if this is a CDI-enabled deployment)
 *
 * @author Marius Bogoevici
 * @author Ales Justin
 */
public class WebContainterIntegrationDeployer extends AbstractSimpleRealDeployer<JBossWebMetaData>
{
   public WebContainterIntegrationDeployer()
   {
      super(JBossWebMetaData.class);
      setOutput(JBossWebMetaData.class);
   }

   @Override
   public void deploy(DeploymentUnit unit, JBossWebMetaData deployment) throws DeploymentException
   {
      if (DeployersUtils.isBootstrapBeanPresent(unit) && unit.getAttachment(DeployersUtils.WELD_FILES) != null)
      {

         List<String> depends = deployment.getDepends();
         if (depends == null)
         {
            depends = new ArrayList<String>();
            deployment.setDepends(depends);
         }
         depends.add(DeployersUtils.getBootstrapBeanName(unit));
      }
   }
}
