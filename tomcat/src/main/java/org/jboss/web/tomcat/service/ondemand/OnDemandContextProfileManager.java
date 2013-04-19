/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc, and individual contributors
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
package org.jboss.web.tomcat.service.ondemand;

import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;

import org.jboss.logging.Logger;
import org.jboss.profileservice.profile.metadata.helpers.ProfileMetaDataFactory;
import org.jboss.profileservice.spi.NoSuchProfileException;
import org.jboss.profileservice.spi.Profile;
import org.jboss.profileservice.spi.ProfileFactory;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.profileservice.spi.ProfileService;
import org.jboss.profileservice.spi.metadata.ProfileMetaData;

/**
 * {@link ContextDemandListener} that creates and registers a {@link ProfileService}
 * {@link Profile} and then {@link #activateProfile() activates it} when
 * it receives a notification that a web request wishes to access a targetted
 * application.
 * 
 * TODO: deal with host name aliases and multiple contexts
 * 
 * TODO: The ProfileService integration aspect of this class duplicates 
 * equivalent functionality used for clustering's deploy-hasingleton directory
 * deployment. Both solve the same conceptual problem of an external event 
 * triggering activation of a profile. Abstract this out and put it in a
 * shared location.
 * 
 * @author Brian Stansberry
 * @version $Revision: 104372 $
 */
public class OnDemandContextProfileManager
{   
   public static final String DEFAULT_SERVICE_NAME = "jboss.web";
   public static final String DEFAULT_HOST_NAME = "localhost";
   public static final String DEFAULT_ROOT_WAR_PROFILE_NAME = "ROOT.war";
   
   /** The profile service */
   private ProfileService profileService;

   /** Integration hook into JBoss Web */
   private OnDemandContextIntegrator contextIntegrator;
   
   /** The root of the profile */
   private URI deploymentRoot;
   
   /** The deployment names. */
   private Collection<String> deploymentNames;
   
   protected final Logger log = Logger.getLogger(getClass());
   
   /** Whether this node has activated its profile */
   private boolean activated;
   
   /** The profile service key domain */
   private String profileDomain;
   
   /** The profile service key server */
   private String profileServer;
   
   /** The profile service key name */
   private String profileName;
   
   /** The profile service key */
   private ProfileKey profileKey;

   /** Name of the JBoss Web service we associate with */
   private String serviceName = DEFAULT_SERVICE_NAME;
   /** Name of the JBoss Web virtual host we associate with */
   private String hostName = DEFAULT_HOST_NAME;
   /** Name of the JBoss Web context we associate with */
   private String contextName;
   /** contextName with a preceding / (if not root context) */
   private String contextPath; // TODO do something smarter
   
   private boolean activateOnDemand = true;
   
   private final ContextDemandListener contextDemandListener = new Listener();

   // ----------------------------------------------------------- Constructors
   
   /**
    * Create a new OnDemandContextProfileManager.
    */
   public OnDemandContextProfileManager()
   {
      super();
   }
   
