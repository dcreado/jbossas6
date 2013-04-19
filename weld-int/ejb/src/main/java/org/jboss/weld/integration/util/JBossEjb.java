package org.jboss.weld.integration.util;

import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.jpa.deployment.PersistenceUnitDeployment;
import org.jboss.kernel.plugins.bootstrap.basic.KernelConstants;
import org.jboss.kernel.spi.dependency.KernelController;

/**
 * Utitlies for use with JBoss EJB implementation
 *
 * @author Pete Muir
 * @author ales.justin@jboss.org
 */
public class JBossEjb
{
   private KernelController controller;

   @Inject(bean = KernelConstants.KERNEL_CONTROLLER_NAME)
   public void setController(KernelController controller)
   {
      this.controller = controller;
   }

   /**
    * Get the PersistenceUnitDeployment from the MC controller.
    *
    * @param name the bean name
    * @return the PersistenceUnitDeployment
    */
   public PersistenceUnitDeployment lookupPersistenceUnitDeployment(String name)
   {
      try
      {
         return (PersistenceUnitDeployment) lookup(name, false);
      }
      catch (NoSuchInstantiatedBeanException e)
      {
         throw new IllegalStateException("PersistenceUnitDeployment " + name + " cannot be found");
      }
   }

   /**
    * Get the bean from the MC controller.
    *
    * @param name the bean name
    * @param shouldExist must the bean be present
    * @return the bean
    */
   public Object lookup(final Object name, boolean shouldExist)
   {
      // Get Controller Context
      ControllerContext context = controller.getInstalledContext(name);
      if (context == null)
      {
         if (shouldExist)
         {
            // less restrictive state look
            ControllerContext cc = controller.getContext(name, null);
            throw new NoSuchInstantiatedBeanException("No such instantiated bean: " + name + " [" + cc + "]");
         }
         else
         {
            return null;
         }
      }

      // If there's an error with the context, throw it
      Throwable error = context.getError();
      if (error != null)
         throw new RuntimeException("Could not lookup object at name \"" + name + "\" due to an error with the underlying MC context.", error);

      // Return
      return context.getTarget();
   }

   private static class NoSuchInstantiatedBeanException extends IllegalArgumentException
   {
      public NoSuchInstantiatedBeanException()
      {
         super();
      }

      public NoSuchInstantiatedBeanException(String arg0, Throwable arg1)
      {
         super(arg0, arg1);
      }

      public NoSuchInstantiatedBeanException(String arg0)
      {
         super(arg0);
      }

      public NoSuchInstantiatedBeanException(Throwable arg0)
      {
         super(arg0);
      }
   }
}