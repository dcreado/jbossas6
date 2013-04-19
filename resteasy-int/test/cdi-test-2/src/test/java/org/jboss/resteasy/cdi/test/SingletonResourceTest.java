package org.jboss.resteasy.cdi.test;

public class SingletonResourceTest extends StatelessSessionBeanResourceTest
{
   @Override
   protected String getTestPrefix()
   {
      return "singleton/";
   }
}
