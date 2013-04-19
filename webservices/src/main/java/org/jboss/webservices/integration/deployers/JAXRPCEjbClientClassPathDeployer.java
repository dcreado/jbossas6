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
package org.jboss.webservices.integration.deployers;

import java.util.Iterator;

import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeansMetaData;
import org.jboss.webservices.integration.util.ASHelper;

/**
 * A deployer for properly setting the classpath for jaxrpc client deployments;
 * it's triggered by the ApplicationClientMetaData which is attached to the unit
 * when a ejb-jar.xml descriptor file is found in the deployment.
 * This deployer then actually enables classpath modification when a jaxrpc-mapping
 * is specified in the descriptor.
 *
 * @author alessio.soldano@jboss.com
 * @since 03-Feb-2010
 */
public class JAXRPCEjbClientClassPathDeployer extends JAXRPCClientClassPathDeployer<EjbJarMetaData>
{
   public JAXRPCEjbClientClassPathDeployer()
   {
      super(EjbJarMetaData.class);
   }

   @Override
   protected boolean isClassPathChangeRequired(VFSDeploymentUnit unit)
   {
      EjbJarMetaData ejbMetaData = ASHelper.getRequiredAttachment(unit, EjbJarMetaData.class);
      EnterpriseBeansMetaData beansMetaData = ejbMetaData.getEnterpriseBeans();
      if (beansMetaData != null)
      {
         for (Iterator<EnterpriseBeanMetaData> beanIt = beansMetaData.iterator(); beanIt.hasNext(); )
         {
            EnterpriseBeanMetaData beanMetaData = beanIt.next();
            if (hasJaxRpcMappingReference(beanMetaData.getServiceReferences()))
            {
               return true;
            }
         }
      }
      return false;
   }
}
