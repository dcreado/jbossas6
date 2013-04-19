/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.weld.integration.deployer.metadata;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.ExcludeDefaultInterceptors;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.annotation.creator.EjbProcessorUtils;
import org.jboss.metadata.annotation.creator.ejb.InterceptorMetaDataCreator;
import org.jboss.metadata.annotation.finder.DefaultAnnotationFinder;
import org.jboss.metadata.ejb.jboss.JBossAssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.spec.EjbJar30MetaData;
import org.jboss.metadata.ejb.spec.EjbJar3xMetaData;
import org.jboss.metadata.ejb.spec.InterceptorBindingMetaData;
import org.jboss.metadata.ejb.spec.InterceptorBindingsMetaData;
import org.jboss.metadata.ejb.spec.InterceptorClassesMetaData;
import org.jboss.metadata.ejb.spec.InterceptorMetaData;
import org.jboss.metadata.ejb.spec.InterceptorOrderMetaData;
import org.jboss.metadata.ejb.spec.InterceptorsMetaData;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.metadata.ejb.spec.NamedMethodMetaData;
import org.jboss.scanning.annotations.spi.AnnotationIndex;
import org.jboss.scanning.annotations.spi.Element;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.ejb.SessionBeanInterceptor;
import org.jboss.weld.integration.ejb.interceptor.Jsr299BindingsInterceptor;

/**
 * Adds wb custom interceptor to ejb deployments.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Marius Bogoevici
 */
@SuppressWarnings("deprecation")
public class WeldEjbInterceptorMetadataDeployer extends WeldAwareMetadataDeployer<JBossMetaData>
{
   private static final Class<SessionBeanInterceptor> INJECTION_INTERCEPTOR_CLASS = SessionBeanInterceptor.class;
   private static final Class<WeldLifecycleInterceptor> CONTEXT_INTERCEPTOR_CLASS = WeldLifecycleInterceptor.class;
   private static final Class<Jsr299BindingsInterceptor> BINDINGS_INTERCEPTOR_CLASS = Jsr299BindingsInterceptor.class;

   public static final String INJECTION_INTERCEPTOR_CLASS_NAME = INJECTION_INTERCEPTOR_CLASS.getName();
   public static final String CONTEXT_INTERCEPTOR_CLASS_NAME = CONTEXT_INTERCEPTOR_CLASS.getName();
   public static final String BINDINGS_INTERCEPTOR_CLASS_NAME = BINDINGS_INTERCEPTOR_CLASS.getName();

   private InterceptorMetaData injectionIMD;
   private InterceptorMetaData bindingsIMD;
   private InterceptorMetaData contextIMD;

   private InterceptorMetaDataCreator interceptorMetaDataCreator;

   public WeldEjbInterceptorMetadataDeployer()
   {
      super(JBossMetaData.class, true);

      interceptorMetaDataCreator = new InterceptorMetaDataCreator(new DefaultAnnotationFinder<AnnotatedElement>());


      addInput("merged." + JBossMetaData.class.getName());
      setStage(DeploymentStages.POST_CLASSLOADER);

      //create and process metadata for JSR299 interceptors
      InterceptorsMetaData interceptorsMetaData = interceptorMetaDataCreator.create(Arrays.<Class<?>>asList(INJECTION_INTERCEPTOR_CLASS, CONTEXT_INTERCEPTOR_CLASS, BINDINGS_INTERCEPTOR_CLASS));

      // create interceptor metadata instance for session beans
      injectionIMD = interceptorsMetaData.get(INJECTION_INTERCEPTOR_CLASS_NAME);

      contextIMD = interceptorsMetaData.get(CONTEXT_INTERCEPTOR_CLASS_NAME);

      // create interceptor metadata instance for JSR-299 specific bindings
      bindingsIMD = interceptorsMetaData.get(BINDINGS_INTERCEPTOR_CLASS_NAME);

   }

