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

import java.util.*;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.security.jacc.PolicyConfiguration;

import org.jboss.beans.metadata.plugins.AbstractDemandMetaData;
import org.jboss.beans.metadata.plugins.AbstractSupplyMetaData;
import org.jboss.beans.metadata.plugins.builder.BeanMetaDataBuilderFactory;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.DemandMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployment.MappedReferenceMetaDataResolverDeployer;
import org.jboss.deployment.dependency.ContainerDependencyMetaData;
import org.jboss.ejb3.Container;
import org.jboss.ejb3.DependencyPolicy;
import org.jboss.ejb3.DeploymentUnit;
import org.jboss.ejb3.EJBContainer;
import org.jboss.ejb3.Ejb3Deployment;
import org.jboss.ejb3.MCDependencyPolicy;
import org.jboss.ejb3.javaee.JavaEEApplication;
import org.jboss.ejb3.javaee.JavaEEComponent;
import org.jboss.ejb3.kernel.JNDIKernelRegistryPlugin;
import org.jboss.ejb3.service.ServiceContainer;
import org.jboss.injection.injector.EEInjector;
import org.jboss.injection.injector.metadata.EnvironmentEntryType;
import org.jboss.injection.injector.metadata.InjectionTargetType;
import org.jboss.injection.injector.metadata.JndiEnvironmentRefsGroup;
import org.jboss.injection.manager.spi.InjectionManager;
import org.jboss.injection.manager.spi.Injector;
import org.jboss.injection.mc.metadata.JndiEnvironmentImpl;
import org.jboss.kernel.Kernel;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;
import org.jboss.metadata.ejb.jboss.jndipolicy.spi.EjbDeploymentSummary;
import org.jboss.metadata.ejb.spec.InterceptorMetaData;
import org.jboss.metadata.ejb.spec.InterceptorsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.switchboard.spi.Barrier;

/**
 * JBoss 5.0 Microkernel specific implementation
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author adrian@jboss.org
 * @version $Revision: 111882 $
 */
public class Ejb3JBoss5Deployment extends Ejb3Deployment
{
   private static Logger log = Logger.getLogger(Ejb3JBoss5Deployment.class);
   private org.jboss.deployers.structure.spi.DeploymentUnit jbossUnit;
   private Map<String, ContainerDependencyMetaData> endpoints;

   public Ejb3JBoss5Deployment(DeploymentUnit ejb3Unit, Kernel kernel, MBeanServer mbeanServer, org.jboss.deployers.structure.spi.DeploymentUnit jbossUnit, JBoss5DeploymentScope deploymentScope, JBossMetaData metaData)
   {
      // Either call the old constructor and do process persistence units
      //super(ejb3Unit, deploymentScope, metaData, persistenceUnitsMetaData);
      // or call the new constructor and don't process persistence units
      super(jbossUnit, ejb3Unit, deploymentScope, metaData);
      
      this.jbossUnit = jbossUnit;
      kernelAbstraction = new JBossASKernel(kernel, mbeanServer);

      // todo maybe mbeanServer should be injected?
      this.mbeanServer = mbeanServer;
      org.jboss.deployers.structure.spi.DeploymentUnit topUnit = jbossUnit.getTopLevel();
      endpoints = (Map<String, ContainerDependencyMetaData>) topUnit.getAttachment(MappedReferenceMetaDataResolverDeployer.ENDPOINT_MAP_KEY);
   }

   protected PolicyConfiguration createPolicyConfiguration() throws Exception
   {
      return null;
   }

   protected void putJaccInService(PolicyConfiguration pc, DeploymentUnit ejb3Unit)
   {
      //Ignore
   }

   public DependencyPolicy createDependencyPolicy(JavaEEComponent component)
   {
      return new JBoss5DependencyPolicy(component);
   }

   @Override
   public JavaEEApplication getApplication()
   {
      // getApplication must return null if there is no ear
      JavaEEApplication app = super.getApplication();
      if(((JBoss5DeploymentScope) app).isEar())
         return app;
      return null;
   }
   
