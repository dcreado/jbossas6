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

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.instantiator.deployer.BeanInstantiatorDeployerBase;
import org.jboss.ejb3.instantiator.deployer.SingletonBeanInstantiatorDeployer;
import org.jboss.ejb3.instantiator.spi.BeanInstantiator;
import org.jboss.kernel.Kernel;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEComponentInformer;
import org.jboss.weld.integration.deployer.DeployersUtils;

/**
 * A deployer that redirects to one of the two supported BeanInstantiator deployers.
 * To be replace with a simpler implementation once the signature to getBeanInstantiator()
 * is modified to pass on the DeploymentUnit - we need it to decide whether this is a
 * CDI deployment or not
 *
 * @author Marius Bogoevici
 */
//TODO: remove this class after updating BeanInstantiatorDeployerBase and register a single deployer
public class RedirectingBeanInstantiatorDeployer extends AbstractDeployer
{

   private Jsr299BeanInstantiatorDeployer jsr299BeanInstantiatorDeployer;
   private SingletonBeanInstantiatorDeployer singletonBeanInstantiatorDeployer;

   public RedirectingBeanInstantiatorDeployer(BeanInstantiator defaultBeanInstantiator, Kernel kernel, JavaEEComponentInformer javaEEComponentInformer)
   {
      super();
      this.addInput(JBossMetaData.class);
      this.addOutput(BeanInstantiator.class);
      jsr299BeanInstantiatorDeployer = new Jsr299BeanInstantiatorDeployer();
      jsr299BeanInstantiatorDeployer.setKernel(kernel);
      jsr299BeanInstantiatorDeployer.setJavaEEComponentInformer(javaEEComponentInformer);
      singletonBeanInstantiatorDeployer = new SingletonBeanInstantiatorDeployer(defaultBeanInstantiator);
      singletonBeanInstantiatorDeployer.setKernel(kernel);
      singletonBeanInstantiatorDeployer.setJavaEEComponentInformer(javaEEComponentInformer);
   }

   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      getApplicableBeanInstantiatorDeployer(unit).deploy(unit);
   }

   @Override
   public void undeploy(DeploymentUnit unit)
   {
      getApplicableBeanInstantiatorDeployer(unit).undeploy(unit);
   }

   public BeanInstantiatorDeployerBase getApplicableBeanInstantiatorDeployer(DeploymentUnit unit)
   {
      if (DeployersUtils.checkForWeldFiles(unit, false))
      {
         return jsr299BeanInstantiatorDeployer;
      }
      else
      {
         return singletonBeanInstantiatorDeployer;
      }
   }
}
