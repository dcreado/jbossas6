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

import java.net.URL;
import java.util.Collection;

import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.xml.BeansXmlParser;

/**
 * {@link BeanDeploymentArchive} implementation for JBoss AS.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @version $Revision: 108657 $
 */
class BeanDeploymentArchiveImpl implements BeanDeploymentArchive
{
   // the beans parser
   private static final BeansXmlParser parser = new BeansXmlParser();

   // identifies this BDA
   private final String id;

   // the Archive that this BDA represents
   private final Archive archive;

   // the WeldBootstrap
   private final Bootstrap bootstrap;

   // the services provided by this BDA
   private final ServiceRegistry services;

   /**
    * Constructor.
    * 
    * @param id        the identifier name of this BeanDeploymentArchive
    * @param bootstrap the Weld bootstrap. Can be null only if {@code archive} has no beans.xml file
    * @param services  the services
    * @param archive   the archive that this BeanDeploymentArchive represents
    */
   public BeanDeploymentArchiveImpl(String id, Bootstrap bootstrap, ServiceRegistry services, Archive archive)
   {
      if (id == null)
         throw new IllegalArgumentException("Null id");
      if (archive == null)
         throw new IllegalArgumentException("Null archive");
      if (services == null)
         throw new IllegalArgumentException("Null services");

      this.id = id;
      this.bootstrap = bootstrap;
      this.services = services;
      this.archive = archive;
   }
   
   public Collection<BeanDeploymentArchive> getBeanDeploymentArchives()
   {
      return archive.getClasspath().getBDAs(this);
   }

   public Collection<String> getBeanClasses()
   {
      return archive.getClasses();
   }

   public BeansXml getBeansXml()
   {
      Collection<URL> urls = archive.getXmlURLs();
      if (urls == null || urls.isEmpty())
      {
         return BeansXml.EMPTY_BEANS_XML;
      }

      if (bootstrap != null)
         return bootstrap.parse(urls);
      else
         return parser.parse(urls);
   }

   public Collection<EjbDescriptor<?>> getEjbs()
   {
      return archive.getEjbs();
   }

   public ServiceRegistry getServices()
   {
      return services;
   }

   public String getId()
   {
      return id;
   }

   /**
    * Returns the archive that this BDA represents.
    * 
    * @return the archive
    */
   public Archive getArchive()
   {
      return archive;
   }
   
   @Override
   public String toString()
   {
      return archive.toString();
   }
}