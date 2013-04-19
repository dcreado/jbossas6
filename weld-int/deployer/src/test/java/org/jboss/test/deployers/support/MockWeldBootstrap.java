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
package org.jboss.test.deployers.support;

import java.net.URL;

import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.Environment;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.manager.api.WeldManager;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class MockWeldBootstrap implements CheckableBootstrap
{
   private boolean create;
   private boolean startInit;
   private boolean shutdown;
   private boolean validateBeans;
   private boolean endInit;
   private boolean deployBeans;

   public MockWeldBootstrap()
   {
      //System.out.println(">>>>>>>>>>>>>>>>>> " + getClass());
      //System.out.println(">>>>>>>>>>>>>>>>>> " + getClass().getClassLoader());
   }

   public void initialize()
   {
      startContainer(null, null);
   }

   public void boot()
   {
      startInitialization().deployBeans().validateBeans().endInitialization();
   }

   public Bootstrap startContainer(Environment environment, Deployment deployment)
   {
      create = true;
      return this;
   }

   public WeldManager getManager(BeanDeploymentArchive beanDeploymentArchive)
   {
      return null;
   }

   public void shutdown()
   {
      shutdown = true;
   }

   public boolean isCreate()
   {
      return create;
   }

   public boolean isBoot()
   {
      return startInit && deployBeans && validateBeans && endInit;
   }

   public boolean isShutdown()
   {
      return shutdown;
   }

   public Bootstrap deployBeans()
   {
      this.deployBeans = true;
      return this;
   }

   public Bootstrap endInitialization()
   {
      this.endInit = true;
      return this;
   }

   public Bootstrap startInitialization()
   {
      this.startInit = true;
      return this;
   }

   public Bootstrap validateBeans()
   {
      this.validateBeans = true;
      return this;
   }
   
   public BeansXml parse(Iterable<URL> urls)
   {
      // TODO Auto-generated method stub
      return null;
   }
   
   public BeansXml parse(URL url)
   {
      // TODO Auto-generated method stub
      return null;
   }
   
   public Iterable<Metadata<Extension>> loadExtensions(ClassLoader classLoader)
   {
      return null;
   }
}