   @Override
   protected void registerEJBContainer(Container container) throws Exception
   {
      // Add the jndi supplies
      MCDependencyPolicy dependsPolicy = (MCDependencyPolicy) container.getDependencyPolicy();
      EJBContainer ejbContainer = (EJBContainer) container;
      JBossEnterpriseBeanMetaData beanMD = ejbContainer.getXml();

      ContainerDependencyMetaData cdmd = null;
      if(endpoints != null)
      {
         String ejbKey = "ejb/" + jbossUnit.getRelativePath() + "#" + container.getEjbName();
         cdmd = endpoints.get(ejbKey);
      }
      else
      {
         log.warn(jbossUnit+" has no ContainerDependencyMetaData attachment");
      }

      if(cdmd != null)
      {
         for(String jndiName : cdmd.getJndiNames())
         {
         String supplyName = JNDIKernelRegistryPlugin.JNDI_DEPENDENCY_PREFIX + jndiName;
         AbstractSupplyMetaData supply = new AbstractSupplyMetaData(supplyName);
         dependsPolicy.getSupplies().add(supply);
         }
      }

      // EJBTHREE-1335: container name in meta data
      generateContainerName(container, beanMD);
      
      // setup switchboard
      Barrier switchBoard = this.getSwitchBoardBarrier(container);
      // the container cannot function without an SwitchBoard Barrier
      if (switchBoard == null)
      {
         throw new RuntimeException("No SwitchBoard Barrier found for bean: " + container.getEjbName() + " in unit: "
               + this.jbossUnit + " (or its component deployment unit)");
      }
      // add dependency on START (and not INSTALLED) state of Switchboard, since the container only requires a fully populated ENC context,
      // but doesn't require a invokable context. An invokable context is only needed by Injector.
      dependsPolicy.addDependency(this.createSwitchBoardDependency(ejbContainer, switchBoard));
      log.debug("Added dependency on Switchboard " + switchBoard.getId() + " for EJB container " + ejbContainer.getName());
      // EJBTHREE-2227 https://issues.jboss.org/browse/EJBTHREE-2227 add dependency on INSTALLED state
      // (i.e. fully populated and invokable ENC) of ALL other Barriers in the DU hierarchy to avoid instantiating
      // the @Service too early
      if (ejbContainer instanceof ServiceContainer)
      {
          this.setupDependencyOnOtherBarriers((ServiceContainer) ejbContainer);
      }
      
      // create and setup Injector(s) for InjectionManager  
      InjectionManager injectionManager = this.getInjectionManager(container);
      // the container cannot function without an InjectionManager
      if (injectionManager == null)
      {
         throw new RuntimeException("No InjectionManager found for bean: " + container.getEjbName() + " in unit: "
               + this.jbossUnit + " (or its component deployment unit)");
      }
      // setup the injectors for the bean and any of its interceptors
      this.setupInjectors(ejbContainer, injectionManager, switchBoard);
      // set the InjectionManager on the EJBContainer
      ejbContainer.setInjectionManager(injectionManager);
      
      super.registerEJBContainer(container);
   }

   private void generateContainerName(Container container, JBossEnterpriseBeanMetaData beanMD)
   {
      ObjectName on = container.getObjectName();
      assert on!=null : "ObjectName was null";

      // Heiko: This should actually generate the name and assign it to ejb3 meta data
      // Currently we stick to copying the values around until an EJB3 team member figures out a proper way      
      beanMD.setGeneratedContainerName(on.getCanonicalName());
   }

   private static EjbDeploymentSummary getUnitSummary(DeploymentUnit unit, JBossEnterpriseBeanMetaData beanMD)
   {
      ClassLoader loader = unit.getClassLoader();
      EjbDeploymentSummary summary = new EjbDeploymentSummary();
      summary.setBeanMD(beanMD);
      summary.setBeanClassName(beanMD.getEjbClass());
      summary.setDeploymentName(unit.getShortName());
      String baseName = unit.getRootFile().getName();
      summary.setDeploymentScopeBaseName(baseName);
      summary.setEjbName(beanMD.getEjbName());
      summary.setLoader(loader);
      summary.setLocal(beanMD.isMessageDriven());
      if(beanMD instanceof JBossSessionBeanMetaData)
      {
         JBossSessionBeanMetaData sbeanMD = (JBossSessionBeanMetaData) beanMD;
         summary.setStateful(sbeanMD.isStateful());
      }
      summary.setService(beanMD.isService());
      return summary;
   }   
   
