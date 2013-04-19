package org.jboss.resteasy.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Providers;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/scan")
public class MyResource
{
   @GET
   @Produces("text/plain")
   public String get()
   {
      return "hello world";
   }

   @Path("resolver")
   @GET
   @Produces("text/plain")
   public String getResolver(@Context Providers providers)
   {
      System.out.println("IN RESOLVER!!!");
      MyContextResolver resolver = (MyContextResolver) providers.getContextResolver(Integer.class, MediaType.TEXT_PLAIN_TYPE);
      return resolver.getContext(null).toString();
   }
}
