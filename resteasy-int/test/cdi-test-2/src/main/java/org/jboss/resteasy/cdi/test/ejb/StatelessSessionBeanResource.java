package org.jboss.resteasy.cdi.test.ejb;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.cdi.test.Cat;
import org.jboss.resteasy.cdi.test.Dog;
import org.jboss.resteasy.cdi.test.Subresource;

@Stateless
public class StatelessSessionBeanResource implements StatelessSessionBeanResourceLocal
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
   
   public StatelessSessionBeanResource()
   {
   }

   @Inject
   public StatelessSessionBeanResource(Cat cat)
   {
      constructorCat = cat;
   }
   
   @Inject
   public void init(Cat cat)
   {
      initializerCat = cat;
   }

   public boolean fieldInjection()
   {
      return cat != null;
   }
   
   public boolean testEjbFieldInjection()
   {
      return statelessEjb.foo();
   }
   
   public boolean jaxrsFieldInjection()
   {
      return uriInfo != null;
   }
   
   public boolean jaxrsSetterInjection()
   {
      return setterUriInfo != null;
   }
   
   public boolean constructorInjection()
   {
      return constructorCat != null;
   }
   
   public boolean initializerInjection()
   {
      return initializerCat != null;
   }
   
   public String jaxrsMethodInjection(String query)
   {
      return query;
   }
   
   public int getId()
   {
      return id;
   }
   
   public Dog testProviders()
   {
      return new Dog();
   }
   
   @Context
   public void setSetterUriInfo(UriInfo setterUriInfo)
   {
      this.setterUriInfo = setterUriInfo;
   }
   
   public Subresource subresource()
   {
      return subresource;
   }
}
