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
package org.jboss.system.server.profileservice.repository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;
import org.jboss.profileservice.spi.NoSuchDeploymentException;
import org.jboss.profileservice.spi.NoSuchProfileException;
import org.jboss.profileservice.spi.ProfileDeployment;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

/**
 * A abstract VFS based source for profile deployments.
 * 
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision: 104372 $
 */
public abstract class AbstractVFSProfileSource
{
   
   /** The deployment factory. */
   private static final LegacyProfileDeploymentFactory deploymentFactory = LegacyProfileDeploymentFactory.getInstance();
   
   /** The repository uris. */
   protected final Collection<URI> uris;

   /** The VFSDeployments. */
   private Map<String, ProfileDeployment> applicationCtxs = new ConcurrentHashMap<String, ProfileDeployment>();
   
   /** Allowed deployments filter. */
   private VirtualFileFilter deploymentFilter;
   
   /** The application files keyed by VirtualFile URI string. */
   private final Map<String, VirtualFile> applicationVFCache = new ConcurrentHashMap<String, VirtualFile>();
   
   /** Do a recursive search - default == true. */
   private boolean recursiveScan = true;

   /** The last time the profile was modified. */
   private volatile long lastModified;
   
   /** The logger. */
   protected final Logger log = Logger.getLogger(getClass());
   
   /**
    * Create the deployment name based on the {@link VirtualFile}.
    * 
    * @param root the virtual file
    * @return the deployment name
    * @throws URISyntaxException 
    */
   protected static String createDeploymentName(VirtualFile root) throws URISyntaxException
   {
      if (root == null)
         throw new IllegalArgumentException("Null root");

      return LegacyProfileDeploymentFactory.createDeploymentName(root);
   }
   
   /**
    * Create the deployment name based on a {@link URI}.
    * 
    * @param uri the deployment uri
    * @return the deployment name
    */
   protected static String createDeploymentName(URI uri)
   {
      return LegacyProfileDeploymentFactory.createDeploymentName(uri);
   }
   
   public AbstractVFSProfileSource(URI[] uris)
   {
      if(uris == null)
         throw new IllegalArgumentException("Null uris");
      
      this.uris = new ArrayList<URI>();
      for(URI uri : uris)
      {
         this.uris.add(uri);
      }
   }

   public VirtualFileFilter getDeploymentFilter()
   {
      return deploymentFilter;
   }
   
   public void setDeploymentFilter(VirtualFileFilter deploymentFilter)
   {
      this.deploymentFilter = deploymentFilter;
   }
   
   public boolean isRecursiveScan()
   {
      return recursiveScan;
   }
   
   public void setRecursiveScan(boolean recursiveScan)
   {
      this.recursiveScan = recursiveScan;
   }
   
   public URI[] getRepositoryURIs()
   {
      return uris.toArray(new URI[uris.size()]);
   }
   
   public Set<String> getDeploymentNames()
   {
      return this.applicationCtxs.keySet();
   }

   public Collection<ProfileDeployment> getDeployments()
   {
      return this.applicationCtxs.values();
   }
   
   public long getLastModified()
   {
      return this.lastModified;
   }
   
   public void destroy() 
   {
      boolean trace = log.isTraceEnabled();
      for(ProfileDeployment deployment : getDeployments())
      {
         try
         {
            deploymentFactory.cleanup(deployment.getName());
         }
         catch(IOException ignore)
         {
            if(trace)
            {
               log.trace("error while cleaning up the the deployment backup", ignore);
            }
         }
      }
      // Unload
      this.applicationCtxs.clear();
      this.applicationVFCache.clear();
      updateLastModfied();
   }
   
   /**
    * Add a deployment to this profile source. 
    * 
    * @param vfsPath the deployment name
    * @param d the deployment
    * @throws Exception
    */
   public void addDeployment(String vfsPath, ProfileDeployment d) throws Exception
   {
      if(d == null)
         throw new IllegalArgumentException("Null deployment");
      
      // Add deployment and vfs
      this.applicationCtxs.put(d.getName(), d);
      if(d.getRoot() != null)
         this.applicationVFCache.put(d.getName(), d.getRoot());
      updateLastModfied();
   }
   
   /**
    * Get the deployment
    * 
    * @param vfsPath the deployment
    * @return the deployment or null if it does not exist
    */
   public ProfileDeployment getDeployment(String vfsPath) throws NoSuchDeploymentException
   {
      if(vfsPath == null)
         throw new IllegalArgumentException("Null vfsPath");

      return this.applicationCtxs.get(vfsPath);
   }
   
   /**
    * Remove a deployment from this source.
    * 
    * @param vfsPath the deployment name
    * @return the deployment
    * @throws NoSuchProfileException if the deployment does not exist
    */
   public ProfileDeployment removeDeployment(String vfsPath) throws Exception
   {
      if(vfsPath == null)
         throw new IllegalArgumentException("Null vfsPath");
      
      // Get the deployment
      ProfileDeployment deployment = getDeployment(vfsPath);
      String deploymentName = deployment.getName();
      // Remove the entries
      this.applicationCtxs.remove(deploymentName);
      cleanupDeployment(deploymentName);
      // Update last modified
      updateLastModfied();
      // Return
      return deployment;
   }
   
