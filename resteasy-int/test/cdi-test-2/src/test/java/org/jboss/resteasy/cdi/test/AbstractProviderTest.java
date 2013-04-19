package org.jboss.resteasy.cdi.test;

import org.junit.Test;

/**
 * Contains general tests executed against every JAX-RS provider.
 * @author Jozef Hartinger
 *
 */
public abstract class AbstractProviderTest extends AbstractTest
{
   
   abstract protected String getTestPrefix();
   
   @Test
   public void testCdiFieldInjection()
   {
      testPlainTextReadonlyResource(BASE_URI + getTestPrefix(), "CDI field injection: true");
   }
   
   @Test
   public void testCdiInitializerInjection()
   {
      testPlainTextReadonlyResource(BASE_URI + getTestPrefix(), "CDI initializer injection: true");
   }
   
   @Test
   public void testJaxrsFieldInjection()
   {
      testPlainTextReadonlyResource(BASE_URI + getTestPrefix(), "JAX-RS field injection: true");
   }
}
