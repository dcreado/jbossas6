/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.deployers;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.common.deployers.spi.AttachmentNames;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.profileservice.profile.metadata.helpers.ProfileMetaDataFactory;
import org.jboss.profileservice.spi.NoSuchProfileException;
import org.jboss.profileservice.spi.Profile;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.profileservice.spi.ProfileService;
import org.jboss.profileservice.spi.metadata.ProfileMetaData;

/**
 * Activates a profile on-demand. The presence of a EJB3.x deployment unit in the server
 * triggers the activation. 
 * 
 * @author Jaikiran Pai
 * @version $Revision: $
 */
// Majority of this implementation is inspired (copied ;) ) from org.jboss.web.tomcat.service.ondemand.OnDemandContextProfileManager
// Forum thread reference: http://community.jboss.org/thread/155800
// JIRA: https://jira.jboss.org/browse/JBAS-8380
public class EJB3MetadataOnDemandDeploymentDeployer extends AbstractDeployer
{
   /** Logger */
   private static final Logger log = Logger.getLogger(EJB3MetadataOnDemandDeploymentDeployer.class);
 
   /** The default profile name to be used if no explicit profile name is provided for the on-demand profile */
   private static final String DEFAULT_EJB3_ONDEMAND_PROFILE_NAME = "EJB3_OnDemand_Profile";
 
   /** The profile service */
   private ProfileService profileService;
 
   /** The root of the profile */
   private URI deploymentRoot;
 
   /** The deployment names. */
   private Collection<String> deploymentNames;
 
   /** Whether this deployer has activated its profile */
   private boolean activated;
 
   /** The profile service key domain */
   private String profileDomain;
 
   /** The profile service key server */
   private String profileServer;
 
   /** The profile service key name */
   private String profileName;
 
   /** The profile service key */
   private ProfileKey profileKey;
 
   /** By default, we active the profile on-demand */
   private boolean activateOnDemand = true;
 
   /**
    * Instantiate the deployer and setup the {@link DeploymentStages} when
    * this deployer is expected to run and set the appropriate input for this deployer
    */
   public EJB3MetadataOnDemandDeploymentDeployer()
   {
      this.setStage(DeploymentStages.POST_CLASSLOADER);
      this.setInput(JBossMetaData.class);
      // ordering
      this.addInput(AttachmentNames.PROCESSED_METADATA);
   }
 
   /**
    * Get the deployment root
    * 
    * @return the deployment root
    */
   public URI getDeploymentRoot()
   {
      return deploymentRoot;
   }
 
   /**
    * Set the deployment root.
    * 
    * @param deploymentRoot the deployment root
    */
   public void setDeploymentRoot(URI deploymentRoot)
   {
      this.deploymentRoot = deploymentRoot;
   }
 
   /**
    * Get the deployment names.
    * 
    * @return the deployment names
    */
   public Collection<String> getDeploymentNames()
   {
      return deploymentNames;
   }
 
   /**
    * Set the deployment names
    * 
    * @param deploymentNames the deployment names
    */
   public void setDeploymentNames(Collection<String> deploymentNames)
   {
      this.deploymentNames = deploymentNames;
   }
 
   /**
    * Set a single deployment
    * 
    * @param name the deployment name
    */
   public void setSingleDeployment(String name)
   {
      this.deploymentNames = Collections.singleton(name);
   }
 
   /**
    * Gets the value that should be used for the 
    * {@link ProfileKey#getDomain() domain} portion of
    * the on-demand @{link Profile}'s {@link #getProfileKey() ProfileKey}.
    * 
    * @return the domain, or null if not set
    */
   public String getProfileDomain()
   {
      return profileDomain;
   }
 
   /**
    * Sets the value that should be used for the 
    * {@link ProfileKey#getDomain() domain} portion of
    * the singleton @{link Profile}'s {@link #getProfileKey() ProfileKey}.
    * 
    * @param profileDomain the domain, or null    
    */
   public void setProfileDomain(String profileDomain)
   {
      this.profileDomain = profileDomain;
   }
 
   /**
    * Gets the value that should be used for the 
    * {@link ProfileKey#getServer() server} portion of
    * the on-demand @{link Profile}'s {@link #getProfileKey() ProfileKey}.
    * 
    * @return the server, or null if not set
    */
   public String getProfileServer()
   {
      return profileServer;
   }
 
