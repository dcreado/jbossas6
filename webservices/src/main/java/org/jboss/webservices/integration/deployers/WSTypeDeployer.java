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

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.webservices.integration.util.ASHelper;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeployment;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointsMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * Detects Web Service deployment type.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSTypeDeployer extends AbstractRealDeployer
{
   /**
    * Constructor.
    */
   public WSTypeDeployer()
   {
      super();

      // inputs
      this.addInput(JBossWebMetaData.class);
      this.addInput(WebservicesMetaData.class);
      this.addInput(WebServiceDeployment.class);

      // outputs
      this.addOutput(DeploymentType.class);
      this.addOutput(JBossWebMetaData.class);
   }

   /**
    * Detects WS deployment type and puts it to the deployment unit attachments.
    *
    * @param unit deployment unit
    * @throws DeploymentException on failure
    */
   @Override
   protected void internalDeploy(final DeploymentUnit unit) throws DeploymentException
   {
      if (this.isJaxwsJseDeployment(unit))
      {
         this.log.debug("Detected JAXWS JSE deployment");
         unit.addAttachment(DeploymentType.class, DeploymentType.JAXWS_JSE);
      }
      else if (this.isJaxwsEjbDeployment(unit))
      {
         this.log.debug("Detected JAXWS EJB3 deployment");
         unit.addAttachment(DeploymentType.class, DeploymentType.JAXWS_EJB3);
      }
      else if (this.isJaxrpcJseDeployment(unit))
      {
         this.log.debug("Detected JAXRPC JSE deployment");
         unit.addAttachment(DeploymentType.class, DeploymentType.JAXRPC_JSE);
      }
      else if (this.isJaxrpcEjbDeployment(unit))
      {
         this.log.debug("Detected JAXRPC EJB21 deployment");
         unit.addAttachment(DeploymentType.class, DeploymentType.JAXRPC_EJB21);
      }
      else if (this.isJaxwsJmsDeployment(unit))
      {
         this.log.debug("Detected JAXWS JMS deployment");
         unit.addAttachment(DeploymentType.class, DeploymentType.JAXWS_JMS);
      }
   }

   /**
    * Returns true if JAXRPC EJB deployment is detected.
    *
    * @param unit deployment unit
    * @return true if JAXRPC EJB, false otherwise
    */
   private boolean isJaxrpcEjbDeployment(final DeploymentUnit unit)
   {
      final boolean hasWebservicesMD = ASHelper.hasAttachment(unit, WebservicesMetaData.class);
      final boolean hasJBossMD = unit.getAllMetaData(JBossMetaData.class).size() > 0;

      return hasWebservicesMD && hasJBossMD;
   }

   /**
    * Returns true if JAXRPC JSE deployment is detected.
    *
    * @param unit deployment unit
    * @return true if JAXRPC JSE, false otherwise
    */
   private boolean isJaxrpcJseDeployment(final DeploymentUnit unit)
   {
      final boolean hasWebservicesMD = ASHelper.hasAttachment(unit, WebservicesMetaData.class);
      final boolean hasJBossWebMD = ASHelper.hasAttachment(unit, JBossWebMetaData.class);

      if (hasWebservicesMD && hasJBossWebMD)
      {
         return ASHelper.getJaxrpcServlets(unit).size() > 0;
      }

      return false;
   }

   /**
    * Returns true if JAXWS EJB deployment is detected.
    *
    * @param unit deployment unit
    * @return true if JAXWS EJB, false otherwise
    */
   private boolean isJaxwsEjbDeployment(final DeploymentUnit unit)
   {
      final boolean hasWSDeployment = ASHelper.hasAttachment(unit, WebServiceDeployment.class);

      if (hasWSDeployment)
      {
         return ASHelper.getJaxwsEjbs(unit).size() > 0;
      }

      return false;
   }

   /**
    * Returns true if JAXWS JSE deployment is detected.
    *
    * @param unit deployment unit
    * @return true if JAXWS JSE, false otherwise
    */
   private boolean isJaxwsJseDeployment(final DeploymentUnit unit)
   {
      final boolean hasJBossWebMD = ASHelper.hasAttachment(unit, JBossWebMetaData.class);

      if (hasJBossWebMD)
      {
         return ASHelper.getJaxwsServlets(unit).size() > 0;
      }

      return false;
   }

   /**
    * Returns true if JAXWS JMS deployment is detected.
    *
    * @param unit deployment unit
    * @return true if JAXWS JMS, false otherwise
    */
   private boolean isJaxwsJmsDeployment(final DeploymentUnit unit)
   {
      return ASHelper.hasAttachment(unit, JMSEndpointsMetaData.class);
   }
}
