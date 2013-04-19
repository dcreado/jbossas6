/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.webservices.integration.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.naming.Referenceable;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.WebServiceRefs;
import javax.xml.ws.soap.MTOM;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.switchboard.javaee.environment.Addressing;
import org.jboss.switchboard.javaee.environment.Handler;
import org.jboss.switchboard.javaee.environment.HandlerChain;
import org.jboss.switchboard.javaee.environment.InjectionTarget;
import org.jboss.switchboard.javaee.environment.PortComponent;
import org.jboss.switchboard.javaee.environment.ServiceRefType;
import org.jboss.switchboard.javaee.jboss.environment.JBossPortComponent;
import org.jboss.switchboard.javaee.jboss.environment.JBossServiceRefType;
import org.jboss.switchboard.mc.spi.MCBasedResourceProvider;
import org.jboss.switchboard.spi.Resource;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedCallPropertyMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainsMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedInitParamMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedPortComponentRefMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedStubPropertyMetaData;
import org.jboss.wsf.spi.serviceref.ServiceRefHandler;
import org.jboss.wsf.spi.serviceref.ServiceRefHandlerFactory;
import org.jboss.wsf.spi.serviceref.ServiceRefHandler.Type;

/**
 * Service reference resource provider.
 *
 * Conventions used in this source code are:
 * <ul>
 *  <li>SBMD - switch board meta data</li>
 *  <li>UMDM - jbossws unified meta data model</li>
 * </ul>
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ServiceRefResourceProvider implements MCBasedResourceProvider<ServiceRefType>
{

   private static final Logger log = Logger.getLogger(ServiceRefResourceProvider.class);

   private final ServiceRefHandler delegate;

   /**
    * Constructor.
    */
   public ServiceRefResourceProvider()
   {
      final SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
      this.delegate = spiProvider.getSPI(ServiceRefHandlerFactory.class).getServiceRefHandler();
   }

   /**
    * Provides service ref resource.
    */
   @Override
   public Resource provide(final DeploymentUnit deploymentUnit, final ServiceRefType serviceRefSBMD)
   {
      final ClassLoader newLoader = deploymentUnit.getClassLoader();
      final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();

      try
      {
         Thread.currentThread().setContextClassLoader(newLoader);
         final UnifiedVirtualFile vfsRoot = this.getUnifiedVirtualFile(deploymentUnit);
         final UnifiedServiceRefMetaData serviceRefUMDM = this.getUnifiedServiceRefMetaData(vfsRoot, serviceRefSBMD,
               newLoader);
         final Referenceable jndiReferenceable = this.delegate.createReferenceable(serviceRefUMDM);

         return new ServiceRefResource(jndiReferenceable);
      }
      finally
      {
         Thread.currentThread().setContextClassLoader(oldLoader);
      }
   }

   /**
    * Inform about type this resource handler can handle.
    * 
    * @return handled type
    */
   @Override
   public Class<ServiceRefType> getEnvironmentEntryType()
   {
      return ServiceRefType.class;
   }

   /**
    * Constructs vfs root from deployment unit.
    * 
    * @param deploymentUnit
    * @return vfs root
    */
   private UnifiedVirtualFile getUnifiedVirtualFile(final DeploymentUnit deploymentUnit)
   {
      DeploymentUnit tempDeploymentUnit = deploymentUnit;
      while (tempDeploymentUnit.isComponent())
         tempDeploymentUnit = tempDeploymentUnit.getParent();

      if (tempDeploymentUnit instanceof VFSDeploymentUnit)
      {
         VFSDeploymentUnit vdu = (VFSDeploymentUnit) tempDeploymentUnit;
         return new VirtualFileAdaptor(vdu.getRoot());
      }
      else
      {
         throw new IllegalArgumentException("Can only handle real VFS deployments: " + tempDeploymentUnit);
      }
   }

   /**
    * Translates service ref switchboard meta data to JBossWS unified service ref meta data.
    * 
    * @param vfsRoot virtual file system root
    * @param serviceRefSBMD service reference switchboard meta data
    * @return unified jbossws service reference meta data
    */
   private UnifiedServiceRefMetaData getUnifiedServiceRefMetaData(final UnifiedVirtualFile vfsRoot,
         final ServiceRefType serviceRefSBMD, final ClassLoader loader)
   {
      final UnifiedServiceRefMetaData serviceRefUMDM = new UnifiedServiceRefMetaData(vfsRoot);
      serviceRefUMDM.setServiceRefName(serviceRefSBMD.getName());
      serviceRefUMDM.setServiceRefType(serviceRefSBMD.getType());
      serviceRefUMDM.setServiceInterface(serviceRefSBMD.getServiceInterface());
      serviceRefUMDM.setWsdlFile(serviceRefSBMD.getWsdlFile());
      serviceRefUMDM.setMappingFile(serviceRefSBMD.getMappingFile());
      serviceRefUMDM.setServiceQName(serviceRefSBMD.getQName());
      serviceRefUMDM.setHandlerChain(serviceRefSBMD.getHandlerChain());

      // propagate addressing properties
      serviceRefUMDM.setAddressingAnnotationSpecified(serviceRefSBMD.isAddressingFeatureEnabled());
      serviceRefUMDM.setAddressingEnabled(serviceRefSBMD.isAddressingEnabled());
      serviceRefUMDM.setAddressingRequired(serviceRefSBMD.isAddressingRequired());
      serviceRefUMDM.setAddressingResponses(serviceRefSBMD.getAddressingResponses());

      // propagate MTOM properties
      serviceRefUMDM.setMtomAnnotationSpecified(serviceRefSBMD.isMtomFeatureEnabled());
      serviceRefUMDM.setMtomEnabled(serviceRefSBMD.isMtomEnabled());
      serviceRefUMDM.setMtomThreshold(serviceRefSBMD.getMtomThreshold());

      // propagate respect binding properties
      serviceRefUMDM.setRespectBindingAnnotationSpecified(serviceRefSBMD.isRespectBindingFeatureEnabled());
      serviceRefUMDM.setRespectBindingEnabled(serviceRefSBMD.isRespectBindingEnabled());

      // process injection targets
      if (serviceRefSBMD.getInjectionTargets() != null && serviceRefSBMD.getInjectionTargets().size() > 0)
      {
         if (serviceRefSBMD.getInjectionTargets().size() > 1)
         {
            // TODO: We should validate all the injection targets whether they're compatible.
            // This means all the injection targets must be assignable or equivalent.
            // If there are @Addressing, @RespectBinding or @MTOM annotations present on injection targets,
            // these annotations must be equivalent for all the injection targets.
         }
         final InjectionTarget injectionTarget = serviceRefSBMD.getInjectionTargets().iterator().next();

         AccessibleObject anAlement = this.findInjectionTarget(loader, injectionTarget);
         this.processAnnotatedElement(anAlement, serviceRefUMDM);
      }

      // propagate port compoments
      final Collection<? extends PortComponent> portComponentsSBMD = serviceRefSBMD.getPortComponents();
      if (portComponentsSBMD != null)
      {
         for (final PortComponent portComponentSBMD : portComponentsSBMD)
         {
            final UnifiedPortComponentRefMetaData portComponentUMDM = this.getUnifiedPortComponentRefMetaData(
                  serviceRefUMDM, portComponentSBMD);
            if (portComponentUMDM.getServiceEndpointInterface() != null || portComponentUMDM.getPortQName() != null)
            {
               serviceRefUMDM.addPortComponentRef(portComponentUMDM);
            }
            else
            {
               log.warn("Ignoring <port-component-ref> without <service-endpoint-interface> and <port-qname>: "
                     + portComponentUMDM);
            }
         }
      }

      // propagate handlers
      final Collection<Handler> handlersSBMD = serviceRefSBMD.getHandlers();
      if (handlersSBMD != null)
      {
         for (final Handler handlerSBMD : handlersSBMD)
         {
            final UnifiedHandlerMetaData handlerUMDM = this.getUnifiedHandlerMetaData(handlerSBMD);
            serviceRefUMDM.addHandler(handlerUMDM);
         }
      }

      // propagate handler chains
      final List<HandlerChain> handlerChainsSBMD = serviceRefSBMD.getHandlerChains();
      if (handlerChainsSBMD != null)
      {
         final UnifiedHandlerChainsMetaData handlerChainsUMDM = this.getUnifiedHandlerChainsMetaData(handlerChainsSBMD);
         serviceRefUMDM.setHandlerChains(handlerChainsUMDM);
      }

      // propagate jboss specific MD
      if (serviceRefSBMD instanceof JBossServiceRefType)
      {
         this.processUnifiedJBossServiceRefMetaData(serviceRefUMDM, serviceRefSBMD);
      }

      // detect JAXWS or JAXRPC type
      this.processType(serviceRefUMDM);

      return serviceRefUMDM;
   }

   /**
    * Translates jboss service ref switchboard meta data to JBossWS unified service ref meta data.
    * 
    * @param serviceRefUMDM service reference unified meta data
    * @param serviceRefSBMD service reference switchboard meta data
    */
   private void processUnifiedJBossServiceRefMetaData(final UnifiedServiceRefMetaData serviceRefUMDM,
         final ServiceRefType serviceRefSBMD)
   {
      final JBossServiceRefType jbossServiceRefSBMD = (JBossServiceRefType) serviceRefSBMD;
      serviceRefUMDM.setServiceImplClass(jbossServiceRefSBMD.getServiceClass());
      serviceRefUMDM.setConfigName(jbossServiceRefSBMD.getConfigName());
      serviceRefUMDM.setConfigFile(jbossServiceRefSBMD.getConfigFile());
      serviceRefUMDM.setWsdlOverride(jbossServiceRefSBMD.getWsdlOverride());
      serviceRefUMDM.setHandlerChain(jbossServiceRefSBMD.getHandlerChain());
   }

   /**
    * Translates handler chains switchboard meta data to JBossWS unified handler chains meta data.
    * 
    * @param handlerChainsSBMD handler chains switchboard meta data
    * @return handler chains JBossWS unified meta data
    */
   private UnifiedHandlerChainsMetaData getUnifiedHandlerChainsMetaData(final List<HandlerChain> handlerChainsSBMD)
   {
      final UnifiedHandlerChainsMetaData handlerChainsUMDM = new UnifiedHandlerChainsMetaData();

      for (final HandlerChain handlerChainSBMD : handlerChainsSBMD)
      {
         final UnifiedHandlerChainMetaData handlerChainUMDM = new UnifiedHandlerChainMetaData();
         handlerChainUMDM.setServiceNamePattern(handlerChainSBMD.getServiceNamePattern());
         handlerChainUMDM.setPortNamePattern(handlerChainSBMD.getPortNamePattern());
         handlerChainUMDM.setProtocolBindings(handlerChainSBMD.getProtocolBindings());

         final List<Handler> handlersSBMD = handlerChainSBMD.getHandlers();
         for (final Handler handlerSBMD : handlersSBMD)
         {
            final UnifiedHandlerMetaData handlerUMDM = getUnifiedHandlerMetaData(handlerSBMD);
            handlerChainUMDM.addHandler(handlerUMDM);
         }

         handlerChainsUMDM.addHandlerChain(handlerChainUMDM);
      }

      return handlerChainsUMDM;
   }

   /**
    * Translates handler switchboard meta data to JBossWS unified handler meta data.
    * 
    * @param handlerSBMD handler switchboard meta data
    * @return handler JBossWS unified meta data
    */
   private UnifiedHandlerMetaData getUnifiedHandlerMetaData(final Handler handlerSBMD)
   {
      final UnifiedHandlerMetaData handlerUMDM = new UnifiedHandlerMetaData();
      handlerUMDM.setHandlerName(handlerSBMD.getHandlerName());
      handlerUMDM.setHandlerClass(handlerSBMD.getHandlerClass());

      // translate handler init params
      final Map<String, String> handlerInitParamsSBMD = handlerSBMD.getInitParams();
      if (handlerInitParamsSBMD != null)
      {
         for (final String initParamName : handlerInitParamsSBMD.keySet())
         {
            final UnifiedInitParamMetaData handlerInitParamUMDM = new UnifiedInitParamMetaData();
            handlerInitParamUMDM.setParamName(initParamName);
            handlerInitParamUMDM.setParamValue(handlerInitParamsSBMD.get(initParamName));
            handlerUMDM.addInitParam(handlerInitParamUMDM);
         }
      }

      // translate handler soap headers
      final Collection<QName> handlerSoapHeadersSBDM = handlerSBMD.getSoapHeaders();
      if (handlerSoapHeadersSBDM != null)
      {
         for (final QName soapHeader : handlerSoapHeadersSBDM)
         {
            handlerUMDM.addSoapHeader(soapHeader);
         }
      }

      // translate handler soap roles
      final Collection<String> handlerSoapRolesSBMD = handlerSBMD.getSoapRoles();
      if (handlerSoapRolesSBMD != null)
      {
         for (final String soapRole : handlerSoapRolesSBMD)
         {
            handlerUMDM.addSoapRole(soapRole);
         }
      }

      // translate handler port names
      final Collection<String> handlerPortNamesSBMD = handlerSBMD.getPortNames();
      if (handlerPortNamesSBMD != null)
      {
         for (final String portName : handlerPortNamesSBMD)
         {
            handlerUMDM.addPortName(portName);
         }
      }

      return handlerUMDM;
   }

   /**
    * Translates port component ref switchboard meta data to JBossWS unified port component ref meta data.
    * 
    * @param serviceRefUMDM service ref unified meta data
    * @param portComponentSBMD port component ref switchboard meta data
    * @return port component ref unified meta data
    */
   private UnifiedPortComponentRefMetaData getUnifiedPortComponentRefMetaData(
         final UnifiedServiceRefMetaData serviceRefUMDM, final PortComponent portComponentSBMD)
   {
      final UnifiedPortComponentRefMetaData portComponentUMDM = new UnifiedPortComponentRefMetaData(serviceRefUMDM);

      // propagate service endpoint interface
      portComponentUMDM.setServiceEndpointInterface(portComponentSBMD.getEndpointInterface());

      // propagate MTOM properties
      portComponentUMDM.setMtomEnabled(portComponentSBMD.isMtomEnabled());
      portComponentUMDM.setMtomThreshold(portComponentSBMD.getMtomThreshold());

      // propagate addressing properties
      final Addressing addressingSBMD = portComponentSBMD.getAddressing();
      if (addressingSBMD != null)
      {
    	 portComponentUMDM.setAddressingAnnotationSpecified(true);
         portComponentUMDM.setAddressingEnabled(addressingSBMD.isEnabled());
         portComponentUMDM.setAddressingRequired(addressingSBMD.isRequired());
         portComponentUMDM.setAddressingResponses(addressingSBMD.getResponses());
      }

      // propagate respect binding properties
      if (portComponentSBMD.isRespectBindingEnabled())
      {
    	 portComponentUMDM.setRespectBindingAnnotationSpecified(true);
         portComponentUMDM.setRespectBindingEnabled(true);
      }

      // propagate link
      portComponentUMDM.setPortComponentLink(portComponentSBMD.getLink());

      // propagate jboss specific MD
      if (portComponentSBMD instanceof JBossPortComponent)
      {
         this.processUnifiedJBossPortComponentRefMetaData(portComponentUMDM, portComponentSBMD);
      }

      return portComponentUMDM;
   }

   /**
    * Translates jboss port component ref switchboard meta data to JBossWS unified port component ref meta data.
    * 
    * @param portComponentUMDM port component unified meta data
    * @param portComponentSBMD port component switchboard meta data
    */
   private void processUnifiedJBossPortComponentRefMetaData(final UnifiedPortComponentRefMetaData portComponentUMDM,
         final PortComponent portComponentSBMD)
   {
      final JBossPortComponent jbossPortComponentSBMD = (JBossPortComponent) portComponentSBMD;

      // propagate port QName
      portComponentUMDM.setPortQName(jbossPortComponentSBMD.getPortQName());

      // propagate configuration properties
      portComponentUMDM.setConfigName(jbossPortComponentSBMD.getConfigName());
      portComponentUMDM.setConfigFile(jbossPortComponentSBMD.getConfigFile());

      // propagate stub properties
      final Map<String, String> stubPropertiesSBMD = jbossPortComponentSBMD.getStubProperties();
      if (stubPropertiesSBMD != null)
      {
         for (final String propertyName : stubPropertiesSBMD.keySet())
         {
            final UnifiedStubPropertyMetaData stubPropertyUMDM = new UnifiedStubPropertyMetaData();
            stubPropertyUMDM.setPropName(propertyName);
            stubPropertyUMDM.setPropValue(stubPropertiesSBMD.get(propertyName));
            portComponentUMDM.addStubProperty(stubPropertyUMDM);
         }
      }

      // propagate call properties
      final Map<String, String> callPropertiesSBMD = jbossPortComponentSBMD.getCallProperties();
      if (callPropertiesSBMD != null)
      {
         for (final String propertyName : callPropertiesSBMD.keySet())
         {
            final UnifiedCallPropertyMetaData callPropertyUMDM = new UnifiedCallPropertyMetaData();
            callPropertyUMDM.setPropName(propertyName);
            callPropertyUMDM.setPropValue(callPropertiesSBMD.get(propertyName));
            portComponentUMDM.addCallProperty(callPropertyUMDM);
         }
      }
   }

   /**
    * Switchboard service ref resource.
    */
   private static final class ServiceRefResource implements Resource
   {
      private final Referenceable target;

      private ServiceRefResource(final Referenceable target)
      {
         this.target = target;
      }

      @Override
      public Object getDependency()
      {
         return null;
      }

      @Override
      public Object getTarget()
      {
         return this.target;
      }
      
      @Override
      public Collection<?> getInvocationDependencies()
      {
         return null;
      }
   }

   private void processAnnotatedElement(final AnnotatedElement anElement, final UnifiedServiceRefMetaData serviceRefUMDM)
   {
      this.processAddressingAnnotation(anElement, serviceRefUMDM);
      this.processMTOMAnnotation(anElement, serviceRefUMDM);
      this.processRespectBindingAnnotation(anElement, serviceRefUMDM);
      this.processHandlerChainAnnotation(anElement, serviceRefUMDM);
      this.processServiceRefType(anElement, serviceRefUMDM);
   }

   // TODO: use classloader to detect service ref type
   private void processType(final UnifiedServiceRefMetaData serviceRefUMDM)
   {
      final boolean isJAXRPC = serviceRefUMDM.getMappingFile() != null // TODO: is mappingFile check required?
            || "javax.xml.rpc.Service".equals(serviceRefUMDM.getServiceInterface());

      serviceRefUMDM.setType(isJAXRPC ? Type.JAXRPC : Type.JAXWS);
   }

   private void processAddressingAnnotation(final AnnotatedElement anElement,
         final UnifiedServiceRefMetaData serviceRefUMDM)
   {
      final javax.xml.ws.soap.Addressing addressingAnnotation = this.getAnnotation(anElement,
            javax.xml.ws.soap.Addressing.class);

      if (addressingAnnotation != null)
      {
         serviceRefUMDM.setAddressingAnnotationSpecified(true);
         serviceRefUMDM.setAddressingEnabled(addressingAnnotation.enabled());
         serviceRefUMDM.setAddressingRequired(addressingAnnotation.required());
         serviceRefUMDM.setAddressingResponses(addressingAnnotation.responses().toString());
      }
   }

   private void processMTOMAnnotation(final AnnotatedElement anElement, final UnifiedServiceRefMetaData serviceRefUMDM)
   {
      final MTOM mtomAnnotation = this.getAnnotation(anElement, MTOM.class);

      if (mtomAnnotation != null)
      {
         serviceRefUMDM.setMtomAnnotationSpecified(true);
         serviceRefUMDM.setMtomEnabled(mtomAnnotation.enabled());
         serviceRefUMDM.setMtomThreshold(mtomAnnotation.threshold());
      }
   }

   private void processRespectBindingAnnotation(final AnnotatedElement anElement,
         final UnifiedServiceRefMetaData serviceRefUMDM)
   {
      final javax.xml.ws.RespectBinding respectBindingAnnotation = this.getAnnotation(anElement,
            javax.xml.ws.RespectBinding.class);

      if (respectBindingAnnotation != null)
      {
         serviceRefUMDM.setRespectBindingAnnotationSpecified(true);
         serviceRefUMDM.setRespectBindingEnabled(respectBindingAnnotation.enabled());
      }
   }

   private void processServiceRefType(final AnnotatedElement anElement, final UnifiedServiceRefMetaData serviceRefUMDM)
   {
      if (anElement instanceof Field)
      {
         final Class<?> targetClass = ((Field) anElement).getType();
         serviceRefUMDM.setServiceRefType(targetClass.getName());

         if (Service.class.isAssignableFrom(targetClass))
            serviceRefUMDM.setServiceInterface(targetClass.getName());
      }
      else if (anElement instanceof Method)
      {
         final Class<?> targetClass = ((Method) anElement).getParameterTypes()[0];
         serviceRefUMDM.setServiceRefType(targetClass.getName());

         if (Service.class.isAssignableFrom(targetClass))
            serviceRefUMDM.setServiceInterface(targetClass.getName());
      }
      else
      {
         final WebServiceRef serviceRefAnnotation = this.getWebServiceRefAnnotation(anElement, serviceRefUMDM);
         Class<?> targetClass = null;
         if (serviceRefAnnotation != null && (serviceRefAnnotation.type() != Object.class))
         {
            targetClass = serviceRefAnnotation.type();
            serviceRefUMDM.setServiceRefType(targetClass.getName());

            if (Service.class.isAssignableFrom(targetClass))
               serviceRefUMDM.setServiceInterface(targetClass.getName());
         }
      }
   }

   private void processHandlerChainAnnotation(final AnnotatedElement anElement,
         final UnifiedServiceRefMetaData serviceRefUMDM)
   {
      final javax.jws.HandlerChain handlerChainAnnotation = this.getAnnotation(anElement, javax.jws.HandlerChain.class);

      if (handlerChainAnnotation != null)
      {
         // Set the handlerChain from @HandlerChain on the annotated element
         String handlerChain = null;
         if (handlerChainAnnotation.file().length() > 0)
            handlerChain = handlerChainAnnotation.file();

         // Resolve path to handler chain
         if (handlerChain != null)
         {
            try
            {
               new URL(handlerChain);
            }
            catch (MalformedURLException ignored)
            {
               final Class<?> declaringClass = getDeclaringClass(anElement);

               handlerChain = declaringClass.getPackage().getName().replace('.', '/') + "/" + handlerChain;
            }

            serviceRefUMDM.setHandlerChain(handlerChain);
         }
      }
   }

   private Class<?> getDeclaringClass(final AnnotatedElement annotatedElement)
   {
      Class<?> declaringClass = null;
      if (annotatedElement instanceof Field)
         declaringClass = ((Field) annotatedElement).getDeclaringClass();
      else if (annotatedElement instanceof Method)
         declaringClass = ((Method) annotatedElement).getDeclaringClass();
      else if (annotatedElement instanceof Class)
         declaringClass = (Class<?>) annotatedElement;

      return declaringClass;
   }

   private <T extends Annotation> T getAnnotation(final AnnotatedElement anElement, final Class<T> annotationClass)
   {
      return anElement != null ? (T) anElement.getAnnotation(annotationClass) : null;
   }

   private WebServiceRef getWebServiceRefAnnotation(final AnnotatedElement anElement,
         final UnifiedServiceRefMetaData serviceRefUMDM)
   {
      final WebServiceRef webServiceRefAnnotation = this.getAnnotation(anElement, WebServiceRef.class);
      final WebServiceRefs webServiceRefsAnnotation = this.getAnnotation(anElement, WebServiceRefs.class);

      if (webServiceRefAnnotation == null && webServiceRefsAnnotation == null)
      {
         return null;
      }

      // Build the list of @WebServiceRef relevant annotations
      final List<WebServiceRef> wsrefList = new ArrayList<WebServiceRef>();

      if (webServiceRefAnnotation != null)
      {
         wsrefList.add(webServiceRefAnnotation);
      }

      if (webServiceRefsAnnotation != null)
      {
         for (final WebServiceRef webServiceRefAnn : webServiceRefsAnnotation.value())
         {
            wsrefList.add(webServiceRefAnn);
         }
      }

      // Return effective @WebServiceRef annotation
      WebServiceRef returnValue = null;
      if (wsrefList.size() == 1)
      {
         returnValue = wsrefList.get(0);
      }
      else
      {
         for (WebServiceRef webServiceRefAnn : wsrefList)
         {
            if (serviceRefUMDM.getServiceRefName().endsWith(webServiceRefAnn.name()))
            {
               returnValue = webServiceRefAnn;
               break;
            }
         }
      }

      return returnValue;
   }

   private AccessibleObject findInjectionTarget(ClassLoader loader, InjectionTarget target)
   {
      Class<?> clazz = null;
      try
      {
         clazz = loader.loadClass(target.getTargetClass());
      }
      catch (ClassNotFoundException e)
      {
         throw new RuntimeException("<injection-target> class: " + target.getTargetClass()
               + " was not found in deployment");
      }

      for (Field field : clazz.getDeclaredFields())
      {
         if (target.getTargetName().equals(field.getName()))
            return field;
      }

      for (Method method : clazz.getDeclaredMethods())
      {
         if (method.getName().equals(target.getTargetName()))
            return method;
      }

      throw new RuntimeException("<injection-target> could not be found: " + target.getTargetClass() + "."
            + target.getTargetName());

   }
}
