package org.jboss.resteasy.cdi.test.noncdi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Path("/nonCdiResource")
@Produces("text/plain")
public class NonCdiResource
{
   private UriInfo constructorInjectedField;
   @Context
   private UriInfo uriInfo;
   @QueryParam("foo")
   private String fieldQuery;
   private UriInfo setterUriInfo;

   public NonCdiResource(@Context UriInfo constructorInjectedField)
   {
      this.constructorInjectedField = constructorInjectedField;
   }

   @GET
   @Path("/jaxrsFieldInjection")
   public boolean jaxrsFieldInjection()
   {
      return uriInfo != null;
   }

   @GET
   @Path("/jaxrsFieldInjection2")
   public String jaxrsFieldInjection2()
   {
      return fieldQuery;
   }

   @GET
   @Path("/jaxrsConstructorInjection")
   public boolean jaxrsConstructorInjection()
   {
      return constructorInjectedField != null;
   }

   @GET
   @Path("/jaxrsSetterInjection")
   public boolean jaxrsSetterInjection()
   {
      return setterUriInfo != null;
   }

   @GET
   @Path("/jaxrsMethodInjection")
   public String jaxrsMethodInjection(@QueryParam("foo") String query)
   {
      return query;
   }

   @GET
   @Path("/toString")
   @Override
   public String toString()
   {
      return super.toString();
   }

   @Context
   public void setSetterUriInfo(UriInfo setterUriInfo)
   {
      this.setterUriInfo = setterUriInfo;
   }
}
