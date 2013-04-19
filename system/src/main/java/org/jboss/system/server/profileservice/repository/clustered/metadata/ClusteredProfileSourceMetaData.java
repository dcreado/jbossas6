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

package org.jboss.system.server.profileservice.repository.clustered.metadata;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.jboss.profileservice.profile.metadata.plugin.PropertyProfileSourceMetaData;
import org.jboss.profileservice.spi.metadata.ProfileSourceMetaData;

/**
 * Base class for {@link ProfileSourceMetaData} that indicates a clustered
 * repository should be used.
 * 
 * @author Brian Stansberry
 */
public abstract class ClusteredProfileSourceMetaData extends PropertyProfileSourceMetaData
{

   /** The source. */
   private List<String> sources;
   
   /** The partition name. */
   private String partitionName;

   @XmlAttribute(name = "partition")
   public String getPartitionName()
   {
      return partitionName;
   }

   public void setPartitionName(String partitionName)
   {
      this.partitionName = partitionName;
   }  

   @XmlTransient
   public String getType()
   {
      return getClass().getName();
   }

   @XmlElement(name = "source")
   public List<String> getSources()
   {
      return sources;
   }
   
   public void setSources(List<String> sources)
   {
      this.sources = sources;
   }
   
}