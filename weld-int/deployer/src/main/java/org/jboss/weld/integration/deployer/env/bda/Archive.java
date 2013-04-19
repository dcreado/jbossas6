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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.integration.util.IdFactory;
import org.jboss.weld.resources.spi.ResourceLoader;

/**
 * An archive is an abstract representation of one or more deployed archives.
 * It mainly contains a list of classes and an optional list of bean.xml file URLs
 * (only when those files are available).
 * <br>
 * Every archive can provide a corresponding BeanDeploymentArchive when requested. Notice
 * that the provided BeanDeploymentArchive is simply a different view of the archive.
 * <br>
 * As for class loading related issues, every archive is associated with the class loader
 * responsible for loading it during deployment and is also associated with a classpath.
 * This classpath contains a list of all archives visible to an archive and is used for
 * iterating over both the archive graph and the corresponding BeanDeploymentArchive graph.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @version $Revision: 109063 $
 * @see Classpath
 */
class Archive
{
   // keep a map of Archive instances
   private static final Map<ClassLoader, WeakReference<Archive>> instances = new WeakHashMap<ClassLoader, WeakReference<Archive>>();
   
   /**
    * Returns the Archive instance corresponding to {@code classLoader}.
    * 
    * @param classLoader key to search for the Archive
    * @return the Archive that corresponds to {@code classLoader}
    */
   public static Archive getInstance(ClassLoader classLoader)
   {
      synchronized(instances)
      {
         WeakReference<Archive> reference = instances.get(classLoader);
         if (reference == null) return null;
         return reference.get();
      }
   }
   
   // the classes contained in this archive
   private final Collection<String> classes;

   // the bean.xml URLs contained in this archive
   private final Collection<URL> xmlURLs;

   // the classloader that loaded this archive
   private final ClassLoader classLoader;
   
   // the classpath of this archive
   private final Classpath classpath;

   // the bda view of this archive, if available
   private BeanDeploymentArchiveImpl bda;

   // the ejbs
   private final Collection<EjbDescriptor<?>> ejbs;

   // the list of all bdaLifecycleListeners
   private final Collection<ArchiveLifecycleListener> lifecycleListeners;

   /**
    * Constructor. Can only be called by ArchiveFactory.
    * 
    * @param archiveInfo contains all information necessary for the initialization of this
    *                    archive
    * @param ejbs        the list of ejb descriptors
    * @see ArchiveFactory#createArchive(ArchiveInfo, Collection)
    */
   public Archive(ArchiveInfo archiveInfo, Collection<EjbDescriptor<?>> ejbs)
   {
      this.lifecycleListeners = new ArrayList<ArchiveLifecycleListener>();
      this.classes = archiveInfo.getEnvironment().getWeldClasses();
      this.xmlURLs = archiveInfo.getEnvironment().getWeldXml();
      this.classLoader = archiveInfo.getClassLoader();
      this.classpath = archiveInfo.getClasspath();
      this.classpath.addArchive(this);
      // configure only the ejbs that are visible to this archive
      this.ejbs = filterDescriptors(archiveInfo, ejbs);
      // update instances map
      synchronized (instances)
      {
         instances.put(this.classLoader, new WeakReference<Archive>(this));
      }
   }

   private static Collection<EjbDescriptor<?>> filterDescriptors(ArchiveInfo archiveInfo, Collection<EjbDescriptor<?>> ejbs)
   {
      ArrayList<EjbDescriptor<?>> descriptors = new ArrayList<EjbDescriptor<?>>();
      for (EjbDescriptor<?> ejbDescriptor: ejbs)
      {
         Collection<String> ejbNames = archiveInfo.getEjbNames();
         if (ejbNames.contains(ejbDescriptor.getEjbName()))
         {
            descriptors.add(ejbDescriptor);
         }
      }
      return descriptors;
   }

   /**
    * Returns all classes contained in this archive.
    * 
    * @return the classes contained in this archive
    */
   public Collection<String> getClasses()
   {
      return classes;
   }

   /**
    * Indicates whether this archive contains the requested class.
    * 
    * @param beanClass a class that could be contained in this archive
    * @return          {@code true} only if this archive contains {@code beanClass}
    */
   public boolean containsClass(Class<?> beanClass)
   {
      return beanClass.getClassLoader() == this.classLoader;
   }
   