   @Override
   protected void internalDeploy(VFSDeploymentUnit unit, JBossMetaData jbmd, Collection<VirtualFile> wbXml) throws DeploymentException
   {
      if (jbmd.getInterceptors() == null)
      {
         InterceptorsMetaData imd = new InterceptorsMetaData();
         EjbJar3xMetaData ejmd = new EjbJar30MetaData();
         ejmd.setInterceptors(imd);
         jbmd.merge(null, ejmd);
      }
      InterceptorsMetaData interceptors = jbmd.getInterceptors();
      interceptors.add(injectionIMD); // clone?
      interceptors.add(bindingsIMD);
      interceptors.add(contextIMD);

      JBossAssemblyDescriptorMetaData assemblyDescriptor = jbmd.getAssemblyDescriptor();
      if (assemblyDescriptor == null)
      {
         assemblyDescriptor = new JBossAssemblyDescriptorMetaData();
         jbmd.setAssemblyDescriptor(assemblyDescriptor);
      }
      InterceptorBindingsMetaData interceptorBindings = assemblyDescriptor.getInterceptorBindings();
      if (interceptorBindings == null)
      {
         interceptorBindings = new InterceptorBindingsMetaData();
         assemblyDescriptor.setInterceptorBindings(interceptorBindings);
      }

      if (jbmd.isEJB3x() && jbmd.getEnterpriseBeans() != null)
      {
         processEjbInterceptorMetadata(jbmd.getEnterpriseBeans(), interceptorBindings, unit);
      }

      // Check to see there is a defined order; if we aren't first, warn
      for (InterceptorBindingMetaData interceptorBinding : interceptorBindings)
      {
         if (interceptorBinding.getInterceptorOrder() != null && !interceptorBinding.getInterceptorOrder().isEmpty())
         {
            if (!INJECTION_INTERCEPTOR_CLASS_NAME.equals(interceptorBinding.getInterceptorOrder().iterator().next()))
            {
               log.warn("The Weld SessionBeanInterceptor is not the inner most EJB interceptor in this deployment. JSR299 injection may not work correctly. Specify " + INJECTION_INTERCEPTOR_CLASS_NAME + " as the first interceptor in the interceptor ordering for " + interceptorBinding.getEjbName());
            }
         }
      }
   }

   private InterceptorBindingMetaData createBinding(String className, String ejbName, NamedMethodMetaData method, boolean excludeClassInterceptors)
   {
      InterceptorClassesMetaData interceptorClasses = new InterceptorClassesMetaData();
      interceptorClasses.add(className);
      return createBinding(interceptorClasses, null, ejbName, method);
   }

   private InterceptorBindingMetaData createBinding(InterceptorClassesMetaData classesMetaData, InterceptorOrderMetaData orderMetaData, String ejbName, NamedMethodMetaData method)
   {
      InterceptorBindingMetaData ibmd = new InterceptorBindingMetaData();
      ibmd.setInterceptorClasses(classesMetaData);
      ibmd.setEjbName(ejbName);
      if (orderMetaData != null)
      {
         ibmd.setInterceptorOrder(orderMetaData);
      }
      if (method != null)
      {
         ibmd.setMethod(method);
      }
      return ibmd;
   }

