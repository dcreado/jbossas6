package org.jboss.weld.integration.provider;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.weld.bootstrap.api.Singleton;
import org.jboss.weld.bootstrap.api.SingletonProvider;
import org.jboss.weld.integration.deployer.env.bda.DUTopLevelClassLoaderGetter;
import org.jboss.weld.integration.deployer.env.bda.TopLevelClassLoaderGetter;

/**
 * JBoss custom singleton provider.
 * It looks for top level classloader to isolate Weld components.
 *
 * @author Marius Bogoevici
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class JBossSingletonProvider extends SingletonProvider
{
   private TopLevelClassLoaderGetter topLevelClassLoaderGetter;

   public void setTopLevelClassLoaderGetter(TopLevelClassLoaderGetter topLevelClassLoaderGetter)
   {
      this.topLevelClassLoaderGetter = topLevelClassLoaderGetter;
   }

   public void start()
   {
      if (topLevelClassLoaderGetter == null)
         topLevelClassLoaderGetter = DUTopLevelClassLoaderGetter.getInstance();
   }
   
   @Override
   public <T> Singleton<T> create(Class<? extends T> expectedType)
   {
      return new TopLevelSingleton<T>();
   }

   private class TopLevelSingleton<T> implements Singleton<T>
   {
      private final ConcurrentMap<ClassLoader, T> store = new ConcurrentHashMap<ClassLoader, T>();

      public T get()
      {
         ClassLoader currentClassLoader = getTopLevelClassLoader();
         T value = store.get(currentClassLoader);
         if (value == null)
         {
            throw new IllegalStateException("Singleton not set for " + currentClassLoader);
         }
         return value;
      }

      public boolean isSet()
      {
         return store.containsKey(getTopLevelClassLoader());
      }

      public void set(T object)
      {
         store.put(getTopLevelClassLoader(), object);
      }

      public void clear()
      {
         store.remove(getTopLevelClassLoader());
      }

      private ClassLoader getTopLevelClassLoader()
      {
         return topLevelClassLoaderGetter.getTopLevelClassLoader(getThreadContextClassLoader());
      }
   }

   private static ClassLoader getThreadContextClassLoader()
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
         {
            public ClassLoader run()
            {
               return Thread.currentThread().getContextClassLoader();
            }
         });
      }
      else
      {
         return Thread.currentThread().getContextClassLoader();
      }
   }
}
