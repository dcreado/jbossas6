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
package org.jboss.weld.integration.deployer.mc;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.beans.metadata.api.model.FromContext;
import org.jboss.beans.metadata.spi.AnnotationMetaData;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.ConstructorMetaData;
import org.jboss.beans.metadata.spi.FeatureMetaData;
import org.jboss.beans.metadata.spi.InstallMetaData;
import org.jboss.beans.metadata.spi.PropertyMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.deployer.helpers.BeanMetaDataDeployerPlugin;
import org.jboss.kernel.plugins.dependency.AbstractKernelControllerContext;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.kernel.weld.metadata.api.annotations.Weld;
import org.jboss.kernel.weld.plugins.annotations.WeldEnabledBeanAnnotationPluginInitializer;
import org.jboss.scanning.annotations.spi.AnnotationIndex;
import org.jboss.scanning.annotations.spi.Element;
import org.jboss.weld.integration.deployer.DeployersUtils;

/**
 * KernelContextCreator to create MC beans that are part of a Weld deployment. These beans must
 * be installed as WeldKernelControllerContexts. Rather than creating the WeldKernelControllerContext's
 * rignt away we create some intermediate beans which get injected with the Weld BootstrapBean once that is up and running
 * which can be used to create WeldKernelControllerContexts later.
 * 
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class WeldBeanMetaDataDeployerPlugin implements BeanMetaDataDeployerPlugin
{
   public static int DEFAULT_ORDER = 50;
   
   private final static String ATTACHMENT = "Annotated_$_" + WeldBeanMetaDataDeployerPlugin.class.getName();
   
   private final int order;
   
   static
   {
      WeldEnabledBeanAnnotationPluginInitializer.initialize();
   }

   /** Map of real bean names and intermediate bean names */
   private final Map<Object, Object> intermediateBeans = new ConcurrentHashMap<Object, Object>();
   
   public WeldBeanMetaDataDeployerPlugin()
   {
      this(-1);
   }
   
   public WeldBeanMetaDataDeployerPlugin(int order)
   {
      if (order > 0)
         this.order = order;
      else
         this.order = DEFAULT_ORDER;
   }
   
   public int getRelativeOrder()
   {
      return order;
   }

   /**
    * Create KernelControllerContexts for the intermediate beans (See: {@link IntermediateWeldBootstrapBean}) that will be 
    * used to create WeldKernelContexts once everything is ready.
    *  
    * @param controller The controller to which the beans will be deployed
    * @param unit The deployment unit we are deploying
    * @param beanMetaData The bean metadata we are deploying
    * @return the created controller context or null if the context is one of the weld ones which should be installed read   
    */
   public KernelControllerContext createContext(Controller controller, DeploymentUnit unit, BeanMetaData beanMetaData)
   {
      if (DeployersUtils.isBootstrapBeanPresent(unit) == false)
      {
         //Not a Weld deployment
         return null;
      }
      
      if (!isMcBeanWithWeldAnnotatedInjectionPoints(controller, unit, beanMetaData))
         return null;

      String beanName = unit.getTopLevel().getName() + "BootstrapBeanInstaller=" + beanMetaData.getName();
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(beanName, IntermediateWeldBootstrapBean.class.getName());
      builder.addPropertyMetaData("bootstrapBean", builder.createInject(DeployersUtils.getBootstrapBeanName(unit), null, ControllerState.CONFIGURED, ControllerState.CREATE));
      builder.addPropertyMetaData("beanMetaDataHolder", new BeanMetaDataPropertyHolder(beanMetaData));
      builder.addPropertyMetaData("context", builder.createFromContextInject(FromContext.CONTEXT));
      builder.addPropertyMetaData("deployment", builder.createInject(DeployersUtils.getDeploymentBeanName(unit.getTopLevel())));
      builder.addPropertyMetaData("creator", builder.createValue(this));
      KernelControllerContext ctx = new AbstractKernelControllerContext(null, builder.getBeanMetaData(), null);
      
      intermediateBeans.put(beanMetaData.getName(), beanName);
      
      return ctx;
   }
   
   private boolean isMcBeanWithWeldAnnotatedInjectionPoints(Controller controller, DeploymentUnit unit, BeanMetaData beanMetaData)
   {
      if (checkBeanMetaDataForWeldAnnotationOverridesOnInjectionPoints(controller, unit, beanMetaData))
         return true;

      return checkAnnotationRepositoryForWeldAnnotationOnInjectionPoints(unit, beanMetaData);
   }
   
   private boolean checkBeanMetaDataForWeldAnnotationOverridesOnInjectionPoints(Controller controller, DeploymentUnit unit, BeanMetaData beanMetaData)
   {
      ConstructorMetaData con = beanMetaData.getConstructor();
      if (con != null)
      {
         if (hasWeldAnnotation(unit, con))
            return true;
      }
      
      List<InstallMetaData> installs = beanMetaData.getInstalls();
      if (installs != null && installs.size() > 0)
      {
         for (InstallMetaData install : installs)
         {
            if (hasWeldAnnotation(unit, install))
               return true;
         }
      }
      
      Set<PropertyMetaData> properties = beanMetaData.getProperties();
      if (properties != null && properties.size() > 0)
      {
         for (PropertyMetaData property : properties)
         {
            if (hasWeldAnnotation(unit, property))
               return true;
         }
      }
      return false;
   }   
   
   private boolean checkAnnotationRepositoryForWeldAnnotationOnInjectionPoints(DeploymentUnit unit, BeanMetaData beanMetaData)
   {
      //This will currently not work if the deployment unit does not contain the classes referenced by the beanmetadata
      
      while (unit.isComponent())
         unit = unit.getParent();
      
      Set<String> classNames = (Set<String>)unit.getAttachment(ATTACHMENT);
      if (classNames == null)
      {
         AnnotationIndex index = unit.getAttachment(AnnotationIndex.class);
         if (index == null)
            return false;
         
         Set<Element<Weld, ?>> result = new HashSet<Element<Weld, ?>>(1);
         result.addAll(index.classHasConstructorAnnotatedWith(Weld.class));
         result.addAll(index.classHasFieldAnnotatedWith(Weld.class));
         result.addAll(index.classHasMethodAnnotatedWith(Weld.class));
         
         if (result.size() == 0)
         {
            classNames = Collections.emptySet();
         }
         else
         {
            classNames = new HashSet<String>();
            for (Element<Weld, ?> item : result)
               classNames.add(item.getOwnerClassName());
         }
         unit.addAttachment(ATTACHMENT, classNames);
      }
      try
      {
         return classNames.contains(beanMetaData.getBean());
      }
      catch (Exception e)
      {
         // AutoGenerated
         throw new RuntimeException(e);
      }
   }
   
   /* I don't want to delete this yet, although the check for the annotations now comes from the annotation repository
   private boolean checkClassInfoForWeldAnnotationOnInjectionPoints(Controller controller, DeploymentUnit unit, BeanMetaData beanMetaData)
   {
      if (controller instanceof KernelController == false)
         throw new IllegalArgumentException(controller + " is not a KernelController");
      ClassInfo classInfo; 
      try
      {
         KernelConfigurator config = ((KernelController)controller).getKernel().getConfigurator(); 
         classInfo = config.getClassInfo(beanMetaData.getBean(), Configurator.getClassLoader(beanMetaData));
      }
      catch (Throwable e)
      {
         throw new RuntimeException(e);
      }      
      if (hasWeldAnnotation(classInfo.getDeclaredConstructors()))
         return true;
      
      while (!classInfo.getName().startsWith("java.lang."))
      {
         if (hasWeldAnnotation(classInfo.getDeclaredFields()))
            return true;
         if (hasWeldAnnotation(classInfo.getDeclaredMethods()))
            return true;
         
         classInfo = classInfo.getSuperclass();
      }
      
      return false;
   }
   
   private boolean hasWeldAnnotation(AnnotatedInfo[] infos)
   {
      if (infos == null || infos.length == 0)
         return false;
      for (AnnotatedInfo info : infos)
      {
         if (info.isAnnotationPresent(Weld.class))
            return true;
      }
      return false;
   }*/
   
   private boolean hasWeldAnnotation(DeploymentUnit unit, FeatureMetaData fmd)
   {
      Set<AnnotationMetaData> anns = fmd.getAnnotations();
      if (anns != null && anns.size() > 0)
      {
         for (AnnotationMetaData ann : anns)
         {
            if (ann.getAnnotationInstance(unit.getClassLoader()).annotationType() == Weld.class)
               return true;
         }
      }
      return false;
   }
   
   public boolean uninstallContext(Controller controller, DeploymentUnit unit, BeanMetaData beanMetaData)
   {
      //Only remove the intermediate bean if there is one
      Object intermediateBean = intermediateBeans.remove(beanMetaData.getName());
      if (intermediateBean != null)
      {
         if (controller.getContext(intermediateBean, null) != null)
         {
            controller.uninstall(intermediateBean);
         }
      }
      //Check that the real bean was deployed by the intermediate bean before undeploying it
      if (controller.getContext(beanMetaData.getName(), null) != null)
         controller.uninstall(beanMetaData.getName());   
      
      //Return true to signal BeanMetaDataDeployer that we have undeployed everything 
      return true;
   }
   
   public void removeIntermediateBean(String name)
   {
      intermediateBeans.remove(name);
   }
}
