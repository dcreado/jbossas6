package org.jboss.weld.integration.injection;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.weld.manager.api.WeldManager;

/**
 * Helper class for injecting Web Beans into non-contextual objects
 *
 * @author Marius Bogoevici
 */
public class NonContextualObjectInjectionHelper
{
   @SuppressWarnings("unchecked")
   public static void injectNonContextualInstance(Object instance, WeldManager beanManager)
   {
      if (beanManager == null)
         throw new IllegalArgumentException("Null bean manager.");

      CreationalContext<Object> creationalContext =  beanManager.createCreationalContext(null);
      InjectionTarget<Object> injectionTarget = (InjectionTarget<Object>) beanManager.fireProcessInjectionTarget(beanManager.createAnnotatedType(instance.getClass()));
      injectionTarget.inject(instance, creationalContext);
   }
}