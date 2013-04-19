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

import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.webservices.integration.util.ASHelper;
import org.jboss.wsf.spi.metadata.webservices.WebserviceDescriptionMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * A deployer for properly setting the classpath for jaxrpc endpoint deployments;
 * it's triggered by the WebservicesMetaData which is attached to the unit
 * when a webservice.xml descriptor file is found in the deployment.
 *
 * @author alessio.soldano@jboss.com
 * @since 03-Feb-2010
 */
public class JAXRPCServerClassPathDeployer extends JAXRPCIntegrationClassPathDeployer<WebservicesMetaData>
{
   public JAXRPCServerClassPathDeployer()
   {
      super(WebservicesMetaData.class);
      setInput(WebservicesMetaData.class); //makes the input mandatory instead of optional
   }

   @Override
   protected boolean isClassPathChangeRequired(VFSDeploymentUnit unit)
   {
      WebservicesMetaData wsmd = ASHelper.getRequiredAttachment(unit, WebservicesMetaData.class);
      WebserviceDescriptionMetaData[] descriptions = wsmd.getWebserviceDescriptions();
      for (WebserviceDescriptionMetaData description : descriptions)
      {
         if (description.getJaxrpcMappingFile() != null)
         {
            return true;
         }
      }
      return false;
   }
}
