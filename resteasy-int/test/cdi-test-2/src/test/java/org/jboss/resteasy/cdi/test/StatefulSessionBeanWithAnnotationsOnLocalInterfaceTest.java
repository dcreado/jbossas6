package org.jboss.resteasy.cdi.test;

import org.junit.Test;

public class StatefulSessionBeanWithAnnotationsOnLocalInterfaceTest extends ResourceTest
{

   @Override
   protected String getTestPrefix()
   {
      return "statefulSessionBeanResource/";
   }
   
   @Test
   public void testEjbFieldInjection()
   {
      testPlainTextReadonlyResource(BASE_URI + getTestPrefix() + "ejbFieldInjection", true);
   }
   
   @Override
   // This test is disabled temporarily
   public void testSubresource()
   {
   }
}
