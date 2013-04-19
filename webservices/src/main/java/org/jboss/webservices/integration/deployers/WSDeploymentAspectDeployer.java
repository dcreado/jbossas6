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
package org.jboss.webservices.integration.deployers;

import java.util.Set;

import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.webservices.integration.util.ASHelper;
import org.jboss.wsf.common.integration.JMSDeploymentAspect;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspect;

/**
 * A deployer that delegates to JBossWS deployment aspect.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WSDeploymentAspectDeployer extends AbstractRealDeployer
{

   /** JBossWS specific inputs/outputs prefix. */
   private static final String JBOSSWS_ATTACHMENT_PREFIX = "jbossws.";

   /** JBossWS specific metadata. */
   private static final String JBOSSWS_METADATA = WSDeploymentAspectDeployer.JBOSSWS_ATTACHMENT_PREFIX + "metadata";

   /** Delegee. */
   private final DeploymentAspect aspect;

   /**
    * Constructor.
    *
    * @param aspect deployment aspect
    */
   WSDeploymentAspectDeployer(final DeploymentAspect aspect)
   {
      super();
      if (aspect instanceof JMSDeploymentAspect)
      {
         // inputs
         this.addInput(org.jboss.system.metadata.ServiceDeployment.class);
         this.addInput(org.jboss.system.metadata.ServiceMetaData.class);
      }
      else
      {
         // inputs
         this.addInput(JBossWebMetaData.class);
         this.addInput(Deployment.class);
         if (aspect.isLast())
         {
            this.addInput(WSDeploymentAspectDeployer.JBOSSWS_METADATA);
         }

         // outputs
         this.addOutput(JBossWebMetaData.class);
         if (!aspect.isLast())
         {
            this.addOutput(WSDeploymentAspectDeployer.JBOSSWS_METADATA);
         }
      }
      // propagate DA requirements and map them to deployer inputs
      final Set<String> inputs = aspect.getRequiresAsSet();
      for (String input : inputs)
      {
         this.addInput(WSDeploymentAspectDeployer.JBOSSWS_ATTACHMENT_PREFIX + input);
      }

      // propagate DA provides and map them to deployer outputs
      final Set<String> outputs = aspect.getProvidesAsSet();
      for (String output : outputs)
      {
         this.addOutput(WSDeploymentAspectDeployer.JBOSSWS_ATTACHMENT_PREFIX + output);
      }

      this.setRelativeOrder(aspect.getRelativeOrder());
      this.aspect = aspect;
   }

   /**
    * If deployed unit is related to web services this method delegates
    * to deployment aspect and calls its create() and start() methods.
    *
    * @param unit deployment unit
    * @throws DeploymentException on deployment failure
    */
   @Override
   protected void internalDeploy(final DeploymentUnit unit) throws DeploymentException
   {
      if (ASHelper.isWebServiceDeployment(unit))
      {
         this.log.debug(this.aspect + " start: " + unit.getName());
         final Deployment dep = ASHelper.getRequiredAttachment(unit, Deployment.class);
         if (this.aspect.canHandle(dep))
         {
            //set the context classloader using the proper one from the deployment aspect
            ClassLoader deployerClassLoader = SecurityActions.getContextClassLoader();
            try
            {
               SecurityActions.setContextClassLoader(this.aspect.getLoader());
               this.aspect.start(dep);
            }
            finally
            {
               SecurityActions.setContextClassLoader(deployerClassLoader);
            }
         }
      }
   }

   /**
    * If undeployed unit is related to web services this method delegates
    * to deployment aspect and calls its stop() and destroy() methods.
    *
    * @param unit deployment unit
    */
   @Override
   protected void internalUndeploy(final DeploymentUnit unit)
   {
      if (ASHelper.isWebServiceDeployment(unit))
      {
         this.log.debug(this.aspect + " stop: " + unit.getName());
         final Deployment dep = ASHelper.getRequiredAttachment(unit, Deployment.class);
         if (this.aspect.canHandle(dep))
         {
            //set the context classloader using the proper one from the deployment aspect
            ClassLoader deployerClassLoader = SecurityActions.getContextClassLoader();
            try
            {
               SecurityActions.setContextClassLoader(this.aspect.getLoader());
               this.aspect.stop(dep);
            }
            finally
            {
               SecurityActions.setContextClassLoader(deployerClassLoader);
            }
         }
      }
   }

   /**
    * Displays also WS deployment aspect being wrapped.
    *
    * @return deployer instance id including wrapped deployment aspect id.
    */
   @Override
   public String toString()
   {
      final StringBuilder sb = new StringBuilder();
      sb.append(super.toString()).append('(').append(this.aspect).append(')');
      return sb.toString();
   }
}
