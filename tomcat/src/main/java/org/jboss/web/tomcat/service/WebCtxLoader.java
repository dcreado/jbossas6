/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.web.tomcat.service;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.jboss.logging.Logger;

/**
 * Initial version of a JBoss implementation of the Tomcat Loader.
 *
 * @author Scott.Stark@jboss.org
 * @version $Revision: 104333 $
 */
public class WebCtxLoader
   implements Lifecycle, Loader
{
   private static final Logger log = Logger.getLogger(WebCtxLoader.class);
   /**
    * The ClassLoader used to scope the ENC
    */
   protected ClassLoader encLoader;
   /**
    * The ClassLoader returned from getClassLoader
    */
   protected ENCLoader ctxLoader;
   protected Container webContainer;
   protected URL warURL;
   protected TomcatInjectionContainer injectionContainer;

   /**
    * The set of repositories associated with this class loader.
    */
   private ArrayList repositories = new ArrayList();

   /**
    * Create a WebCtxLoader given the ENC scoping class loader.
    *
    * @param encLoader
    */
   public WebCtxLoader(ClassLoader encLoader)
   {
      this(encLoader, null);
   }
   public WebCtxLoader(ClassLoader encLoader, TomcatInjectionContainer container)
   {
      this.encLoader = encLoader;
      this.ctxLoader = new ENCLoader(encLoader);
      injectionContainer = container;
   }

   /**
    * Use an explicit classpath
    * 
    * @param classpath
    */
   public void setClasspath(List<URL> classpath)
   {
      for(URL path : classpath)
      {
         ctxLoader.addURLInternal(path);
      }
   }

   /**
    * Build the classpath from the war url WEB-INF/{classes,lib}. 
    * 
    * @param warURL
    * @throws MalformedURLException
    */
   public void setWarURL(URL warURL) throws MalformedURLException
   {
      this.warURL = warURL;
      String path = warURL.getFile();
      File classesDir = new File(path, "WEB-INF/classes");
      if (classesDir.exists())
      {
         ctxLoader.addURLInternal(classesDir.toURL());
      }
      File libDir = new File(path, "WEB-INF/lib");
      if (libDir.exists())
      {
         File[] jars = libDir.listFiles();
         int length = jars != null ? jars.length : 0;
         for (int j = 0; j < length; j++)
         {
            File jar = jars[j];
            if(jar.getAbsolutePath().endsWith(".jar"))
            {
               ctxLoader.addURLInternal(jar.toURL());
            }
         }
      }
   }

   public void addLifecycleListener(LifecycleListener listener)
   {
   }

   public LifecycleListener[] findLifecycleListeners()
   {
      return new LifecycleListener[0];
   }

   public void removeLifecycleListener(LifecycleListener listener)
   {
   }

   public void start() throws LifecycleException
   {
      // ctxLoader is set upon construction and nullified during stop
      if (this.ctxLoader == null)
         throw new LifecycleException("WebCtxLoader cannot be restarted");

      if (injectionContainer != null)
      {
         log.debug("injectionContainer enabled and processing beginning with JBoss WebCtxLoader");
         // we need to do this because the classloader is initialize by the web container and
         // the injection container needs the classloader so that it can build up Injectors and ENC populators
         injectionContainer.setClassLoader(getClassLoader());
         injectionContainer.processMetadata();
      }
      ServletContext servletContext = ((Context) webContainer).getServletContext();
      if (servletContext == null)
         return;
   }

   public void stop() throws LifecycleException
   {
      // Remove the ctxLoader mapping kept by the DirContextURLStreamHandler 
      DirContextURLStreamHandler.unbind(ctxLoader);
      org.apache.commons.logging.LogFactory.release(ctxLoader);
      org.apache.commons.logging.LogFactory.release(encLoader);
      this.encLoader = null;
      this.ctxLoader = null;
      this.repositories.clear();
      this.warURL = null;
      this.webContainer = null;
   }

   public void backgroundProcess()
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   /**
    * We must pass the wrapped encLoader as tomcat needs to see a unique
    * class loader that is distinct from the thread context class loader seen
    * to be in effect when the web app is started. This is due to how it
    * binds contexts using the DirContextURLStreamHandler class.
    *
    * @return The ENC scoping class loader
    * @see org.apache.naming.resources.DirContextURLStreamHandler
    */
   public ClassLoader getClassLoader()
   {
      return ctxLoader;
   }

   public Container getContainer()
   {
      return webContainer;
   }

   public void setContainer(Container container)
   {
      webContainer = container;

   }

   public boolean getDelegate()
   {
      return false;
   }

   public void setDelegate(boolean delegate)
   {
   }

   public String getInfo()
   {
      return null;
   }

   public boolean getReloadable()
   {
      return false;
   }

   public void setReloadable(boolean reloadable)
   {
   }

   public void addPropertyChangeListener(PropertyChangeListener listener)
   {
   }

   public void addRepository(String repository)
   {
      if (repositories.contains(repository) == true)
         return;
      repositories.add(repository);
   }

   public String[] findLoaderRepositories() {
      return findRepositories();
   }

   public String[] findRepositories()
   {
      String[] tmp = new String[repositories.size()];
      repositories.toArray(tmp);
      return tmp;
   }

   public boolean modified()
   {
      return false;
   }

   public void removePropertyChangeListener(PropertyChangeListener listener)
   {
   }

   /**
    * A trival extension of URLClassLoader that uses an empty URL[] as its
    * classpath so that all work is delegated to its parent.
    */
   static class ENCLoader extends URLClassLoader
   {
      private URL[] urllist = new URL[0];

      ENCLoader(ClassLoader parent)
      {
         super(new URL[0], parent);
      }

      void addURLInternal(URL url)
      {
         URL[] result = new URL[urllist.length + 1];
         for (int i = 0; i < urllist.length; i++)
            result[i] = urllist[i];
         result[urllist.length] = url;
         urllist = result;
      }

      public URL[] getURLs()
      {
         return urllist;
      }
   }
}
