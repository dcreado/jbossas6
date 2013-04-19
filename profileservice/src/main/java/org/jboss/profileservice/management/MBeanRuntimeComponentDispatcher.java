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
package org.jboss.profileservice.management;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.spi.management.ContextStateMapper;
import org.jboss.profileservice.plugins.management.util.AbstractManagedComponentRuntimeDispatcher;

/**
 * MBean runtime component dispatcher.
 *
 * @author Jason T. Greene
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 * @author Scott.Stark@jboss.org
 */
public class MBeanRuntimeComponentDispatcher extends AbstractManagedComponentRuntimeDispatcher
{
   
   /** The MBeanServer. */
   private final MBeanServer mbeanServer;

   public MBeanRuntimeComponentDispatcher(MBeanServer mbeanServer)
   {
      if(mbeanServer ==  null)
      {
         throw new IllegalStateException("null mbean server");
      }
      this.mbeanServer = mbeanServer;
   }

   /**
    * {@inheritDoc}
    */
   protected Object get(Object name, String getter) throws Throwable
   {
      return mbeanServer.getAttribute(new ObjectName(name.toString()), getter);
   }

   /**
    * {@inheritDoc}
    */
   protected void set(Object name, String setter, Object value) throws Throwable
   {
      mbeanServer.setAttribute(new ObjectName(name.toString()), new Attribute(setter, value));   
   }

   /**
    * {@inheritDoc}
    */
   protected Object invoke(Object name, String methodName, Object[] parameters, String[] signature) throws Throwable
   {
      return mbeanServer.invoke(new ObjectName(name.toString()), methodName, parameters, signature);
   }
 
   public String getState(Object name)
   {
      try
      {
         if (mbeanServer.isRegistered(new ObjectName(name.toString())))
            return ControllerState.INSTALLED.getStateString();
      }
      catch (Exception e)
      {
         // Failure = Not installed
      }
      
      return ControllerState.NOT_INSTALLED.getStateString();
   }

   public <T extends Enum<?>> T mapControllerState(Object name, ContextStateMapper<T> mapper)
   {
      if(name == null)
         throw new IllegalArgumentException("null name");
      if(mapper == null)
         throw new IllegalArgumentException("null mapper");

      ControllerState current = ControllerState.NOT_INSTALLED;
      
      try
      {
         if (mbeanServer.isRegistered(new ObjectName(name.toString())))
            current = ControllerState.INSTALLED;
      }
      catch (Exception e)
      {
         // Failure = Not installed
      }
      
      return mapper.map(current.getStateString(), ControllerState.INSTALLED.getStateString());
   }
   
}
