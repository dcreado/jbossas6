package org.jboss.resteasy.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/scan")
public interface MyResource
{
   @GET
   @Produces("text/plain")
   String get();

   @GET
   @Produces("text/plain")
   @Path("exception")
   public String testException();

   @Path("/custom-exception")
   @GET
   String throwException();

   @Path("/ejb-exception")
   @GET
   String throwEjbException();
}
