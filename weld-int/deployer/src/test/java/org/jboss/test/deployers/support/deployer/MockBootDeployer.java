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
package org.jboss.test.deployers.support.deployer;

import org.jboss.beans.metadata.plugins.AbstractDependencyValueMetaData;
import org.jboss.beans.metadata.plugins.AbstractInjectionValueMetaData;
import org.jboss.beans.metadata.plugins.AbstractValueMetaData;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.ValueMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.beans.metadata.spi.builder.ParameterMetaDataBuilder;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.deployer.env.AbstractBootstrapInfoDeployer;
import org.jboss.weld.integration.deployer.env.BootstrapInfo;
import org.jboss.weld.integration.deployer.env.helpers.BootstrapBean;

/**
 * Mock wb boot deployer.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class MockBootDeployer extends AbstractBootstrapInfoDeployer
{
   public MockBootDeployer()
   {
      super(false);
      setDisableOptional(true);
      addOutput(BeanMetaData.class);
   }

   public void deployInternal(DeploymentUnit unit, BootstrapInfo info) throws DeploymentException
   {
      ValueMetaData ejbServicesValue = info.getEjbServices();
      if (ejbServicesValue == null)
         throw new DeploymentException("Missing ejb services: " + unit);

      ValueMetaData deploymentValue = info.getDeployment();
      if (deploymentValue == null)
         throw new DeploymentException("Missing deployment: " + unit);

      String unitName = unit.getName();
      String envName = unitName + "_JBossWebBeanDiscovery";
      BeanMetaDataBuilder envWrapper = BeanMetaDataBuilder.createBuilder(envName, "org.jboss.test.deployers.support.WeldDEWrapper");
      envWrapper.addConstructorParameter(Deployment.class.getName(), deploymentValue);
      unit.addAttachment(envName + "_" + BeanMetaData.class.getSimpleName(), envWrapper.getBeanMetaData());

      String bootstrapName = DeployersUtils.getBootstrapBeanName(unit);
      BeanMetaDataBuilder bootstrap = BeanMetaDataBuilder.createBuilder(bootstrapName, "org.jboss.test.deployers.support.MockWeldBootstrap");
      bootstrap.setCreate("initialize");
      bootstrap.setStart("boot");
      bootstrap.setDestroy("shutdown");
      bootstrap.addDependency("RealTransactionManager"); // so we know TM is present in JBossTransactionServices
      // call dynamic dependency creator for EJBs
      ParameterMetaDataBuilder install = bootstrap.addInstallWithParameters("createDepenencies", "DynamicDependencyCreator", null, ControllerState.CONFIGURED);
      install.addParameterMetaData(Object.class.getName(), bootstrapName);
      install.addParameterMetaData(Iterable.class.getName(), bootstrap.createInject(ejbServicesValue.getUnderlyingValue(), "ejbContainerNames"));
      install.addParameterMetaData(String.class.getName(), "Start");
      install.addParameterMetaData(String.class.getName(), "Start");

      ParameterMetaDataBuilder jndiInstall = bootstrap.addInstallWithParameters("bind", "JndiBinder");
      jndiInstall.addParameterMetaData(DeploymentUnit.class.getName(), unit);

      ParameterMetaDataBuilder jndiUninstall = bootstrap.addUninstallWithParameters("unbind", "JndiBinder");
      jndiUninstall.addParameterMetaData(DeploymentUnit.class.getName(), unit);

      unit.addAttachment(bootstrapName + "_" + BeanMetaData.class.getSimpleName(), bootstrap.getBeanMetaData());
   }
   
}