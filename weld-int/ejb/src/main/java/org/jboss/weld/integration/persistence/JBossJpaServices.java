package org.jboss.weld.integration.persistence;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.common.deployers.spi.AttachmentNames;
import org.jboss.jpa.deployment.ManagedEntityManagerFactory;
import org.jboss.jpa.deployment.PersistenceUnitDeployment;
import org.jboss.jpa.injection.InjectedEntityManagerFactory;
import org.jboss.jpa.resolvers.PersistenceUnitDependencyResolver;
import org.jboss.jpa.tx.TransactionScopedEntityManager;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.integration.util.AbstractJBossServices;

/**
 * JPA Weld Utitlies
 *
 * @author Pete Muir
 * @author ales.justin@jboss.org
 * @author JBoss EJB3 team
 */
public class JBossJpaServices extends AbstractJBossServices implements JpaInjectionServices
{
   public JBossJpaServices() throws NamingException
   {
      super();
   }

   protected PersistenceUnitDependencyResolver persistenceUnitDependencyResolver;

   public void setPersistenceUnitDependencyResolver(PersistenceUnitDependencyResolver persistenceUnitDependencyResolver)
   {
      this.persistenceUnitDependencyResolver = persistenceUnitDependencyResolver;
   }

   public Collection<Class<?>> discoverEntities()
   {
      return Collections.emptyList();
   }

   @SuppressWarnings({"deprecation"})
   public EntityManager resolvePersistenceContext(InjectionPoint injectionPoint)
   {
      if (!injectionPoint.getAnnotated().isAnnotationPresent(PersistenceContext.class))
      {
         throw new IllegalArgumentException("No @PersistenceContext annotation found on injection point " + injectionPoint);
      }
      if (injectionPoint.getMember() instanceof Method && ((Method) injectionPoint.getMember()).getParameterTypes().length != 1)
      {
         throw new IllegalArgumentException("Injection point represents a method which doesn't follow JavaBean conventions (must have exactly one parameter) " + injectionPoint);
      }
      try
      {
         String persistenceUnitName = injectionPoint.getAnnotated().getAnnotation(PersistenceContext.class).unitName();
         return new TransactionScopedEntityManager(lookupPersistenceUnitDeployment(persistenceUnitName).getManagedFactory());
      }
      catch (IllegalStateException e)
      {
         throw new IllegalStateException("Unable to resolve persistence context for " + injectionPoint);
      }
   }

   public EntityManagerFactory resolvePersistenceUnit(InjectionPoint injectionPoint)
   {
      if (!injectionPoint.getAnnotated().isAnnotationPresent(PersistenceUnit.class))
      {
         throw new IllegalArgumentException("No @PersistenceUnit annotation found on injection point " + injectionPoint);
      }
      if (injectionPoint.getMember() instanceof Method && ((Method) injectionPoint.getMember()).getParameterTypes().length != 1)
      {
         throw new IllegalArgumentException("Injection point represents a method which doesn't follow JavaBean conventions (must have exactly one parameter) " + injectionPoint);
      }
      try
      {
         return resolvePersistenceUnit((injectionPoint.getAnnotated().getAnnotation(PersistenceUnit.class).unitName()));
      }
      catch (IllegalStateException e)
      {
         throw new IllegalStateException("Unable to resolve persistence context for " + injectionPoint);
      }
   }

   @SuppressWarnings({"deprecation"})
   private EntityManagerFactory resolvePersistenceUnit(String unitName)
   {
      PersistenceUnitDeployment deployment = lookupPersistenceUnitDeployment(unitName);
      ManagedEntityManagerFactory managedFactory = deployment.getManagedFactory();
      return new InjectedEntityManagerFactory(managedFactory);
   }

   private PersistenceUnitDeployment lookupPersistenceUnitDeployment(String unitName)
   {
      if (unitName == null)
      {
         throw new IllegalArgumentException("unitName is null");
      }
      String beanName = getPersistenceUnitSupplier(topLevelDeploymentUnit, persistenceUnitDependencyResolver, unitName);
      if (beanName == null)
      {
         throw new IllegalStateException("No persistence unit available for " + unitName);
      }
      return jbossEjb.lookupPersistenceUnitDeployment(beanName);
   }

   private static String getPersistenceUnitSupplier(DeploymentUnit deploymentUnit, PersistenceUnitDependencyResolver persistenceUnitDependencyResolver, String persistenceUnitName)
   {
      if ((deploymentUnit.getAttachment(AttachmentNames.PROCESSED_METADATA, JBossMetaData.class) != null && deploymentUnit.getAttachment(JBossMetaData.class).isEJB3x()) || (deploymentUnit.getAttachment(JBossWebMetaData.class) != null))
      {
         try
         {
            return persistenceUnitDependencyResolver.resolvePersistenceUnitSupplier(deploymentUnit, persistenceUnitName);
         }
         catch (IllegalArgumentException e)
         {
            // No-op, means we can't find the PU in this DU
         }
      }
      for (DeploymentUnit child : deploymentUnit.getChildren())
      {
         String beanName = getPersistenceUnitSupplier(child, persistenceUnitDependencyResolver, persistenceUnitName);
         if (beanName != null)
         {
            return beanName;
         }
      }
      return null;
   }
}
