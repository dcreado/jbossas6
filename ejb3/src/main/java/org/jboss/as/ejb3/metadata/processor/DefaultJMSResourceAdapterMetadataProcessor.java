/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
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

package org.jboss.as.ejb3.metadata.processor;

import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossMessageDrivenBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.process.ProcessingException;
import org.jboss.metadata.process.processor.JBossMetaDataProcessor;

/**
 * Responsible for setting the default JMS resource adapter name to be used for EJB3.x Message Driven Beans which
 *  do *not* explicitly set the resource adapter name
 * 
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class DefaultJMSResourceAdapterMetadataProcessor implements JBossMetaDataProcessor<JBossMetaData>
{

   private static Logger logger = Logger.getLogger(DefaultJMSResourceAdapterMetadataProcessor.class);

   private String defaultResourceAdapterName;

   /**
    * Constructs a {@link DefaultJMSResourceAdapterMetadataProcessor} 
    *  
    * @param defaultJMSResourceAdapterName The default resource adapter name to be used
    *       for EJB3.x MDBs which do not explicitly set the resource adapter name 
    */
   public DefaultJMSResourceAdapterMetadataProcessor(String defaultJMSResourceAdapterName)
   {
      this.defaultResourceAdapterName = defaultJMSResourceAdapterName;
   }

   /**
    * Sets the default JMS resource adapter name to be used for EJB3.x Message Driven Beans which
    * do *not* explicitly set the resource adapter name
    */
   public JBossMetaData process(JBossMetaData metadata) throws ProcessingException
   {
      // we are only concerned with EJB3.x
      if (!metadata.isEJB3x())
      {
         return metadata;
      }
      JBossEnterpriseBeansMetaData enterpriseBeans = metadata.getEnterpriseBeans();
      if (enterpriseBeans == null)
      {
         return metadata;
      }
      for (JBossEnterpriseBeanMetaData enterpriseBean : enterpriseBeans)
      {
         // only check for MDBs
         if (!enterpriseBean.isMessageDriven())
         {
            continue;
         }
         JBossMessageDrivenBeanMetaData messageDrivenBean = (JBossMessageDrivenBeanMetaData) enterpriseBean;
         String raName = messageDrivenBean.getResourceAdapterName();
         // if ra name is not explicitly specified then set the default ra name
         if (raName == null || raName.trim().isEmpty())
         {
            messageDrivenBean.setResourceAdapterName(this.defaultResourceAdapterName);
            if (logger.isTraceEnabled())
            {
               logger.trace("Set the default resource adapter name to " + this.defaultResourceAdapterName
                     + " for message driven bean " + messageDrivenBean.getEjbName());
            }
         }
      }
      return metadata;
   }

}
