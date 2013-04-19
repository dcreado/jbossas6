package org.jboss.weld.integration.util;

import java.beans.Introspector;
import java.lang.reflect.Method;

public class Reflections
{

   /**
    * Creates an instance from a class name
    *
    * @param name The class name
    * @return The instance
    * @throws ClassNotFoundException If the class if not found
    */
   public static Class<?> classForName(String name, ClassLoader classLoader) throws ClassNotFoundException
   {
      try
      {
         return classLoader.loadClass(name);
      }
      catch (Exception e)
      {
         return Class.forName(name);
      }
   }

   public static String getPropertyName(Method method)
   {
      String methodName = method.getName();
      if (methodName.matches("^(get).*") && method.getParameterTypes().length == 0)
      {
         return Introspector.decapitalize(methodName.substring(3));
      }
      else if (methodName.matches("^(is).*") && method.getParameterTypes().length == 0)
      {
         return Introspector.decapitalize(methodName.substring(2));
      }
      else
      {
         return null;
      }

   }

}