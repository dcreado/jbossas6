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
package org.jboss.profileservice.management.views;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.logging.Logger;
import org.jboss.managed.api.DeploymentState;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedObject;
import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementOperation;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.jboss.managed.plugins.ManagedDeploymentImpl;
import org.jboss.metadata.spi.MetaData;
import org.jboss.profileservice.management.mbean.spi.MBeanManagedObjectFactory;
import org.jboss.profileservice.management.mbean.spi.ManagedMBeanDeploymentFactory;
import org.jboss.profileservice.management.mbean.spi.ManagedMBeanDeploymentFactory.MBeanComponent;
import org.jboss.profileservice.management.mbean.spi.ManagedMBeanDeploymentFactory.MBeanDeployment;
import org.jboss.profileservice.plugins.management.util.AbstractManagementProxyFactory;
import org.jboss.profileservice.plugins.management.util.ManagedDeploymentProcessor;
import org.jboss.profileservice.plugins.management.view.AbstractProfileView;
import org.jboss.profileservice.plugins.management.view.AbstractProfileViewWrapper;
import org.jboss.profileservice.plugins.spi.ProfileView;
import org.jboss.profileservice.spi.action.ModificationEvent;
import org.jboss.profileservice.spi.action.ProfileModificationType;

/**
 * The {@code ManagedMBeanDeploymentFactory} integration profile view.
 * 
 * @author Scott.Stark@jboss.org
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision$
 */
public class MBeanProfileView extends AbstractProfileViewWrapper
{

   /** The logger. */
   private static final Logger log = Logger.getLogger(MBeanProfileView.class);

   /** The profile view. */
   private AbstractProfileView view = new AbstractProfileView();
   
   /** */
   private HashMap<String, ManagedMBeanDeploymentFactory> mdfs =
      new HashMap<String, ManagedMBeanDeploymentFactory>();
   
   /** The MBean server. */
   private MBeanServer mbeanServer;
   
   /** The MBeanMOFactory. */
   private MBeanManagedObjectFactory mbeanMOFactory = new MBeanManagedObjectFactory();

   /** The MBean proxy factory. */
   private AbstractManagementProxyFactory mbeanProxyFactory;
   
   public MBeanServer getMbeanServer()
   {
      return mbeanServer;
   }

   public void setMbeanServer(MBeanServer mbeanServer)
   {
      this.mbeanServer = mbeanServer;
   }

   public AbstractManagementProxyFactory getMbeanProxyFactory()
   {
      return mbeanProxyFactory;
   }
   
   public void setMbeanProxyFactory(AbstractManagementProxyFactory mbeanProxyFactory)
   {
      this.mbeanProxyFactory = mbeanProxyFactory;
   }
   
   @Override
   public AbstractManagementProxyFactory getProxyFactory()
   {
      return getMbeanProxyFactory();
   }
   
   public void addManagedMBeanDeployments(ManagedMBeanDeploymentFactory factory)
   {
      log.trace("addManagedDeployment, "+factory);
      String name = factory.getFactoryName();
      this.mdfs.put(name, factory);
   }
   public void removeManagedMBeanDeployments(ManagedMBeanDeploymentFactory factory)
   {
      log.trace("removeManagedDeployment, "+factory);
      String name = factory.getFactoryName();
      this.mdfs.remove(name);
   }

   protected ProfileView getDelegate()
   {
      return view;
   }

