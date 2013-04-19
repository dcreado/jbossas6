/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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
package org.jboss.webservices.integration.injection;

import java.util.Collection;

import javax.xml.ws.WebServiceContext;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.switchboard.javaee.jboss.environment.JBossResourceEnvRefType;
import org.jboss.switchboard.mc.spi.MCBasedResourceProvider;
import org.jboss.switchboard.spi.Resource;
import org.jboss.wsf.common.injection.ThreadLocalAwareWebServiceContext;

/**
 * WebServiceContext resource provider.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WebServiceContextResourceProvider implements MCBasedResourceProvider<JBossResourceEnvRefType>
{

   @Override
   public Resource provide(final DeploymentUnit unit, final JBossResourceEnvRefType resEnvRef)
   {
      return new WebServiceContextResource(ThreadLocalAwareWebServiceContext.getInstance());
   }

   @Override
   public Class<JBossResourceEnvRefType> getEnvironmentEntryType()
   {
      return JBossResourceEnvRefType.class;
   }

   /**
    * Switchboard web service context resource.
    */
   private static final class WebServiceContextResource implements Resource
   {
      private final WebServiceContext target;

      private WebServiceContextResource(final WebServiceContext target)
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
}
