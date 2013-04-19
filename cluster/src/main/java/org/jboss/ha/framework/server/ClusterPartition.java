/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009 Red Hat, Inc. and individual contributors
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
package org.jboss.ha.framework.server;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.jboss.beans.metadata.api.annotations.Create;
import org.jboss.beans.metadata.api.annotations.Destroy;
import org.jboss.beans.metadata.api.annotations.Start;
import org.jboss.beans.metadata.api.annotations.Stop;
import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.ha.core.framework.server.DistributedReplicantManagerImpl;
import org.jboss.ha.core.framework.server.HAPartitionImpl;
import org.jboss.ha.framework.server.deployers.DefaultHAPartitionDependencyCreator;
import org.jboss.ha.framework.server.deployers.HAPartitionDependencyCreator;
import org.jboss.ha.framework.server.spi.HAPartitionCacheHandler;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.kernel.spi.dependency.KernelControllerContextAware;
import org.jboss.managed.api.ManagedOperation.Impact;
import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementObjectID;
import org.jboss.managed.api.annotation.ManagementOperation;
import org.jboss.managed.api.annotation.ManagementParameter;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.jboss.naming.NonSerializableFactory;

/**
 * Extends the superclass to expose a ManagedObject interface.
 *
 * @author <a href="mailto:sacha.labourey@cogito-info.ch">Sacha Labourey</a>.
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>.
 * @author Scott.Stark@jboss.org
 * @author brian.stansberry@jboss.com
 * @author Vladimir Blagojevic
 * @author <a href="mailto:galder.zamarreno@jboss.com">Galder Zamarreno</a>
 * @version $Revision: 105748 $
 */
@ManagementObject(componentType=@ManagementComponent(type="MCBean", subtype="HAPartition"),
                  properties=ManagementProperties.CLASS_AND_EXPLICIT,
                  classProperties={@ManagementProperty(name="stateString",use={ViewUse.STATISTIC})},
                  isRuntime=true)