   /**
    * Process EJB interceptors metadata and make sure that the JSR 299 bindings interceptor is added after the EJB3 ones
    * There are 3 possibilities
    * - there are no EJB3 interceptor definitions or all the existing ones are defaults: set a default binding
    * - there are EJB3 interceptors bound on classes, case in which we need to:
    *     - bind the JSR299 interceptor after the class interceptors
    *     - verify if the existing EJB3 interceptors exclude defaults; if the latter is the case then we need to add
    *       CDI bindings (context and injection) at the class level, otherwise EJB3 handling won't happen correctly;
    * - there are EJB3 interceptors bound on methods, case in which we need to:
    *     - verify if the existing EJB3 interceptors exclude defaults; if the latter is the case then we need to add
    *       CDI bindings (context and injection) at the class level, otherwise EJB3 handling won't happen correctly;
    *     - in order to add the JSR299interceptor *last*, we will exclude the class level interceptors for this particular
    *       method and re-add the class level interceptors before the mapped method-level interceptors, leaving the
    *       JSR299 interceptor last
    * @param jBossEnterpriseBeansMetaData
    * @param interceptorBindings
    * @param deploymentUnit
    */
   private void processEjbInterceptorMetadata(JBossEnterpriseBeansMetaData jBossEnterpriseBeansMetaData, InterceptorBindingsMetaData interceptorBindings, DeploymentUnit deploymentUnit)
   {
      // we need to figure out whether there are class-level or method-level bindings
      List<InterceptorBindingMetaData> classInterceptorBindings = new ArrayList<InterceptorBindingMetaData>();
      List<InterceptorBindingMetaData> methodInterceptorBindings = new ArrayList<InterceptorBindingMetaData>();


      for (InterceptorBindingMetaData interceptorBinding : interceptorBindings)
      {
         if (!"*".equals(interceptorBinding.getEjbName())
               && null != interceptorBinding.getEjbName()
               && !interceptorBinding.getEjbName().trim().equals(""))
         {
            //record class-level bindings may need them later
            if (interceptorBinding.getMethod() == null)
            {
               classInterceptorBindings.add(interceptorBinding);
            }
         }

         // record method level interceptor bindings
         if (null != interceptorBinding.getMethod())
         {
            methodInterceptorBindings.add(interceptorBinding);
         }

      }
      Set<NamedMethodMetaData> processedMethodMetadatas = new HashSet<NamedMethodMetaData>();

      if (!excludeClassLevelInterceptorsExist(deploymentUnit)
            && methodInterceptorBindings.isEmpty()
            && classInterceptorBindings.isEmpty()
            && getMethodsWithExcludedClasses(deploymentUnit).isEmpty()
            && getMethodsWithExcludedDefaults(deploymentUnit).isEmpty())
      {
         // there are no class or method level EJB3 interceptor bindings, nor are there any exclusions:
         // we can simply set up defaults
         interceptorBindings.add(0, createBinding(INJECTION_INTERCEPTOR_CLASS_NAME, "*", null, false));
         interceptorBindings.add(0, createBinding(CONTEXT_INTERCEPTOR_CLASS_NAME, "*", null, false));
         interceptorBindings.add(createBinding(BINDINGS_INTERCEPTOR_CLASS_NAME, "*", null, false));
      }
      else
      {

         for (JBossEnterpriseBeanMetaData enterpriseBeanMetaData : jBossEnterpriseBeansMetaData)
         {
            // add a bindings for EJB interceptors - they need to be added after class level interceptors, so we can't add a default
            String ejbName = enterpriseBeanMetaData.getEjbName();
            interceptorBindings.add(0, createBinding(INJECTION_INTERCEPTOR_CLASS_NAME, ejbName, null, false));
            interceptorBindings.add(0, createBinding(CONTEXT_INTERCEPTOR_CLASS_NAME, ejbName, null, false));
            interceptorBindings.add(createBinding(BINDINGS_INTERCEPTOR_CLASS_NAME, ejbName, null, false));
         }


         // if there are method level bindings, then we need to execute the JSR299 interceptor last, but it has been
         // already added at the class level (we must do so, otherwise lifecycle interception won't happen. As well, we
         // don't have access to all the methods so we can't add bindings directly. But if we keep things this way,
         // then the JSR299 interceptor will execute before EJB3 method-level interceptors, which is not right, so will
         // exclude all the class-level bindings and add the original bindings on the method before the current bindings
         if (!methodInterceptorBindings.isEmpty())
         {
            for (InterceptorBindingMetaData methodInterceptorBinding : methodInterceptorBindings)
            {

               // re-add class level interceptors in their original order
               if (!methodInterceptorBinding.isExcludeClassInterceptors())
               {
                  // create a local list to preserve the original ordering of the class-level interceptors
                  List<InterceptorBindingMetaData> replacementInterceptorBindings = new ArrayList<InterceptorBindingMetaData>();
                  for (InterceptorBindingMetaData classInterceptorBinding : classInterceptorBindings)
                  {
                     if (methodInterceptorBinding.getEjbName().equals(classInterceptorBinding.getEjbName()))
                     {
                        replacementInterceptorBindings.add(0, createBinding(classInterceptorBinding.getInterceptorClasses(), classInterceptorBinding.getInterceptorOrder(), methodInterceptorBinding.getEjbName(), methodInterceptorBinding.getMethod()));
                     }
                  }
                  // add new bindings at the beginning of the list so that they will be mapped first on the method
                  interceptorBindings.addAll(0, replacementInterceptorBindings);
               }
               interceptorBindings.add(createBinding(BINDINGS_INTERCEPTOR_CLASS_NAME, methodInterceptorBinding.getEjbName(), methodInterceptorBinding.getMethod(), methodInterceptorBinding.isExcludeClassInterceptors()));
               // exclude class level interceptors so that our new bindings won't be executed twice
               processedMethodMetadatas.add(methodInterceptorBinding.getMethod());
               methodInterceptorBinding.setExcludeClassInterceptors(true);
            }
         }

         for (Element<ExcludeClassInterceptors, Method> element : getMethodsWithExcludedClasses(deploymentUnit))
         {
            //re-add CDI interceptors if they would be otherwise excluded
            for (JBossEnterpriseBeanMetaData enterpriseBeanMetaData : jBossEnterpriseBeansMetaData)
            {

               if (enterpriseBeanMetaData.getEjbClass().equals(element.getOwnerClassName()))
               {
                  String ejbName = enterpriseBeanMetaData.getEjbName();
                  NamedMethodMetaData namedMethod = new NamedMethodMetaData();
                  namedMethod.setMethodName(element.getAnnotatedElement().getName());
                  MethodParametersMetaData methodParams = EjbProcessorUtils.getMethodParameters(element.getAnnotatedElement());
                  namedMethod.setMethodParams(methodParams);
                  if (!processedMethodMetadatas.contains(namedMethod))
                  {
                     interceptorBindings.add(0, createBinding(INJECTION_INTERCEPTOR_CLASS_NAME, ejbName, namedMethod, false));
                     interceptorBindings.add(0, createBinding(CONTEXT_INTERCEPTOR_CLASS_NAME, ejbName, namedMethod, false));
                     interceptorBindings.add(createBinding(BINDINGS_INTERCEPTOR_CLASS_NAME, ejbName, namedMethod, false));
                     processedMethodMetadatas.add(namedMethod);
                  }
               }
            }
         }

      }
   }

