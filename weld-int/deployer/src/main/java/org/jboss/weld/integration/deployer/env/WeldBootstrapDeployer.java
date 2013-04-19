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

import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.ValueMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.beans.metadata.spi.builder.ParameterMetaDataBuilder;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.SingletonProvider;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.deployer.env.bda.DUTopLevelClassLoaderGetter;
import org.jboss.weld.integration.deployer.env.bda.DeploymentImpl;
import org.jboss.weld.integration.provider.JBossSingletonProvider;


/**
 * Deploy Weld boostrap service.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class WeldBootstrapDeployer extends AbstractBootstrapInfoDeployer
{
   private SingletonProvider singletonProvider;

   public WeldBootstrapDeployer()
   {
      super(false);
      setDisableOptional(true);
      addOutput(BeanMetaData.class);
      addInput(DeployersUtils.JAVAX_VALIDATION_VALIDATOR_FACTORY);
   }

   /**
    * Setter for allowing the injection of a different type of SingletonProvider
    *
    * @param singletonProvider the singleton provider
    */
   public void setSingletonProvider(SingletonProvider singletonProvider)
   {
      this.singletonProvider = singletonProvider;
   }

   /**
    * Configures the SingletonProvider to be used by the application server
    */
   public void start()
   {
      if (singletonProvider != null)
      {
         SingletonProvider.initialize(singletonProvider);
      }
      else
      {
         // set up the default Singleton provider, which in this case is JBossSingletonProvider
         JBossSingletonProvider provider = new JBossSingletonProvider();
         provider.setTopLevelClassLoaderGetter(DUTopLevelClassLoaderGetter.INSTANCE);
         SingletonProvider.initialize(provider);
      }
   }

   /**
    * Removes the SingletonProvider - we assume that no application is running anymore
    * in this application server when the deployer is uninstalled (shutdown) 
    */
   public void stop()
   {
      SingletonProvider.reset();
   }

   @Override
   protected void deployInternal(DeploymentUnit unit, BootstrapInfo info) throws DeploymentException
   {
      ValueMetaData ejbServicesValue = info.getEjbServices();
      if (ejbServicesValue == null)
      {
         throw new DeploymentException("Missing ejb services: " + unit);
      }

      ValueMetaData ejbInjectionServicesValue = info.getEjbInjectionServices();
      if (ejbInjectionServicesValue == null)
      {
         throw new DeploymentException("Missing ejb injection services: " + unit);
      }

      ValueMetaData deploymentValue = info.getDeployment();
      if (deploymentValue == null)
      {
         throw new DeploymentException("Missing deployment: " + unit);
      }

      String bootstrapName = DeployersUtils.getBootstrapBeanName(unit);
      BeanMetaDataBuilder bootstrap = BeanMetaDataBuilder.createBuilder(bootstrapName, "org.jboss.weld.integration.deployer.env.helpers.BootstrapBean");

      bootstrap.addConstructorParameter(Bootstrap.class.getName(), createBootstrap(unit));
      bootstrap.addConstructorParameter(DeploymentImpl.class.getName(), deploymentValue);
      bootstrap.addPropertyMetaData("ejbServices", ejbServicesValue);
      bootstrap.addPropertyMetaData("ejbInjectionServices", ejbInjectionServicesValue);
      bootstrap.addPropertyMetaData("jpaServices", createServiceConnector("JBossJpaServices", "org.jboss.weld.integration.persistence.JBossJpaServices", unit));
      bootstrap.addPropertyMetaData("resourceServices", bootstrap.createInject("JBossResourceServices"));
      bootstrap.addPropertyMetaData("transactionServices", bootstrap.createInject("JBossTransactionServices"));
      bootstrap.addPropertyMetaData("securityServices", bootstrap.createInject("JBossSecurityServices"));
      bootstrap.addPropertyMetaData("validationServices", createValidationServices(unit));
      bootstrap.setCreate("initialize");
      bootstrap.setStart("boot");
      bootstrap.setDestroy("shutdown");
      bootstrap.addDependency("RealTransactionManager"); // so we know TM is present in JBossTransactionServices

      //Make the bootstrap depend on this deployment unit so that we know all sub deployments have been processed
      bootstrap.addDependency(unit.getName());


      // call dynamic dependency creator for EJBs
      ParameterMetaDataBuilder install = bootstrap.addInstallWithParameters("createDepenencies", "DynamicDependencyCreator", null, ControllerState.CONFIGURED);
      install.addParameterMetaData(Object.class.getName(), bootstrapName);
      install.addParameterMetaData(Iterable.class.getName(), bootstrap.createInject(ejbServicesValue.getUnderlyingValue(), "ejbContainerNames"));
      install.addParameterMetaData(String.class.getName(), "Start");
      install.addParameterMetaData(String.class.getName(), ControllerState.PRE_INSTALL.getStateString());

      ParameterMetaDataBuilder jndiInstall = bootstrap.addInstallWithParameters("bind", "JndiBinder", ControllerState.INSTALLED, ControllerState.START);
      jndiInstall.addParameterMetaData(DeploymentUnit.class.getName(), unit);

      ParameterMetaDataBuilder jndiUninstall = bootstrap.addUninstallWithParameters("unbind", "JndiBinder");
      jndiUninstall.addParameterMetaData(DeploymentUnit.class.getName(), unit);

      unit.addAttachment(DeployersUtils.getBootstrapBeanAttachmentName(unit), bootstrap.getBeanMetaData());
   }

   /**
    * Create bootstrap bean.
    *
    * @param unit the deployment unit
    * @return new injected bootstrap metadata
    */
   protected ValueMetaData createBootstrap(DeploymentUnit unit)
   {
      String beanName = unit.getName() + "_WeldBootstrap";
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(beanName, "org.jboss.weld.bootstrap.WeldBootstrap");
      unit.addAttachment(beanName + "_" + BeanMetaData.class.getSimpleName(), builder.getBeanMetaData());
      return builder.createInject(beanName);
   }

   protected ValueMetaData createValidationServices(DeploymentUnit unit) throws DeploymentException
   {
      Object validatorFactory = unit.getAttachment(DeployersUtils.JAVAX_VALIDATION_VALIDATOR_FACTORY);

      if (validatorFactory == null && isValidationFactoryRequired(unit))
      {
         throw new DeploymentException("Missing ValidatorFactory attachment in deployment: " + unit);
      }
      String beanName = unit.getName() + "_JBossValidationServices";
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(beanName, "org.jboss.weld.integration.validation.JBossValidationServices");
      unit.addAttachment(beanName + "_" + BeanMetaData.class.getSimpleName(), builder.getBeanMetaData());
      builder.addConstructorParameter(DeployersUtils.JAVAX_VALIDATION_VALIDATOR_FACTORY, validatorFactory);
      return builder.createInject(beanName);
   }

   protected boolean isValidationFactoryRequired(DeploymentUnit deploymentUnit)
   {
      //TODO: define more strict criteria for determining whether the presence of a validation factory is required (e.g. JSF, JPA deployments) 
      return false;
   }
}
