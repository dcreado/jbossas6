package org.jboss.resteasy.cdi.test;

import org.junit.Test;


public class AlternativeTest extends AbstractTest
{
   @Test
   public void testAlternative()
   {
      testPlainTextReadonlyResource(BASE_URI + "alternative", "MockResource");
   }
}
