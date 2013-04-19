/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.annotation.HandlesTypes;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.scanning.web.spi.ResourcesIndex;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * A deployer that processes ServletContainerInitializer.
 * 
 * @author Remy Maucherat
 * @version $Revision: 93820 $
 */
public class ServletContainerInitializerDeployer extends AbstractDeployer
{
   public static final String SCI_ATTACHMENT_NAME = "sci."+WebMetaData.class.getName();
   public static final String SCI_HANDLESTYPES_ATTACHMENT_NAME = "sci.handlestypes."+WebMetaData.class.getName();

   private List<URL> sciJars = null;

   public List<URL> getSciJars()
   {
      return sciJars;
   }

   public void setSciJars(List<URL> sciJars)
   {
      this.sciJars = sciJars;
   }

   /**
    * Create the SCI information.
    */
   public ServletContainerInitializerDeployer()
   {
      setStage(DeploymentStages.POST_CLASSLOADER);
      setInput(JBossWebMetaData.class);
      addInput(ResourcesIndex.class);
      addInput(MergedJBossWebMetaDataDeployer.WEB_ORDER_ATTACHMENT_NAME);
      addInput(MergedJBossWebMetaDataDeployer.WEB_SCIS_ATTACHMENT_NAME);
      addOutput(SCI_ATTACHMENT_NAME);
      addOutput(SCI_HANDLESTYPES_ATTACHMENT_NAME);
   }

   @SuppressWarnings("unchecked")
   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      if (!unit.getSimpleName().endsWith(".war"))
      {
         return;
      }
      Set<ServletContainerInitializer> scis = new HashSet<ServletContainerInitializer>();
      // Load the shared ServletContainerInitializer services
      if (sciJars == null)
      {
         ServiceLoader<ServletContainerInitializer> serviceLoader = 
            ServiceLoader.load(ServletContainerInitializer.class, this.getClass().getClassLoader());
         for (ServletContainerInitializer service : serviceLoader)
         {
            scis.add(service);
         }
      }
      else
      {
         for (URL jarURL : sciJars)
         {
            try
            {
               VirtualFile virtualFile = VFS.getChild(jarURL);
               VirtualFile sci = virtualFile.getChild("META-INF/services/javax.servlet.ServletContainerInitializer");
               if (sci.exists())
               {
                  ServletContainerInitializer service = loadSci(unit, sci, jarURL.getPath(), false);
                  if (service != null)
                  {
                     scis.add(service);
                  }
               }
            }
            catch (URISyntaxException e)
            {
               DeploymentException.rethrowAsDeploymentException("Deployment error processing SCI for JAR: " + jarURL, e);
            }
         }
      }
      // Find local ServletContainerInitializer services
      List<String> order = 
         (List<String>) unit.getAttachment(MergedJBossWebMetaDataDeployer.WEB_ORDER_ATTACHMENT_NAME);
      Map<String, VirtualFile> localScis = (Map<String, VirtualFile>) 
         unit.getAttachment(MergedJBossWebMetaDataDeployer.WEB_SCIS_ATTACHMENT_NAME);
      if (order != null && localScis != null)
      {
         for (String jar : order)
         {
            VirtualFile sci = localScis.get(jar);
            if (sci != null)
            {
               ServletContainerInitializer service = loadSci(unit, sci, jar, true);
               if (service != null)
               {
                  scis.add(service);
               }
            }
         }
      }
      unit.addAttachment(SCI_ATTACHMENT_NAME, scis);

