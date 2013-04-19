package org.jboss.resteasy.cdi.test;

import org.junit.Test;

public class ProviderTest extends AbstractProviderTest
{
   @Override
   protected String getTestPrefix()
   {
      return "resource/providers";
   }

   @Test
   public void testCdiConstructorInjection()
   {
      testPlainTextReadonlyResource(BASE_URI + getTestPrefix(), "CDI constructor injection: true");
   }
}
