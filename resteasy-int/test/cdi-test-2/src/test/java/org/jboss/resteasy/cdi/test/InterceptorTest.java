package org.jboss.resteasy.cdi.test;

import org.junit.Test;


public class InterceptorTest extends AbstractTest
{
   @Test
   public void testInterceptor()
   {
      testPlainTextReadonlyResource(BASE_URI + "interceptor/interceptedMethod", true);
   }
   
   // @Test
   // WELD-557
   public void testJaxrsFieldInjection()
   {
      testPlainTextReadonlyResource(BASE_URI + "interceptor/jaxrsFieldInjection", true);
   }
}
