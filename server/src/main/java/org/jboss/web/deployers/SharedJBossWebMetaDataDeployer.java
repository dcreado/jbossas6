/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.web.deployers;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.web.jboss.JBoss60WebMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.xb.binding.JBossXBException;
import org.jboss.xb.binding.Unmarshaller;
import org.jboss.xb.binding.UnmarshallerFactory;
import org.jboss.xb.binding.sunday.unmarshalling.SchemaBindingResolver;
import org.jboss.xb.binding.sunday.unmarshalling.SingletonSchemaResolverFactory;

/**
 * A deployer that processes the shared web.xml and jboss-web.xml.
 * 
 * @author Remy Maucherat
 * @version $Revision: 93820 $
 */
public class SharedJBossWebMetaDataDeployer extends AbstractDeployer
{
   public static final String SHARED_JBOSSWEB_ATTACHMENT_NAME = "shared."+ JBossWebMetaData.class.getName();

   private String webXml = null;
   private String jbossWebXml = null;
   private JBossWebMetaData sharedJBossWebMetaData = null;
   private WebMetaData sharedWebMetaData = null;

   public String getWebXml()
   {
      return webXml;
   }

   public void setWebXml(String webXml)
   {
      this.webXml = webXml;
   }

   public String getJbossWebXml()
   {
      return jbossWebXml;
   }

   public void setJbossWebXml(String jbossWebXml)
   {
      this.jbossWebXml = jbossWebXml;
   }

   public SharedJBossWebMetaDataDeployer()
   {
      setStage(DeploymentStages.POST_CLASSLOADER);
      addOutput(SHARED_JBOSSWEB_ATTACHMENT_NAME);
   }

   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      synchronized (this)
      {
         if ((sharedWebMetaData == null && webXml != null)
               || (sharedJBossWebMetaData == null && jbossWebXml != null))
         {
            UnmarshallerFactory factory = UnmarshallerFactory.newInstance();
            Unmarshaller unmarshaller = factory.newUnmarshaller();
            SchemaBindingResolver resolver = SingletonSchemaResolverFactory
                  .getInstance().getSchemaBindingResolver();
            if (webXml != null)
            {
               try
               {
                  sharedWebMetaData = (WebMetaData) unmarshaller.unmarshal(webXml, resolver);
               }
               catch (JBossXBException e)
               {
                  throw new DeploymentException("Error parsing shared web.xml", e);
               }
            }
            if (jbossWebXml != null)
            {
               try
               {
                  sharedJBossWebMetaData = (JBossWebMetaData) unmarshaller.unmarshal(jbossWebXml, resolver);
               }
               catch (JBossXBException e)
               {
                  throw new DeploymentException("Error parsing shared jboss-web.xml", e);
               }
            }
         }
      }

      if (sharedWebMetaData != null || sharedJBossWebMetaData != null)
      {
         JBossWebMetaData clone = new JBoss60WebMetaData();
         clone.merge(sharedJBossWebMetaData, sharedWebMetaData);
         unit.addAttachment(SHARED_JBOSSWEB_ATTACHMENT_NAME, clone);
      }
  }
   
}