   // ----------------------------------------------------------  Properties


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
    * @return the domain, or <code>null</code> if not set
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
    * @param profileDomain the domain, or <code>null</code>
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
    * @return the server, or <code>null</code> if not set
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
    * @param profileServer the server, or <code>null</code>
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
    * @return the name, or <code>null</code> if not set and 
    *         {@link #setContextName(String) contextName} is also unset.
    */
   public String getProfileName()
   {
      if (profileName == null)
      {
         if (contextName != null)
         {
            if ("".equals(contextName))
            {
               profileName = DEFAULT_ROOT_WAR_PROFILE_NAME;
            }
            else
            {
               profileName = contextName + ".war";
            }
         }
      }
      return profileName;
   }

   /**
    * Sets the value that should be used for the 
    * {@link ProfileKey#getName() name} portion of
    * the singleton @{link Profile}'s {@link #getProfileKey() ProfileKey}.
    * 
    * @param profileName the name, or <code>null</code>
    */
   public void setProfileName(String profileName)
   {
      this.profileName = profileName;
   }
   
   /**
    * Gets whether this object has activated its profile.
    * 
    * @return <code>true</code> if {@link #activateProfile()} has successfully
    *         completed and {@link #releaseProfile()} has not been called;
    *         <code>false</code> otherwise.
    */
   public boolean isActivated()
   {
      return activated;
   }
   
   

   public String getServiceName()
   {
      return serviceName;
   }

   public void setServiceName(String serviceName)
   {
      if (serviceName == null)
      {
         throw new IllegalArgumentException("serviceName is null");
      }
      this.serviceName = serviceName;
   }

   public String getHostName()
   {
      return hostName;
   }

   public void setHostName(String hostName)
   {
      if (hostName == null)
      {
         throw new IllegalArgumentException("hostName is null");
      }
      this.hostName = hostName;
   }

   public String getContextName()
   {
      return contextName;
   }

   public void setContextName(String contextName)
   {
      if (contextName == null)
      {
         throw new IllegalArgumentException("contextName is null");
      }
      if ("ROOT".equals(contextName) || "/ROOT".equals(contextName))
      {
         contextName = "";
      }
      this.contextName = contextName;
   }

   /**
    * Sets the ProfileService reference.
    * 
    * @param profileService the profileService. Cannot be <code>null</code>
    * 
    * @throws IllegalArgumentException if <code>profileService</code> is <code>null</code>
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
    * Sets the {@link OnDemandContextIntegrator} used to integrate with the
    * web server
    * 
    * @param contextManager the manager. Cannot be <code>null</code>
    * 
    * @throws IllegalArgumentException if <code>contextManager</code> is <code>null</code>
    */
   public void setOnDemandContextIntegrator(OnDemandContextIntegrator contextManager)
   {
      if (contextManager == null)
      {
         throw new IllegalArgumentException("contextManager is null");
      }
      this.contextIntegrator = contextManager;
   } 

   /**
    * Gets whether the profile should be activated on during the {@link #start()}
    * phase of this bean's deployment rather than on receipt of an HTTP request. 
    * This property allows a simple configuration to turn off the "on-demand"
    * behavior for environments (e.g. production servers) where a more
    * deterministic startup is appropriate.
    * 
    * @return <code>false</code> if the profile should be activated as part of
    *         startup of this bean; <code>true</code> if activation should
    *         be deferred until an HTTP request is received. Default is
    *         <code>true</code>
    *         
    * @deprecated This is a temporary API for AS 6.0.0.M2; something else
    *             may replace it in later releases
    */
   public boolean isActivateOnDemand()
   {
      return activateOnDemand;
   }

   /**
    * Sets whether the profile should be activated on during the {@link #start()}
    * phase of this bean's deployment rather than on receipt of an HTTP request. 
    * This property allows a simple configuration to turn off the "on-demand"
    * behavior for environments (e.g. production servers) where a more
    * deterministic startup is appropriate.
    * 
    * @param activateOnDemand <code>false</code> if the profile should be 
    *                          activated as part of startup of this bean; 
    *                          <code>true</code> if activation should be 
    *                          deferred until an HTTP request is received.
    *         
    * @deprecated This is a temporary API for AS 6.0.0.M2; something else
    *             may replace it in later releases
    */
   public void setActivateOnDemand(boolean activateOnDemand)
   {
      this.activateOnDemand = activateOnDemand;
   }  

   // -----------------------------------------------------------------  Public

   /**
    * Builds a profile from the {@link #getURIList() URI list} and registers
    * it under the configured {@link #getProfileKey()}.
    */
   public void start() throws Exception
   {
      if (profileService == null)
      {
         throw new IllegalStateException("Must configure ProfileService");
      }
      
      if (contextIntegrator == null)
      {
         throw new IllegalStateException("Must configure OnDemandContextManager");
      }
      
      if (serviceName == null)
      {
         throw new IllegalStateException("Must configure serviceName");
      }
      
      if (hostName == null)
      {
         throw new IllegalStateException("Must configure hostName");
      }
      
      if (contextName == null)
      {
         throw new IllegalStateException("Must configure contextName");
      }

      if(deploymentRoot == null)
      {
         throw new IllegalStateException("Must configure deployment root");
      }
      
      if(deploymentNames == null)
      {
         throw new IllegalStateException("Must configure deployment name");
      }
      
      // TODO add dependencies on bootstrap profiles 
      String[] rootSubProfiles = new String[0];
      // Create a hotdeployment profile
      // FIXME JBAS-7720 restore hot deploy capability (and make it configurable too)
      ProfileMetaData metadata = ProfileMetaDataFactory.createFilteredProfileMetaData(getProfileName(), 
            deploymentRoot, this.deploymentNames.toArray(new String[this.deploymentNames.size()]));

      this.profileKey = this.profileService.registerProfile(metadata);
      
      if (this.activateOnDemand)
      {
         contextIntegrator.registerContextDemandListener(contextDemandListener);
         contextPath = (contextName.length() == 0 || '/' == contextName.charAt(0) ? contextName : "/" + contextName);
         contextIntegrator.registerOnDemandContext(serviceName, hostName, contextPath);  
      }
      else
      {    
         // FIXME we don't validate as we expect the PS to do it at the end
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
      
      if (profileService != null &&  profKey != null)
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
         finally
         {
            if (this.contextIntegrator != null)
            {
               this.contextIntegrator.removeContextDemandListener(contextDemandListener);
            }
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
    * @return the key. Will not return <code>null</code>
    * 
    * @throws IllegalStateException if {@link #getProfileName()} returns <code>null</code>
    * 
    * @see #getProfileDomain() 
    * @see #getProfileServer() 
    * @see #getProfileName()
    */
   public ProfileKey getProfileKey()
   {
      if (this.profileKey == null)
      {
         String name = getProfileName();
         if (name == null)
         {
            throw new IllegalStateException("Must configure profileName or contextName before calling getProfileKey()");
         }
         this.profileKey = new ProfileKey(getProfileDomain(), getProfileServer(), getProfileName());
      }
      return this.profileKey;
   }
   
   // ----------------------------------------------------------------  Private
   
   private synchronized void activateProfile(boolean validate) throws Exception
   {
      if (this.profileService == null)
      {
         throw new IllegalStateException("Must configure the ProfileService");
      }
      ProfileKey profKey = getProfileKey();
      if (this.profileService.getActiveProfileKeys().contains(profKey) == false)
      {         
         this.profileService.activateProfile(profKey);
         if (validate)
         {
            // Validate if the activation was successful
            this.profileService.validateProfile(profKey);
         }
         
         this.activated = true;
      }
      else
      {
         log.warn("Profile " + profKey + " is already activated");
         this.activated = true;
      }
      
   }

   /**
    * Tells the ProfileService to {@link ProfileService#releaseProfile(ProfileKey) release the profile}. 
    * Called by the HASingletonController when we are no longer the singleton master.
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
            log.warn("No Profile is registered under key " + getProfileKey());
         }
         
         this.activated = false;
      }
   }
   
   private class Listener implements ContextDemandListener
   {
      public void contextDemanded(String serviceName, String hostName, String contextName)
      {
         if (OnDemandContextProfileManager.this.contextPath.equals(contextName) 
               && OnDemandContextProfileManager.this.hostName.equals(hostName) 
               && OnDemandContextProfileManager.this.serviceName.equals(serviceName))
         {
            AccessController.doPrivileged(new PrivilegedAction<Object>()                  
            {
               public Object run()
               {
                  try
                  {
                     OnDemandContextProfileManager.this.activateProfile();
                  }
                  catch (Exception e)
                  {
                     log.error("Unable to activate profile " + getProfileKey(), e);
                  }
                  return null;
               }
            });
         }
      }
   }
   
}
