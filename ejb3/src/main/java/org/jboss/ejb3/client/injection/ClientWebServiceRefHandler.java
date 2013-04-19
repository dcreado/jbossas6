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
package org.jboss.ejb3.client.injection;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;



import org.jboss.ejb3.client.JndiPropertyInjector;
import org.jboss.injection.EncInjector;
import org.jboss.injection.InjectionContainer;
import org.jboss.injection.InjectionHandler;
import org.jboss.injection.InjectionUtil;
import org.jboss.injection.Injector;
import org.jboss.injection.WebServiceRefHandler;
import org.jboss.injection.lang.reflect.BeanProperty;
import org.jboss.injection.lang.reflect.BeanPropertyFactory;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;
import org.jboss.metadata.javaee.spec.ResourceInjectionTargetMetaData;
import org.jboss.metadata.javaee.spec.ServiceReferenceMetaData;
import org.jboss.metadata.javaee.spec.ServiceReferencesMetaData;

/**
 * Handles service-ref and {@link WebServiceRef} on classes processed 
 * by the application client container.
 * 
 * Unlike the {@link WebServiceRefHandler}, this class does not create
 * {@link EncInjector}s for the service-ref(s), since the ENC injection
 * should happen on the server. This class just creates the field/method
 * {@link Injector}s for the service-ref(s)
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class ClientWebServiceRefHandler<X extends RemoteEnvironment> implements InjectionHandler<X>
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(ClientWebServiceRefHandler.class);
   
   /**
    * Service references
    */
   private Map<String, ServiceReferenceMetaData> srefMap = new HashMap<String, ServiceReferenceMetaData>();

   @Override
   public void handleClassAnnotations(Class<?> clazz, InjectionContainer container)
   {
      throw new IllegalStateException("Annotations are not handled in the client");

   }

   @Override
   public void handleFieldAnnotations(Field field, InjectionContainer container,
         Map<AccessibleObject, Injector> injectors)
   {
      throw new IllegalStateException("Annotations are not handled in the client");

   }

   @Override
   public void handleMethodAnnotations(Method method, InjectionContainer container,
         Map<AccessibleObject, Injector> injectors)
   {
      throw new IllegalStateException("Annotations are not handled in the client");

   }

   /**
    * {@inheritDoc}
    * 
    * Note that this method does not create {@link EncInjector}s since the ENC injectors
    * should be created on server side. This method just creates field/method {@link Injector}s for
    * the service-ref(s)
    */
   @Override
   public void loadXml(X xml, InjectionContainer container)
   {
      if (xml == null)
      {
         return;
      }
      ServiceReferencesMetaData serviceRefs = xml.getServiceReferences();
      if (serviceRefs == null)
         return;
      for (ServiceReferenceMetaData sref : serviceRefs)
      {
         if (this.srefMap.get(sref.getServiceRefName()) != null)
         {
            throw new IllegalStateException("Duplicate <service-ref-name> in " + sref);
         }

         srefMap.put(sref.getServiceRefName(), sref);

         String encName = "env/" + sref.getServiceRefName();
         AnnotatedElement annotatedElement = sref.getAnnotatedElement();
         if (annotatedElement == null)
         {
            if (sref.getInjectionTargets() != null && sref.getInjectionTargets().size() > 0)
            {
               for (ResourceInjectionTargetMetaData trg : sref.getInjectionTargets())
               {
                  annotatedElement = InjectionUtil.findInjectionTarget(container.getClassloader(), trg);
                  addInjector(container, encName, annotatedElement);
               }
            }
            else
            {
               logger.warn("No injection target for service-ref: " + sref.getServiceRefName());
            }
         }
         // annotated classes do not specify injection target
         else if (!(annotatedElement instanceof java.lang.reflect.Type))
         {
            addInjector(container, encName, annotatedElement);
         }
      }

   }
   
   /**
    * Create and add a {@link Injector} into the <code>container</code>, for the passed <code>encName</code> 
    * and <code>annotatedElement</code>
    * 
    * @param container The container to which the injector will be added
    * @param encName The ENC name
    * @param annotatedElement The annotated field/method
    */
   private void addInjector(InjectionContainer container, String encName, AnnotatedElement annotatedElement)
   {
      
      Injector jndiInjector;
      if(annotatedElement instanceof Method)
      {
         BeanProperty prop = BeanPropertyFactory.create((Method) annotatedElement);
         jndiInjector = new JndiPropertyInjector(prop, encName, container.getEnc());
      }
      else if(annotatedElement instanceof Field)
      {
         BeanProperty prop = BeanPropertyFactory.create((Field) annotatedElement);
         jndiInjector = new JndiPropertyInjector(prop, encName, container.getEnc());
      }
      else
      {
         throw new IllegalStateException("Annotated element for '" + encName + "' is niether Method nor Field: " + annotatedElement);
      }
      // add the injector
      container.getInjectors().add(jndiInjector);
   }


}
