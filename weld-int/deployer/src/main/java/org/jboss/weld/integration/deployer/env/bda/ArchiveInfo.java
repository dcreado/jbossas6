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
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.integration.deployer.env.WeldDiscoveryEnvironment;
import org.jboss.weld.integration.util.EjbDiscoveryUtils;

import com.google.common.collect.ForwardingCollection;

/**
 * Contains information necessary for the creation of an Archive.
 * All the information contained in a ArchiveInfo is gathered during deployment and is
 * not considered initialized (i.e., this information is not ready to be used for
 * BeanDeploymentArchive and Deployment creation) 
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 110432 $
 */
public class ArchiveInfo
{
   // creates the classpaths for the archive
   private static final ClasspathFactory classpathFactory = ClasspathFactory.getInstance();

   // keeps track of the instances that are currently under use by deployers
   private static final Map<ClassLoader, WeakReference<ArchiveInfo>> instances = new WeakHashMap<ClassLoader, WeakReference<ArchiveInfo>>();
   private Collection<String> ejbNames;
   
   // adapts an Archive to a Classpath
   private ArchiveToClasspath classpathAdapter;
   // the classpath
   private Classpath classpath;
   // the class loader
   private ClassLoader classLoader;
   // the discovery environment
   private final WeldDiscoveryEnvironment environment;

   /**
    * Returns the ArchiveInfo instance that corresponds to the given class loader.
    * Only ArchiveInfo instances under use can be returned.
    *
    * @param classLoader the class loader
    * @return            the ArchiveInfo instance that contains {@code classLoader}
    */
   public static final ArchiveInfo getInstance(ClassLoader classLoader)
   {
      // Unwrap from WeakReference before return
      WeakReference<ArchiveInfo> reference = instances.get(classLoader);
      if (reference == null)
         return null;

      return reference.get();
   }

   /**
    * Creates an ArchiveInfo to keep track of all data related to an archive
    * during deployment.
    * 
    * @param unit the deployment unit
    */
   public ArchiveInfo(final DeploymentUnit unit)
   {
      this(unit.getClassLoader(), new ForwardingCollection<String>()
      {
         private Collection<String> ejbs;

         protected Collection<String> delegate()
         {
            if (ejbs == null)
            {
               synchronized (this)
               {
                  if (ejbs == null)
                     ejbs = EjbDiscoveryUtils.getVisibleEJbNames(unit);
               }
            }
            return ejbs;
         }
      });
   }

   /**
    * Creates an ArchiveInfo to keep track of all data related to an archive
    * during deployment.
    *
    * @param classLoader the classLoader that is loading the archive under deployment.
    * @param ejbNames the names of the EJBs that are deployed in this archive
    */
   public ArchiveInfo(ClassLoader classLoader, Collection<String> ejbNames)
   {
      // must wrap in WeakReference because value refers strongly to its own key
      // see WeakHashMap javadoc
      instances.put(classLoader, new WeakReference<ArchiveInfo>(this));
      this.classLoader = classLoader;
      this.classpath = classpathFactory.create(classLoader);
      this.environment = new WeldDiscoveryEnvironment();
      this.ejbNames = ejbNames;
   }

   /**
    * Returns the classloader that is loading the archive under deployment.
    * 
    * @return the classloader
    */
   public ClassLoader getClassLoader()
   {
      return this.classLoader;
   }

   /**
    * The classpath of the archive under deployment.
    * 
    * @return the claspath
    */
   public Classpath getClasspath()
   {
      return classpath;
   }

   /**
    * Returns the archive environment information.
    * 
    * @return the environment information
    */
   public WeldDiscoveryEnvironment getEnvironment()
   {
      return this.environment;
   }

   /**
    * Gets the EJBs of this archive.
    *
    * @return ejb names
    */
   public Collection<String> getEjbNames()
   {
      return ejbNames;
   }

   /**
    * Returns a classpath adapter, indicating that the archive under deployment will be
    * used as a classpath itself.
    * 
    * @return a classpath adapter
    */
   ArchiveToClasspath getClasspathAdapter()
   {
      if (classpathAdapter == null)
         classpathAdapter = new ArchiveToClasspath();

      return classpathAdapter;
   }
   
   /**
    * Indicates whether a classpath adapter for this archive has been created.
    * 
    * @return {@code true} if a classpath adapter has been created
    */
   boolean hasClasspathAdapter()
   {
      return classpathAdapter != null;
   }

   /**
    * Performs cleanup of this archive, indicating that it is no longer in use and will
    * be discarded.
    */
   void cleanUp()
   {
      instances.remove(classLoader);
   }
   
   public String toString()
   {
      return "ArchiveInfo[" + classLoader + "]";
   }
}