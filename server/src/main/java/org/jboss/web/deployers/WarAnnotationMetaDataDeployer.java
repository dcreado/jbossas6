/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.annotation.creator.AnnotationContext;
import org.jboss.metadata.annotation.creator.web.Web30MetaDataCreator;
import org.jboss.metadata.annotation.finder.AnnotationFinder;
import org.jboss.metadata.annotation.finder.DefaultAnnotationFinder;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.scanning.web.spi.ResourcesIndex;
import org.jboss.vfs.VirtualFile;

/**
 * A POST_CLASSLOADER deployer which generates metadata from
 * annotations
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision: 93949 $
 */
public class WarAnnotationMetaDataDeployer extends AbstractDeployer
{
   public static final String WEB_ANNOTATED_ATTACHMENT_NAME = "annotated."+WebMetaData.class.getName();

   public WarAnnotationMetaDataDeployer()
   {
      setStage(DeploymentStages.POST_CLASSLOADER);
      addInput(WebMetaData.class);
      addInput(ResourcesIndex.class);
      addOutput(WEB_ANNOTATED_ATTACHMENT_NAME);
   }

   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      if (unit instanceof VFSDeploymentUnit == false)
         return;
      
      VFSDeploymentUnit vfsDeploymentUnit = (VFSDeploymentUnit) unit;
      deploy(vfsDeploymentUnit);
   }

   /**
    * Process the 
    * 
    * @param unit the unit
    * @throws DeploymentException for any error
    */
   protected void deploy(VFSDeploymentUnit unit)
      throws DeploymentException
   {
      if (!unit.getSimpleName().endsWith(".war"))
      {
         return;
      }

      VirtualFile root = unit.getRoot();
      if(root.isFile())
         return;

      List<VirtualFile> classpath = unit.getClassPath();
      if(classpath == null || classpath.isEmpty())
         return;

      if (log.isTraceEnabled())
         log.trace("Deploying annotations for unit: " + unit + ", classpath: " + classpath);

      try
      {
         processMetaData(unit, classpath);
      }
      catch (Exception e)
      {
         throw DeploymentException.rethrowAsDeploymentException("Cannot process metadata", e);
      }
   }

   /**
    * Process metadata.
    *
    * @param unit the deployment unit
    * @param classpath the classpath
    * @throws Exception for any error
    */
   protected void processMetaData(VFSDeploymentUnit unit, List<VirtualFile> classpath) throws Exception
   {
      ResourcesIndex ri = unit.getAttachment(ResourcesIndex.class);
      if (ri == null)
      {
         return;
      }
      AnnotationFinder<AnnotatedElement> finder = new DefaultAnnotationFinder<AnnotatedElement>();
      Web30MetaDataCreator creator = new Web30MetaDataCreator(finder);
      // Merge processed annotations from all scopes 
      Set<Class<? extends Annotation>> annotations = new HashSet<Class<? extends Annotation>>();
      AnnotationContext annotationContext = creator.getAnnotationContext();
      if (annotationContext.getTypeAnnotations() != null)
         annotations.addAll(annotationContext.getTypeAnnotations());
      if (annotationContext.getMethodAnnotations() != null)
         annotations.addAll(annotationContext.getMethodAnnotations());
      if (annotationContext.getFieldAnnotations() != null)
         annotations.addAll(annotationContext.getFieldAnnotations());

      boolean metaData = false;
      for (VirtualFile path : classpath)
      {
         Set<Class<?>> annotatedClasses = new HashSet<Class<?>>();
         for (Class<? extends Annotation> annotation : annotations)
         {
            annotatedClasses.addAll(ri.getAnnotatedClasses(path, annotation));
         }
         WebMetaData annotationMetaData = creator.create(annotatedClasses);
         if (annotationMetaData != null)
         {
            unit.addAttachment(WEB_ANNOTATED_ATTACHMENT_NAME + ":" + path.getName(), annotationMetaData, WebMetaData.class);
            metaData = true;
         }
      }
      if (metaData)
         unit.addAttachment(WEB_ANNOTATED_ATTACHMENT_NAME, Boolean.TRUE);
   }

}
