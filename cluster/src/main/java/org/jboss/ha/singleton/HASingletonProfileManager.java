/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ha.singleton;

import java.net.URI;

import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementObjectID;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.jboss.profileservice.profile.metadata.helpers.ProfileMetaDataFactory;
import org.jboss.profileservice.profile.metadata.plugin.PropertyProfileMetaData;
import org.jboss.profileservice.profile.metadata.plugin.ScanPeriod;
import org.jboss.profileservice.spi.NoSuchProfileException;
import org.jboss.profileservice.spi.Profile;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.profileservice.spi.ProfileService;
import org.jboss.profileservice.spi.metadata.ProfileMetaData;

/**
 * Extends {@link HASingletonProfileActivator} by actually creating and
 * registering a {@link Profile} from a configurable set of URIs during
 * the {@link #start()} phase, deregistering it in the {@link #stop()} phase.
 * 
 * @author Brian Stansberry
 * @version $Revision: 104372 $
 */
@ManagementObject(componentType=@ManagementComponent(type="MCBean", subtype="HASingletonProfileManager"),
      properties=ManagementProperties.EXPLICIT)
public class HASingletonProfileManager extends HASingletonProfileActivator implements HASingletonProfileManagerMBean
{   
   
   /** The list of URIs to scan */
   private URI deployURI;
   
   /**
    * Create a new HASingletonProfileManager.
    *
    */
   public HASingletonProfileManager()
   {
      super();
   }
   
   // ----------------------------------------------------------  Properties

   @ManagementProperty(use={ViewUse.CONFIGURATION}, description="List of URIs from which the Profile content is obtained")
   public URI getDeployURI()
   {
      return deployURI;
   }
   
   public void setDeployURI(URI deployURI)
   {
      this.deployURI = deployURI;
   }
   

   @ManagementProperty(use={ViewUse.STATISTIC}, description="ProfileKey for the Profile we activate")
   @ManagementObjectID(type="HASingletonProfileManager")
   public ProfileKey getProfileKey()
   {
      return super.getProfileKey();
   }

   // -----------------------------------------------------------------  Public

   /**
    * Builds a profile from the {@link #getURIList() URI list} and registers
    * it under the configured {@link #getProfileKey()}.
    */
   public void start() throws Exception
   {    
      if (getProfileService() == null)
      {
         throw new IllegalStateException("Must configure profileService");
      }
      
      // TODO add dependencies on bootstrap profiles
      String[] rootSubProfiles = new String[0];
      // Create a hotdeployment profile
      ProfileMetaData metadata = createProfileMetaData(true, deployURI, rootSubProfiles);
      
      setProfileKey(getProfileService().registerProfile(metadata));
   }
   
   /**
    * Unregisters the profile registered in {@link #start()}.
    */
   public void stop() throws Exception
   {      
      ProfileService profSvc = getProfileService();
      ProfileKey profKey = getProfileKey();
      if (profSvc != null &&  profKey != null)
      {
         try
         {
            // Inactivate first if needed
            if (profSvc.getActiveProfileKeys().contains(profKey))
            {
               releaseProfile();
            }
            
            profSvc.unregisterProfile(profKey);
         }
         catch (NoSuchProfileException e)
         {
            log.warn("Could not unregister unknown profile " + profKey);
         }
      }
   }
   
   // ----------------------------------------------------------------  Private

   /**
    * Create a profile meta data.
    * 
    * @param name the profile name.
    * @param repositoryType the repository type.
    * @param uris the repository uris.
    * @param subProfiles a list of profile dependencies.
    * @return the profile meta data.
    */
   private ProfileMetaData createProfileMetaData(boolean hotDeployment, URI deployURI, String[] subProfiles)
   {
      PropertyProfileMetaData metaData = null;
      if(hotDeployment)
      {
         metaData = ProfileMetaDataFactory.createHotDeploymentScanningProfile(getProfileName(), deployURI, new ScanPeriod());         
      }
      else
      {
         metaData = ProfileMetaDataFactory.createImmutableScanningProfile(getProfileName(), deployURI);
      }
      return metaData;
   }
   
}
