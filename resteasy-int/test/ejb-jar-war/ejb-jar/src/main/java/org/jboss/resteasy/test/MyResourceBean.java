package org.jboss.resteasy.test;

import javax.ejb.EJBException;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ejb.Stateless;
import javax.ejb.EJBContext;
import javax.annotation.Resource;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/scan")
@Stateless
public class MyResourceBean implements MyResource
{
   @Resource
   private EJBContext ctx;

   @GET
   @Produces("text/plain")
   public String get()
   {
      if (ctx == null)
      {
         System.out.println("NO CONTEXT!!!!!");
         throw new WebApplicationException(500);
      }
      System.out.println("CONTEXT WAS THERE!");
      return "hello world";
   }

   @GET
   @Produces("text/plain")
   @Path("exception")
   public String testException()
   {
      throw new WebApplicationException(412);
   }

   @Path("/custom-exception")
   @GET
   public String throwException()
   {
      throw new MyException();
   }


   @Path("/ejb-exception")
   @GET
   public String throwEjbException()
   {
      throw new EJBException(new MyException());
   }
}
