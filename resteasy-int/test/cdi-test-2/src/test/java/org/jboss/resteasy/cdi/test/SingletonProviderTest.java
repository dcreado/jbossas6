package org.jboss.resteasy.cdi.test;

public class SingletonProviderTest extends BeanClassLocalViewEjbProviderTest
{
   @Override
   protected String getTestPrefix()
   {
      return "singleton/providers";
   }
}
