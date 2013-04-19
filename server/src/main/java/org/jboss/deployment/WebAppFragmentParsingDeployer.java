/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.deployment;

import java.util.Set;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.structure.MetaDataTypeFilter;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.deployer.SchemaResolverDeployer;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.web.spec.WebFragmentMetaData;
import org.jboss.vfs.VirtualFile;

/**
 * An ObjectModelFactoryDeployer for translating web-fragment.xml descriptors into
 * WebFragmentMetaData instances.
 * 
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision: 82920 $
 */
public class WebAppFragmentParsingDeployer extends SchemaResolverDeployer<WebFragmentMetaData>
{
   public WebAppFragmentParsingDeployer()
   {
      super(WebFragmentMetaData.class);
      setName("web-fragment.xml");
      setAllowMultipleFiles(true);
      setFilter(MetaDataTypeFilter.ALL);
   }

   /**
    * Get the virtual file path for the web-fragment descriptor in the
    * DeploymentContext.getMetaDataPath.
    * 
    * @return the current virtual file path for the web-fragment descriptor
    */
   public String getWebFragmentXmlPath()
   {
      return getName();
   }
   /**
    * Set the virtual file path for the web-fragment descriptor in the
    * DeploymentContext.getMetaDataLocation. The standard path is web-fragment.xml
    * to be found in the /META-INF/ metdata path in JARs located in /WEB-INF/lib
    * in the webapp.
    * 
    * @param WebFragmentMetaData - new virtual file path for the web-fragment descriptor
    */
   public void setWebFragmentXmlPath(String WebFragmentMetaData)
   {
      setName(WebFragmentMetaData);
   }

   protected void init(VFSDeploymentUnit unit, WebFragmentMetaData metaData, VirtualFile file) throws Exception
   {
      unit.addAttachment(WebFragmentMetaData.class.getName() + ":" + file.getPathNameRelativeTo(unit.getRoot()), metaData, getOutput());
   }
   
   protected void createMetaData(DeploymentUnit unit, Set<String> names, String suffix, String key) throws DeploymentException
   {
      // First see whether it already exists
      WebFragmentMetaData result = getMetaData(unit, key);
      if (result != null && allowsReparse() == false)
         return;

      // Create it
      try
      {
         result = parse(unit, getName(), suffix, result);
      }
      catch (Exception e)
      {
         throw DeploymentException.rethrowAsDeploymentException("Error creating managed object for " + unit.getName(), e);
      }

      // Doesn't exist
      if (result == null)
         return;
      
      // Register it
      unit.getTransientManagedObjects().addAttachment(key, result, getOutput());
   }

}
