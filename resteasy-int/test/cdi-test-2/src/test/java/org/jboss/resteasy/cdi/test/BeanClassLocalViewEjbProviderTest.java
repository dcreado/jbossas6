package org.jboss.resteasy.cdi.test;

import org.junit.Test;

public class BeanClassLocalViewEjbProviderTest extends AbstractProviderTest
{
   @Override
   protected String getTestPrefix()
   {
      return "beanClassLocalViewEjb/providers";
   }
   
   @Test
   public void testEjbInjection()
   {
      testPlainTextReadonlyResource(BASE_URI + getTestPrefix(), "EJB injection: true");
   }
}