   protected Barrier getSwitchBoardBarrier(Container container)
   {
      if (this.isSharedENC(this.jbossUnit))
      {
         return this.jbossUnit.getAttachment(Barrier.class);
      }
      
      // This is a bit brittle since we are relying on the internal knowledge of
      // what org.jboss.ejb3.deployers.EJBsDeployer uses as a name to attach the EJB metadata
      // component to the deployment unit. However, we don't have an alternate way to get hold
      // of the name. So for now, let's just use this
      String ejbComponentName = JBossEnterpriseBeanMetaData.class.getName() + "." + container.getEjbName();
      // get the component DU for the EJB of this container from the parent (non-component) DU
      org.jboss.deployers.structure.spi.DeploymentUnit componentDU = this.jbossUnit.getComponent(ejbComponentName);
      if (componentDU == null)
      {
         // without the component DU, there's no way we can get hold of the switchboard Barrier.
         // That effectively makes the container unusable. Hence throw this exception
         throw new RuntimeException("Component Deployment Unit for bean: " + container.getEjbName()
               + " not found in unit: " + this.jbossUnit);
      }
      return componentDU.getAttachment(Barrier.class);
   }
   
   protected InjectionManager getInjectionManager(Container container)
   {
      // This is a bit brittle since we are relying on the internal knowledge of
      // what org.jboss.ejb3.deployers.EJBsDeployer uses as a name to attach the EJB metadata
      // component to the deployment unit. However, we don't have an alternate way to get hold
      // of the name. So for now, let's just use this
      String ejbComponentName = JBossEnterpriseBeanMetaData.class.getName() + "." + container.getEjbName();
      // get the component DU for the EJB of this container from the parent (non-component) DU
      org.jboss.deployers.structure.spi.DeploymentUnit componentDU = this.jbossUnit.getComponent(ejbComponentName);
      if (componentDU == null)
      {
         // without the component DU, there's no way we can get hold of the correct InjectionManager.
         // That effectively makes the container unusable. Hence throw this exception
         throw new RuntimeException("Component Deployment Unit for bean: " + container.getEjbName()
               + " not found in unit: " + this.jbossUnit);
      }
      // get the InjectionManager
      return componentDU.getAttachment(InjectionManager.class);
   }
   
   protected void setupInjectors(EJBContainer ejbContainer, InjectionManager injectionManager, Barrier switchBoard)
   {
      // Let's first create EEInjector (which pulls from ENC and pushes to EJB instance) for EJB
      // and then create a EEInjector for each of the interceptors for the bean

      JBossEnterpriseBeanMetaData beanMetaData = ejbContainer.getXml();
      // convert JBMETA metadata to jboss-injection specific metadata
      JndiEnvironmentRefsGroup jndiEnvironment = new JndiEnvironmentImpl(beanMetaData, ejbContainer.getClassloader());
      // For optimization, we'll create an Injector only if there's atleast one InjectionTarget
      if (this.hasInjectionTargets(jndiEnvironment))
      {
         // create the injector
         EEInjector eeInjector = new EEInjector(jndiEnvironment);
         // add the injector the injection manager
         injectionManager.addInjector(eeInjector);
         // Deploy the Injector as a MC bean (so that the fully populated naming context (obtained via the SwitchBoard
         // Barrier) gets injected.
         String injectorMCBeanName = this.getInjectorMCBeanNamePrefix() + ",bean=" + ejbContainer.getEjbName();
         BeanMetaData injectorBMD = this.createInjectorBMD(injectorMCBeanName, eeInjector, switchBoard);
         this.jbossUnit.addAttachment(BeanMetaData.class + ":" + injectorMCBeanName, injectorBMD);
         
         // Add the Injector dependency on the deployment (so that the DU doesn't
         // get started till the Injector is available)
         DependencyPolicy dependsPolicy = ejbContainer.getDependencyPolicy();
         dependsPolicy.addDependency(injectorMCBeanName);
         log.debug("Added Injector dependency: " + injectorMCBeanName + " for EJB: " + ejbContainer.getEjbName() + " in unit " + this.jbossUnit);
      }
      
      // Now setup injectors for the interceptors of the bean
      InterceptorsMetaData interceptors = JBossMetaData.getInterceptors(beanMetaData.getEjbName(), beanMetaData.getJBossMetaData());
      if (interceptors == null || interceptors.isEmpty())
      {
         return;
      }
      for (InterceptorMetaData interceptor : interceptors)
      {
         if (interceptor == null)
         {
            continue;
         }
         JndiEnvironmentRefsGroup jndiEnvironmentForInterceptor = new JndiEnvironmentImpl(interceptor, ejbContainer.getClassloader());
         // For optimization, we'll create an Injector only if there's atleast one InjectionTarget
         if (this.hasInjectionTargets(jndiEnvironmentForInterceptor))
         {
            // create the injector
            EEInjector lazyEEInjector = new EEInjector(jndiEnvironmentForInterceptor);
            // add the injector the injection manager
            injectionManager.addInjector(lazyEEInjector);
            // Deploy the Injector as a MC bean (so that the fully populated naming context (obtained via the SwitchBoard
            // Barrier) gets injected.
            String interceptorInjectorMCBeanName = this.getInjectorMCBeanNamePrefix() + ",bean=" + ejbContainer.getEjbName() + ",interceptor=" + interceptor.getName();
            BeanMetaData injectorBMD = this.createInjectorBMD(interceptorInjectorMCBeanName, lazyEEInjector, switchBoard);
            this.jbossUnit.addAttachment(BeanMetaData.class + ":" + interceptorInjectorMCBeanName, injectorBMD);
            
            // Add the Injector dependency on the deployment (so that the DU doesn't
            // get started till the Injector is available)
            DependencyPolicy dependsPolicy = ejbContainer.getDependencyPolicy();
            dependsPolicy.addDependency(interceptorInjectorMCBeanName);
            log.debug("Added Injector dependency: " + interceptorInjectorMCBeanName + " for interceptor "
                  + interceptor.getName() + " of EJB: " + ejbContainer.getEjbName() + " in unit " + this.jbossUnit);
         }
         
      }

   }

