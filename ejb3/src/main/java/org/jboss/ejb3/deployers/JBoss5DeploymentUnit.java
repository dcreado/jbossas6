/*
* JBoss, Home of Professional Open Source
* Copyright 2005, Red Hat Middleware LLC., and individual contributors as indicated
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
package org.jboss.ejb3.deployers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.ejb3.interceptor.InterceptorInfoRepository;
import org.jboss.ejb3.vfs.impl.vfs3.VirtualFileFilterAdapter;
import org.jboss.ejb3.vfs.impl.vfs3.VirtualFileWrapper;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.FilterVirtualFileVisitor;
import org.jboss.vfs.util.SuffixesExcludeFilter;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 101688 $
 */
public class JBoss5DeploymentUnit implements org.jboss.ejb3.DeploymentUnit
{
   private VFSDeploymentUnit unit;
   private ClassLoader classLoader;
   private InterceptorInfoRepository interceptorInfoRepository;
   private Map defaultPersistenceProperties;

   public JBoss5DeploymentUnit(VFSDeploymentUnit unit)
   {
      this(unit, unit.getClassLoader());
   }
   
   public JBoss5DeploymentUnit(VFSDeploymentUnit unit, ClassLoader classLoader)
   {
      assert unit != null : "unit is null";
      assert classLoader != null : "classLoader is null";
      
      this.unit = unit;
      this.classLoader = classLoader;
      this.interceptorInfoRepository = new InterceptorInfoRepository(classLoader);
   }

   public Object addAttachment(String name, Object attachment)
   {
      return unit.addAttachment(name, attachment);
   }
   public Object getAttachment(String name)
   {
      return unit.getAttachment(name);
   }
   public Object removeAttachment(String name)
   {
      return unit.removeAttachment(name);
   }

   public org.jboss.ejb3.vfs.spi.VirtualFile getRootFile()
   {
      return new VirtualFileWrapper(unit.getFile(""));
   }
   
   public String getRelativePath()
   {
      return unit.getRelativePath();
   }

   public URL getRelativeURL(String jar)
   {
      try
      {
         return new URL(jar);
      }
      catch (MalformedURLException e)
      {
         try
         {
            if (getUrl() == null)
               throw new RuntimeException("relative <jar-file> not allowed when standalone deployment unit is used");
            return new URL(getUrl(), jar);
         }
         catch (Exception e1)
         {
            throw new RuntimeException("could not find relative path: " + jar, e1);
         }
      }
   }

   URL extractDescriptorUrl(String resource)
   {
      try
      {
         VirtualFile vf = unit.getMetaDataFile(resource);
         if (vf == null) return null;
         return vf.toURL();
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public URL getPersistenceXml()
   {
      return extractDescriptorUrl("persistence.xml");
   }

   public URL getEjbJarXml()
   {
      return extractDescriptorUrl("ejb-jar.xml");
   }

   public URL getJbossXml()
   {
      return extractDescriptorUrl("jboss.xml");
   }

   public org.jboss.ejb3.vfs.spi.VirtualFile getMetaDataFile(String name)
   {
      return new VirtualFileWrapper(unit.getMetaDataFile(name));
   }
   
   public List<Class> getClasses()
   {
      return null;
   }

   public ClassLoader getClassLoader()
   {
      return classLoader;
   }

   public ClassLoader getResourceLoader()
   {
      return getClassLoader();
   }

   public String getShortName()
   {
      return unit.getFile("").getName();
   }

   public URL getUrl()
   {
      try
      {
         return unit.getFile("").toURL();
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public String getDefaultEntityManagerName()
   {
      String url = getUrl().toString();
      String name = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'));
      return name;
   }

   public Map getDefaultPersistenceProperties()
   {
      return defaultPersistenceProperties;
   }

   public void setDefaultPersistenceProperties(Map defaultPersistenceProperties)
   {
      this.defaultPersistenceProperties = defaultPersistenceProperties;
   }

   public Hashtable getJndiProperties()
   {
      return null;
   }

   public InterceptorInfoRepository getInterceptorInfoRepository()
   {
      return interceptorInfoRepository;
   }

   public List<org.jboss.ejb3.vfs.spi.VirtualFile> getResources(org.jboss.ejb3.vfs.spi.VirtualFileFilter filter)
   {
      List<VirtualFile> classPath = unit.getClassPath();
      if(classPath == null || classPath.isEmpty())
         return Collections.emptyList();

      VisitorAttributes va = new VisitorAttributes();
      va.setLeavesOnly(true);
      SuffixesExcludeFilter noJars = new SuffixesExcludeFilter(Arrays.asList(".zip", ".ear", ".jar", ".rar", ".war", ".sar",".har", ".aop")); // TODO:  Where should these come from?
      va.setRecurseFilter(noJars);
      FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(new VirtualFileFilterAdapter(filter), va);

      for(VirtualFile root : classPath)
      {
         try
         {
            // The class path can contain subdirectories of other entries
            // these must be ignored.
            if(isChildOf(classPath, root))
               continue;
            
            if( root.isLeaf() == false )
               root.visit(visitor);
         }
         catch (IOException e)
         {
            throw new RuntimeException(e);
         }
      }
      final List<VirtualFile> matches = visitor.getMatched(); 
      final List<org.jboss.ejb3.vfs.spi.VirtualFile> wrappedMatches = new ArrayList<org.jboss.ejb3.vfs.spi.VirtualFile>(matches.size());
      for(VirtualFile match : matches) 
      {
         wrappedMatches.add(new VirtualFileWrapper(match));
      }
      return wrappedMatches;
   }
   
   private static boolean isChildOf(VirtualFile other, final VirtualFile file)
      throws IOException
   {
      for(org.jboss.vfs.VirtualFile child : other.getChildren())
      {
         if(child.equals(file))
            return true;
         
         if(isChildOf(child, file))
            return true;
      }
      
      return false;
   }
   
   private static boolean isChildOf(List<VirtualFile> others, VirtualFile file)
      throws IOException
   {
      for(VirtualFile other : others)
      {
         if(file != other && isChildOf(other, file))
            return true;
      }
      return false;
   }
}
