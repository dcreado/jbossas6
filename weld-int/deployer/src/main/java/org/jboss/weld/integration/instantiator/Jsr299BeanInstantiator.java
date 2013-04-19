/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2010, Red Hat Middleware LLC, and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.jboss.weld.integration.instantiator;

import org.jboss.ejb3.instantiator.impl.Ejb31SpecBeanInstantiator;
import org.jboss.ejb3.instantiator.spi.BeanInstantiationException;
import org.jboss.ejb3.instantiator.spi.InvalidConstructionParamsException;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.integration.deployer.env.helpers.BootstrapBean;
import org.jboss.weld.manager.api.WeldManager;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionTarget;
import java.util.concurrent.atomic.AtomicReference;

public class Jsr299BeanInstantiator extends Ejb31SpecBeanInstantiator
{

   private BootstrapBean bootstrapBean;

   private String bdaId;

   private JBossEnterpriseBeanMetaData enterpriseBeanMetaData;

   private AtomicReference<WeldManager> weldManagerRef = new AtomicReference<WeldManager>();

   public Jsr299BeanInstantiator(JBossEnterpriseBeanMetaData enterpriseBeanMetaData)
   {
      if (enterpriseBeanMetaData == null)
      {
         throw new RuntimeException("EnterpriseBeanMetadata cannot be null");
      }
      this.enterpriseBeanMetaData = enterpriseBeanMetaData;
   }

   public void setBdaId(String bdaId)
   {
      this.bdaId = bdaId;
   }

   public void setBootstrapBean(BootstrapBean bootstrapBean)
   {
      this.bootstrapBean = bootstrapBean;
   }

   public <T> T create(Class<T> clazz, Object[] objects) throws IllegalArgumentException, InvalidConstructionParamsException, BeanInstantiationException
   {
      WeldManager weldManager = getWeldManager();
      EjbDescriptor<Object> ejbDescriptor = weldManager.getEjbDescriptor(enterpriseBeanMetaData.getEjbName());
      if (ejbDescriptor.getBeanClass().equals(clazz))
      {
         SessionBean<Object> bean = (SessionBean) weldManager.getBean(ejbDescriptor);
         InjectionTarget<Object> injectionTarget = weldManager.createInjectionTarget(ejbDescriptor);
         CreationalContext<Object> creationalContext = weldManager.createCreationalContext(bean);
         T instance = (T) injectionTarget.produce(creationalContext);
         return instance;
      }
      else
      {
         return super.create(clazz, objects);
      }
   }

   private WeldManager getWeldManager()
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
      for (BeanDeploymentArchive beanDeploymentArchive : bootstrapBean.getDeployment().getBeanDeploymentArchives())
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

   public void uninstall()
   {
      weldManagerRef.set(null);
      enterpriseBeanMetaData = null;
   }


}
