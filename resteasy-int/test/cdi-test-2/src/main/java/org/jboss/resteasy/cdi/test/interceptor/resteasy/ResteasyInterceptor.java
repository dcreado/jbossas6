package org.jboss.resteasy.cdi.test.interceptor.resteasy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.cdi.test.Cat;
import org.jboss.resteasy.spi.interception.MessageBodyWriterContext;
import org.jboss.resteasy.spi.interception.MessageBodyWriterInterceptor;

@Provider
@ServerInterceptor
public class ResteasyInterceptor implements MessageBodyWriterInterceptor
{

   @Inject
   private Cat cat;
   private Cat constructorCat;
   private Cat initializerCat;
   @Context
   private Providers providers;

   public ResteasyInterceptor()
   {
   }

   @Inject
   public ResteasyInterceptor(Cat cat)
   {
      constructorCat = cat;
   }

   @Inject
   public void init(Cat cat)
   {
      initializerCat = cat;
   }

   public void write(MessageBodyWriterContext context) throws IOException, WebApplicationException
   {
      context.proceed();

      if (!printProviderInjectionInformation(context.getAnnotations()))
      {

         BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(context.getOutputStream()));
         bw.write("CDI-enabled RESTEasy Interceptor");
         bw.write("\nCDI field injection: " + (cat != null));
         bw.write("\nCDI constructor injection: " + (constructorCat != null));
         bw.write("\nCDI initializer injection: " + (initializerCat != null));
         bw.write("\nJAX-RS field injection: " + (providers != null));
         bw.write("\nProvider toString(): " + toString());
         bw.flush();
      }
   }

   private boolean printProviderInjectionInformation(Annotation[] annotations)
   {
      for (Annotation annotation : annotations)
      {
         if (Path.class.isAssignableFrom(annotation.annotationType()))
         {
            Path path = (Path) annotation;
            if ("/resteasyInterceptor".equals(path.value()))
            {
               return false;
            }
         }
      }
      return true;
   }
}
