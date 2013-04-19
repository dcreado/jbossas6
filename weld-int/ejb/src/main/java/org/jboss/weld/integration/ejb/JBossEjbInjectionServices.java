package org.jboss.weld.integration.ejb;

import javax.ejb.EJB;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.naming.NamingException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.jboss.ejb3.ejbref.resolver.spi.EjbReference;
import org.jboss.ejb3.ejbref.resolver.spi.EjbReferenceResolver;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.integration.util.AbstractJBossServices;
import org.jboss.weld.integration.vdf.DeploymentUnitAware;

/**
 * An implementation of EjbInjectionServices for JBoss EJB3
 *
 * @author Pete Muir
 * @author ales.justin@jboss.org
 * @author Marius Bogoevici
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone </a>
 */
public class JBossEjbInjectionServices extends AbstractJBossServices implements EjbInjectionServices, DeploymentUnitAware
{
   private EjbReferenceResolver resolver;

   public JBossEjbInjectionServices() throws NamingException
   {
      super();
   }

   public void setResolver(EjbReferenceResolver resolver)
   {
      this.resolver = resolver;
   }

   public Object resolveEjb(InjectionPoint injectionPoint)
   {
      if (!injectionPoint.getAnnotated().isAnnotationPresent(EJB.class))
      {
         throw new IllegalArgumentException("No @EJB annotation found on injection point " + injectionPoint);
      }
      if (injectionPoint.getMember() instanceof Method && ((Method) injectionPoint.getMember()).getParameterTypes().length != 1)
      {
         throw new IllegalArgumentException("Injection point represents a method which doesn't follow JavaBean conventions (must have exactly one parameter) " + injectionPoint);
      }
      EJB annotation = injectionPoint.getAnnotated().getAnnotation(EJB.class);
      // Get properties from the annotation
      String beanName = annotation.beanName();
      String beanInterface = annotation.beanInterface().getName();

      // Supply beanInterface from reflection if not explicitly-defined
      if (beanInterface == null || beanInterface.equals(Object.class.getName()))
      {
         if (injectionPoint.getMember() instanceof Field && injectionPoint.getType() instanceof Class<?>)
         {
            beanInterface = ((Class<?>) injectionPoint.getType()).getName();
         }
         else if (injectionPoint.getMember() instanceof Method)
         {
            Method method = (Method) injectionPoint.getMember();
            beanInterface = method.getParameterTypes()[0].getName();
         }
      }

      String jndiName = resolver.resolveEjb(topLevelDeploymentUnit, new EjbReference(beanName, beanInterface, null));
      if (jndiName == null)
      {
         throw new IllegalStateException("No EJBs available which can be injected into " + injectionPoint);
      }
      try
      {
         return context.lookup(jndiName);
      }
      catch (NamingException e)
      {
         throw new RuntimeException("Error retreiving EJB from JNDI for injection point " + injectionPoint, e);
      }
   }

   @Override
   public void cleanup()
   {
      resolver = null;
   }
}