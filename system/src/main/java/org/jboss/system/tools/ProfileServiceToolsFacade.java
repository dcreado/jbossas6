/*
* JBoss, Home of Professional Open Source
* Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.system.tools;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.profileservice.profile.metadata.plugin.HotDeploymentProfileMetaData;
import org.jboss.profileservice.profile.metadata.plugin.PropertyProfileSourceMetaData;
import org.jboss.profileservice.profile.metadata.plugin.ScanPeriod;
import org.jboss.profileservice.repository.artifact.ArtifactRepositoryManager;
import org.jboss.profileservice.spi.NoSuchProfileException;
import org.jboss.profileservice.spi.Profile;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.profileservice.spi.ProfileService;
import org.jboss.profileservice.spi.action.ActionController;
import org.jboss.profileservice.spi.managed.ManagedProfile;
import org.jboss.profileservice.spi.metadata.ProfileMetaData;
import org.jboss.profileservice.spi.repository.artifact.ArtifactId;
import org.jboss.profileservice.spi.repository.artifact.ArtifactRepository;
import org.jboss.vfs.util.PathTokenizer;

/**
 * Facade partially supporting the legacy usage of the URLDeploymentScanner for JBossTools.
 * 
 * TODO we need to work with them and provide a better way to deploy deployments, as it does
 * not make any sense at all to rely on a HDScanner, which maybe is enabled or not.
 * They either have to rely on a tool provided through ProfileService to distribute the content
 * to a hot-deployment folder, or there is no guarantee that the deployment will actually be deployed.
 * 
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision$
 */
public class ProfileServiceToolsFacade
{

   /** The profile prefix. */
   private static final String TOOLS_PREFIX = "JBossTools-";
   
   /** The interval. */
   private int scanPeriod = 5;
   
   /** The time unit. */
   private TimeUnit timeUnit = TimeUnit.SECONDS;

   private ActionController actionController;
   
   /** The profile service. */
   private ProfileService profileService;
   
   /** The artifact repository manager. */
   private ArtifactRepositoryManager repositoryManager;

   /** The created profiles. */
   private final Map<URI, ProfileKey> profiles = new HashMap<URI, ProfileKey>();
   
   /**
    * Get the scan period.
    * 
    * @return the scan period
    */
   public int getScanPeriod()
   {
      return scanPeriod;
   }
   
   /**
    * Set the scan period
    * 
    * @param scanPeriod the scan period
    */
   public void setScanPeriod(int scanPeriod)
   {
      this.scanPeriod = scanPeriod;
   }
   
   /**
    * Get the scan time unit.
    * 
    * @return the scan time unit
    */
   public TimeUnit getTimeUnit()
   {
      return timeUnit;
   }
   
   /**
    * Set the scan time unit.
    * 
    * @param timeUnit the scan time unit
    */
   public void setTimeUnit(TimeUnit timeUnit)
   {
      this.timeUnit = timeUnit;
   }
   
   /**
    * Get the profile service. 
    * 
    * @return the profile service
    */
   public ProfileService getProfileService()
   {
      return profileService;
   }

   /**
    * Set the profile service.
    * 
    * @param profileService the profile service
    */
   public void setProfileService(ProfileService profileService)
   {
      this.profileService = profileService;
   }
   
   /**
    * Get the artifact repository manager.
    * 
    * @return the artifact repository manager
    */
   public ArtifactRepositoryManager getRepositoryManager()
   {
      return repositoryManager;
   }
   
   /**
    * Set the artifact repository manager
    * 
    * @param repositoryManager the artifact repository manager
    */
   public void setRepositoryManager(ArtifactRepositoryManager repositoryManager)
   {
      this.repositoryManager = repositoryManager;
   }
   
   public ActionController getActionController() {
	   return actionController;
   }
   public void setActionController(ActionController actionController) {
      this.actionController = actionController;
   }
   
   /**
    * start()
    */
   public void start()
   {
      if(profileService == null)
      {
         throw new IllegalStateException("null profile service");
      }
      if(repositoryManager == null)
      {
         throw new IllegalStateException("null repository manager");
      }
      if(actionController == null) {
    	  throw new IllegalStateException("null action controller"); 
      }
   }
   
   /**
    * stop()
    */
   public void stop()
   {
      final Collection<ProfileKey> profiles = getRegisteredProfiles();
      for(ProfileKey key : profiles)
      {
         try
         {
            profileService.deactivateProfile(key);
            profileService.unregisterProfile(key);
         }
         catch(NoSuchProfileException ignore)
         {
            //
         }
      }
   }
   
