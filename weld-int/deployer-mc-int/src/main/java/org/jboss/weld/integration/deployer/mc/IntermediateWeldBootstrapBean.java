/*
* JBoss, Home of Professional Open Source.
* Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.weld.integration.deployer.mc;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.kernel.weld.plugins.dependency.WeldKernelControllerContext;
import org.jboss.logging.Logger;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.integration.deployer.env.helpers.BootstrapBean;

/**
 * Intermediate bean installed by BeanMetaDataDeployer using {@link WeldBeanMetaDataDeployerPlugin}.
 * It holds the BeanMetaData BeanMetaDataDeployer thinks it is installing and gets injected with the 
 * Weld BootstrapBean and Deployment once the Weld BootstrapBean has been initialized. Once that happens
 * we can get a reference to the Weld BeanManager so that we can create a WeldKernelControllerContext
 * with the original BeanMetaData during this bean's start stage. The created WeldKernelControllerContext
 * contains an install callback to remove this intermediate bean from the controller.
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class IntermediateWeldBootstrapBean
{
   final static Logger log = Logger.getLogger(IntermediateWeldBootstrapBean.class.getName());
   
   /**
    * The bootstrap bean
    */
   private BootstrapBean bootstrapBean;
   
   /**
    * The weld deployment containing this bean 
    */
   private Deployment deployment;
   
   /**
    * The actual MC bean metadata we want to install as a weld kernel controller context
    */
   private BeanMetaDataPropertyHolder beanMetaDataHolder;
   
   /**
    * The controller context of this bean
    */
   private ControllerContext context;
   
   /**
    * The WeldKernelControllerContextCreator that created this bean.
    */
   private WeldBeanMetaDataDeployerPlugin creator;

   /**
    * Get the bootstrapBean
    * @return the bootstrapBean
    */
   public BootstrapBean getBootstrapBean()
   {
      return bootstrapBean;
   }

   /**
    * Set the bootstrapBean
    * @param bootstrapBean the bootstrapBean to set
    */
   public void setBootstrapBean(BootstrapBean bootstrapBean)
   {
      this.bootstrapBean = bootstrapBean;
   }

   /**
    * Get the deployment
    * @return the deployment
    */
   public Deployment getDeployment()
   {
      return deployment;
   }

   /**
    * Set the deployment
    * @param deployment the deployment to set
    */
   public void setDeployment(Deployment deployment)
   {
      this.deployment = deployment;
   }


   /**
    * Get the context
    * @return the context
    */
   public ControllerContext getContext()
   {
      return context;
   }

   /**
    * Set the context
    * @param context the context to set
    */
   public void setContext(ControllerContext context)
   {
      this.context = context;
   }

   /**
    * Get the beanMetaDataHolder
    * @return the beanMetaDataHolder
    */
   public BeanMetaDataPropertyHolder getBeanMetaDataHolder()
   {
      return beanMetaDataHolder;
   }

   /**
    * Set the beanMetaDataHolder
    * @param beanMetaDataHolder the beanMetaDataHolder to set
    */
   public void setBeanMetaDataHolder(BeanMetaDataPropertyHolder beanMetaDataHolder)
   {
      this.beanMetaDataHolder = beanMetaDataHolder;
   }

   /**
    * Get the creator
    * @return the creator
    */
   public WeldBeanMetaDataDeployerPlugin getCreator()
   {
      return creator;
   }

   /**
    * Set the creator
    * @param creator the creator to set
    */
   public void setCreator(WeldBeanMetaDataDeployerPlugin creator)
   {
      this.creator = creator;
   }

   /**
    * Called when the bean starts, and checks that it has been injected with the necessary bootstrapBean,
    * beanMetaData, context and deployment to create a WeldKernelControllerContext.
    * 
    * @throws IllegalStateException if bootstrapBean, beanMetaDataHolder, context or deployment have not been set.
    * @throws Exception if an error occured installing the bean
    */
   public void create() throws Exception
   {
      if (bootstrapBean == null)
         throw new IllegalStateException("Null bootstrap bean");
      if (beanMetaDataHolder == null)
         throw new IllegalStateException("Null bean metadata");
      if (context == null)
         throw new IllegalStateException("Null context");
      if (deployment == null)
         throw new IllegalStateException("Null deployment");
      if (deployment.getBeanDeploymentArchives().size() == 0)
         throw new IllegalStateException("Zero bean deployment archives in the deployment");
      
      BeanDeploymentArchive archive = deployment.getBeanDeploymentArchives().iterator().next();
      
      if (deployment.getBeanDeploymentArchives().size() > 1)
         log.warn("More than one bean deployment archives, using the first " + archive);
      
      BeanManager manager = bootstrapBean.getBootstrap().getManager(archive);
      if (manager == null)
         throw new IllegalStateException("Could not find a manager for archive " + null);
      
      //When the created weld kernel controller context is installed, call installCreatedBean() which removes 
      //this bean from the controller
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(beanMetaDataHolder.getBeanMetaData());
      builder.addInstallWithParameters("installCreatedBean", (String)context.getName(), ControllerState.INSTALLED);
      
      WeldKernelControllerContext ctx = new WeldKernelControllerContext(null, beanMetaDataHolder.getBeanMetaData(), null, manager);

      try
      {
         context.getController().install(ctx);
      }
      catch(Throwable t)
      {
         throw new Exception(t);
      }
      creator.removeIntermediateBean(beanMetaDataHolder.getBeanMetaData().getName());
   }
   
   /**
    * Called when the created bean reaches INSTALLED state.
    * Uninstalls the context of this temp bean
    */
   public void installCreatedBean()
   {
      context.getController().uninstall(context.getName());
   }
}
