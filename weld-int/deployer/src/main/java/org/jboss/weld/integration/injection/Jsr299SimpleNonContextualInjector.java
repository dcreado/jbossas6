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

package org.jboss.weld.integration.injection;

import org.jboss.injection.manager.spi.InjectionContext;
import org.jboss.injection.manager.spi.InjectionException;
import org.jboss.injection.manager.spi.Injector;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.integration.deployer.env.helpers.BootstrapBean;
import org.jboss.weld.manager.api.WeldManager;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionTarget;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Marius Bogoevici
 */
public class Jsr299SimpleNonContextualInjector implements Injector
{

   private BootstrapBean bootstrapBean;
   private String bdaId;
   private AtomicReference<WeldManager> weldManagerRef;

   public Jsr299SimpleNonContextualInjector(String bdaId)
   {
      this.bdaId = bdaId;
      this.weldManagerRef = new AtomicReference<WeldManager>();
   }

   public void setBootstrapBean(BootstrapBean bootstrapBean)
   {
      this.bootstrapBean = bootstrapBean;
   }

   public <T> void inject(InjectionContext<T> injectionContext) throws InjectionException
   {
      WeldManager weldManager = initWeldManagerIfNecessary();
      Object instance = injectionContext.getInjectionTarget();

      if (weldManager == null)
         throw new IllegalArgumentException("Null bean manager.");

      CreationalContext<Object> creationalContext =  weldManager.createCreationalContext(null);
      InjectionTarget<Object> injectionTarget = (InjectionTarget<Object>) weldManager.fireProcessInjectionTarget(weldManager.createAnnotatedType(instance.getClass()));
      injectionTarget.inject(instance, creationalContext);
      injectionContext.proceed();
   }

   private WeldManager initWeldManagerIfNecessary()
   {
      WeldManager weldManager = weldManagerRef.get();
      if (weldManager == null)
      {
         weldManager = locateWeldManager();
      }
      weldManagerRef.compareAndSet(null, weldManager);
      return weldManager;
   }

   private WeldManager locateWeldManager()
   {
      BeanDeploymentArchive foundBeanDeploymentArchive = null;
      for (BeanDeploymentArchive beanDeploymentArchive: bootstrapBean.getDeployment().getBeanDeploymentArchives())
      {
         if (beanDeploymentArchive.getId().equals(bdaId))
         {
            foundBeanDeploymentArchive = beanDeploymentArchive;
         }
      }
      if (foundBeanDeploymentArchive == null)
      {
         throw new IllegalStateException("Cannot find BeanManager for BeanDeploymentArchive with id=" + bdaId);
      }

      return bootstrapBean.getBootstrap().getManager(foundBeanDeploymentArchive);
   }

   public void release()
   {
      bootstrapBean = null;
      weldManagerRef.set(null);
   }
}
