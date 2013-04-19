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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb.deployers.EjbDeployment;
import org.jboss.ejb.deployers.MergedJBossMetaDataDeployer;
import org.jboss.ejb3.EJBContainer;
import org.jboss.ejb3.Ejb3Deployment;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.webservices.integration.util.ASHelper;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeclaration;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeployment;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * WebServiceDeployment deployer processes EJB containers and its metadata and creates WS adapters wrapping it.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSEJBAdapterDeployer extends AbstractRealDeployer
{
   /**
    * Constructor.
    */
   public WSEJBAdapterDeployer()
   {
      super();

      // inputs
      this.addInput(MergedJBossMetaDataDeployer.EJB_MERGED_ATTACHMENT_NAME);
      this.addInput(EjbDeployment.class);
      this.addInput(Ejb3Deployment.class);
      this.addInput(WebservicesMetaData.class);

      // outputs
      this.addOutput(WebServiceDeployment.class);
   }

   /**
    * Deploys WebServiceDeployment meta data.
    *
    * @param unit deployment unit
    * @throws DeploymentException exception
    */
   @Override
   protected void internalDeploy(final DeploymentUnit unit) throws DeploymentException
   {
      final JBossMetaData mergedMD = (JBossMetaData) unit
            .getAttachment(MergedJBossMetaDataDeployer.EJB_MERGED_ATTACHMENT_NAME);
      final Ejb3Deployment ejb3Deployment = ASHelper.getOptionalAttachment(unit, Ejb3Deployment.class);

      if (mergedMD != null)
      {
         final WebServiceDeploymentAdapter wsDeploymentAdapter = new WebServiceDeploymentAdapter();
         final List<WebServiceDeclaration> endpoints = wsDeploymentAdapter.getServiceEndpoints();

         for (final JBossEnterpriseBeanMetaData ejbMD : mergedMD.getEnterpriseBeans())
         {
            final String ejbName = ejbMD.determineContainerName();

            if (ejbMD.getEjbClass() != null)
            {
               this.log.debug("Creating webservice EJB adapter for: " + ejbName);
               final EJBContainer ejbContainer = this.getContainer(ejb3Deployment, ejbMD);
               endpoints.add(new WebServiceDeclarationAdapter(ejbMD, ejbContainer, unit.getClassLoader()));
            }
            else
            {
               this.log.warn("Ingoring EJB deployment with null classname: " + ejbName);
            }
         }

         unit.addAttachment(WebServiceDeployment.class, wsDeploymentAdapter);
      }
   }

   /**
    * Returns EJB container if EJB3 deployment is detected and EJB meta data does not represent entity bean.
    *
    * @param ejb3Deployment EJB3 deployment meta data
    * @param ejbMD EJB meta data
    * @return EJB container or null if not EJB3 stateless bean
    * @throws DeploymentException if some error occurs
    */
   private EJBContainer getContainer(final Ejb3Deployment ejb3Deployment, final JBossEnterpriseBeanMetaData ejbMD)
         throws DeploymentException
   {
      if ((ejb3Deployment != null) && (!ejbMD.isEntity()))
      {
         try
         {
            final ObjectName objName = new ObjectName(ejbMD.determineContainerName());
            return (EJBContainer) ejb3Deployment.getContainer(objName);
         }
         catch (MalformedObjectNameException e)
         {
            throw new DeploymentException(e);
         }
      }

      return null;
   }

   /**
    * Adopts EJB3 bean meta data to a
    * {@link org.jboss.wsf.spi.deployment.integration.WebServiceDeclaration}.
    */
   private static final class WebServiceDeclarationAdapter implements WebServiceDeclaration
   {

      /** EJB meta data. */
      private final JBossEnterpriseBeanMetaData ejbMetaData;

      /** EJB container. */
      private final EJBContainer ejbContainer;

      /** Class loader. */
      private final ClassLoader loader;

      /**
       * Constructor.
       *
       * @param ejbMetaData EJB metadata
       * @param ejbContainer EJB container
       * @param loader class loader
       */
      private WebServiceDeclarationAdapter(final JBossEnterpriseBeanMetaData ejbMetaData,
            final EJBContainer ejbContainer, final ClassLoader loader)
      {
         super();

         this.ejbMetaData = ejbMetaData;
         this.ejbContainer = ejbContainer;
         this.loader = loader;
      }

      /**
       * Returns EJB container name.
       *
       * @return container name
       */
      public String getContainerName()
      {
         return this.ejbMetaData.determineContainerName();
      }

      /**
       * Returns EJB name.
       *
       * @return name
       */
      public String getComponentName()
      {
         return this.ejbMetaData.getName();
      }

      /**
       * Returns EJB class name.
       *
       * @return class name
       */
      public String getComponentClassName()
      {
         return this.ejbMetaData.getEjbClass();
      }

      /**
       * Returns requested annotation associated with EJB container or EJB bean.
       *
       * @param annotationType annotation type
       * @param <T> annotation class type
       * @return requested annotation or null if not found
       */
      public <T extends Annotation> T getAnnotation(final Class<T> annotationType)
      {
         final boolean haveEjbContainer = this.ejbContainer != null;

         if (haveEjbContainer)
         {
            return this.ejbContainer.getAnnotation(annotationType);
         }
         else
         {
            final Class<?> bean = this.getComponentClass();
            return (T) bean.getAnnotation(annotationType);
         }
      }

      /**
       * Loads ejb class from associated loader.
       *
       * @return ejb class instance
       */
      private Class<?> getComponentClass()
      {
         try
         {
            return this.loader.loadClass(this.getComponentClassName());
         }
         catch (ClassNotFoundException cnfe)
         {
            throw new RuntimeException("Failed to load component class: " + this.getComponentClassName()
                  + " from loader: " + this.loader);
         }
      }
   }

   /**
    * Adopts an EJB deployment to a 
    * {@link org.jboss.wsf.spi.deployment.integration.WebServiceDeployment}. 
    */
   private static final class WebServiceDeploymentAdapter implements WebServiceDeployment
   {
      /** List of endpoints. */
      private final List<WebServiceDeclaration> endpoints = new ArrayList<WebServiceDeclaration>();

      /**
       * Constructor.
       */
      private WebServiceDeploymentAdapter()
      {
         super();
      }

      /**
       * Returns endpoints list.
       *
       * @return endpoints list
       */
      public List<WebServiceDeclaration> getServiceEndpoints()
      {
         return this.endpoints;
      }
   }
}
