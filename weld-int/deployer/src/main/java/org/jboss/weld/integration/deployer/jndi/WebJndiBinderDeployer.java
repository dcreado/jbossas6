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

import org.jboss.beans.metadata.plugins.AbstractInjectionValueMetaData;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEModuleInformer;
import org.jboss.reloaded.naming.spi.JavaEEModule;
import org.jboss.weld.integration.deployer.DeployersUtils;

/**
 * @author Marius Bogoevici
 */
public class WebJndiBinderDeployer extends AbstractSimpleRealDeployer<JBossWebMetaData>
{

   private JavaEEModuleInformer informer;

   public WebJndiBinderDeployer(JavaEEModuleInformer javaEEModuleInformer)
   {
      super(JBossWebMetaData.class);
      informer = javaEEModuleInformer;
      setOutput(BeanMetaData.class);
   }

   @Override
   public void deploy(DeploymentUnit deploymentUnit, JBossWebMetaData jBossWebMetaData) throws DeploymentException
   {
//      BeanMetaData bbBMD = getBootstrapBeanAttachment(deploymentUnit.getTopLevel());
//      if (bbBMD != null && deploymentUnit.isAttachmentPresent(JBossWebMetaData.class))
//      {
//         AbstractInjectionValueMetaData javaModule = new AbstractInjectionValueMetaData(getModuleBeanName(deploymentUnit));
//
//         String beanMetaDataMcNamespace = getModuleBeanName(deploymentUnit);
//         BeanMetaDataBuilder moduleBinderBuilder = BeanMetaDataBuilder.createBuilder(beanMetaDataMcNamespace + "_JavaModuleJndiBinder", JavaEEModuleJndiBinder.class.getName());
//         moduleBinderBuilder.addConstructorParameter(JavaEEModule.class.getName(), javaModule);
//         moduleBinderBuilder.addInstall("bindToJavaComp");
//         moduleBinderBuilder.addUninstall("unbind");
//         deploymentUnit.getTopLevel().addAttachment(beanMetaDataMcNamespace + "_JavaModuleJndiBinder", moduleBinderBuilder.getBeanMetaData());
//      }
   }

   private BeanMetaData getBootstrapBeanAttachment(DeploymentUnit deploymentUnit)
   {
      String bootstrapName = DeployersUtils.getBootstrapBeanName(deploymentUnit.getTopLevel());
      String bbAttachmentName = bootstrapName + "_" + BeanMetaData.class.getSimpleName();

      BeanMetaData bbBMD = deploymentUnit.getTopLevel().getAttachment(bbAttachmentName, BeanMetaData.class);
      return bbBMD;
   }

   private String getModuleBeanName(DeploymentUnit deploymentUnit)
   {
      String appName = informer.getApplicationName(deploymentUnit);
      String moduleName = informer.getModuleName(deploymentUnit);
      String name = "jboss.naming:";
      if (appName != null)
      {
         name += "application=" + appName + ",";
      }
      name += "module=" + moduleName;
      return name;
   }


}
