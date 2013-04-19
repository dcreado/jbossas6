package org.jboss.resteasy.test.jboss;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ScanningTest
{
   @Test
   public void testIt() throws Exception
   {
      ClientRequest request = new ClientRequest("http://localhost:8080/scanned-test/prefixed/scan");
      ClientResponse<String> response = request.get(String.class);
      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals("hello world", response.getEntity());

   }

   @Test
   public void testScannedContextResolver() throws Exception
   {
      ClientRequest request = new ClientRequest("http://localhost:8080/scanned-test/prefixed/scan/resolver");
      String res = request.getTarget(String.class);
      Assert.assertEquals(res, "42");
   }


   @Test
   public void testItIntf() throws Exception
   {
      ClientRequest request = new ClientRequest("http://localhost:8080/scanned-test/prefixed/scan-intf");
      ClientResponse<String> response = request.get(String.class);
      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals("hello world", response.getEntity());

   }

   @Test
   public void testScannedContextResolverIntf() throws Exception
   {
      ClientRequest request = new ClientRequest("http://localhost:8080/scanned-test/prefixed/scan-intf/resolver");
      String res = request.getTarget(String.class);
      Assert.assertEquals(res, "42");
   }
}
