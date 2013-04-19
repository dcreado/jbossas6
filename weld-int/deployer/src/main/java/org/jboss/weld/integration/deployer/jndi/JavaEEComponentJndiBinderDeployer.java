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
package org.jboss.weld.integration.deployer.jndi;

import org.jboss.beans.metadata.plugins.AbstractInjectionValueMetaData;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.dependency.plugins.AbstractDependencyItem;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.dependency.spi.DependencyInfo;
import org.jboss.dependency.spi.DependencyItem;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.jpa.javaee.JavaEEModuleInformer;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.metadata.ejb.jboss.*;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEComponentInformer;
import org.jboss.reloaded.naming.spi.JavaEEComponent;
import org.jboss.weld.integration.deployer.DeployersUtils;

/**
 * @author Marius Bogoevici
 */
public class JavaEEComponentJndiBinderDeployer extends AbstractSimpleRealDeployer<JBossEnterpriseBeanMetaData>
{

   private JavaEEComponentInformer informer;

   public JavaEEComponentJndiBinderDeployer(JavaEEComponentInformer javaEEComponentInformer)
   {
      super(JBossEnterpriseBeanMetaData.class);
      informer = javaEEComponentInformer;
      setComponentsOnly(true);
      setOutput(BeanMetaData.class);
   }

   @Override
   public void deploy(DeploymentUnit deploymentUnit, JBossEnterpriseBeanMetaData jBossEnterpriseBeanMetaData) throws DeploymentException
   {
   }

}
