package org.jboss.resteasy.test.smoke;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AppConfig
{
   @Path("/my")
   public static class MyResource
   {
      @GET
      @Produces("text/quoted")
      public String get()
      {
         return "hello";
      }

      @GET
      @Path("exception")
      public String getException()
      {
         throw new FooException();
      }

      @GET
      @Path("exception/count")
      @Produces("text/plain")
      public String getCount()
      {
         return Integer.toString(FooExceptionMapper.num_instantiations);
      }
   }

   public static class FooException extends RuntimeException
   {
   }

   @Provider
   public static class FooExceptionMapper implements ExceptionMapper<FooException>
   {
      public static int num_instantiations = 0;

      public FooExceptionMapper()
      {
         num_instantiations++;
      }

      public Response toResponse(FooException exception)
      {
         return Response.status(412).build();
      }
   }

   @Provider
   @Produces("text/quoted")
   public static class QuotedTextWriter implements MessageBodyWriter<String>
   {
      public static int num_instantiations = 0;

      public QuotedTextWriter()
      {
         num_instantiations++;
      }

      public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
      {
         return type.equals(String.class);
      }

      public long getSize(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
      {
         return -1;
      }

      public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException
      {
         s = "\"" + s + "\"";
         entityStream.write(s.getBytes());
      }
   }

}