   protected void cleanupDeployment(String deploymentName)
   {
      try
      {
         this.applicationVFCache.remove(deploymentName);
         // Cleanup the backup
         deploymentFactory.cleanup(deploymentName);
      }
      catch(Exception e)
      {
         log.debug("failed to cleanup deployment " + deploymentName, e);
      }
   }
   
   /**
    * Load all the applications under the applicationDir.
    * 
    * @param applicationDir the application directory
    * @throws IOException
    */
   protected void loadApplications(VirtualFile applicationDir) throws Exception
   {
      ArrayList<VirtualFile> added = new ArrayList<VirtualFile>();
      addedDeployment(added, applicationDir);
      for (VirtualFile vf : added)
      {
         ProfileDeployment vfCtx = createDeployment(vf);
         addDeployment(vfCtx.getName(), vfCtx);
      }
   }
   
   /**
    * Scan the children of the root for deployments.
    * 
    * @param list a list of virtual files, where new deployments are added to
    * @param root the root to scan
    * @throws IOException
    * @throws URISyntaxException
    */
   protected void addedDeployments(List<VirtualFile> list, VirtualFile root) throws IOException, URISyntaxException
   {
      if(root.isFile() == true)
      {
         addedDeployment(list, root);
      }
      else
      {
         List<VirtualFile> components = root.getChildren();
         for (VirtualFile component : components)
         {
            addedDeployment(list, component);
         }         
      }
   }
   
   /**
    * Scan a given virtualFile for deployments.
    * 
    * @param list a list of virtual files, where new deployments are added to
    * @param component the root file
    * @throws IOException
    * @throws URISyntaxException
    */
   protected void addedDeployment(List<VirtualFile> list, VirtualFile component) throws IOException, URISyntaxException
   {
      // Excluding files from scanning
      if(deploymentFilter != null && this.deploymentFilter.accepts(component) == false)
      {
         if(log.isTraceEnabled())
            log.trace("ignoring "+ component);
         return;
      }
      
      // Check if we accept this deployment
      String key = createDeploymentName(component);
      if(acceptsDeployment(key) == false)
         return;

      // If it's a directory or exploded deployment
      if(component.isFile() == false)
      {
         // Check the name
         if(isRecursiveScan() && component.getName().indexOf('.') == -1)
         {
            // recurse if not '.' in name and recursive search is enabled
            addedDeployments(list, component);            
         }
         else
         {
            list.add(component);
         }
      }
      else
      {
         list.add(component);
      }
   }
   
   /**
    * Check if the deployment should be added.
    * 
    * @param name the deployment name
    * @return  
    */
   protected boolean acceptsDeployment(String name)
   {
      return applicationCtxs.containsKey(name) == false;
   }
   
   /**
    * Try to find a deployment, based on the cached
    * virtual roots of deployments we have.
    * 
    * @param name the deployment name
    * @return a collection of matching names
    */
   protected List<String> findDeploymentContent(String name)
   {
      if(this.applicationVFCache.containsKey(name))
      {
         return Collections.singletonList(name);
      }
      
      List<String> contents = new ArrayList<String>();
      for(String cacheName : this.applicationVFCache.keySet())
      {
         String fixedName = cacheName;
         if(cacheName.endsWith("/"))
            fixedName = cacheName.substring(0, cacheName.length() -1);
         
         if(fixedName.endsWith(name))
         {
            contents.add(cacheName);
         }
      }
      return contents;
   }
   
   /**
    * Add a virtualFile to the local virtualFileCache
    * 
    * @param vf the virtual file
    * @return the name
    * @throws MalformedURLException
    * @throws URISyntaxException
    */
   @Deprecated
   protected String addVirtualFileCache(VirtualFile vf) throws MalformedURLException, URISyntaxException
   {
      String uri = createDeploymentName(vf);
      this.applicationVFCache.put(uri, vf);
      return uri;
   }

   /**
    * Get a cached virtual file.
    * 
    * @param name the name
    * @return the virtual file or null, if it does not exist
    */
   @Deprecated
   protected VirtualFile getCachedVirtualFile(String name)
   {
      return this.applicationVFCache.get(name);
   }
   
   /**
    * Get a cached virtual file. This will add the uri to the
    * virtualFile if it does not exist.
    * 
    * @param uri the uri
    * @return the virtual file
    * @throws Exception
    */
   @Deprecated
   protected VirtualFile getCachedVirtualFile(URI uri) throws IOException 
   {
      VirtualFile vf = getCachedVirtualFile(uri.toString());
      if(vf == null)
      {
         vf = VFS.getChild(uri);
         String name = createDeploymentName(uri);
         this.applicationVFCache.put(name, vf);
      }
      return vf;
   }
   
   /**
    * Create a profile deployment.
    * 
    * @param vf the original virtual file
    * @return the profile deployment
    * @throws Exception
    */
   protected abstract ProfileDeployment createDeployment(VirtualFile vf) throws Exception;
   
   /**
    * Create a ProfileDeployment, based on avirtual file.
    * 
    * @param profileName the profile name
    * @param name the deployment name
    * @param vf the deployment root
    * @return the profile deployment
    * @throws Exception
    */
   protected static ProfileDeployment createDeployment(String profileName, String name, VirtualFile original) throws Exception
   {           
      return deploymentFactory.createProfileDeployment(profileName, name, original);
   }
   
   /**
    * Internally update the lastModified timestamp.
    */
   protected void updateLastModfied()
   {
      this.lastModified = System.currentTimeMillis();
   }
}
