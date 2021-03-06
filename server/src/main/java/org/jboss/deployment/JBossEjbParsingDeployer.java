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

import java.net.URL;

import org.jboss.bootstrap.api.as.config.JBossASServerConfig;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.deployer.SchemaResolverDeployer;
import org.jboss.metadata.ApplicationMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * An ObjectModelFactoryDeployer for translating jboss.xml descriptors into
 * JBossMetaData instances.
 * 
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision: 101688 $
 */
@SuppressWarnings("deprecation")
public class JBossEjbParsingDeployer extends SchemaResolverDeployer<JBossMetaData>
{
   private JBossMetaData standardMetaData;
   /** URL to standardjboss.xml */
   private URL standardJBossXmlPath;
   /** Whether a missing standardjboss.xml should be ignored */
   private boolean ignoreMissingStandardJBossXml = false;

   /**
    * Create a new JBossEjbParsingDeployer.
    */
   public JBossEjbParsingDeployer()
   {
      // Output the jboss.xml metadata
      super(JBossMetaData.class);
      setName("jboss.xml");

      // Optional ejb-jar.xml metadata
      addInput(EjbJarMetaData.class);
      // Output the legacy jboss ejb metadata
      addOutput(ApplicationMetaData.class.getName());
      // Output the standardjboss.xml metadata
      addOutput("standardjboss.xml");
   }

   public URL getStandardJBossXmlPath()
   {
      return standardJBossXmlPath;
   }
   public void setStandardJBossXmlPath(URL standardJBossXmlPath)
   {
      this.standardJBossXmlPath = standardJBossXmlPath;
   }


   public boolean isIgnoreMissingStandardJBossXml()
   {
      return ignoreMissingStandardJBossXml;
   }
   public void setIgnoreMissingStandardJBossXml(
         boolean ignoreMissingStandardJBossXml)
   {
      this.ignoreMissingStandardJBossXml = ignoreMissingStandardJBossXml;
   }

   // FIXME This should not be here 
   @Override
   protected void createMetaData(DeploymentUnit unit, String name, String suffix)
         throws DeploymentException
   {
      super.createMetaData(unit, name, suffix);
      
      JBossMetaData jbossMetaData = unit.getAttachment(getOutput());
      EjbJarMetaData ejbJarMetaData = unit.getAttachment(EjbJarMetaData.class);
      if (ejbJarMetaData != null || jbossMetaData != null)
      {
         // Save this as a transient(non-managed) attachment
         // only for EJB2.x and earlier beans (since standardjboss.xml does not apply for EJB3.x and
         // later)
         if (this.isPreEJB3x(ejbJarMetaData, jbossMetaData))
         {
            JBossMetaData stdMetaData = getStandardMetaData();
            if(stdMetaData != null)
               unit.addAttachment("standardjboss.xml", stdMetaData);
         }

         if (jbossMetaData != null)
         {
            // For legacy - but its totally redundant????
            ApplicationMetaData amd = new ApplicationMetaData(jbossMetaData);
            unit.addAttachment(ApplicationMetaData.class, amd);
         }
      }
   }

   private JBossMetaData getStandardMetaData() throws DeploymentException
   {
      if (standardMetaData == null)
      {
         try
         {
            if(standardJBossXmlPath == null)
            {
               // Use default server conf/standardjboss.xml location
               final String configPropName = JBossASServerConfig.PROP_KEY_JBOSSAS_SERVER_CONF_URL;
               String configPath = System.getProperty(configPropName);
               if(configPath == null )
               {
                  if(ignoreMissingStandardJBossXml == false)
                     throw new DeploymentException("standardjboss.xml not specified and "+configPropName+" does not exist");
                  return null;
               }
               URL configUrl = new URL(configPath);
               standardJBossXmlPath = new URL(configUrl, "standardjboss.xml");
            }

            VirtualFile stdJBoss = VFS.getChild(standardJBossXmlPath);
            if (stdJBoss == null && ignoreMissingStandardJBossXml == false)
            {
               throw new DeploymentException("standardjboss.xml not found in config dir: " + standardJBossXmlPath);
            }
            standardMetaData = super.parse(stdJBoss);
         }
         catch (Exception ex)
         {
            DeploymentException.rethrowAsDeploymentException(ex.getMessage(), ex);
         }
      }
      return standardMetaData;
   }
   
   /**
    * @return Returns true if either the passed <code>ejbJarMetadata</code> or the <code>jbossMetadata</code>
    * represents a EJB2.x or EJB1.x deployment
    * 
    * @param ejbJarMetadata ejb-jar metadata
    * @param jbossMetadata jboss metadata
    * 
    */
   private boolean isPreEJB3x(EjbJarMetaData ejbJarMetadata, JBossMetaData jbossMetadata)
   {
      if (ejbJarMetadata != null && (ejbJarMetadata.isEJB2x() || ejbJarMetadata.isEJB1x()))
      {
         return true;
      }
      
      if (jbossMetadata != null && (jbossMetadata.isEJB2x() || jbossMetadata.isEJB1x()))
      {
         return true;
      }
      return false;
   }
   
}
