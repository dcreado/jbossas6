package org.jboss.weld.integration.deployer.env.bda;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.resources.spi.ResourceLoadingException;

/**
 * A (@link ResourceLoader} implementation that uses a specific @{link ClassLoader}
 *
 * @author Marius Bogoevici
 * @author Ales Justin
 */
public class ClassLoaderResourceLoader implements ResourceLoader
{
   private WeakReference<ClassLoader> clRef;

   public ClassLoaderResourceLoader(ClassLoader classLoader)
   {
      clRef = new WeakReference<ClassLoader>(classLoader);
   }

   public Class<?> classForName(String name)
   {
      ClassLoader classLoader = clRef.get();
      if (classLoader == null)
         throw new ResourceLoadingException("Error loading class " + name + ", classloader GC-ed.");

      try
      {
         return classLoader.loadClass(name);
      }
      catch (ClassNotFoundException e)
      {
         throw new ResourceLoadingException("Error loading class " + name, e);
      }
      catch (NoClassDefFoundError e)
      {
         throw new ResourceLoadingException("Error loading class " + name, e);
      }
      catch (TypeNotPresentException e)
      {
         throw new ResourceLoadingException("Error loading class " + name, e);
      }
   }

   public URL getResource(String name)
   {
      ClassLoader classLoader = clRef.get();
      if (classLoader == null)
         return null;

      return classLoader.getResource(name);
   }

   public Collection<URL> getResources(String name)
   {
      ClassLoader classLoader = clRef.get();
      if (classLoader == null)
         return Collections.emptySet();

      try
      {
         final Enumeration<URL> enumResources = classLoader.getResources(name);
         ArrayList<URL> resources = new ArrayList<URL>();
         while (enumResources.hasMoreElements())
         {
            resources.add(enumResources.nextElement());
         }
         return resources;
      }
      catch (IOException e)
      {
         throw new ResourceLoadingException("Error loading resource " + name, e);
      }
   }

   public void cleanup()
   {
     // do nothing - the reference to consider a way of removing the CL reference , but this is currently not possible
     // due to BeanDeploymentArchiveImplementation instances being shared amongst deployments
     // setting this to null would cause those deployments to fail
   }
}
