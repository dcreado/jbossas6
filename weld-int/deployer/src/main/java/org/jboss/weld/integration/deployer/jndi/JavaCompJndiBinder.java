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
package org.jboss.weld.integration.deployer.jndi;

import javax.enterprise.inject.spi.BeanManager;
import javax.naming.Context;
import javax.naming.LinkRef;
import javax.naming.NamingException;
import javax.naming.Reference;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEModuleInformer;
import org.jboss.reloaded.naming.spi.JavaEEApplication;
import org.jboss.reloaded.naming.spi.JavaEEComponent;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;

/**
 * @author Marius Bogoevici
 */
public abstract class JavaCompJndiBinder
{
   protected Logger log = Logger.getLogger(getClass());
   private String applicationName;
   private String moduleName;

   public JavaCompJndiBinder(String applicationName, String moduleName)
   {
      this.applicationName = applicationName;
      this.moduleName = moduleName;
   }

   public void bindToJavaComp()
   {
      try
      {
         Context compContext = getJavaCompContext();
         BeanManager beanManagerContext = null;
         try
         {
            beanManagerContext = (BeanManager) compContext.lookup("BeanManager");
         } catch (NamingException e)
         {
            // ignore (this time only) - we just try to figure out whether BeanManager is registered already
         }
         if (beanManagerContext == null)
         {
            String path = applicationName == null? applicationName : (applicationName + "/" + moduleName);
            compContext.bind("BeanManager", new LinkRef("java:global/cdi/" + path +"/BeanManager"));
         }
      }
      catch (NamingException e)
      {
         log.error("Could not bound BeanManager on " + getJavaContextDescription());
      }
   }

   protected abstract Context getJavaCompContext() throws NamingException;

   protected abstract String getJavaContextDescription();

   public void unbind()
   {
      try
      {
         getJavaCompContext().unbind("BeanManager");
      }
      catch (NamingException e)
      {
         log.error("Cound not unbind java:comp/BeanManager for " + getJavaContextDescription());
      }
   }

   
}
