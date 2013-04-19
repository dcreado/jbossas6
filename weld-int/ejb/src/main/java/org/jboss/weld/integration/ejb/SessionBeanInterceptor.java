/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.integration.ejb;

import java.io.Serializable;
import java.lang.reflect.Field;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.interceptor.InvocationContext;

import org.jboss.ejb3.BeanContext;
import org.jboss.ejb3.Container;
import org.jboss.ejb3.EJBContextImpl;
import org.jboss.ejb3.Ejb3Registry;
import org.jboss.ejb3.stateful.StatefulSessionContextImpl;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.manager.api.WeldManager;


/**
 * Interceptor for handling EJB post-construct tasks
 *
 * @author Pete Muir
 * @author Marius Bogoevici
 */
public class SessionBeanInterceptor implements Serializable
{
   private static final long serialVersionUID = 7327757031821596782L;

   private static final Field statefulGuidField;
   private static final Field beanContextField;

   static
   {
      try
      {
         statefulGuidField = StatefulSessionContextImpl.class.getDeclaredField("containerGuid");
         statefulGuidField.setAccessible(true);
         beanContextField = EJBContextImpl.class.getDeclaredField("beanContext");
         beanContextField.setAccessible(true);
      }
      catch (SecurityException e)
      {
         throw new RuntimeException(e);
      }
      catch (NoSuchFieldException e)
      {
         throw new RuntimeException(e);
      }
   }

   @Resource
   private EJBContext ejbContext;

   @Resource(mappedName="java:comp/BeanManager")
   private WeldManager beanManager;

   private CreationalContext<Object> creationalContext;

   public static final String CREATIONAL_CONTEXT = "org.jboss.weld.integration.ejb.SessionBeanInterceptor.creationalContext";
   public static final String EJB_DESCRIPTOR = "org.jboss.weld.integration.ejb.SessionBeanInterceptor.ejbName";

   /**
    * Gets the underlying target and calls the post-construct method
    *
    * @param invocationContext The invocation context
    * @throws Exception
    */
   @PostConstruct
   public void postConstruct(InvocationContext invocationContext) throws Exception
   {
      String ejbName = getEjbName();
      EjbDescriptor<Object> descriptor = beanManager.getEjbDescriptor(ejbName);
      InjectionTarget<Object> injectionTarget = beanManager.createInjectionTarget(descriptor);
      Bean<Object> bean = beanManager.getBean(descriptor);
      creationalContext = beanManager.createCreationalContext(bean);
      injectionTarget.inject(invocationContext.getTarget(), creationalContext);
      invocationContext.getContextData().put(CREATIONAL_CONTEXT, creationalContext);
      invocationContext.getContextData().put(EJB_DESCRIPTOR, descriptor);
      invocationContext.proceed();
   }

   /**
    * Gets the underlying target and calls the pre-destroy method
    *
    * @param invocationContext The invocation context
    * @throws Exception
    */
   @PreDestroy
   public void preDestroy(InvocationContext invocationContext) throws Exception
   {
      creationalContext.release();
      invocationContext.proceed();
   }

   private String getEjbName()
   {
      if (ejbContext instanceof StatefulSessionContextImpl)
      {
         try
         {

            String guid = (String) statefulGuidField.get(ejbContext);
            Container container = Ejb3Registry.getContainer(guid);
            return container.getEjbName();
         }
         catch (IllegalArgumentException e)
         {
            throw new RuntimeException(e);
         }
         catch (IllegalAccessException e)
         {
            throw new RuntimeException(e);
         }
      }
      else if (ejbContext instanceof EJBContextImpl<?, ?>)
      {
         try
         {
            BeanContext<?> beanContext = (BeanContext<?>) beanContextField.get(ejbContext);
            return beanContext.getContainer().getEjbName();
         }
         catch (IllegalArgumentException e)
         {
            throw new RuntimeException(e);
         }
         catch (IllegalAccessException e)
         {
            throw new RuntimeException(e);
         }
      }
      else
      {
         throw new IllegalStateException("Unable to extract ejb name from EJBContext " + ejbContext);
      }

   }

}