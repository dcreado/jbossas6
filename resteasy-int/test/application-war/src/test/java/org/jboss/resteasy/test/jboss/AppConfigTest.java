package org.jboss.resteasy.test.jboss;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AppConfigTest
{
   private void _test(HttpClient client, String uri, String body)
   {
      {
         GetMethod method = new GetMethod(uri);
         try
         {
            method.addRequestHeader("Accept", "text/quoted");
            int status = client.executeMethod(method);
            Assert.assertEquals(status, HttpResponseCodes.SC_OK);
            Assert.assertEquals(body, method.getResponseBodyAsString());
         }
         catch (IOException e)
         {
            throw new RuntimeException(e);
         }
      }

   }


   @Test
   public void testIt()
   {
      HttpClient client = new HttpClient();
      _test(client, "http://localhost:8080/application-config-test/my", "\"hello\"");
   }

   @Test
   public void testSingletons() throws Exception
   {
      ClientRequest request = new ClientRequest("http://localhost:8080/application-config-test/my/exception");
      ClientResponse response = request.get();
      Assert.assertEquals(412, response.getStatus());

      ClientRequest countRequest = new ClientRequest("http://localhost:8080/application-config-test/my/exception/count");
      String res = countRequest.getTarget(String.class);
      Assert.assertEquals("1", res);

      request.clear();
      response = request.get();
      Assert.assertEquals(412, response.getStatus());

      countRequest.clear();
      res = countRequest.getTarget(String.class);
      Assert.assertEquals("1", res);
   }
}
