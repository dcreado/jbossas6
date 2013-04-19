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
package org.jboss.weld.integration.deployer.env.bda;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import org.jboss.classloader.plugins.loader.ClassLoaderToLoaderAdapter;
import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloader.spi.Loader;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.logging.Logger;

/**
 * Given the ClassLoader that is loading an archive during deployment, this factory
 * creates the corresponding classpath.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 108370 $
 */
class ClasspathFactory
{
   // the log
   private static Logger log = Logger.getLogger(ClasspathFactory.class);

   // the instance
   private static final ClasspathFactory instance = new ClasspathFactory();
   
   /**
    * Returns the singleton instance.
    * 
    * @return the ClasspathFactory instance
    */
   public static final ClasspathFactory getInstance()
   {
      return instance;
   }
   
   // the ClassLoaderSystem
   private volatile ClassLoaderSystem system;

   // the default domain
   private volatile ClassLoaderDomain defaultDomain;

   // the default classpath, corresponds to DefaultDomain
   private volatile Classpath defaultClasspath;

   // lib archives provider
   private LibraryArchivesProvider libArchivesProvider;

   // a list of domains
   private final Map<Loader, WeakReference<Classpath>> domainToClasspath;

   private ClasspathFactory()
   {
      domainToClasspath = new WeakHashMap<Loader, WeakReference<Classpath>>();
   }

   protected ClassLoaderSystem getSystem()
   {
      if (system == null)
         setSystem(ClassLoaderSystem.getInstance());
         
      return system;
   }

   public void setSystem(ClassLoaderSystem system)
   {
      if (system == null)
         throw new IllegalArgumentException("Null system");

      this.system = system;
      defaultDomain = system.getDefaultDomain();
      defaultClasspath = new ClasspathImpl(defaultDomain.getName());
   }

   public void setLibArchivesProvider(LibraryArchivesProvider libArchivesProvider)
   {
      this.libArchivesProvider = libArchivesProvider;
   }

   /**
    * Create libs.
    *
    * A bit of impl detail, in case we change the actual behavior:
    *  libs will be automatically added to default classpath,
    *  hence no need for explicit addition.
    *
    * @throws Exception for any error
    */
   public void create() throws Exception
   {
      if (libArchivesProvider != null)
         libArchivesProvider.getLibraries();
   }

   /**
    * Creates the Classpath corresponding to ClassLoader.
    * 
    * @param classLoader the ClassLoader
    * @return            a classpath that contains a list of the archives visible to ClassLoader
    */
   public Classpath create(ClassLoader classLoader)
   {
      Module module = SecurityActions.getModuleForClassLoader(classLoader);
      ClassLoaderDomain domain = null;
      ClassLoaderSystem cls = getSystem(); // intialize system
      // TODO -- why this check for a parent domain name?
      if (module != null && module.getDeterminedParentDomainName() != null)
      {
         domain = cls.getDomain(module.getDeterminedDomainName());
      }
      return getClasspath(domain);
   }

   @SuppressWarnings({"SynchronizeOnNonFinalField", "SynchronizationOnLocalVariableOrMethodParameter"})
   private Classpath getClasspath(Loader domain)
   {
      if (domain == null || domain == defaultDomain)
      {
         return defaultClasspath;
      }
      synchronized(domain)
      {
         Classpath classpath = getCachedClasspath(domain);
         if (classpath == null)
         {
            if (domain instanceof ClassLoaderToLoaderAdapter)
            {
               ClassLoaderToLoaderAdapter cl2la = (ClassLoaderToLoaderAdapter) domain;
               ClassLoader unitLoader = SecurityActions.getClassLoader(cl2la);
               ArchiveInfo archiveInfo = unitLoader == null? null: ArchiveInfo.getInstance(unitLoader);
               if (archiveInfo == null)
               {
                  classpath = new NoDuplicatesClasspath(domain.toString());
               }
               else
               {
                  classpath = archiveInfo.getClasspathAdapter();
               }
            }
            else
            {
               if (domain instanceof ClassLoaderDomain)
               {
                  ClassLoaderDomain clDomain = (ClassLoaderDomain) domain;
                  Classpath parentClasspath = getClasspath(clDomain.getParent());
                  classpath = new NoDuplicatesClasspath(clDomain.getName(), parentClasspath);
               }
               else
               {
                  throw new RuntimeException("Domain is of unexpected type: " + domain + " - " + domain.getClass());
               }
            }
            addClasspathToCache(domain, classpath);
         }
         return classpath;
      }
   }

   private Classpath getCachedClasspath(Loader domain)
   {
      WeakReference<Classpath> ref = domainToClasspath.get(domain);
      if (ref == null)
      {
         return null;
      }
      return ref.get();
   }

   private void addClasspathToCache(Loader domain, Classpath domainClasspath)
   {
      domainToClasspath.put(domain, new WeakReference<Classpath>(domainClasspath));
   }
}