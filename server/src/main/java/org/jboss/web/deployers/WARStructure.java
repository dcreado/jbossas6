/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.web.deployers;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.structure.ContextInfo;
import org.jboss.deployers.spi.structure.MetaDataType;
import org.jboss.deployers.vfs.plugins.structure.AbstractVFSArchiveStructureDeployer;
import org.jboss.deployers.vfs.spi.structure.StructureContext;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;
import org.jboss.vfs.util.automount.Automounter;
import org.jboss.vfs.util.automount.MountOption;

import java.io.IOException;
import java.util.List;

/**
 * WARStructure.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 111899 $
 */
public class WARStructure extends AbstractVFSArchiveStructureDeployer
{
   /** The default filter which allows jars/jar directories */
   public static final VirtualFileFilter DEFAULT_WEB_INF_LIB_FILTER = new SuffixMatchFilter(".jar", VisitorAttributes.DEFAULT);
   
   /** The web-inf/lib filter */
   private VirtualFileFilter webInfLibFilter = DEFAULT_WEB_INF_LIB_FILTER;

   /** The web-inf/lib/[some-archive]/META-INF filter */
   private VirtualFileFilter webInfLibMetaDataFilter;

   /** Whether to include web-inf in the classpath */
   private boolean includeWebInfInClasspath;

   /**
    * Sets the default relative order 1000.
    *
    */
   public WARStructure()
   {
      setRelativeOrder(1000);
   }

   /**
    * Get the webInfLibFilter.
    * 
    * @return the webInfLibFilter.
    */
   public VirtualFileFilter getWebInfLibFilter()
   {
      return webInfLibFilter;
   }

   /**
    * Set the webInfLibFilter.
    * 
    * @param webInfLibFilter the webInfLibFilter.
    * @throws IllegalArgumentException for a null filter
    */
   public void setWebInfLibFilter(VirtualFileFilter webInfLibFilter)
   {
      if (webInfLibFilter == null)
         throw new IllegalArgumentException("Null filter");
      this.webInfLibFilter = webInfLibFilter;
   }

   /**
    * Get webInfLibMetaDataFilter
    *
    * @return the webInfLibMetaDataFilter
    */
   public VirtualFileFilter getWebInfLibMetaDataFilter()
   {
      return webInfLibMetaDataFilter;
   }

   /**
    * Set the webInfLibMetaDataFilter.
    *
    * @param webInfLibMetaDataFilter the webInfLibFilter.
    */
   public void setWebInfLibMetaDataFilter(VirtualFileFilter webInfLibMetaDataFilter)
   {
      this.webInfLibMetaDataFilter = webInfLibMetaDataFilter;
   }

   /**
    * Should we include web-inf in classpath.
    *
    * @param includeWebInfInClasspath the include web-inf flag
    */
   public void setIncludeWebInfInClasspath(boolean includeWebInfInClasspath)
   {
      this.includeWebInfInClasspath = includeWebInfInClasspath;
   }

