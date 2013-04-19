package org.jboss.resteasy.cdi.test;

import org.junit.Test;

/**
 * Contains general tests executed against every CDI-enabled JAX-RS resource.
 * @author Jozef Hartinger
 *
 */
public abstract class AbstractCdiResourceTest extends AbstractResourceTest
{
   @Test
   public void testCdiFieldInjection()
   {
      testPlainTextReadonlyResource(BASE_URI + getTestPrefix() + "fieldInjection", true);
   }
   
   @Test
   public void testCdiConstructorInjection()
   {
      testPlainTextReadonlyResource(BASE_URI + getTestPrefix() + "constructorInjection", true);
   }
   
   @Test
   public void testCdiInitializerInjection()
   {
      testPlainTextReadonlyResource(BASE_URI + getTestPrefix() + "initializerInjection", true);
   }
   
   @Test
   public void testSubresource()
   {
      testPlainTextReadonlyResource(BASE_URI + getTestPrefix() + "subresource", "bar");
   }
}