   /**
    * Returns true if the passed {@link JndiEnvironmentRefsGroup} has atleast one {@link EnvironmentEntryType environment entry}
    * with an {@link InjectionTargetType injection target}. Else, returns false
    *  
    * @param jndiEnv
    * @return
    */
   private boolean hasInjectionTargets(JndiEnvironmentRefsGroup jndiEnv)
   {
      Collection<EnvironmentEntryType> envEntries = jndiEnv.getEntries();
      if (envEntries == null || envEntries.isEmpty())
      {
         return false;
      }
      for (EnvironmentEntryType envEntry : envEntries)
      {
         Collection<InjectionTargetType> injectionTargets = envEntry.getInjectionTargets();
         if (injectionTargets != null && !injectionTargets.isEmpty())
         {
            return true;
         }
      }
      return false;   
   }
   
   /**
    * Creates and returns {@link BeanMetaData} for the passed {@link EEInjector injector} and sets up
    * dependency on the passed {@link Barrier SwitchBoard barrier}.
    * 
    * @param injectorMCBeanName
    * @param injector
    * @param barrier
    * @return
    */
   protected BeanMetaData createInjectorBMD(String injectorMCBeanName, EEInjector injector, Barrier barrier)
   {
      BeanMetaDataBuilder builder = BeanMetaDataBuilderFactory.createBuilder(injectorMCBeanName, injector.getClass().getName());
      builder.setConstructorValue(injector);

      // add dependency on SwitchBoard Barrier
      builder.addDemand(barrier.getId(), ControllerState.CREATE, ControllerState.START, null);

      // return the Injector BMD
      return builder.getBeanMetaData();
   }
   

   /**
    * Returns the prefix for the MC bean name, for a {@link Injector injector}
    * 
    * @return
    */
   protected String getInjectorMCBeanNamePrefix()
   {
      StringBuilder sb = new StringBuilder("jboss-injector:");
      org.jboss.deployers.structure.spi.DeploymentUnit topLevelUnit = this.jbossUnit.isTopLevel() ? this.jbossUnit : this.jbossUnit.getTopLevel();
      sb.append("topLevelUnit=");
      sb.append(topLevelUnit.getSimpleName());
      sb.append(",");
      sb.append("unit=");
      sb.append(this.jbossUnit.getSimpleName());

      return sb.toString();
   }

   private boolean isSharedENC(org.jboss.deployers.structure.spi.DeploymentUnit deploymentUnit)
   {
      JBossMetaData jbossMetaData = deploymentUnit.getAttachment(JBossMetaData.class);
      JBossWebMetaData jbosswebMetaData = deploymentUnit.getAttachment(JBossWebMetaData.class);
      if (jbossMetaData != null && jbosswebMetaData != null)
      {
         return true;
      }
      return false;
   }
   
