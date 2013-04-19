/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.visitor.ClassFilter;
import org.jboss.classloading.spi.visitor.ResourceVisitor;
import org.jboss.deployers.spi.classloading.ResourceLookupProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.integration.deployer.env.WeldDiscoveryEnvironment;

/**
 * Find all Weld libraries.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class LibraryDiscoveryService implements LibraryArchivesProvider
{
   private ResourceLookupProvider<Module> provider;
   private Iterable<URL> excludedUrls;

   private Set<Archive> libs;
   private AtomicBoolean checked = new AtomicBoolean();

   public LibraryDiscoveryService(ResourceLookupProvider<Module> provider)
   {
      if (provider == null)
         throw new IllegalArgumentException("Null provider");
      this.provider = provider;
   }

   public synchronized Set<Archive> getLibraries() throws Exception
   {
      if (libs == null || checked.get() == false)
      {
         libs = new HashSet<Archive>();

         List<VirtualFile> excludedFiles = null;
         Map<Module, Set<URL>> modules = provider.getMatchingModules();
         for (Map.Entry<Module, Set<URL>> entry : modules.entrySet())
         {
            Set<URL> urls = entry.getValue();
            if (excludedUrls != null && excludedFiles == null)
            {
               excludedFiles = new ArrayList<VirtualFile>();
               for (URL eu : excludedUrls)
                  excludedFiles.add(VFS.getChild(eu));
            }
            Set<VirtualFile> files = new HashSet<VirtualFile>();
            for (URL u : urls)
            {
               VirtualFile vf = VFS.getChild(u);
               boolean include = true;
               if (excludedFiles != null)
               {
                  for (VirtualFile ef : excludedFiles)
                  {
                     if (vf.getParentFileList().contains(ef))
                     {
                        include = false;
                        break;
                     }
                  }
               }
               if (include)
                  files.add(vf);
            }
            if (files.isEmpty() == false)
            {                                                     
               ClassLoader cl = SecurityActions.getClassLoaderForModule(entry.getKey());
               // is this module already installed; past CL stage
               if (cl != null)
               {
                  // create an ArchiveInfo with the ClassLoader
                  ArchiveInfo archiveInfo = new ArchiveInfo(cl, Collections.<String>emptyList());
                  // finally create the Archive
                  Archive archive = ArchiveFactory.createArchive(archiveInfo, new ArrayList<EjbDescriptor<?>>());
                  // ... and the corresponding BDA

                  // create bda
                  BeanDeploymentArchive bda = archive.createBeanDeploymentArchive(NoopServiceRegistry.INSTANCE);
                  if (bda != null)
                     libs.add(archive);

                  // the env
                  WeldDiscoveryEnvironment environment = archiveInfo.getEnvironment();
                  // fill in the Weld classes
                  // the WBDiscoveryVisitor from ArchiveDiscoveryDeployer...
                  ResourceVisitor visitor = environment.visitor();
                  Module module = entry.getKey();

                  for (VirtualFile child : files)
                  {
                     URL beansXmlURL = child.getChild(provider.getResourceName()).toURL();
                     environment.addWeldXmlURL(beansXmlURL);

                     module.visit(visitor, ClassFilter.INSTANCE, null, child.toURL());
                  }
               }
            }
         }
         checked.set(true);
      }
      return libs;
   }

   /**
    * Refresh libs
    */
   public synchronized void refresh()
   {
      checked.set(false);
   }

   public void setExcludedUrls(Iterable<URL> excludedUrls)
   {
      this.excludedUrls = excludedUrls;
   }

   private static class NoopServiceRegistry implements ServiceRegistry
   {
      private static ServiceRegistry INSTANCE = new NoopServiceRegistry();

      public <S extends Service> void add(Class<S> type, S service)
      {
      }

      public void addAll(Collection<Map.Entry<Class<? extends Service>, Service>> services)
      {
      }

      public Set<Map.Entry<Class<? extends Service>, Service>> entrySet()
      {
         return Collections.emptySet();
      }

      public <S extends Service> S get(Class<S> type)
      {
         return null;
      }

      public <S extends Service> boolean contains(Class<S> type)
      {
         return true; // fake it?
      }

      public void cleanup()
      {
      }

      public Iterator<Service> iterator()
      {
         return Collections.<Service>emptyList().iterator();
      }
   }
}