public class ClusterPartition
   extends HAPartitionImpl
   implements ClusterPartitionMBean, KernelControllerContextAware
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   /** Whether to bind the partition into JNDI */
   private boolean bindIntoJndi = true;
   
   private HAPartitionDependencyCreator  haPartitionDependencyCreator;
   private KernelControllerContext kernelControllerContext;
   
   // Legacy config properties, just to support management interface
   private HAPartitionCacheHandler cacheHandler;
   
   // Static --------------------------------------------------------

    // Constructors --------------------------------------------------
   
   public ClusterPartition()
   {
      this.logHistory("Partition object created");
   }

   // ----------------------------------------------------------------- Service
   
   protected void createService() throws Exception
   {
      super.createService();    

      // Add a well-known MC alias that other beans can depend on
      addCanonicalAlias();
   }
   
   protected void startService() throws Exception
   {
      super.startService();
         
      // Register with the service locator
      HAPartitionLocator.getHAPartitionLocator().registerHAPartition(this);
      
      // Bind ourself in the public JNDI space if configured to do so
      if (this.bindIntoJndi)
      {
         Context ctx = new InitialContext();
         this.bind(HAPartitionLocator.getStandardJndiBinding(this.getPartitionName()),
                   this, ClusterPartition.class, ctx);
         this.log.debug("Bound in JNDI under /HAPartition/" + this.getPartitionName());
      }
      
   }

   protected void stopService() throws Exception
   {
      super.stopService();
      
      if (this.bindIntoJndi)
      {
         String boundName = HAPartitionLocator.getStandardJndiBinding(this.getPartitionName());
         InitialContext ctx = null;
         try
         {
            // the following statement fails when the server is being shut down (07/19/2007)
            ctx = new InitialContext();
            ctx.unbind(boundName);
         }
         catch (Exception e) {
            this.log.error("partition unbind operation failed", e);
         }
         finally
         {
            if (ctx != null)
            {
               ctx.close();
            }
         }
         NonSerializableFactory.unbind(boundName);
      }
      
      HAPartitionLocator.getHAPartitionLocator().deregisterHAPartition(this);
   }
   
   @Override
   protected void destroyService()
   {      
      removeCanonicalAlias();
      
      super.destroyService();
   }

   /**
    * Adds an alias to our controller context -- the concatenation of
    * {@link #getAliasPrefix()} and {@link #getPartitionName()}.
    * This mechanism allows Ejb2HAPartitionDependencyDeployer to add
    * dependencies to deployments based on the partition name specified in
    * their metadata, without needing to know the bean name of this partition.
    */
   private void addCanonicalAlias()
   {
      if (kernelControllerContext != null)
      {
         KernelController kc = (KernelController) kernelControllerContext.getController();
         String aliasName = getHaPartitionDependencyCreator().getHAPartitionDependencyName(this.getPartitionName());
         try
         {
            kc.addAlias(aliasName, kernelControllerContext.getName());
         }
         catch (Throwable t)
         {
            log.error("Failed adding alias " + aliasName + " to context " + kernelControllerContext.getName(), t);
         }
      }
   }

   /**
    * Removes the alias created in {@link #addCanonicalAlias()}
    */
   private void removeCanonicalAlias()
   {
      if (kernelControllerContext != null)
      {
         KernelController kc = (KernelController) kernelControllerContext.getController();
         String aliasName = getHaPartitionDependencyCreator().getHAPartitionDependencyName(this.getPartitionName());
         Set<Object> aliases = kernelControllerContext.getAliases();
         if (aliases != null && aliases.contains(aliasName))
         {
            try
            {
               kc.removeAlias(aliasName);
            }
            catch (Throwable t)
            {
               log.error("Failed removing alias " + aliasName + " from context " + kernelControllerContext.getName(), t);
            }
         }
      }
   }

   // HAPartition implementation ----------------------------------------------
   
   @ManagementProperty(use={ViewUse.STATISTIC}, description="The identifier for this node in cluster topology views")
   public String getNodeName()
   {
      return super.getNodeName();
   }

   @ManagementProperty(use={ViewUse.CONFIGURATION}, description="Deprecated, legacy term for group name")
   @ManagementObjectID(type="HAPartition")
   public String getGroupName()
   {
      return super.getGroupName();
   }
   
   @ManagementProperty(use={ViewUse.CONFIGURATION}, description="Deprecated, legacy term for group name")
   @ManagementObjectID(type="HAPartition")
   public String getPartitionName()
   {
      return super.getPartitionName();
   }

   @ManagementProperty(use={ViewUse.STATISTIC}, description="Identifier for the current topology view")
   public long getCurrentViewId()
   {
      return super.getCurrentViewId();
   }
   
   @ManagementProperty(use={ViewUse.STATISTIC}, description="The current cluster topology view")
   public Vector<String> getCurrentView()
   {
      return super.getCurrentView();
   }

   @ManagementProperty(use={ViewUse.STATISTIC}, description="Whether this node is acting as the group coordinator for the partition")
   public boolean isCurrentNodeCoordinator ()
   {
      return super.isCurrentNodeCoordinator();
   }
   

   @ManagementProperty(use={ViewUse.CONFIGURATION, ViewUse.RUNTIME}, 
         description="Whether to allow synchronous notifications of topology changes")
   public boolean getAllowSynchronousMembershipNotifications()
   {
      return super.getAllowSynchronousMembershipNotifications();
   }

   /**
    * Helper method that binds the partition in the JNDI tree.
    * @param jndiName Name under which the object must be bound
    * @param who Object to bind in JNDI
    * @param classType Class type under which should appear the bound object
    * @param ctx Naming context under which we bind the object
    * @throws Exception Thrown if a naming exception occurs during binding
    */
   protected void bind(String jndiName, Object who, Class<?> classType, Context ctx) throws Exception
   {
      // Ah ! This service isn't serializable, so we use a helper class
      //
      NonSerializableFactory.bind(jndiName, who);
      Name n = ctx.getNameParser("").parse(jndiName);
      while (n.size () > 1)
      {
         String ctxName = n.get (0);
         try
         {
            ctx = (Context)ctx.lookup (ctxName);
         }
         catch (NameNotFoundException e)
         {
            this.log.debug ("creating Subcontext " + ctxName);
            ctx = ctx.createSubcontext (ctxName);
         }
         n = n.getSuffix (1);
      }

      // The helper class NonSerializableFactory uses address type nns, we go on to
      // use the helper class to bind the service object in JNDI
      //
      StringRefAddr addr = new StringRefAddr("nns", jndiName);
      Reference ref = new Reference(classType.getName (), addr, NonSerializableFactory.class.getName (), null);
      ctx.rebind (n.get (0), ref);
   }
   
   /*
    * Allows caller to specify whether the partition instance should be bound into JNDI.  Default value is true.
    * This method must be called before the partition is started as the binding occurs during startup.
    * 
    * @param bind  Whether to bind the partition into JNDI.
    */
   public void setBindIntoJndi(boolean bind)
   {
       this.bindIntoJndi = bind;
   }
   
   /*
    * Allows caller to determine whether the partition instance has been bound into JNDI.
    * 
    * @return true if the partition has been bound into JNDI.
    */
   @ManagementProperty(description="Whether this HAPartition should bind itself into JNDI")
   public boolean getBindIntoJndi()
   {
       return this.bindIntoJndi;
   }

   public synchronized HAPartitionDependencyCreator getHaPartitionDependencyCreator()
   {
      if (haPartitionDependencyCreator == null)
      {
         haPartitionDependencyCreator = DefaultHAPartitionDependencyCreator.INSTANCE;
      }
      return haPartitionDependencyCreator;
   }

   public synchronized void setHaPartitionDependencyCreator(HAPartitionDependencyCreator haPartitionDependencyCreator)
   {
      this.haPartitionDependencyCreator = haPartitionDependencyCreator;
   }

   // --------------------------------------------------- ClusterPartitionMBean
   
   @ManagementOperation(description="Gets a listing of significant events since " +
   		                            "the instantiation of this service",
                        impact=Impact.ReadOnly)
   public String showHistory()
   {
      return super.showHistory();
   }

   @ManagementOperation(description="Gets an XML format listing of significant events since " +
                                    "the instantiation of this service",
                        impact=Impact.ReadOnly)
   public String showHistoryAsXML()
   {
      return super.showHistoryAsXML();
   }

   @ManagementProperty(use={ViewUse.STATISTIC}, description="The release version of JGroups")
   public String getJGroupsVersion()
   {
      return super.getJGroupsVersion();
   }

   @ManagementProperty(use={ViewUse.STATISTIC}, 
         description="Name of the CacheManager configuration used for deriving the JGroups channel stack name")
   public String getCacheConfigName()
   {
      return this.cacheHandler == null ? null : this.cacheHandler.getCacheConfigName();
   }
   
   @ManagementProperty(use={ViewUse.STATISTIC}, description="Name of the JGroups protocol stack configuration")
   public String getChannelStackName()
   {
      return this.cacheHandler == null ? super.getChannelStackName() : this.cacheHandler.getChannelStackName();
   }

   @ManagementProperty(description="Time (in ms) to allow for state transfer to finish")
   public long getStateTransferTimeout() {
      return super.getStateTransferTimeout();
   }

   @ManagementProperty(use={ViewUse.CONFIGURATION, ViewUse.RUNTIME},
         description="Time (in ms) to allow for group RPCs to return")
   public long getMethodCallTimeout() {
      return super.getMethodCallTimeout();
   }

   public String getName()
   {
      return org.jboss.util.Classes.stripPackageName(log.getName());
   }
   
   // KernelControllerContextAware --------------------------------------------
   
   public void setKernelControllerContext(KernelControllerContext controllerContext) throws Exception
   {
      this.kernelControllerContext = controllerContext;
   }

   public void unsetKernelControllerContext(KernelControllerContext controllerContext) throws Exception
   {
      this.kernelControllerContext = null;
   }
   
   // ManagedObject interface for lifecycle ----------------------------------
   
 
   @ManagementOperation(description="Create the HAPartition",
         impact=Impact.WriteOnly)
   @Override
   public void create() throws Exception
   {
      if (this.kernelControllerContext != null)
         pojoChange(ControllerState.CREATE);
      else
         super.create();
   }

   @ManagementOperation(description="Start the HAPartition",
         impact=Impact.WriteOnly)
   @Override
   public void start() throws Exception
   {
      if (this.kernelControllerContext != null)
         pojoChange(ControllerState.START);
      else
         super.start();
   }

   @ManagementOperation(description="Stop the HAPartition",
         impact=Impact.WriteOnly)
   @Override
   public void stop()
   {
      if (this.kernelControllerContext != null)
         pojoChange(ControllerState.CREATE);
      else
         super.stop();
   }

   @ManagementOperation(description="Destroy the HAPartition",
         impact=Impact.WriteOnly)
   @Override
   public void destroy()
   {
      if (this.kernelControllerContext != null)
         pojoChange(ControllerState.CONFIGURED);
      else
         super.destroy();
   }
   
   // ManagedObject interface for DRM ---------------------------------------
   
   @ManagementOperation(description="List all known DistributedReplicantManager keys and the nodes that have registered bindings",
         impact=Impact.ReadOnly)
   public String listDRMContent() throws Exception
   {
      DistributedReplicantManagerImpl drm = getDistributedReplicantManagerImpl();
      return drm == null ? null : drm.listContent();
   }

   @ManagementOperation(description="List in XML format all known DistributedReplicantManager keys and the nodes that have registered bindings",
         impact=Impact.ReadOnly)
   public String listDRMContentAsXml() throws Exception
   {
      DistributedReplicantManagerImpl drm = getDistributedReplicantManagerImpl();
      return drm == null ? null : drm.listXmlContent();
   }
   
   @ManagementOperation(description="Returns the names of the nodes that have registered objects with the DistributedReplicantManager under the given key",
                        impact=Impact.ReadOnly,
                        params={@ManagementParameter(name="key",
                                                     description="The name of the service")})
   public List<String> lookupDRMNodeNames(String key)
   {
      DistributedReplicantManagerImpl drm = getDistributedReplicantManagerImpl();
      return drm == null ? null : drm.lookupReplicantsNodeNames(key);
   }
   
   @ManagementOperation(description="Returns a hash of the list of nodes that " +
                                    "have registered an object with the DistributedReplicantManager under  the given key",
                        impact=Impact.ReadOnly,
                        params={@ManagementParameter(name="key",
                                                     description="The name of the service")})
   public int getDRMServiceViewId(String key)
   {
      DistributedReplicantManagerImpl drm = getDistributedReplicantManagerImpl();
      return drm == null ? null : drm.getReplicantsViewId(key);
   }
   
   @ManagementOperation(description="Returns whether the DistributedReplicantManager considers this node to be the master for the given service",
         impact=Impact.ReadOnly,
         params={@ManagementParameter(name="key", description="The name of the service")})
   public boolean isDRMMasterForService(String key)
   {
      DistributedReplicantManagerImpl drm = getDistributedReplicantManagerImpl();
      return drm == null ? null : drm.isMasterReplica(key);
   }
   
   @ManagementOperation(description="Get a collection of the names of all keys for which the DistributedReplicantManager has bindings",
         impact=Impact.ReadOnly)
   public Collection<String> getDRMServiceNames()
   {
      DistributedReplicantManagerImpl drm = getDistributedReplicantManagerImpl();
      return drm == null ? null : drm.getAllServices();
   }
   
   // Public -----------------------------------------------------------------

   public void setCacheHandler(HAPartitionCacheHandler handler)
   {
      this.cacheHandler = handler;
   } 

   @Create
   public void pojoCreate() throws Exception
   {
      super.create();
   }
   
   @Start
   public void pojoStart() throws Exception
   {
      super.start();
   }

   @Stop
   public void pojoStop() throws Exception
   {
      super.stop();
   }
   
   @Destroy
   public void pojoDestroy() throws Exception
   {
      super.destroy();
   }
   

   // Protected --------------------------------------------------------------
      
   // Private ----------------------------------------------------------------


   private void pojoChange(ControllerState state)
   {
      Controller controller = kernelControllerContext.getController();
      try
      {
         controller.change(kernelControllerContext, state);
      }
      catch (RuntimeException e)
      {
         throw e;
      }
      catch (Error e)
      {
         throw e;
      }
      catch (Throwable t)
      {
         throw new RuntimeException("Error changing state of " + kernelControllerContext.getName() + " to " + state.getStateString(), t);
      }
   }
   // Inner classes ----------------------------------------------------------
   
}
