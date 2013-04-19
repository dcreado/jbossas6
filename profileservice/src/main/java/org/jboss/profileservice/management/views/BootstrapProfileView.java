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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.BeanMetaDataFactory;
import org.jboss.bootstrap.api.server.Server;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.kernel.spi.deployment.KernelDeployment;
import org.jboss.kernel.spi.metadata.KernelMetaDataRepository;
import org.jboss.logging.Logger;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.managed.api.ManagedObject;
import org.jboss.managed.api.ManagedOperation;
import org.jboss.managed.api.MutableManagedObject;
import org.jboss.managed.api.factory.ManagedObjectFactory;
import org.jboss.managed.plugins.ManagedComponentImpl;
import org.jboss.managed.plugins.ManagedDeploymentImpl;
import org.jboss.managed.plugins.ManagedOperationImpl;
import org.jboss.managed.plugins.factory.ManagedObjectFactoryBuilder;
import org.jboss.metadata.spi.MetaData;
import org.jboss.profileservice.plugins.management.util.ManagedDeploymentProcessor;
import org.jboss.profileservice.plugins.management.view.AbstractProfileView;
import org.jboss.profileservice.plugins.management.view.AbstractProfileViewWrapper;
import org.jboss.profileservice.plugins.spi.ProfileView;


/**
 * The bootstrap deployment view.
 * 
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision$
 */
public class BootstrapProfileView extends AbstractProfileViewWrapper
{

   /** The logger. */
   private static final Logger log = Logger.getLogger(BootstrapProfileView.class);
   
   /** The managed object factory. */
   private final static ManagedObjectFactory managedObjectFactory = ManagedObjectFactoryBuilder.create();
   
   /** The profile view. */
   private final AbstractProfileView view = new AbstractProfileView();

   /** The kernel. */
   private final Kernel kernel;
   
   /** The bootstrap server. */
   private Server server;
   
   /** The deployments. */
   private final Map<String, KernelDeployment> deployments;
   
   public BootstrapProfileView(Kernel kernel, Map<String, KernelDeployment> deployments)
   {
      this.kernel = kernel;
      this.deployments = deployments;
   }
   
   protected ProfileView getDelegate()
   {
      return view;
   }

   public Server getServer()
   {
      return server;
   }
   
   public void setServer(Server server)
   {
      this.server = server;
   }
   
   public boolean load()
   {
      if(deployments == null)
      {
         return false;
      }
      if(view.load())
      {
         processServer();
         for(Entry<String, KernelDeployment> entry : deployments.entrySet())
         {
            String deploymentName = entry.getKey();
            try
            {
               processDeployment(deploymentName, entry.getValue());
            }
            catch(Exception e)
            {
               log.error("failed to process managed deployment " + deploymentName);
            }
         }
         return true;
      }
      return false;
   }

   /**
    * TODO Process the Bootstrap server.
    */
   protected void processServer()
   {
      ComponentType type = new ComponentType("MCBean", "MCServer");
      ManagedObject serverMO = managedObjectFactory.initManagedObject(server, "JBossServer", null);
      if (serverMO.getOperations() != null && serverMO.getOperations().size() == 0)
      {
         ManagedOperationImpl shutdown = new ManagedOperationImpl("Shutdown the server", "shutdown");
         if (serverMO instanceof MutableManagedObject)
         {
            HashSet<ManagedOperation> ops = new HashSet<ManagedOperation>();
            ops.add(shutdown);
            MutableManagedObject mmo = MutableManagedObject.class.cast(serverMO);
            mmo.setOperations(ops);
         }
      }
      ManagedComponentImpl serverComp = new ManagedComponentImpl(type, null, serverMO);

      // ServerConfig
      type = new ComponentType("MCBean", "ServerConfig");
      ManagedObject mo = managedObjectFactory.initManagedObject(server.getConfiguration(), null);
      ManagedComponentImpl configComp = new ManagedComponentImpl(type, null, mo);

      view.addManagedComponent(serverComp);
      view.addManagedComponent(configComp);
      
   }
   
   /**
    * Process a kernel deployment.
    * 
    * @param deploymentName the deployment name
    * @param deployment the kernel deployment
    * @throws Exception 
    */
   protected void processDeployment(String deploymentName, KernelDeployment deployment) throws Exception
   {
      ManagedDeploymentProcessor processor = new ManagedDeploymentProcessor(getProxyFactory());
      ManagedDeployment md = createManagedDeployment(deploymentName, deployment);
      processor.processRootManagedDeployment(md, view);
   }
   
   /**
    * Create a managed deployment based on the KernelDeployment
    * 
    * @param kernelDeployment the kernel deployment
    * @return the managed deployment
    */
   ManagedDeployment createManagedDeployment(String deploymentName, KernelDeployment kernelDeployment)
   {
      Map<String, ManagedObject> managedObjects = new HashMap<String, ManagedObject>();
      Collection<BeanMetaDataFactory> beanFactories = kernelDeployment.getBeanFactories();
      if(beanFactories != null && beanFactories.isEmpty() == false)
      {
         for(BeanMetaDataFactory beanFactory : beanFactories)
         {
            Collection<BeanMetaData> beans = beanFactory.getBeans();
            if(beans != null && beans.isEmpty() == false)
            {
               for(BeanMetaData bmd : beans)
               {
                  String name = bmd.getName();
                  ControllerContext context = getKernelController().getContext(bmd.getName(), null);
                  MetaData metaData = getMetaDataRepository().getMetaData(context);
                  // Create the managed object
                  ManagedObject mo = managedObjectFactory.initManagedObject(bmd, null, metaData, name, null);
                  if(mo != null)
                  {
                     managedObjects.put(name, mo);
                  }
               }
            }
         }
      }
      return new ManagedDeploymentImpl(kernelDeployment.getName(), kernelDeployment.getName(), null, managedObjects);
   }
   
   KernelController getKernelController()
   {
      return kernel.getController();
   }
   
   KernelMetaDataRepository getMetaDataRepository()
   {
      return kernel.getMetaDataRepository();
   }

   public void removeComponent(ManagedComponent update, ManagedComponent original) throws Exception
   {
      // nothing
   }
   
   public void updateComponent(ManagedComponent update, ManagedComponent original) throws Exception
   {
      // TODO at least dispatch
   }
   
}

