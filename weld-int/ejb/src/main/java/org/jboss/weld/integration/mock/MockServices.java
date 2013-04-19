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

package org.jboss.weld.integration.mock;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import org.jboss.beans.metadata.spi.factory.BeanFactory;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.integration.vdf.DeploymentUnitAware;

/**
 * Mock Weld services, in case we need to disable some real service.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class MockServices implements IMockServices, Serializable, BeanFactory, DeploymentUnitAware, InvocationHandler
{
   private static final long serialVersionUID = 1L;

   private List<Class<?>> interfaces;
   private volatile Object proxy;

   public MockServices(Class<?>... interfaces)
   {
      if (interfaces == null || interfaces.length == 0)
         throw new IllegalArgumentException("Null or empty interfaces");

      this.interfaces = new ArrayList<Class<?>>();
      this.interfaces.addAll(Arrays.asList(interfaces));
      this.interfaces.add(IMockServices.class);
      this.interfaces.add(Serializable.class);
      this.interfaces.add(DeploymentUnitAware.class);
   }

   /**
    * Create proxy, exposing all interfaces from ctor.
    *
    * @return proxy
    */
   public Object createBean()
   {
      if (proxy == null)
      {
         proxy = Proxy.newProxyInstance(
               MockServices.class.getClassLoader(),
               interfaces.toArray(new Class<?>[interfaces.size()]),
               this);
      }
      return proxy;
   }

   public void setDeploymentUnit(DeploymentUnit unit)
   {
      // ignore
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      // invoke Object and impl mocks on this mock instance
      Class<?> declaringClass = method.getDeclaringClass();
      if (declaringClass.equals(Object.class) || declaringClass.equals(IMockServices.class))
         return method.invoke(this, args);

      if (Map.class.isAssignableFrom(method.getReturnType()))
         return Collections.emptyMap();
      if (Set.class.isAssignableFrom(method.getReturnType()))
         return Collections.emptySet();
      if (Collection.class.isAssignableFrom(method.getReturnType()))
         return Collections.emptyList();
      else
         return null; // TODO -- mock return values as well?
   }

   public String toString()
   {
      return "Weld-Mock-Services::" + proxy;
   }

   // -- impl details

   public Collection getEjbs()
   {
      return Collections.emptyList();
   }

   public Collection getEjbContainerNames()
   {
      return Collections.emptyList();
   }
}
