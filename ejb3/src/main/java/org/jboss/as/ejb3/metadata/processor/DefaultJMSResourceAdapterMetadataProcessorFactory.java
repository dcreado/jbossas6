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

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.deployers.metadata.processor.JBossMetaDataProcessorFactory;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.process.processor.JBossMetaDataProcessor;

/**
 * Creates a {@link DefaultJMSResourceAdapterMetadataProcessor} which is responsible 
 * for setting the default JMS resource adapter name for EJB3.x message driven beans
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class DefaultJMSResourceAdapterMetadataProcessorFactory implements JBossMetaDataProcessorFactory<JBossMetaData>
{

   private String defaultRAName;

   /**
    * Constructs a {@link DefaultJMSResourceAdapterMetadataProcessorFactory}
    * @param raName The default ra name
    */
   public DefaultJMSResourceAdapterMetadataProcessorFactory(String raName)
   {
      this.defaultRAName = raName;
   }

   /**
    * Returns an instance of {@link DefaultJMSResourceAdapterMetadataProcessor}
    */
   public JBossMetaDataProcessor<JBossMetaData> create(DeploymentUnit deploymentUnit)
   {
      return new DefaultJMSResourceAdapterMetadataProcessor(this.defaultRAName);
   }

}
