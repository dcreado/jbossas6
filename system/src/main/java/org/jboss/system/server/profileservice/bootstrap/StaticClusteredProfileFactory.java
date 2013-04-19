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

package org.jboss.system.server.profileservice.bootstrap;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.profileservice.profile.metadata.plugin.FarmingProfileMetaData;
import org.jboss.profileservice.spi.metadata.ProfileMetaData;
import org.jboss.system.server.profileservice.repository.clustered.metadata.ClusteredProfileSourceMetaData;
import org.jboss.system.server.profileservice.repository.clustered.metadata.HotDeploymentClusteredProfileSourceMetaData;
import org.jboss.system.server.profileservice.repository.clustered.metadata.ImmutableClusteredProfileSourceMetaData;

/**
 * Expands upon the StaticProfileFactory to include a subprofiles
 * for farmed content.
 * 
 * @author Brian Stansberry
 */
public class StaticClusteredProfileFactory extends StaticBootstrapProfileFactory
{

   /** The deploy-hasingleton profile name. */
   private static final String HASINGLETON_NAME = "deploy-hasingleton";
   
   /** The farm profile name. */
   private static final String FARM_NAME = "farm";
   
   /** The hasingleton uris. */
   private List<URI> hasingletonURIs;
   
   /** The farm uris. */
   private List<URI> farmURIs;
   
//   private String partitionName;

   public List<URI> getHASingletonURIs()
   {
      return hasingletonURIs;
   }

   public void setHASingletonURIs(List<URI> hasingletonURIs)
   {
      this.hasingletonURIs = hasingletonURIs;
   }

   public List<URI> getFarmURIs()
   {
      return farmURIs;
   }

   public void setFarmURIs(List<URI> farmURIs)
   {
      this.farmURIs = farmURIs;
   }

//   public String getPartitionName()
//   {
//      return partitionName;
//   }
//
//   public void setPartitionName(String partitionName)
//   {
//      this.partitionName = partitionName;
//   }
//
//   @Override
//   public void create() throws Exception
//   {
//      super.create();
//      
//      if (this.farmURIs != null || this.hasingletonURIs != null)
//      {
//         if (this.partitionName == null)
//         {
//            throw new IllegalStateException("Null partition name.");
//         }
//      }
//   }
   
   /**
    * Create the cluster profiles, including the application profile from the
    * StaticProfileFactory.
    * 
    */
   @Override
   protected void createApplicationProfiles(List<ProfileMetaData> profiles, List<String> dependencies)
   {
      // Create the application profile
      super.createApplicationProfiles(profiles, dependencies);
      
      // Create the farm profile
      ProfileMetaData farm = null;
      if (getFarmURIs() != null)
      {
         URI[] farmURIs = getFarmURIs().toArray(new URI[getFarmURIs().size()]);
         farm = createFarmingProfileMetaData(
               FARM_NAME, true, farmURIs, dependencies);
         // Add
         profiles.add(farm);
         dependencies.add(FARM_NAME);
      }
      // Create the hasingleton profile
      if (getHASingletonURIs() != null && getHASingletonURIs().isEmpty() == false)
      {
         if(getHASingletonURIs().size() != 1)
         {
            // WARN
         }
         URI hasingletonURI = getHASingletonURIs().get(0);
         // Note HASingleton can't depend on others or it will get undeployed prematurely
         ProfileMetaData hasingletons = createScanningProfile(HASINGLETON_NAME, hasingletonURI,
               Collections.EMPTY_SET, true);
         // Add
         profiles.add(hasingletons);
         dependencies.add(HASINGLETON_NAME);
      }
   }

   private ProfileMetaData createFarmingProfileMetaData(String name, boolean hotDeployment, URI[] uris, Collection<String> subProfiles)
   {
      FarmingProfileMetaData farming = new FarmingProfileMetaData();
      farming.setName(name);
      // create source
      farming.setSource(createClusteredSource(uris, hotDeployment));
      if(hotDeployment)
      {
         // Configure a HDSCanner
         farming.setScanPeriod(getScanPeriod());
      }
      processDependencies(farming, subProfiles);
      return farming;
   }
   
   /**
    * Create a profile repository source meta data.
    * 
    * @param uris the uris for the repository
    * @param hotDeployment to create a hotDeployment profile
    * 
    * @return the profile source meta data.
    */
   protected ClusteredProfileSourceMetaData createClusteredSource(URI[] uris, boolean hotDeployment)
   {
      ClusteredProfileSourceMetaData source = null;
      if(hotDeployment)
      {
         source = new HotDeploymentClusteredProfileSourceMetaData();
      }
      else
      {
         source = new ImmutableClusteredProfileSourceMetaData();
      }
      
//      source.setPartitionName(getPartitionName());
      
      List<String> sources = new ArrayList<String>();
      for(URI uri : uris)
         sources.add(uri.toString());
      source.setSources(sources);
      return source;
   }

}