   /**
    * Sets the value that should be used for the 
    * {@link ProfileKey#getServer() server} portion of
    * the on-demand @{link Profile}'s {@link #getProfileKey() ProfileKey}.
    * 
    * @param profileServer the server, or null    
    */
   public void setProfileServer(String profileServer)
   {
      this.profileServer = profileServer;
   }
 
   /**
    * Gets the value that should be used for the 
    * {@link ProfileKey#getName() name} portion of
    * the on-demand @{link Profile}'s {@link #getProfileKey() ProfileKey}.
    * 
    * @return Returns the profile name if it is set. Else returns {@link #DEFAULT_EJB3_ONDEMAND_PROFILE_NAME}
    */
   public String getProfileName()
   {
      if (profileName == null)
      {
         this.profileName = DEFAULT_EJB3_ONDEMAND_PROFILE_NAME;
      }
      return profileName;
   }
 
   /**
    * Sets the value that should be used for the 
    * {@link ProfileKey#getName() name} portion of
    * the singleton @{link Profile}'s {@link #getProfileKey() ProfileKey}.
    * 
    * @param profileName the name, or null    
    */
   public void setProfileName(String profileName)
   {
      this.profileName = profileName;
   }
 
   /**
    * Gets whether this deployer has activated its profile.
    * 
    * @return true if {@link #activateProfile()} has successfully
    *         completed and {@link #releaseProfile()} has not been called;
    *         false otherwise.
    */
   public boolean isActivated()
   {
      return activated;
   }
 
   /**
    * Sets the ProfileService reference.
    * 
    * @param profileService the profileService. Cannot be null     
    * @throws IllegalArgumentException if profileService is null    
    */
   public void setProfileService(ProfileService profileService)
   {
      if (profileService == null)
      {
         throw new IllegalArgumentException("profileService is null");
      }
 
      this.profileService = profileService;
   }
   
   /**
    * Gets whether the profile should be activated on during the {@link #start()}
    * phase of this bean's deployment rather than on first EJB3.x deployment.
    * This property allows a simple configuration to turn off the "on-demand"
    * behavior for environments (e.g. production servers) where a more
    * deterministic startup is appropriate.
    * 
    * @return false if the profile should be activated as part of
    *         startup of this bean; true if activation should
    *         be deferred until an EJB3.x deployment is available in the server. 
    *         Default is true            
    *
    */
   public boolean isActivateOnDemand()
   {
      return activateOnDemand;
   }
 
   /**
    * Sets whether the profile should be activated on during the {@link #start()}
    * phase of this bean's deployment rather than on first EJB3.x deployment. 
    * This property allows a simple configuration to turn off the "on-demand"
    * behavior for environments (e.g. production servers) where a more
    * deterministic startup is appropriate.
    * 
    * @param activateOnDemand false if the profile should be 
    *                          activated as part of startup of this bean; 
    *                          true if activation should be 
    *                          deferred until an EJB3.x deployment is available in the server.
    * 
    */
   public void setActivateOnDemand(boolean activateOnDemand)
   {
      this.activateOnDemand = activateOnDemand;
   } 
 
   /**
    * Builds a profile from the  {@link #getDeploymentRoot()} and {@link #getDeploymentNames() list} 
    * and registers it under the configured {@link #getProfileKey()}.
    */
   public void start() throws Exception
   {
      if (profileService == null)
      {
         throw new IllegalStateException("Must configure ProfileService");
      }
 
      if (deploymentRoot == null)
      {
         throw new IllegalStateException("Must configure deployment root");
      }
 
      if (deploymentNames == null || deploymentNames.isEmpty())
      {
         throw new IllegalStateException("Must configure deployment name(s)");
      }
 
      // TODO add dependencies on bootstrap profiles 
      String[] rootSubProfiles = new String[0];
      // Create a hotdeployment profile
      // FIXME JBAS-7720 restore hot deploy capability (and make it configurable too)
      ProfileMetaData profileMetaData = ProfileMetaDataFactory.createFilteredProfileMetaData(getProfileName(),deploymentRoot, this.deploymentNames.toArray(new String[this.deploymentNames.size()]));
      // register the profile metadata with the profile service
      this.profileKey = this.profileService.registerProfile(profileMetaData);
 
      // if on-demand is disabled, then just activate it now 
      if (this.activateOnDemand == false)
      {
         // we don't validate as we expect the PS to do it at the end
         // of startup; need to check if this is correct 
         activateProfile(false);
      }
   }
 
