package org.jboss.resteasy.cdi.test;

import org.junit.Test;

/**
 * Contains general tests executed against every JAX-RS resource.
 * @author Jozef Hartinger
 *
 */
public abstract class AbstractResourceTest extends AbstractTest
{
   abstract protected String getTestPrefix();

   @Test
   public void testJaxrsFieldInjection()
   {
      testPlainTextReadonlyResource(BASE_URI + getTestPrefix() + "jaxrsFieldInjection", true);
   }

   @Test
   public void testJaxrsSetterInjection()
   {
      testPlainTextReadonlyResource(BASE_URI + getTestPrefix() + "jaxrsSetterInjection", true);
   }

   @Test
   public void testJaxrsMethodInjection()
   {
      testPlainTextReadonlyResource(BASE_URI + getTestPrefix() + "jaxrsMethodInjection?foo=bar", "bar");
   }
}
