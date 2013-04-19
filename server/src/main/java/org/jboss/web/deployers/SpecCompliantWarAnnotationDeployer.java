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
package org.jboss.web.deployers;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;

import javax.annotation.ManagedBean;
import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.jsp.tagext.SimpleTag;
import javax.servlet.jsp.tagext.Tag;

import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.deployment.JSFDeployment;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.managed.bean.metadata.ManagedBeanDeploymentMetaData;
import org.jboss.managed.bean.metadata.ManagedBeanMetaData;
import org.jboss.metadata.annotation.creator.AnnotationContext;
import org.jboss.metadata.annotation.creator.web.Web30MetaDataCreator;
import org.jboss.metadata.annotation.finder.AnnotationFinder;
import org.jboss.metadata.annotation.finder.DefaultAnnotationFinder;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.scanning.web.spi.ResourcesIndex;
import org.jboss.vfs.VirtualFile;

/**
 * Servlet spec (and JSP spec) lists only specific set of class types which 
 * are eligible for Java EE resource injections. Unlike the {@link WarAnnotationMetaDataDeployer},
 * which picks up all classes in the classpath for annotation processing and subsequent metadata creation,
 * this {@link SpecCompliantWarAnnotationDeployer} attempts to scan only the following types
 * of classes for Java EE resource injections.
 * <p>
 * As determined by the Servlet spec and JSP spec, the following class types will be scanned for resource injection
 * annotations:
 * <ul>
 *  <li>javax.servlet.Servlet</li>
 *  <li>javax.servlet.Filter</li>
 *  <li>javax.servlet.ServletContextListener</li>
 *  <li>javax.servlet.ServletContextAttributeListener</li>
 *  <li>javax.servlet.ServletRequestListener</li>
 *  <li>javax.servlet.ServletRequestAttributeListener</li>
 *  <li>javax.servlet.http.HttpSessionListener</li>
 *  <li>javax.servlet.http.HttpSessionAttributeListener</li>
 *  <li>javax.servlet.AsyncListener</li>
 *  <li>javax.servlet.jsp.tagext.Tag</li>
 *  <li>javax.servlet.jsp.tagext.SimpleTag</li>
 * </ul> 
 * TODO: This is currently a work-in-progress deployer. The blocker currently, is that the JSF managed bean
 * integration implementation assumes that the WarAnnotationDeployer will scan for annotations on a JSF managed bean.
 * However, for this {@link SpecCompliantWarAnnotationDeployer} to work correctly, we need to know if a specific class
 * has been marked as a JSF managed bean (via faces-config.xml). Currently there's no way to get that information
 * during deployment phase of a unit. Needs more thought on how to get this working cleanly. 
 * @see https://issues.jboss.org/browse/JBAS-8775. Once this deployer is fully functional, it will replace the
 * {@link WarAnnotationMetaDataDeployer} 
 * 
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SpecCompliantWarAnnotationDeployer extends WarAnnotationMetaDataDeployer
{

   public SpecCompliantWarAnnotationDeployer()
   {
       // run after the JSFDeployment attaching deployer(s) have run, so that we can get hold of the jsf managed beans
       this.addInput(JSFDeployment.class);
       // Java EE Managed bean annotation deployer should run before this, so that we can get hold of Java EE managed beans
       // and associated interceptors info
       this.addInput(ManagedBeanDeploymentMetaData.class);
   }
    
   @Override
   protected void processMetaData(VFSDeploymentUnit unit, List<VirtualFile> classpath) throws Exception
   {
      ResourcesIndex resourceIndex = unit.getAttachment(ResourcesIndex.class);
      if (resourceIndex == null)
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

      Collection<Class<?>> specEligibleResourceInjectionClasses = this.getResourceInjectionEligibleWebAppClasses(resourceIndex, classpath);
      boolean metaData = false;
      final JSFDeployment jsfDeployment = unit.getAttachment(JSFDeployment.class);
      final ManagedBeanDeploymentMetaData managedBeanDeployment = unit.getAttachment(ManagedBeanDeploymentMetaData.class);
      for (VirtualFile path : classpath)
      {
         Collection<Class<?>> eligibleAnnotatedClasses = new HashSet<Class<?>>();
         for (Class<? extends Annotation> annotation : annotations)
         {
            Collection<Class<?>> annotatedClasses = resourceIndex.getAnnotatedClasses(path, annotation);
            // include the jsf and Java EE6 managed beans as eligible for resource injection
            specEligibleResourceInjectionClasses.addAll(this.getManagedBeansRelatedClasses(jsfDeployment, managedBeanDeployment, annotatedClasses));
            // filter out any extra non-spec classes which shouldn't be picked up for resource injection processing
            eligibleAnnotatedClasses.addAll(this.retainResourceInjectionEligibleWebAppClasses(specEligibleResourceInjectionClasses, annotatedClasses));
         }
         WebMetaData annotationMetaData = creator.create(eligibleAnnotatedClasses);
         if (annotationMetaData != null)
         {
            unit.addAttachment(WEB_ANNOTATED_ATTACHMENT_NAME + ":" + path.getName(), annotationMetaData,
                  WebMetaData.class);
            metaData = true;
         }
      }
      if (metaData)
      {
         unit.addAttachment(WEB_ANNOTATED_ATTACHMENT_NAME, Boolean.TRUE);
      }
   }

   protected Collection<Class<?>> getResourceInjectionEligibleWebAppClasses(ResourcesIndex resourceIndex, List<VirtualFile> classpath)
   {
      Set<Class<?>> eligibleClasses = new HashSet<Class<?>>();
      
      for (VirtualFile file : classpath)
      {
         // javax.servlet.Servlet type classes
         Collection<Class<? extends Servlet>> servlets = resourceIndex.getInheritedClasses(file, Servlet.class);
         if (servlets != null)
         {
            eligibleClasses.addAll(servlets);
         }
         // javax.servlet.Filter classes
         Collection<Class<? extends Filter>> filters = resourceIndex.getInheritedClasses(file, Filter.class);
         if (filters != null)
         {
            eligibleClasses.addAll(filters);
         }
         // javax.servlet.ServletContextListener classes
         Collection<Class<? extends ServletContextListener>> servletContextListeners = resourceIndex.getInheritedClasses(file, ServletContextListener.class);
         if (servletContextListeners != null)
         {
            eligibleClasses.addAll(servletContextListeners);
         }
         // javax.servlet.ServletContextAttributeListener classes
         Collection<Class<? extends ServletContextAttributeListener>> servletCtxAttributeListeners = resourceIndex.getInheritedClasses(file, ServletContextAttributeListener.class);
         if (servletCtxAttributeListeners != null)
         {
            eligibleClasses.addAll(servletCtxAttributeListeners);
         }
         // javax.servlet.ServletRequestListener classes
         Collection<Class<? extends ServletRequestListener>> servletRequestListeners = resourceIndex.getInheritedClasses(file, ServletRequestListener.class);
         if (servletRequestListeners != null)
         {
            eligibleClasses.addAll(servletRequestListeners);
         }
         // javax.servlet.ServletRequestAttributeListener classes
         Collection<Class<? extends ServletRequestAttributeListener>> servletRequestAttrListeners = resourceIndex.getInheritedClasses(file, ServletRequestAttributeListener.class);
         if (servletRequestAttrListeners != null)
         {
            eligibleClasses.addAll(servletRequestAttrListeners);
         }
         // javax.servlet.http.HttpSessionListener classes
         Collection<Class<? extends HttpSessionListener>> httpSessionListeners = resourceIndex.getInheritedClasses(file, HttpSessionListener.class);
         if (httpSessionListeners != null)
         {
            eligibleClasses.addAll(httpSessionListeners);
         }
         // javax.servlet.http.HttpSessionAttributeListener classes
         Collection<Class<? extends HttpSessionAttributeListener>> httpSessionAttrListeners = resourceIndex.getInheritedClasses(file, HttpSessionAttributeListener.class);
         if (httpSessionAttrListeners != null)
         {
            eligibleClasses.addAll(httpSessionAttrListeners);
         }
         // javax.servlet.AsyncListener classes
         Collection<Class<? extends AsyncListener>> asyncListeners = resourceIndex.getInheritedClasses(file, AsyncListener.class);
         if (asyncListeners != null)
         {
            eligibleClasses.addAll(asyncListeners);
         }
         
         // JSP resource injection support
         // javax.servlet.jsp.tagext.Tag classes
         Collection<Class<? extends Tag>> jspTags = resourceIndex.getInheritedClasses(file, Tag.class);
         if (jspTags != null)
         {
            eligibleClasses.addAll(jspTags);
         }
         
         // javax.servlet.jsp.tagext.SimpleTag classes
         Collection<Class<? extends SimpleTag>> simpleTags = resourceIndex.getInheritedClasses(file, SimpleTag.class);
         if (simpleTags != null)
         {
            eligibleClasses.addAll(simpleTags);
         }

      }
      
      return eligibleClasses;
   }
   
   private Collection<Class<?>> retainResourceInjectionEligibleWebAppClasses(Collection<Class<?>> specEligibleClasses, Collection<Class<?>> annotatedClasses)
   {
      Set<Class<?>> eligibleClasses = new HashSet<Class<?>>();
      
      for (Class<?> annotatedClass : annotatedClasses)
      {
         for (Class<?> specEligibleClass : specEligibleClasses)
         {
            if (annotatedClass.isAssignableFrom(specEligibleClass))
            {
               eligibleClasses.add(annotatedClass);
               break;
            }
         }
      }
      return eligibleClasses;
   }

    private Collection<Class<?>> getManagedBeansRelatedClasses(final JSFDeployment jsfDeployment, final ManagedBeanDeploymentMetaData managedBeanDeployment, Collection<Class<?>> classes)
    {
        if (classes == null || classes.isEmpty())
        {
            return new HashSet<Class<?>>();
        }
        Collection<String> jsfManagedBeansClassNames = Collections.emptySet();
        if (jsfDeployment != null)
        {
            jsfManagedBeansClassNames = jsfDeployment.getManagedBeans();
        }
        Collection<Class<?>> managedBeans = new HashSet<Class<?>>();
        for (Class<?> klass : classes)
        {
            if (this.isJavaEE6ManagedBean(klass) || this.isInterceptorToJavaEE6ManagedBean(klass, managedBeanDeployment))
            {
                managedBeans.add(klass);
            }
            else if (this.isJSFManagedBean(jsfManagedBeansClassNames, klass))
            {
                managedBeans.add(klass);
            }
        }
        return managedBeans;
    }

    private boolean isJSFManagedBean(final Collection<String> jsfManagedBeanClassNames, final Class<?> klass)
    {
        if (jsfManagedBeanClassNames == null || jsfManagedBeanClassNames.isEmpty())
        {
            return false;
        }
        return jsfManagedBeanClassNames.contains(klass.getName());
    }

    private boolean isJavaEE6ManagedBean(final Class<?> klass)
    {
        return klass.isAnnotationPresent(ManagedBean.class);
    }

    private boolean isInterceptorToJavaEE6ManagedBean(final Class<?> klass, final ManagedBeanDeploymentMetaData managedBeanDeploymentMetaData) {
        if (managedBeanDeploymentMetaData == null) {
            return false;
        }
        Collection<ManagedBeanMetaData> managedBeans = managedBeanDeploymentMetaData.getManagedBeans();
        if (managedBeans == null || managedBeans.isEmpty()) {
            return false;
        }
        for (ManagedBeanMetaData managedBean : managedBeans) {
            Collection<InterceptorMetadata<?>> interceptors = managedBean.getAllInterceptors();
            if (interceptors == null || interceptors.isEmpty()) {
                continue;
            }
            for (InterceptorMetadata<?> interceptor : interceptors) {
                final String interceptorClassName = interceptor.getInterceptorClass().getClassName();
                if (klass.getName().equals(interceptorClassName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