   public boolean doDetermineStructure(StructureContext structureContext) throws DeploymentException
   {
      ContextInfo context = null;
      VirtualFile file = structureContext.getFile();
      try
      {
         boolean trace = log.isTraceEnabled();

         // the WEB-INF
         VirtualFile webinf;
         
         // We require either a WEB-INF or the name ends in .war
         if (hasValidSuffix(file.getName()) == false)
         {
           webinf = file.getChild("WEB-INF");
           if (webinf.exists())
           {
              if (trace)
                 log.trace("... ok - directory has a WEB-INF subdirectory");
           }
           else
           {
              if (trace)
                 log.trace("... no - doesn't look like a war and no WEB-INF subdirectory.");
              return false;
           }
         }
         else if (trace)
         {
            log.trace("... ok - name ends in .war.");
         } 

         // Check for a META-INF for metadata
         // FIXME: This is not spec legal, descriptors could be loaded from this location
         String[] metaDataLocations = new String[]{"WEB-INF", "WEB-INF/classes/META-INF"};
         // Create a context for this war file and all its metadata locations
         context = createContext(structureContext, metaDataLocations);

         // Add all children as locations for TLDs (except classes and lib), recursively
         webinf = file.getChild("WEB-INF");
         boolean webinfExists = webinf.exists();
         if (webinfExists)
         {
            List<VirtualFile> children = webinf.getChildren();
            for (VirtualFile child : children)
            {
               if (!isLeaf(child) && (!"lib".equals(child.getName())) && (!"classes".equals(child.getName())))
               {
                  addMetaDataPath(structureContext, context, "WEB-INF/" + child.getName(), MetaDataType.ALTERNATIVE);
                  addPathsRecursively(structureContext, context, child, "WEB-INF/" + child.getName());
               }
            }
         }

         // Check for jars in WEB-INF/lib
         List<VirtualFile> archives = null;
         try
         {
            VirtualFile webinfLib = file.getChild("WEB-INF/lib");
            if (webinfLib.exists())
            {
               archives = webinfLib.getChildren(webInfLibFilter);
               // Add the jars' META-INF for metadata
               for (VirtualFile jar : archives)
               {
                  Automounter.mount(file, jar);
                  // either same as plain lib filter, null or accepts the jar
                  if (webInfLibMetaDataFilter == null || webInfLibMetaDataFilter == webInfLibFilter || webInfLibMetaDataFilter.accepts(jar))
                  {
                     VirtualFile metaInf = jar.getChild("META-INF");
                     if (metaInf.exists() && !isLeaf(metaInf))
                     {
                        addMetaDataPath(structureContext, context, "WEB-INF/lib/" + jar.getName() + "/META-INF", MetaDataType.ALTERNATIVE);
                        List<VirtualFile> children = metaInf.getChildren();
                        for (VirtualFile child : children)
                        {
                           if (!isLeaf(child) && (!"resources".equals(child.getName())))
                           {
                              addMetaDataPath(structureContext, context, "WEB-INF/lib/" + jar.getName() + "/META-INF/" + child.getName(), MetaDataType.ALTERNATIVE);
                              addPathsRecursively(structureContext, context, child, "WEB-INF/lib/" + jar.getName() + "/META-INF/" + child.getName());
                           }
                        }
                     }
                  }
               }
            }
         }
         catch (IOException e)
         {
            log.warn("Exception looking for WEB-INF/lib, " + file.getPathName() + ", " + e);
         }
         
         // Add the war manifest classpath entries
         addClassPath(structureContext, file, false, true, context);

         // Check for WEB-INF/classes
         VirtualFile classes = file.getChild("WEB-INF/classes");
         // Add WEB-INF/classes if present
         if (classes.exists())
            addClassPath(structureContext, classes, true, false, context);
         else if (trace)
            log.trace("No WEB-INF/classes for: " + file.getPathName());

         // and the top level jars in WEB-INF/lib
         if (archives != null)
         {
            for (VirtualFile jar : archives)
               addClassPath(structureContext, jar, true, true, context);
         }
         else if (trace)
         {
            log.trace("No WEB-INF/lib for: " + file.getPathName());
         }

         // do we include WEB-INF in classpath
         if (includeWebInfInClasspath && webinfExists)
         {
            addClassPath(structureContext, webinf, true, false, context);
         }

         // There are no subdeployments for wars
         return true;
      }
      catch (Exception e)
      {
         // Remove the invalid context
         if (context != null)
            structureContext.removeChild(context);

         throw DeploymentException.rethrowAsDeploymentException("Error determining structure: " + file.getName(), e);
      }
   }

   @Override
   protected boolean hasValidSuffix(String name)
   {
      return name.toLowerCase().endsWith(".war");
   }
   

   @Override
   protected void performMount(VirtualFile file) throws IOException
   {
      Automounter.mount(file, MountOption.EXPANDED, MountOption.COPY);
   }

   protected void addPathsRecursively(StructureContext context, ContextInfo info, VirtualFile parent, String path) throws IOException
   {
      List<VirtualFile> children = parent.getChildren();
      for (VirtualFile child : children)
      {
         if (!isLeaf(child))
         {
            String newPath = path + "/" + child.getName();
            addMetaDataPath(context, info, newPath, MetaDataType.ALTERNATIVE);
            addPathsRecursively(context, info, child, newPath);
         }
      }
   }
   
}