   /**
    * Add a deployment URI
    * 
    * @param deploymentURI the deployment URI
    */
   public void addURI(URI deploymentURI)
   {
      synchronized(profiles)
      {
         if(profiles.containsKey(deploymentURI))
         {
            return;
         }
         // Check if the deployment URI is already managed by one of the repositories
         // NOTE this does not mean that hot-deployment is actually enabled
         if(managesLocation(deploymentURI))
         {
            return;
         }
         try
         {
            // Register
            final ProfileKey key = profileService.registerProfile(createProfileMetaData(deploymentURI));
            profiles.put(deploymentURI, key);
            // Activate
            profileService.activateProfile(key);
            profileService.validateProfile(key);
         }
         catch(Exception e)
         {
            new RuntimeException("failed add deployment uri", e);
         }
      }
   }

   /**
    * Remove a deployment URI
    * 
    * @param deploymentURI the deployment URI
    */
   public void removeURI(URI deploymentURI)
   {
      synchronized(profiles)
      {
         final ProfileKey key = profiles.remove(deploymentURI);
         if(key != null)
         {
            try
            {
               profileService.deactivateProfile(key);
               profileService.unregisterProfile(key);
            }
            catch(NoSuchProfileException ignore)
            {
               //
            }
         }
      }
   }
   
   /**
    * Get the registered profiles.
    * 
    * @return the profiles
    */
   protected Collection<ProfileKey> getRegisteredProfiles()
   {
      synchronized(profiles)
      {
         return profiles.values();
      }
   }
   
   /**
    * Check whether there a different profile managing a certain {@code URI}
    * 
    * @param uri the uri
    * @return true, if there is a repository managing this uri, false otherwise 
    * @throws URISyntaxException
    */
   public boolean managesURI(URI uri)
   {
      return managesLocation(uri);
   }
   
   /**
    * List all registered deployment roots.
    * 
    * @return the deployment urls.
    */
   public String[] listDeployedURLs() {
      final Collection<String> names = new HashSet<String>();
      for(final ProfileKey key : actionController.getActiveProfiles()) {
    	  final ManagedProfile managed = actionController.getManagedProfile(key);
    	  final Profile profile = managed.getProfile();
    	  final Set<String> deploymentNames = profile.getDeploymentNames();
    	  if(deploymentNames != null && deploymentNames.isEmpty() == false) {
    		  names.addAll(deploymentNames);  
    	  }
      }
      return names.toArray(new String[names.size()]);
   }

   /**
    * Check whether a repository manages a certain {@code URI}.
    * 
    * @param deploymentURI the deploymentURI
    * @return
    */
   protected boolean managesLocation(URI deploymentURI)
   {
      for(final ArtifactRepository<ArtifactId> repository : repositoryManager.getRepositories())
      {
         final URI artifactRepositoryURI = repository.getRepositoryURI();
         if(isRelative(artifactRepositoryURI.getPath(), deploymentURI.getPath()))
         {
            // If it's managed we ignore it, either the deployment gets picked up
            // by the HDScanner of the corresponding profile or nothing will happen.
            return true;
         }
      }
      return false;
   }
   
   /**
    * Check whether a path is relative to another. 
    * 
    * @param root the root path
    * @param relative the relative path
    * @return true or false
    */
   protected boolean isRelative(final String root, final String relative)
   {
      final Iterator<String> roots = PathTokenizer.getTokens(root).iterator();
      final Iterator<String> relatives = PathTokenizer.getTokens(relative).iterator();
      while(roots.hasNext())
      {
         if(relatives.hasNext() == false)
         {
            // Would recursively redeploy
            return true;
         }
         final String r = roots.next();
         final String o = relatives.next();
         if(r.equals(o) == false)
         {
            return false;
         }
      }
      return true;
   }
 
   /**
    * Create the profile meta data 
    * 
    * @param uri the deployment uri
    * @return the profile meta data
    */
   protected ProfileMetaData createProfileMetaData(final URI uri)
   {
      final HotDeploymentProfileMetaData profile = new HotDeploymentProfileMetaData();
      profile.setName(createProfileName(uri));
      profile.setSource(createSource(uri));
      profile.setScanPeriod(createScannerConfiguration());
      return profile;
   }
   
   /**
    * Create the profile name, based on a URI.
    * 
    * @param uri the deployment URI
    * @return the profile name
    */
   protected String createProfileName(final URI uri)
   {
      return TOOLS_PREFIX + uri.hashCode();
   }

   /**
    * Create a profile source based on the deployment uri.
    * 
    * @param uri the deployment URI
    * @return the profile source meta data
    */
   protected PropertyProfileSourceMetaData createSource(final URI uri)
   {
      return new PropertyProfileSourceMetaData(uri.toString());
   }

   /**
    * Create a hot-deployment scanner configuration.
    * 
    * @return the scanner configuration
    */
   protected ScanPeriod createScannerConfiguration()
   {
      final ScanPeriod configuration = new ScanPeriod();
      configuration.setScanPeriod(getScanPeriod());
      configuration.setStartAutomatically(true);
      configuration.setTimeUnit(getTimeUnit());
      configuration.setDisabled(false);
      return configuration;
   }
   
}

