package org.jboss.weld.integration.deployer.env.helpers;

import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.integration.deployer.env.bda.DeploymentImpl;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.jboss.weld.validation.spi.ValidationServices;

/**
 * A bean version of bootstrap that delegates to the underlying bootstrap impl
 *
 * @author Pete Muir
 *
 */
public class BootstrapBean
{

   private final Bootstrap bootstrap;
   private final DeploymentImpl deployment;

   public BootstrapBean(Bootstrap bootstrap, DeploymentImpl deployment)
   {
      this.bootstrap = bootstrap;
      this.deployment = deployment;
   }

   public void setEjbServices(EjbServices ejbServices)
   {
      addDeploymentService(EjbServices.class, ejbServices);
   }

   public void setEjbInjectionServices(EjbInjectionServices service)
   {
      addBeanDeploymentArchiveService(EjbInjectionServices.class, service);
   }

   public void setJpaServices(JpaInjectionServices jpaServices)
   {
      addBeanDeploymentArchiveService(JpaInjectionServices.class, jpaServices);
   }

   public void setResourceServices(ResourceInjectionServices resourceServices)
   {
      addBeanDeploymentArchiveService(ResourceInjectionServices.class, resourceServices);
   }

   public Deployment getDeployment()
   {
      return deployment;
   }

   public void setTransactionServices(TransactionServices transactionServices)
   {
      addDeploymentService(TransactionServices.class, transactionServices);
   }

   public void setValidationServices(ValidationServices validationServices)
   {
      addDeploymentService(ValidationServices.class, validationServices);
   }

   public void setSecurityServices(SecurityServices securityServices)
   {
      addDeploymentService(SecurityServices.class, securityServices);
   }

   public void setProxyServices(ProxyServices proxyServices)
   {
      addDeploymentService(ProxyServices.class, proxyServices);
   }

   private <S extends Service> void addDeploymentService(Class<S> type, S service)
   {
      getDeployment().getServices().add(type, service);
   }

   private <S extends Service> void addBeanDeploymentArchiveService(Class<S> type, S service)
   {
      deployment.addBootstrapService(type, service);
   }

   public void boot()
   {
      bootstrap.startInitialization().deployBeans().validateBeans().endInitialization();
   }

   /**
    * @return the bootstrap
    */
   public Bootstrap getBootstrap()
   {
      return bootstrap;
   }

   public void initialize()
   {
      deployment.initialize(bootstrap);
      bootstrap.startContainer(Environments.EE_INJECT, deployment);
   }

   public void shutdown()
   {
      bootstrap.shutdown();
   }

}