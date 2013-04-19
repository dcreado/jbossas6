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

import java.util.HashMap;
import java.util.Map;

import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.ejb.spi.EjbServices;

/**
 * Creates a ServiceRegistry for newly loaded BDAs 
 * 
 * @see DeploymentImpl#loadBeanDeploymentArchive(Class)
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class ServiceRegistryFactory
{
   private EjbServices ejbServices;
   private Map<Class<? extends Service>, Service> services;

   public ServiceRegistryFactory(EjbServices ejbServices)
   {
      this.ejbServices = ejbServices;
      services = new HashMap<Class<? extends Service>, Service>();
   }

   public void addService(Class<? extends Service> serviceType, Service service)
   {
      services.put(serviceType, service);
   }

   public ServiceRegistry create()
   {
      ServiceRegistry serviceRegistry = new SimpleServiceRegistry();
      serviceRegistry.addAll(services.entrySet());
      serviceRegistry.add(EjbServices.class, ejbServices);
      return serviceRegistry;
   }
}