   public boolean load()
   {
      // Load
      if(this.view.load())
      {
         // Process mbean components that need to be exposed as ManagedDeployment/ManagedComponent
         for(ManagedMBeanDeploymentFactory mdf : mdfs.values())
         {
            log.trace("Processing deployments for factory: "+mdf.getFactoryName());
            Collection<MBeanDeployment> deployments = mdf.getDeployments(mbeanServer);
            for(MBeanDeployment md : deployments)
            {
               log.trace("Saw MBeanDeployment: "+md);
               HashMap<String, ManagedObject> unitMOs = new HashMap<String, ManagedObject>();
               Collection<MBeanComponent> components = md.getComponents();
               if(components != null)
               {
                  for(MBeanComponent comp : components)
                  {
                     log.trace("Saw MBeanComponent: "+comp);
                     try
                     {
                        ManagedObject mo = createManagedObject(comp.getName(), mdf.getDefaultViewUse(), mdf.getPropertyMetaMappings());

                        String name = comp.getName().getCanonicalName();
                        ManagementObject moAnn = createMOAnnotation(name, comp.getType(), comp.getSubtype());

                        // Both the ManagementObject and ManagementComponent annotation need to be in the MO annotations
                        mo.getAnnotations().put(ManagementObject.class.getName(), moAnn);
                        ManagementComponent mcAnn = moAnn.componentType();
                        mo.getAnnotations().put(ManagementComponent.class.getName(), mcAnn);
                        unitMOs.put(name, mo);
                     }
                     catch(Exception e)
                     {
                        log.warn("Failed to create ManagedObject for: "+comp, e);
                     }
                  }
               }
               try
               {
                  ManagedDeploymentProcessor processor = new ManagedDeploymentProcessor(this.mbeanProxyFactory);
                  ManagedDeploymentImpl mdi = new ManagedDeploymentImpl(md.getName(), md.getName(), null, unitMOs);
                  mdi.setTypes(Collections.singleton("external-mbean"));
                  processor.processRootManagedDeployment(mdi, DeploymentState.STARTED, view);
               }
               catch(Exception e)
               {
                  log.warn("Failed to process ManagedDeployment for: " + md.getName(), e);
               }
            }
         }
         return true;
      }
      return false;
   }

   private ManagedObject createManagedObject(ObjectName mbean, String defaultViewUse, Map<String, String> propertyMetaMappings) throws Exception
   {
      MBeanInfo info = mbeanServer.getMBeanInfo(mbean);
      ClassLoader mbeanLoader = mbeanServer.getClassLoaderFor(mbean);
      MetaData metaData = null;
      ViewUse[] viewUse = defaultViewUse == null ? null : new ViewUse[] { ViewUse.valueOf(defaultViewUse) };
      ManagedObject mo = mbeanMOFactory.getManagedObject(mbean, info, mbeanLoader, metaData, viewUse, propertyMetaMappings);
      return mo;
   }

   public void removeComponent(ManagedComponent update, ManagedComponent original) throws Exception
   {
      // nothing
   }
   
   public void updateComponent(ManagedComponent update, ManagedComponent original) throws Exception
   {
      // nothing
   }

   public void notify(ModificationEvent event)
   {
      // We bind our reload lifecycle to all other profiles
      // since we don't want to reload too often.
      if(event.getModificationType() != ProfileModificationType.GET)
      {
         view.markAsModified();
      }
   }

   private ManagementObject createMOAnnotation(final String name, final String type, final String subtype)
   {
      return new ManagementObjectAnnotationImpl(name, type, subtype);
   }
   
   @SuppressWarnings("all")
   private static final class ManagementObjectAnnotationImpl implements ManagementObject, Serializable
   {
      private static final long serialVersionUID=5355799336353299850L;

      private final String name;
      private final String type;
      private final String subtype;

      @SuppressWarnings("all")
      private final class ManagementComponentAnnotationImpl implements ManagementComponent, Serializable
      {
         private static final long serialVersionUID=5355799336353299850L;

         public String subtype()
         {
            return subtype;
         }

         public String type()
         {
            return type;
         }

         public Class<? extends Annotation> annotationType()
         {
            return ManagementComponent.class;
         }
      }

      private ManagementObjectAnnotationImpl(String name, String type, String subtype)
      {
         this.name=name;
         this.type=type;
         this.subtype=subtype;
      }

      public String attachmentName()
      {
         return "";
      }

      public ManagementProperty[] classProperties()
      {
         return new ManagementProperty[0];
      }

      public ManagementComponent componentType()
      {
         return new ManagementComponentAnnotationImpl();
      }

      public String description()
      {
         return "";
      }

      public boolean isRuntime()
      {
         return true;
      }

      public String name()
      {
         return name;
      }

      public ManagementOperation[] operations()
      {
         return new ManagementOperation[0];
      }

      public ManagementProperties properties()
      {
         return ManagementProperties.ALL;
      }

      public Class<?> targetInterface()
      {
         return Object.class;
      }

      public String type()
      {
         return "";
      }

      public Class<? extends Annotation> annotationType()
      {
         return ManagementObject.class;
      }
   }

   
}

