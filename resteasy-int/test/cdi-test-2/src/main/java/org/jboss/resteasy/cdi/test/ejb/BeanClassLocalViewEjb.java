package org.jboss.resteasy.cdi.test.ejb;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.cdi.test.Cat;
import org.jboss.resteasy.cdi.test.Subresource;

@Stateless
@Path("/beanClassLocalViewEjb")
@Produces("text/plain")
public class BeanClassLocalViewEjb
{
   private static int uniqueId = 0;
   private int id = uniqueId++;

   @Inject
   private Cat cat;
   @EJB
   private InjectedStatelessEjbLocal statelessEjb;
   private Cat constructorCat;
   private Cat initializerCat;
   @Context
   private UriInfo uriInfo;
   private UriInfo setterUriInfo;
   @Inject
   private Subresource subresource;

   public BeanClassLocalViewEjb()
   {
   }

   @Inject
   public BeanClassLocalViewEjb(Cat cat)
   {
      constructorCat = cat;
   }

   @Inject
   public void init(Cat cat)
   {
      initializerCat = cat;
   }

   @GET
   @Path("/fieldInjection")
   public boolean fieldInjection()
   {
      return cat != null;
   }

   @GET
   @Path("/ejbFieldInjection")
   public boolean testEjbFieldInjection()
   {
      return statelessEjb.foo();
   }

   @GET
   @Path("/jaxrsFieldInjection")
   public boolean jaxrsFieldInjection()
   {
      return uriInfo != null;
   }

   @GET
   @Path("/jaxrsSetterInjection")
   public boolean jaxrsSetterInjection()
   {
      return setterUriInfo != null;
   }

   @GET
   @Path("/constructorInjection")
   public boolean constructorInjection()
   {
      return constructorCat != null;
   }

   @GET
   @Path("/initializerInjection")
   public boolean initializerInjection()
   {
      return initializerCat != null;
   }

   @GET
   @Path("/jaxrsMethodInjection")
   public String jaxrsMethodInjection(@QueryParam("foo") String query)
   {
      return query;
   }

   @GET
   @Path("/toString")
   public int getId()
   {
      return id;
   }
   
   @GET
   @Path("/providers")
   public void testProviders()
   {
      throw new NullPointerException();
   }

   @Context
   public void setSetterUriInfo(UriInfo setterUriInfo)
   {
      this.setterUriInfo = setterUriInfo;
   }

   @Path("/subresource")
   public Subresource subresource()
   {
      return subresource;
   }
}
