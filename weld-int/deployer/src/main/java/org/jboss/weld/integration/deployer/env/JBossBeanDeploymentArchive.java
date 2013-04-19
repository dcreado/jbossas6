package org.jboss.weld.integration.deployer.env;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;

public class JBossBeanDeploymentArchive implements BeanDeploymentArchive
{

   private final WeldDiscoveryEnvironment environment;
   private final Collection<EjbDescriptor<?>> ejbDescriptors;
   private final ServiceRegistry services;
   private final String id;
   private Bootstrap bootstrap;

   public JBossBeanDeploymentArchive(String id, WeldDiscoveryEnvironment environment, Collection<EjbDescriptor<?>> ejbDescriptors)
   {
      this.environment = environment;
      this.ejbDescriptors = ejbDescriptors;
      this.services = new SimpleServiceRegistry();
      this.id = id;
   }

   public Collection<String> getBeanClasses()
   {
      return environment.getWeldClasses();
   }

   public List<BeanDeploymentArchive> getBeanDeploymentArchives()
   {
      return Collections.emptyList();
   }

   public BeansXml getBeansXml()
   {
      if (bootstrap == null)
      {
         throw new IllegalStateException("bootstrap must not be null");
      }
      return bootstrap.parse(environment.getWeldXml());
   }

   public Collection<EjbDescriptor<?>> getEjbs()
   {
      return ejbDescriptors;
   }

   public ServiceRegistry getServices()
   {
      return services;
   }

   public String getId()
   {
      return id;
   }

   public void setBootstrap(Bootstrap bootstrap)
   {
      this.bootstrap = bootstrap;
   }
   
}