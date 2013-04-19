package org.jboss.resteasy.test.jboss;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.junit.Test;
import org.junit.Assert;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class EjbTest
{
   @Test
   public void testIt() throws Exception
   {
      ClientRequest request = new ClientRequest("http://localhost:8080/ejb-test/scan");
      ClientResponse<String> response = request.get(String.class);
      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals("hello world", response.getEntity());

   }

   @Test
   public void testException() throws Exception
   {
      ClientRequest request = new ClientRequest("http://localhost:8080/ejb-test/scan/exception");
      ClientResponse<String> response = request.get(String.class);
      Assert.assertEquals(412, response.getStatus());

      request = new ClientRequest("http://localhost:8080/ejb-test/scan/custom-exception");
      response = request.get(String.class);
      Assert.assertEquals(412, response.getStatus());

      request = new ClientRequest("http://localhost:8080/ejb-test/scan/ejb-exception");
      response = request.get(String.class);
      Assert.assertEquals(412, response.getStatus());
   }}
