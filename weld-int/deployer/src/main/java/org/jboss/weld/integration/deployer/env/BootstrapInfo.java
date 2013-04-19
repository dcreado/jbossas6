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
package org.jboss.weld.integration.deployer.env;

import org.jboss.beans.metadata.spi.ValueMetaData;

/**
 * Simple bootstrap info class, used as attachment key.
 * Holds inject values for deployment and various services.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class BootstrapInfo
{
   private ValueMetaData ejbServices;
   public static final String EJB_SERVICES = BootstrapInfo.class.getSimpleName() + "EJB_SERVICES";

   private ValueMetaData ejbInjectionServices;
   public static final String EJB_INJECTION_SERVICES = BootstrapInfo.class.getSimpleName() + "EJB_INJECTION_SERVICES";

   private ValueMetaData deployment;
   public static final String DEPLOYMENT = BootstrapInfo.class.getSimpleName() + "DEPLOYMENT";

   private ValueMetaData servletServices;
   public static final String SERVLET_SERVICES = BootstrapInfo.class.getSimpleName() + "SERVLET_SERVICES";

   public ValueMetaData getDeployment()
   {
      return deployment;
   }

   public void setDeployment(ValueMetaData deployment)
   {
      this.deployment = deployment;
   }

   public ValueMetaData getEjbServices()
   {
      return ejbServices;
   }

   public void setEjbServices(ValueMetaData ejbServices)
   {
      this.ejbServices = ejbServices;
   }

   public ValueMetaData getEjbInjectionServices()
   {
      return ejbInjectionServices;
   }

   public void setEjbInjectionServices(ValueMetaData ejbInjectionServices)
   {
      this.ejbInjectionServices = ejbInjectionServices;
   }

   public ValueMetaData getServletServices()
   {
      return servletServices;
   }

   public void setServletServices(ValueMetaData servletServices)
   {
      this.servletServices = servletServices;
   }
}