package org.jboss.resteasy.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Providers;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/scan-intf")
public interface MyIntf
{
   @GET
   @Produces("text/plain")
   String get();

   @Path("resolver")
   @GET
   @Produces("text/plain")
   String getResolver(@Context Providers providers);
}
