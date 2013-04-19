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

import javax.enterprise.inject.spi.Extension;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.EjbServices;

/**
 * {@link Deployment} implementation for JBoss AS.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class DeploymentImpl implements Deployment
{
   private static ArchiveLoader archiveLoader = new ArchiveLoader();
   
   // the name of this deployment
   private String name;

   // the collection of archives contained in this deployment 
   private final ArchiveCollection archives;

   // the services provided by this deployment
   private final ServiceRegistry services;

   // holds the services to be added to all BDAs contained in this deployment
   private final ServiceRegistry bdaServices;

   // a collection of all archives that have been loaded by this DeploymentImpl, i.e.
   // created for the purpose of serving loadBeanDeploymentArchive
   private Collection<Archive> loadedArchives;

   // ServiceRegistryFactory to be used by loaded BDAs
   private ServiceRegistryFactory loadedBDAServiceRegistry;

   private Iterable<Metadata<Extension>> extensions;

   /**
    * Constructor.
    * 
    * @param name         a name that identifies this deployment
    * @param archiveInfos the information that will be used for creation of the archives contained in this deployment
    * @param ejbs         the ejb descriptors
    * @param ejbServices  the ejb services
    */
   public DeploymentImpl(String name, Collection<ArchiveInfo> archiveInfos, Collection<EjbDescriptor<?>> ejbs, EjbServices ejbServices)
   {
      this.name = name;
      this.archives = new ArchiveCollection();
      this.services = new SimpleServiceRegistry();
      this.bdaServices = new SimpleServiceRegistry();
      for (ArchiveInfo archiveInfo: archiveInfos)
      {
         archives.add(ArchiveFactory.createArchive(archiveInfo, ejbs));
      }
      this.loadedBDAServiceRegistry = new ServiceRegistryFactory(ejbServices);
      this.loadedArchives = new HashSet<Archive>();
   }

   public void initialize(Bootstrap bootstrap)
   {
      for (Archive archive: archives)
      {
         if (archive.hasXml())
         {
            archive.createBeanDeploymentArchive(bootstrap, bdaServices);
         }
      }
      extensions = bootstrap.loadExtensions(Thread.currentThread().getContextClassLoader());
   }
   
   public Collection<BeanDeploymentArchive> getBeanDeploymentArchives()
   {
      return archives.getBDAs();
   }

   public ServiceRegistry getServices()
   {
     return this.services;
   }

   public BeanDeploymentArchive loadBeanDeploymentArchive(Class<?> beanClass)
   {
      Archive archive = archiveLoader.load(beanClass, this);
      loadedArchives.add(archive);
      BeanDeploymentArchive bda = archive.getBeanDeploymentArchive();
      if (archive.getBeanDeploymentArchive() == null)
      {
         ServiceRegistry serviceRegistry = loadedBDAServiceRegistry.create();
         bda = archive.createBeanDeploymentArchive(serviceRegistry);
      }
      return bda;
   }

   /**
    * Add bootstrap service to all BDAs in this deployment.
    * 
    * @param <S>  the service type to add
    * @param type the service type to add
    * @param service  the service implementation
    */
   public <S extends Service> void addBootstrapService(Class<S> type, S service)
   {
      for(BeanDeploymentArchive beanDeploymentArchive: getBeanDeploymentArchives())
      {
         beanDeploymentArchive.getServices().add(type, service);
      }
      bdaServices.add(type, service);
      loadedBDAServiceRegistry.addService(type, service);
   }

   public void undeploy()
   {
      for(Iterator<Archive> iterator = archives.iterator(); iterator.hasNext(); )
      {
         Archive archive = iterator.next();
         iterator.remove();
         archive.undeploy();
      }
      for (Archive archive: loadedArchives)
      {
         // avoid a race condition where archiveLoader gives an ok to undeploy
         // the archive while another thread executing archiveLoader.load
         // retrieves the same archive
         synchronized(archive.getClassLoader())
         {
            if (archiveLoader.unload(archive, this))
            {
               archive.undeploy();
            }
         }
      }
   }

   public Iterable<Metadata<Extension>> getExtensions()
   {
      return extensions;
   }

   @Override
   public String toString()
   {
      return "Deployment[" + name + "]";
   }
}