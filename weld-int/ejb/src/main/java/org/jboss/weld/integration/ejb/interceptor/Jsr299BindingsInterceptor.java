/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.weld.integration.ejb.interceptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.jboss.weld.ejb.spi.helpers.ForwardingEjbServices;
import org.jboss.weld.integration.ejb.JBossEjbServices;
import org.jboss.weld.integration.ejb.SessionBeanInterceptor;
import org.jboss.weld.manager.api.WeldManager;
import org.jboss.weld.serialization.spi.ContextualStore;
import org.jboss.weld.serialization.spi.helpers.SerializableContextualInstance;

/**
 * Interceptor for applying the JSR-299 specific interceptor bindings.
 *
 * It is a separate interceptor, as it needs to be applied after all
 * the other existing interceptors.
 *
 * @author Marius Bogoevici
 */
public class Jsr299BindingsInterceptor implements Serializable
{

   private static final long serialVersionUID = -1999613731498564948L;

   @Resource(mappedName="java:comp/BeanManager")
   private WeldManager beanManager;

   private Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> interceptorInstances;

   private String ejbName;

   @PostConstruct
   public void doPostConstruct(InvocationContext invocationContext) throws Exception
   {
      init(invocationContext);
      doLifecycleInterception(invocationContext, InterceptionType.POST_CONSTRUCT);
   }

   private  void init(InvocationContext invocationContext)
   {
      // create contextual instances for inteDITrceptors
      interceptorInstances = new ConcurrentHashMap<String, SerializableContextualInstance<Interceptor<Object>, Object>>();
      ejbName = ((EjbDescriptor<?>) invocationContext.getContextData().get(SessionBeanInterceptor.EJB_DESCRIPTOR)).getEjbName();
      InterceptorBindings interceptorBindings = getInterceptorBindings(ejbName);
      if (interceptorBindings != null)
      {
         for (Interceptor<?> interceptor : interceptorBindings.getAllInterceptors())
         {
            addInterceptorInstance((Interceptor<Object>)interceptor, invocationContext);
         }

      }
   }

   private InterceptorBindings getInterceptorBindings(String ejbName)
   {
      EjbServices ejbServices = beanManager.getServices().get(EjbServices.class);
      if (ejbServices instanceof ForwardingEjbServices)
      {
         ejbServices = ((ForwardingEjbServices)ejbServices).delegate();
      }
      InterceptorBindings interceptorBindings = null;
      if (ejbServices instanceof JBossEjbServices)
      {
          interceptorBindings = ((JBossEjbServices)ejbServices).getInterceptorBindings(ejbName);
      }
      return interceptorBindings;
   }

   @SuppressWarnings("unchecked")
   private void addInterceptorInstance(Interceptor<Object> interceptor, InvocationContext invocationContext)
   {
      CreationalContext<Object> creationalContext = (CreationalContext<Object>) invocationContext.getContextData().get(SessionBeanInterceptor.CREATIONAL_CONTEXT);
      Object instance = beanManager.getContext(interceptor.getScope()).get(interceptor, creationalContext);
      SerializableContextualInstance<Interceptor<Object>,Object> serializableContextualInstance
            = beanManager.getServices().get(ContextualStore.class).<Interceptor<Object>,Object>getSerializableContextualInstance(interceptor, instance, creationalContext);
      interceptorInstances.put(interceptor.getBeanClass().getName(), serializableContextualInstance);
   }

   @PreDestroy
   public void doPreDestroy(InvocationContext invocationContext) throws Exception
   {
      doLifecycleInterception(invocationContext, InterceptionType.PRE_DESTROY);
   }

   @AroundInvoke
   public Object doAroundInvoke(InvocationContext invocationContext) throws Exception
   {
      return doMethodInterception(invocationContext, InterceptionType.AROUND_INVOKE);
   }

   private void doLifecycleInterception(InvocationContext invocationContext, InterceptionType interceptionType)
         throws Exception
   {
      InterceptorBindings interceptorBindings = getInterceptorBindings(ejbName);
      if (interceptorBindings != null)
      {
         List<Interceptor<?>> currentInterceptors = interceptorBindings.getLifecycleInterceptors(interceptionType);
         delegateInterception(invocationContext, interceptionType, currentInterceptors);
      }
      else
      {
         invocationContext.proceed();
      }
   }

   private Object doMethodInterception(InvocationContext invocationContext, InterceptionType interceptionType)
         throws Exception
   {
      InterceptorBindings interceptorBindings = getInterceptorBindings(ejbName);
      if (interceptorBindings != null)
      {
         List<Interceptor<?>> currentInterceptors = interceptorBindings.getMethodInterceptors(interceptionType, invocationContext.getMethod());
         return delegateInterception(invocationContext, interceptionType, currentInterceptors);
      }
      else
      {
         return invocationContext.proceed();
      }
   }

   private Object delegateInterception(InvocationContext invocationContext, InterceptionType interceptionType, List<Interceptor<?>> currentInterceptors)
         throws Exception
   {
      List<Object> currentInterceptorInstances = new ArrayList<Object>();
      for (Interceptor<?> interceptor: currentInterceptors)
      {
         currentInterceptorInstances.add(interceptorInstances.get(interceptor.getBeanClass().getName()).getInstance());
      }
      if (currentInterceptorInstances.size() > 0)
      {
         return new DelegatingInterceptorInvocationContext(invocationContext, currentInterceptors, currentInterceptorInstances, interceptionType).proceed();
      }
      else
      {
         return invocationContext.proceed();
      }

   }
}