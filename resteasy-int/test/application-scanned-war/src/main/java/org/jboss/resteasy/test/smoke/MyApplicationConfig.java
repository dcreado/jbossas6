package org.jboss.resteasy.test.smoke;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@ApplicationPath("/app-path")
public class MyApplicationConfig extends Application
{
   private Set<Class<?>> classes = new HashSet<Class<?>>();
   private Set<Object> singletons = new HashSet<Object>();

   public static boolean created = false;

   public MyApplicationConfig()
   {
      created = true;
      singletons.add(new AppConfig.MyResource());
      singletons.add(new AppConfig.QuotedTextWriter());
      singletons.add(new AppConfig.FooExceptionMapper());
   }

   public Set<Class<?>> getClasses()
   {
      return classes;
   }

   @Override
   public Set<Object> getSingletons()
   {
      return singletons;
   }

}
