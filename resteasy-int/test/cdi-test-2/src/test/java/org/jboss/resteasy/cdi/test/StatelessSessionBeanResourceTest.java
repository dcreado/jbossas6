package org.jboss.resteasy.cdi.test;

import org.junit.Test;


public class StatelessSessionBeanResourceTest extends AbstractCdiResourceTest
{

   @Override
   protected String getTestPrefix()
   {
      return "statelessEjb/";
   }
   
   @Test
   public void testEjbFieldInjection()
   {
      testPlainTextReadonlyResource(BASE_URI + getTestPrefix() + "ejbFieldInjection", true);
   }
}
