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
package org.jboss.as.ejb3.switchboard.resource.provider;

import java.util.Collection;

import javax.ejb.TimerService;
import javax.naming.LinkRef;

import org.jboss.switchboard.spi.Resource;

/**
 * {@link Resource} for {@link TimerService} reference
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class TimerServiceResource implements Resource
{

   /**
    * The name of MC bean responsible for binding the internal (java:internal/TimerService)
    * jndi name
    */
   private String mcBeanName;
   
   public TimerServiceResource(String mcBeanDependencyName)
   {
      this.mcBeanName = mcBeanDependencyName;
   }
   
   @Override
   public Object getDependency()
   {
      return this.mcBeanName;
   }

   @Override
   public Object getTarget()
   {
      return new LinkRef("java:internal/TimerService");
   }

   @Override
   public Collection<?> getInvocationDependencies()
   {
      return null;
   }
}
