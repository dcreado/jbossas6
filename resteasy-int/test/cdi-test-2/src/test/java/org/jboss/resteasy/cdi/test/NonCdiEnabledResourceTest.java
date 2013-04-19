package org.jboss.resteasy.cdi.test;

import org.jboss.resteasy.cdi.test.noncdi.NonCdiResource;
import org.junit.Test;

/**
 * This test tests that JAX-RS dependency injection is performed on a JAX-RS resource {@link NonCdiResource}
 * which is not a CDI Bean (because of innapropriate constructor).
 * 
 * @author Jozef Hartinger
 *
 */
public class NonCdiEnabledResourceTest extends AbstractResourceTest
{

   @Override
   protected String getTestPrefix()
   {
      return "nonCdiResource/";
   }
   
   @Test
   public void testJaxrsConstructorInjection()
   {
      testPlainTextReadonlyResource(BASE_URI + getTestPrefix() + "jaxrsConstructorInjection", true);
   }

}