      // Process HandlesTypes for ServletContainerInitializer
      Map<Class<?>, Set<ServletContainerInitializer>> typesMap = 
         new HashMap<Class<?>, Set<ServletContainerInitializer>>();
      Map<ServletContainerInitializer, Set<Class<?>>> handlesTypes = 
         new HashMap<ServletContainerInitializer, Set<Class<?>>>();
      for (ServletContainerInitializer service : scis)
      {
         if (service.getClass().isAnnotationPresent(HandlesTypes.class))
         {
            HandlesTypes handlesTypesAnnotation = service.getClass().getAnnotation(HandlesTypes.class);
            Class<?>[] typesArray = handlesTypesAnnotation.value();
            if (typesArray != null)
            {
               for (Class<?> type : typesArray)
               {
                  Set<ServletContainerInitializer> servicesSet = typesMap.get(type);
                  if (servicesSet == null)
                  {
                     servicesSet = new HashSet<ServletContainerInitializer>();
                     typesMap.put(type, servicesSet);
                  }
                  servicesSet.add(service);
                  handlesTypes.put(service, new HashSet<Class<?>>());
               }
            }
         }
      }
      
      ResourcesIndex ri = unit.getAttachment(ResourcesIndex.class);
      if (ri == null)
      {
         unit.addAttachment(SCI_HANDLESTYPES_ATTACHMENT_NAME, handlesTypes);
         return;
      }
      Class<?>[] typesArray = typesMap.keySet().toArray(new Class<?>[0]);
      // Find classes which extend, implement, or are annotated by HandlesTypes
      if (typesArray.length > 0 && unit instanceof VFSDeploymentUnit)
      {
         VFSDeploymentUnit vfsUnit = (VFSDeploymentUnit) unit;
         List<VirtualFile> classpath = vfsUnit.getClassPath();
         try
         {
            for (VirtualFile classpathItem : classpath)
            {
               for (Class<?> type : typesArray)
               {
                  if (type.isAnnotation())
                  {
                     Set<Class<?>> classes = ri.getAnnotatedClasses(classpathItem, (Class<Annotation>) type);
                     Set<ServletContainerInitializer> sciSet = typesMap.get(type);
                     for (ServletContainerInitializer sci : sciSet)
                     {
                        handlesTypes.get(sci).addAll(classes);
                     }
                  }
                  else
                  {
                     Set classes = ri.getInheritedClasses(classpathItem, type);
                     Set<ServletContainerInitializer> sciSet = typesMap.get(type);
                     for (ServletContainerInitializer sci : sciSet)
                     {
                        Set<Class<?>> sciClasses = handlesTypes.get(sci);
                        for (Object clazz : classes)
                        {
                           sciClasses.add((Class<?>) clazz);
                        }
                     }
                  }
               }
            }
         }
         catch (Exception e)
         {
            DeploymentException.rethrowAsDeploymentException("Deployment error scanning HandlesTypes", e);
         }
      }
      unit.addAttachment(SCI_HANDLESTYPES_ATTACHMENT_NAME, handlesTypes);
   }
   
   
   private ServletContainerInitializer loadSci(DeploymentUnit unit, VirtualFile sci, String jar, boolean error)
      throws DeploymentException
   {
      ServletContainerInitializer service = null;
      InputStream is = null;
      try
      {
         // Get the ServletContainerInitializer class name
         is = sci.openStream();
         BufferedReader reader = new BufferedReader(new InputStreamReader(is));
         String servletContainerInitializerClassName = reader.readLine();
         int pos = servletContainerInitializerClassName.indexOf('#');
         if (pos > 0) {
            servletContainerInitializerClassName = servletContainerInitializerClassName.substring(0, pos);
         }
         servletContainerInitializerClassName = servletContainerInitializerClassName.trim();
         // Instantiate the ServletContainerInitializer
         service = (ServletContainerInitializer) unit.getClassLoader()
            .loadClass(servletContainerInitializerClassName).newInstance();
      }
      catch (Exception e)
      {
         if (error)
         {
            DeploymentException.rethrowAsDeploymentException("Deployment error processing SCI for JAR: " + jar, e);
         }
         else
         {
            log.info("Skipped SCI for JAR: " + jar, e);
         }
      }
      finally
      {
         try
         {
            if (is != null)
               is.close();
         }
         catch (IOException e)
         {
            ;
         }
      }
      return service;
   }

}
