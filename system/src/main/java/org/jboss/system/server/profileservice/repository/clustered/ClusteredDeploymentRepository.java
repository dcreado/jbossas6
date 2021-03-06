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

package org.jboss.system.server.profileservice.repository.clustered;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.jboss.profileservice.spi.DeploymentContentFlags;
import org.jboss.profileservice.spi.DeploymentOption;
import org.jboss.profileservice.spi.ModificationInfo;
import org.jboss.profileservice.spi.ProfileDeployment;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.system.server.profileservice.repository.HotDeploymentRepository;
import org.jboss.system.server.profileservice.repository.clustered.local.LocalContentManager;
import org.jboss.system.server.profileservice.repository.clustered.local.LocalContentManagerFactory;
import org.jboss.system.server.profileservice.repository.clustered.metadata.RepositoryContentMetadata;
import org.jboss.system.server.profileservice.repository.clustered.metadata.RepositoryItemMetadata;
import org.jboss.system.server.profileservice.repository.clustered.metadata.RepositoryRootMetadata;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * DeploymentRepository that keeps its contents in sync across a cluster.
 * 
 * @author Brian Stansberry
 * 
 * @version $Revision: $
 */
public class ClusteredDeploymentRepository
   extends HotDeploymentRepository
{
   private enum LoadStatus { UNLOADED, LOADING, LOADED };
   
   /** Handles interaction with the cluster */
   private RepositoryClusteringHandler clusteringHandler;
   /** URIs of the VFS roots we monitor, keyed by their name */
   private final Map<String, URI> namedURIMap;
   
   private final Map<ProfileKey, RepositoryClusteringHandler> clusteringHandlers;
   private final Set<LocalContentManagerFactory<?>> localContentManagerFactories ;
   private LocalContentManager<?> localContentManager;
   
   /** 
    * Whether we are loaded. Mutations of this field must occur with
    * at least a local lock.
    */
   private volatile LoadStatus loadStatus = LoadStatus.UNLOADED;
   
   private boolean created;
      
   /**
    * Create a new ClusteredDeploymentRepository.
    * 
    * @param key
    * @param uris
    * @throws IOException 
    */
   public ClusteredDeploymentRepository(ProfileKey key, URI[] uris, 
         Map<ProfileKey, RepositoryClusteringHandler> clusteringHandlers, 
         Set<LocalContentManagerFactory<?>> persisterFactories) 
         throws IOException
   {
      super(key, validateURIs(uris, key));
      
      if (clusteringHandlers == null)
      {
         throw new IllegalArgumentException("Null clusteringHandlers");
      }
      
      if (persisterFactories == null)
      {
         throw new IllegalArgumentException("Null persisterFactories");
      }
      this.namedURIMap = getNamedURIMap(getRepositoryURIs());
      this.clusteringHandlers = clusteringHandlers;
      this.localContentManagerFactories = persisterFactories;
   }
   
   // -------------------------------------------------------------- Properties

   public String getPartitionName()
   {
      // Prefer to return the value from our handler
      return this.clusteringHandler == null ? null 
                                            : this.clusteringHandler.getPartitionName();
   }

   public RepositoryClusteringHandler getClusteringHandler()
   {
      return clusteringHandler;
   }
   
   public synchronized boolean registerClusteringHandler(RepositoryClusteringHandler handler)
   {
      if (handler == null)
      {
         throw new IllegalArgumentException("handler is null");
      }
      
      boolean update = this.getProfileKey().equals(handler.getProfileKey());
      if (update)
      {
         this.clusteringHandler = handler;
      }
      return update;
   }
   
   public synchronized boolean unregisterClusteringHandler(RepositoryClusteringHandler handler)
   {
      if (handler == null)
      {
         throw new IllegalArgumentException("handler is null");
      }
      
      boolean update = this.getProfileKey().equals(handler.getProfileKey());
      
      if (update)
      {
         this.clusteringHandler = null;
      }
      
      return update;
   }

   // ---------------------------------------------------- DeploymentRepository

   /**
    * Extends superclass to check whether our configured URIs actually exist,
    * if so validating that a clustering handler has been injected, throwing
    * an <code>IllegalStateException</code> if not. This allows this
    * repository to function as an empty repository in a non-clustered server 
    * that doesn't have a RepositoryClusteringHandler, so long as the 
    * configured URIs don't actually exist (e.g. AS "default" config w/o 
    * a "farm/" dir). If the configured URIs *do* exist, that implies this
    * repository is meant to work, and a missing clustering handler dependency
    * is an exception condition.
    * 
    * {@inheritDoc}
    */
   @Override
   public void create() throws Exception
   {
      super.create();
      
      // See if our URIs actually exist. If not we don't care
      // if we are missing required dependencies. 
      boolean needToFunction = false;      
      for (URI uri : this.getRepositoryURIs())
      {
         if (getCachedVirtualFile(uri).exists())
         {
            needToFunction = true;
            break;
         }
      }
      
      if (needToFunction)
      {  
         this.clusteringHandler = this.clusteringHandlers.get(getProfileKey());
         
         if (this.clusteringHandler == null)
         {
            throw new IllegalStateException("Must register RepositoryClusteringHandler " +
            		"before calling create()");
         }
         
         for (LocalContentManagerFactory<?> factory : localContentManagerFactories)
         {
            if (factory.accepts(namedURIMap.values()))
            {
               this.localContentManager = factory.getLocalContentManager(namedURIMap, getProfileKey(), this.clusteringHandler.getLocalNodeName());
               break;
            }
         }
         
         if (this.localContentManager == null)
         {
            throw new IllegalStateException("No registered RepositoryContentPersisterFactory " +
            		"is capable of handling URIs " + namedURIMap.values() + 
            		" -- registeredFactories are " + localContentManagerFactories);
         }
         
         this.localContentManager.initialize();
      }
      
      this.created = true;
   }
   
   

   @Override
   public void destroy()
   {
      try
      {
         super.destroy();
      }
      finally
      {
         if (this.localContentManager != null)
         {
            this.localContentManager.shutdown();
         }
      }
         
   }

   public void load() throws Exception
   {      
      if (!created)
      {
         create();
      }
      
      if (this.clusteringHandler == null)
      {
         // We would have failed in create() if we actually had any valid URIs
         // configured. So this means there are no valid URIs and we
         // have nothing to do.
         return;
      }     
      
      this.clusteringHandler.initialize(this.localContentManager);
      
      // Take control of the cluster
      if (!this.clusteringHandler.lockGlobally())
      {
         throw new RuntimeException("Cannot acquire global lock");
      }
      try
      {         
         if (this.loadStatus != LoadStatus.UNLOADED)
         {
            log.warn("load() called when repository status is " + this.loadStatus);
            return;
         }
         
         this.loadStatus = LoadStatus.LOADING;
         
         // Bring our content in line with the cluster 
         clusteringHandler.synchronizeContent(true);
         
         // Load applications
         for(URI uri : getRepositoryURIs())
         {
            VirtualFile root = getCachedVirtualFile(uri);
            loadApplications(root);
         }
         
         updateLastModfied();
         
         this.loadStatus = LoadStatus.LOADED;
      }
      catch (Throwable t)
      {
         // Revert back to unloaded status
         this.loadStatus = LoadStatus.UNLOADED;
         
         if (t instanceof Exception)
         {
            throw (Exception) t;
         }
         else if (t instanceof Error)
         {
            throw (Error) t;
         }
         else
         {
            UnknownError unk = new UnknownError("Caught unknown Throwable");
            unk.initCause(t);
            throw unk;
         }
      }
      finally
      {
         this.clusteringHandler.unlockGlobally();
         
         if (this.loadStatus != LoadStatus.LOADED)
         {
            this.clusteringHandler.shutdown();
         }
      }
   }
   
   public String addDeploymentContent(String vfsPath, InputStream contentIS, DeploymentOption... options) throws IOException
   {
      if (this.clusteringHandler == null)
      {
         throw new IllegalStateException("Must register RepositoryClusteringHandler before invoking addDeploymentContent()");
      }
      
      String repositoryName = null;
      
      // Take control of the cluster
      if (!this.clusteringHandler.lockGlobally())
      {
         throw new RuntimeException("Cannot acquire global lock");
      }
      try
      {      
         RepositoryContentMetadata existingRCMD = new RepositoryContentMetadata(this.localContentManager.getOfficialContentMetadata());         
         RepositoryItemMetadata newRIMD = this.clusteringHandler.addDeploymentContent(vfsPath, contentIS);
         RepositoryRootMetadata existingRRMD = existingRCMD.getRepositoryRootMetadata(newRIMD.getRootName());
         boolean exists = existingRRMD != null && existingRRMD.getItemMetadata(newRIMD.getRelativePathElements()) != null;
         VirtualFile root = getCachedVirtualFile(namedURIMap.get(newRIMD.getRootName()));
         VirtualFile contentVF = root.getChild(newRIMD.getRelativePath());

         try
         {
            // Add the new virtual file to the cache
            repositoryName = addVirtualFileCache(contentVF);
            
            // Cleanup 
            if(exists)
               cleanUpRoot(contentVF);
         }
         catch(URISyntaxException e)
         {
            throw new RuntimeException(e); // This should not happen anyway
         }

         // Lock the content
         setDeploymentContentFlags(repositoryName, DeploymentContentFlags.LOCKED);
      }
      finally
      {
         this.clusteringHandler.unlockGlobally();
      }
      
      return repositoryName;
   }

   public ProfileDeployment removeDeployment(String vfsPath) throws Exception
   {
      if (this.clusteringHandler == null)
      {
         throw new IllegalStateException("Must register RepositoryClusteringHandler before invoking removeDeployment()");
      }
      
      // Take control of the cluster
      if (!this.clusteringHandler.lockGlobally())
      {
         throw new RuntimeException("Cannot acquire global lock");
      }
      try
      {
         ProfileDeployment deployment = getDeployment(vfsPath);
         if (deployment != null)
         {
            VirtualFile vf = VFS.getChild(vfsPath);
            for (Map.Entry<String, URI> entry : namedURIMap.entrySet())
            {
               String relativePath = null;
               VirtualFile root = VFS.getChild(entry.getValue());
               try
               {
                  relativePath = vf.getPathNameRelativeTo(root);
               }
               catch (IllegalArgumentException notParent)
               {
                  continue;
               }
               this.clusteringHandler.removeDeploymentContent(entry.getKey(), relativePath);
            }
            
            return super.removeDeployment(deployment.getName());
         }
         else
         {
            return null;
         }
      }
      finally
      {
         this.clusteringHandler.unlockGlobally();
      }
   }

   public Collection<ModificationInfo> getModifiedDeployments() throws Exception
   {
      if (this.clusteringHandler == null)
      {
         return Collections.emptyList();
      }
      
      if (this.loadStatus != LoadStatus.LOADED)
      {
         log.debug("Ignoring getModifiedDeployments() call as status is " + this.loadStatus);
      }
      
      // Only acquire a local lock until we know whether we actually 
      // have a modification
      RepositoryContentMetadata baseContent = null;
      RepositoryContentMetadata latestContent = null;
      if (!this.clusteringHandler.lockLocally())
      {
         // Don't throw an exception; just log error and indicate no mods
         log.error("getModifiedDeployments(): Cannot acquire local lock");
         return Collections.emptySet();
      }
      
      try
      {
         baseContent = this.localContentManager.getOfficialContentMetadata();
         latestContent = this.localContentManager.getCurrentContentMetadata();         
         boolean unmodified = latestContent.equals(baseContent);
         
         if (unmodified)
         {
            // Our metadata is in sync, so our content is in sync with the
            // cluster. However, we might have had changes made to our
            // content (e.g. via addDeploymentContenxt(...)) that have not
            // been deployed. So, check for those...
            return super.getModifiedDeployments();
         }
      }
      finally
      {
         this.clusteringHandler.unlockLocally();
      }
      
      
      // If we got here, our content is out of sync with the cluster, so take 
      // control of the cluster and bring the cluster in line with our current state      
      if (!this.clusteringHandler.lockGlobally())
      {
         // Don't throw an exception; just log error and indicate no mods
         log.error("getModifiedDeployments(): Cannot acquire global lock");
         return Collections.emptySet();
      }
      try
      {      
         // Tell the clustering handler to synchronize, but without
         // pulling anything to cluster -- just push our changes
         latestContent = this.clusteringHandler.synchronizeContent(false);
         
         return super.getModifiedDeployments();
      }
      finally
      {
         this.clusteringHandler.unlockGlobally();
      }      
   }
   
   public void unload()
   {      
      if (this.clusteringHandler == null)
      {
         // We would have failed in create() if we actually had any valid URIs
         // configured. So this means there are no valid URIs and we
         // have nothing to do.
         // Just call the superclass method to let it do whatever cleanup
         // it wants.
         super.unload();
         return;
      }
      
      if (this.loadStatus != LoadStatus.UNLOADED)
      {
         boolean locked = this.clusteringHandler.lockLocally();
         try
         {
            if (!locked)
            {
               log.warn("remove(): failed to acquire local lock");
            }
            this.loadStatus = LoadStatus.UNLOADED;
                        
            super.unload();
            
         }
         finally
         {
            if (locked)
            {
               this.clusteringHandler.unlockLocally();
            }
            
            try
            {
               this.clusteringHandler.shutdown();
            }
            catch (Exception e)
            {
               log.error("Caught exception shutting down RepositoryClusteringHandler", e);
            }
         }
      }      
   }

   public void remove() throws Exception
   {      
      this.clusteringHandler = null;
      if (this.localContentManager != null)
      {
         this.localContentManager.shutdown();
      }
      this.localContentManager = null; 
      this.created = false;     
   }

   // ----------------------------------------------------------------- Private

   private static URI[] validateURIs(URI[] uris, ProfileKey profileKey)
   {
      List<URI> list = new ArrayList<URI>();
      for (URI uri : uris)
      {
         try
         {
            VFS.getChild(uri);
            list.add(uri);
         }
         catch (Exception e)
         {
            Logger slog = Logger.getLogger(ClusteredDeploymentRepository.class);
            slog.error("Problem accessing URI " + uri + " -- it will not be " +
                 "used for profile " + profileKey + ". Problem was: " + 
                 e.getLocalizedMessage());
         }         
      }
      return list.toArray(new URI[list.size()]);
   }
   
   private Map<String, URI> getNamedURIMap(URI[] uris) throws IOException
   {
      Map<String, URI> map = new HashMap<String, URI>();
      if (uris != null)
      {
         for (URI uri : uris)
         {
            VirtualFile vf = getCachedVirtualFile(uri);
            map.put(vf.getName(), uri);
         }
      }
      
      return map;
   }

}