   private DemandMetaData createSwitchBoardDependency(EJBContainer container, Barrier switchBoard)
   {
      AbstractDemandMetaData switchboardDependency = new AbstractDemandMetaData();
      switchboardDependency.setDemand(switchBoard.getId());
      switchboardDependency.setWhenRequired(ControllerState.CREATE);
      // This is really hacky for @Service bean containers.
      // The issue with ServiceContainer is that it instantiates (and that leads to injection)
      // the bean instance in its start() lifecycle call. Ideally, like the @Singleton EJB3.1 beans
      // we should have done it outside of the container, so that the container could just depend 
      // on a START(ed) switchboard which provides only a populated but not invokable ENC.
      // However, due to the nature of changes involved to support that for @Service and given that
      // we have deprecated @Service (starting AS6), this is relatively better way to handle @Service
      // beans.
      if (container instanceof ServiceContainer)
      {
         // ServiceContainer requires a populated and invokable ENC (== INSTALLED state of switchboard)
         switchboardDependency.setTargetState(ControllerState.INSTALLED);
      }
      else
      {
         // container requires only a populated ENC (== START state of switchboard)
         switchboardDependency.setTargetState(ControllerState.START);
      }

      return switchboardDependency;
   }

    /**
     * Finds all {@link Barrier}s within the deployment hierarchy of this {@link ServiceContainer}
     * and sets up a dependency on INSTALLED state of each of them.
     *
     * @param serviceContainer The EJB3 @Service container
     */
   private void setupDependencyOnOtherBarriers(final ServiceContainer serviceContainer)
   {
      // EJBTHREE-2227 https://issues.jboss.org/browse/EJBTHREE-2227 add dependency on INSTALLED state
      // (i.e. fully populated and invokable ENC) of ALL other Barriers in the DU hierarchy to avoid instantiating
      // the @Service too early
      final MCDependencyPolicy dependsPolicy = (MCDependencyPolicy) serviceContainer.getDependencyPolicy();

      final Set<String> barriers = this.getBarrierIdsFromAllDeploymentUnitsInHierarchy(this.jbossUnit);
      for (String barrier : barriers) {
         if (barrier == null) {
            continue;
         }
         AbstractDemandMetaData switchboardDependency = new AbstractDemandMetaData();
         switchboardDependency.setDemand(barrier);
         switchboardDependency.setWhenRequired(ControllerState.CREATE);
         // ServiceContainer requires a populated and invokable ENC (== INSTALLED state of switchboard)
         switchboardDependency.setTargetState(ControllerState.INSTALLED);

         // add dependency on INSTALLED state (i.e. fully populated and invokable ENC) of Barrier
         dependsPolicy.addDependency(switchboardDependency);
      }

   }

    private Set<String> getBarrierIdsFromAllDeploymentUnitsInHierarchy(final org.jboss.deployers.structure.spi.DeploymentUnit deploymentUnit) {
       final Set<String> barriers = new HashSet<String>();
       // get the top level DU in the hierarchy of the DU
       final org.jboss.deployers.structure.spi.DeploymentUnit topLevelDU = deploymentUnit.getTopLevel();
       final List<org.jboss.deployers.structure.spi.DeploymentUnit> allDUs = new ArrayList<org.jboss.deployers.structure.spi.DeploymentUnit>();
       allDUs.add(topLevelDU);
       // now fetch all the child DUs recursively from the top level DU
       allDUs.addAll(this.getChildrenRecursively(topLevelDU));
       // find any Barrier in each of the DU
       for (org.jboss.deployers.structure.spi.DeploymentUnit du : allDUs) {
          final Barrier barrier = du.getAttachment(Barrier.class);
          if (barrier != null) {
             barriers.add(barrier.getId());
          }
       }
       return barriers;
    }

    private List<org.jboss.deployers.structure.spi.DeploymentUnit> getChildrenRecursively(final org.jboss.deployers.structure.spi.DeploymentUnit deploymentUnit) {
       List<org.jboss.deployers.structure.spi.DeploymentUnit> allChildren = new ArrayList<org.jboss.deployers.structure.spi.DeploymentUnit>();
       List<org.jboss.deployers.structure.spi.DeploymentUnit> children = deploymentUnit.getChildren();
       if (children != null && !children.isEmpty()) {

          allChildren.addAll(children);
          // find child DU of each child
          for (org.jboss.deployers.structure.spi.DeploymentUnit child : children) {
             if (child == null) {
                continue;
             }
             allChildren.addAll(this.getChildrenRecursively(child));
          }
       }
       // check component DUs too
       final List<org.jboss.deployers.structure.spi.DeploymentUnit> componentDUs = deploymentUnit.getComponents();
       if (componentDUs != null && !componentDUs.isEmpty()) {

          allChildren.addAll(componentDUs);
          // find child DU of each component DU
          for (org.jboss.deployers.structure.spi.DeploymentUnit componentDU : componentDUs) {
             if (componentDU == null) {
                continue;
             }
             allChildren.addAll(this.getChildrenRecursively(componentDU));
          }
       }
       return allChildren;
    }

}
