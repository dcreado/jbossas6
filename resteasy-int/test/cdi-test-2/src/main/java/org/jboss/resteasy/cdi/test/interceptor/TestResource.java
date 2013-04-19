package org.jboss.resteasy.cdi.test.interceptor;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 * This test verifies that a JAX-RS resource method invocation
 * can be intercepted by Interceptor bound using CDI interceptor
 * binding.
 * 
 * @author Jozef Hartinger
 *
 */

@Path("/interceptor")
@Produces("text/plain")
public class TestResource
{
   @Context
   private UriInfo uriInfo;
   
   @GET
   @Path("/interceptedMethod")
   @TestInterceptorBinding
   public boolean getValue()
   {
      return false;
   }
   
   @GET
   @Path("/jaxrsFieldInjection")
   public boolean jaxrsFieldInjection()
   {
      return uriInfo != null;
   }
}