   /**
    * Indicates whether {@code beanClass} is visible to this BDA.
    *
    * @param   beanClass the beanClass
    * @return  {@code true} if {@code beanClass} is visible to this BDA
    */
   public boolean isClassVisible(Class<?> beanClass)
   {
      Class<?> loadedClass;
      try
      {
         loadedClass = this.getClassLoader().loadClass(beanClass.getName());
      }
      catch (ClassNotFoundException e)
      {
         return false;
      }
      return loadedClass == beanClass;
   }

   /**
    * Adds a class to this archive.
    * 
    * @param beanClass a class whose ClassLoader is associated with this archive
    */
   public void addClass(Class<?> beanClass)
   {
      // TODO Move this higher up the hierarchy, so we never load the class
      classes.add(beanClass.getName());
   }

   /**
    * Returns the URLs of all bean.xml files contained in this archive.
    * 
    * @return the URLs of the bean.xml files
    */
   public Collection<URL> getXmlURLs()
   {
      return xmlURLs;
   }

   /**
    * Indicates whether this archive has one or more bean.xml files
    * 
    * @return {@code true} if this archive contains a bean.xml file
    */
   public boolean hasXml()
   {
      return xmlURLs != null && !xmlURLs.isEmpty();
   }

   /**
    * Returns the classloader that loaded this archive.
    * 
    * @return the classloader that loaded this archive
    */
   public ClassLoader getClassLoader()
   {
      return classLoader;
   }

   /**
    * Returns the classpath of this archive, that contains all archives visible to this
    * archive. This classpath can also be used to retrieve the bdas visible to the bda
    * associated with this archive, when this bda is avaialble.
    * 
    * @return the classpath
    */
   public Classpath getClasspath()
   {
      return classpath;
   }

   /**
    * Returns the BeanDeploymentArchive that corresponds to this archive.
    * 
    * @return the BeanDeploymentArchive representing this archive. May be {@code null} if
    *         it has not been created
    */
   public BeanDeploymentArchive getBeanDeploymentArchive()
   {
      return bda;
   }

   /**
    * Creates the BeanDeploymentArchive that corresponds to this archive. This method
    * never returns duplicates.
    * 
    * @param bootstrap the Weld boostrap 
    * @param services the services
    * @return the BeanDeploymentArchive representing this archive. If this bda has not
    *         been created, it is created and returned
    */
   public BeanDeploymentArchive createBeanDeploymentArchive(Bootstrap bootstrap, ServiceRegistry services)
   {
      if (bda == null)
      {
         services.add(ResourceLoader.class, new ClassLoaderResourceLoader(classLoader));
         bda = new BeanDeploymentArchiveImpl(IdFactory.getIdFromClassLoader(classLoader), bootstrap, services, this);
         for (ArchiveLifecycleListener listener: lifecycleListeners)
         {
            // notifies the listener that this archive became visible as a BDA
            listener.archiveVisible(this, bda);
         }
      }
      return bda;
   }
   
   /**
    * Creates the BeanDeploymentArchive that corresponds to this archive. This method
    * never returns duplicates.
    * Use this method if you are sure that this Archive has no beans.xml file.
    *
    * @param services the services
    * @return the BeanDeploymentArchive representing this archive. If this bda has not
    *         been created, it is created and returned
    */
   public BeanDeploymentArchive createBeanDeploymentArchive(ServiceRegistry services)
   {
      return createBeanDeploymentArchive(null, services);
   }

   /**
    * Adds an ArchiveLifecycleListener. When called prior to BDA creation, this listener
    * will be notified when this archive becomes visible through the BDA view. The
    * listener will also be notified when this archive (and its BDA, if available) is
    * destroyed (undeployment)
    * 
    * @param listener an ArchiveLifecycleListener
    */
   public void addLifecycleListener(ArchiveLifecycleListener listener)
   {
      this.lifecycleListeners.add(listener);
      if (bda != null)
      {
         listener.archiveVisible(this, bda);
      }
   }

   /**
    * Return the collection of EJBDescriptors.
    * 
    * @return the ejbs associated with this Archive
    */
   public Collection<EjbDescriptor<?>> getEjbs()
   {
      return ejbs;
   }

   /**
    * Notifies this archive that it is being undeployed.
    */
   public void undeploy()
   {
      synchronized(instances)
      {
         instances.remove(this.classLoader);
      }
      for (ArchiveLifecycleListener listener: lifecycleListeners)
      {
         listener.archiveDestroyed(this);
      }
   }

   @Override
   public String toString()
   {
      return "Archive[" + classLoader + "]";
   }
}