   /**
    * Unregisters the profile registered in {@link #start()}.
    */
   public void stop() throws Exception
   {
      ProfileKey profKey = null;
      try
      {
         profKey = getProfileKey();
      }
      catch (IllegalStateException e)
      {
         return;
      }
 
      if (profileService != null && profKey != null)
      {
         try
         {
            // Inactivate first if needed
            if (profileService.getActiveProfileKeys().contains(profKey))
            {
               releaseProfile();
            }
 
            profileService.unregisterProfile(profKey);
         }
         catch (NoSuchProfileException e)
         {
            log.warn("Could not unregister unknown profile " + profKey);
         }
      }
   }
 
   /**
    * Tells the ProfileService to 
    * {@link ProfileService#activateProfile(ProfileKey) activate the on-demand profile}.
    */
   public void activateProfile() throws Exception
   {
      activateProfile(true);
   }
 
   /**
    * Gets the key for the {@link Profile} that we activate and release.
    * 
    * @return the key. Will not return null     
    * @throws IllegalStateException if {@link #getProfileName()} returns null     
    * @see #getProfileDomain() 
    * @see #getProfileServer() 
    * @see #getProfileName()
    */
   public ProfileKey getProfileKey()
   {
      if (this.profileKey == null)
      {
         String profileName = getProfileName();
         if (profileName == null)
         {
            throw new IllegalStateException("Must configure profileName or contextName before calling getProfileKey()");
         }
         // create the ProfileKey
         this.profileKey = new ProfileKey(getProfileDomain(), getProfileServer(), profileName);
      }
      return this.profileKey;
   }
 
   /**
    * First checks whether the on-demand profile is already activated. If yes, then this method
    * returns immediately. If the profile hasn't yet been activated, the method then checks 
    * whether the <code>unit</code> is a EJB3.x deployment. If not, the method returns immediately.
    * <p>
    *   For EJB3.x deployments, this method then activates the on-demand {@link Profile} containing the
    *   on-demand deployments
    * </p>
    * 
    * @param unit The {@link DeploymentUnit} currently being deployed
    * @throws DeploymentException
    */
   @Override
   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      if (this.isActivated())
      {
         return;
      }
      // get the metadata
      JBossMetaData metadata = unit.getAttachment(JBossMetaData.class);
 
      if (metadata.isEJB3x() == false)
      {
         return;
      }
      
      // activate the on-demand profile
      try
      {
         this.activateProfile(true);
      }
      catch (Exception e)
      {
         throw new DeploymentException("Could not activate on-demand profile: " + this.getProfileName()
               + " while deploying unit: " + unit);
      }
   }
 
   /**
    * Activates the on-demand {@link Profile}
    * 
    * @param validate If true, then the {@link Profile} is validated after being activated
    * @throws Exception
    */
   private synchronized void activateProfile(boolean validate) throws Exception
   {
      if (this.profileService == null)
      {
         throw new IllegalStateException("Must configure the ProfileService");
      }
      
      ProfileKey profKey = getProfileKey();
      
      // only activate the profile if it's *not* already activated
      if (this.profileService.getActiveProfileKeys().contains(profKey) == false)
      {
         if (log.isDebugEnabled())
         {
            log.debug("Activating on-demand profile: " + profKey);
         }
         // activate
         this.profileService.activateProfile(profKey);
         
         if (validate)
         {
            if (log.isDebugEnabled())
            {
               log.debug("Validating on-demand profile: " + profKey);
            }
            
            // Validate if the activation was successful
            this.profileService.validateProfile(profKey);
         }
 
         this.activated = true;
      }
      else
      {
         if (log.isDebugEnabled())
         {
            log.debug("Profile " + profKey + " is already activated");
         }
         this.activated = true;
      }
 
   }
 
   /**
    * Tells the ProfileService to {@link ProfileService#releaseProfile(ProfileKey) release the profile}. 
    * 
    */
   private synchronized void releaseProfile() throws Exception
   {
      if (this.activated)
      {
         try
         {
            this.profileService.deactivateProfile(getProfileKey());
         }
         catch (NoSuchProfileException e)
         {
            log.warn("Can't deactivate profile since no Profile is registered under key " + getProfileKey());
         }
 
         this.activated = false;
      }
   }
 
}