   private boolean excludeClassLevelInterceptorsExist(DeploymentUnit unit)
   {
      AnnotationIndex annotationRepository = unit.getAttachment(AnnotationIndex.class);
      if (annotationRepository != null)
      {
         return !annotationRepository.classIsAnnotatedWith(ExcludeDefaultInterceptors.class).isEmpty();
      }
      else
      {
         // if annotations are not scanned we won't bother checking each EJB class
         // let interceptors just go at the class level
         return true;
      }
   }

   private Set<Element<ExcludeDefaultInterceptors,Method>> getMethodsWithExcludedDefaults(DeploymentUnit unit)
   {
      AnnotationIndex annotationRepository = unit.getAttachment(AnnotationIndex.class);

      if (annotationRepository != null)
      {
         return annotationRepository.classHasMethodAnnotatedWith(ExcludeDefaultInterceptors.class);

      }
      else
      {
         // no scanning info, ignore those methods
         return Collections.emptySet();
      }
   }

   private Set<Element<ExcludeClassInterceptors,Method>> getMethodsWithExcludedClasses(DeploymentUnit unit)
   {
      AnnotationIndex annotationRepository = unit.getAttachment(AnnotationIndex.class);

      if (annotationRepository != null)
      {
         return annotationRepository.classHasMethodAnnotatedWith(ExcludeClassInterceptors.class);
      }
      else
      {
         // no scanning info, ignore those methods
         return Collections.emptySet();
      }
   }
}