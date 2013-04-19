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

import org.jboss.beans.metadata.api.model.InjectOption;
import org.jboss.beans.metadata.plugins.AbstractInjectionValueMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.injection.manager.spi.Injector;
import org.jboss.injection.manager.spi.InjectionManager;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.util.IdFactory;

/**
 * A deployer for a Jsr299Injector
 *
 * @author Marius Bogoevici
 */
public class Jsr299InjectorDeployer extends AbstractSimpleRealDeployer<InjectionManager>
{
   private KernelController kernelController;

   public Jsr299InjectorDeployer()
   {
      super(InjectionManager.class);
      setStage(DeploymentStages.REAL);
      setWantComponents(true);
      setOutput(InjectionManager.class);
   }

    public void setKernelController(KernelController kernelController)
   {
      this.kernelController = kernelController;
   }

   @Override
   public void deploy(DeploymentUnit unit, InjectionManager deployment) throws DeploymentException
   {
      if (isCDIDeployment(unit))
      {
         String bdaId = IdFactory.getIdFromClassLoader(unit.getClassLoader());
         try
         {

             Injector injector = null;
             BeanMetaDataBuilder builder = null;
             if (!unit.isComponent())
             {

                 injector = new Jsr299SimpleNonContextualInjector(bdaId);
                 builder = BeanMetaDataBuilder.createBuilder(getJsr299InjectorMcBeanName(unit), Jsr299SimpleNonContextualInjector.class.getName());
             }
             else
             {
                 JBossEnterpriseBeanMetaData enterpriseBeanMetaData = unit.getAttachment(JBossEnterpriseBeanMetaData.class);
                 if (enterpriseBeanMetaData != null && enterpriseBeanMetaData.getJBossMetaData().isEJB3x())
                 {
                     injector = new Jsr299InterceptorInjector(bdaId, enterpriseBeanMetaData);
                     builder = BeanMetaDataBuilder.createBuilder(getJsr299InjectorMcBeanName(unit), Jsr299InterceptorInjector.class.getName());
                 }
             }
             if (injector != null)
             {
                 AbstractInjectionValueMetaData injectionValueMetaData = new AbstractInjectionValueMetaData(DeployersUtils.getBootstrapBeanName(unit));
                 injectionValueMetaData.setInjectionOption(InjectOption.CALLBACK);
                 builder.addPropertyMetaData("bootstrapBean", injectionValueMetaData);
                 builder.addUninstall("release");
                 kernelController.install(builder.getBeanMetaData(), injector);
                 deployment.addInjector(injector);
             }
         }
         catch (Throwable throwable)
         {
            throw new DeploymentException(throwable);
         }
      }
   }

   private boolean isCDIDeployment(DeploymentUnit unit)
   {
      return unit.getAttachment(DeployersUtils.WELD_FILES) != null;
   }

   @Override
   public void undeploy(DeploymentUnit unit, InjectionManager deployment)
   {
      if (isCDIDeployment(unit) && hasInjector(unit))
      {
         kernelController.uninstall(getJsr299InjectorMcBeanName(unit));
      }
   }

   private String getJsr299InjectorMcBeanName(DeploymentUnit deploymentUnit)
   {
      if (!deploymentUnit.isComponent())
      {
         return deploymentUnit.getName() + "_Jsr299BeanInjector";
      }
      else
      {
         return deploymentUnit.getParent().getName() + "/" + deploymentUnit.getName() + "_Jsr299Injector";
      }
   }

   private boolean hasInjector(DeploymentUnit deploymentUnit)
   {
      if (!deploymentUnit.isComponent())
      {
         return true;
      }
      else
      {
         JBossEnterpriseBeanMetaData enterpriseBeanMetaData = deploymentUnit.getAttachment(JBossEnterpriseBeanMetaData.class);
         return (enterpriseBeanMetaData != null && enterpriseBeanMetaData.getJBossMetaData().isEJB3x());
      }
   }
}
