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
package org.jboss.test.deployers.support;

import java.util.Collections;

import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.ejb.api.SessionObjectReference;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.jboss.weld.injection.spi.EjbInjectionServices;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class MockEjbInjectionServices implements EjbInjectionServices
{
   public void setDeploymentUnit(DeploymentUnit unit)
   {
   }

   public Object resolveEjb(InjectionPoint injectionPoint)
   {
      return null;
   }

   public Object resolvePersistenceContext(InjectionPoint injectionPoint)
   {
      return null;
   }

   public Object resolveResource(InjectionPoint injectionPoint)
   {
      return null;
   }

   public void removeEjb(Object instance)
   {
   }

   public SessionObjectReference resolveEjb(EjbDescriptor<?> descriptor)
   {
      return null;
   }

   public void registerInterceptors(EjbDescriptor<?> ejbDescriptor, InterceptorBindings interceptorBindings)
   {
      // do nothing
   }

   public Iterable<EjbDescriptor<?>> getEjbs()
   {
      return Collections.emptySet();
   }

   public Object resolveRemoteEjb(String jndiName, String mappedName, String ejbLink)
   {
      return null;
   }

   public Iterable<String> getEjbContainerNames()
   {
      return Collections.emptySet();
   }
   
   
   public void cleanup() {}

}