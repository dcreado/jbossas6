package org.jboss.resteasy.cdi.test.ejb;

import javax.ejb.Local;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.annotations.Form;
import org.jboss.resteasy.cdi.test.Dog;
import org.jboss.resteasy.cdi.test.FormCarrier;
import org.jboss.resteasy.cdi.test.Subresource;

@Local
@Path("/statefulSessionBeanResource")
@Produces("text/plain")
public interface StatefulSessionBeanResourceLocal
{
   @GET
   @Path("/fieldInjection")
   public boolean fieldInjection();
   
   @GET
   @Path("/ejbFieldInjection")
   public boolean testEjbFieldInjection();
   
   @GET
   @Path("/jaxrsFieldInjection")
   public boolean jaxrsFieldInjection();
   
   @GET
   @Path("/jaxrsFieldInjection2")
   public String jaxrsFieldInjection2();
   
   @GET
   @Path("/jaxrsSetterInjection")
   public boolean jaxrsSetterInjection();
   
   @GET
   @Path("/constructorInjection")
   public boolean constructorInjection();
   
   @GET
   @Path("/initializerInjection")
   public boolean initializerInjection();
   
   @GET
   @Path("/jaxrsMethodInjection")
   public String jaxrsMethodInjection(@QueryParam("foo") String query);
   
   @GET
   @Path("/toString")
   public int getId();
   
   @GET
   @Path("/form/{foo}")
   public boolean form(@Form FormCarrier form);
   
   @GET
   @Path("/providers")
   public Dog testProviders();
   
   public void setSetterUriInfo(UriInfo setterUriInfo);
   
   @Path("/subresource")
   public Subresource subresource();

   public void remove();
}
