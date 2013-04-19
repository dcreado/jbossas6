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

import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.spi.management.ContextStateMapper;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.kernel.spi.registry.KernelBus;
import org.jboss.kernel.spi.registry.KernelRegistryEntry;
import org.jboss.metatype.api.values.MetaValueFactory;
import org.jboss.profileservice.plugins.management.util.AbstractManagedComponentRuntimeDispatcher;

/**
 * Microcontainer KernelBus runtime component dispatcher.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 * @author Scott.Stark@jboss.org
 * @version $Revision: 104372 $
 */
public class KernelBusRuntimeComponentDispatcher extends AbstractManagedComponentRuntimeDispatcher
{
   
   private KernelBus bus;
   private Kernel kernel;

   public KernelBusRuntimeComponentDispatcher(Kernel kernel)
   {
      this(kernel, null);
   }

   public KernelBusRuntimeComponentDispatcher(Kernel kernel, MetaValueFactory valueFactory)
   {
      if (kernel == null)
         throw new IllegalArgumentException("Null kernel");

      this.kernel = kernel;
      this.bus = kernel.getBus();
   }

   /**
    * Check kernel and bus.
    */
   public void start()
   {
      if (kernel == null)
         throw new IllegalArgumentException("Null kernel");
      if (bus == null)
         throw new IllegalArgumentException("Null kernel bus");
   }

   /**
    * {@inheritDoc}
    */
   protected Object get(Object name, String getter) throws Throwable
   {
      return bus.get(name, getter);
   }

   /**
    * {@inheritDoc}
    */
   protected Object invoke(Object name, String methodName, Object[] parameters, String[] signature) throws Throwable
   {
      return bus.invoke(name, methodName, parameters, signature);
   }

   /**
    * {@inheritDoc}
    */
   protected void set(Object name, String setter, Object value) throws Throwable
   {
      bus.set(name, setter, value);
   }

   public String getState(Object name)
   {
      KernelController controller = kernel.getController();
      ControllerContext context = controller.getContext(name, null);
      if (context == null)
         throw new IllegalStateException("Context not installed: " + name);

      ControllerState state = context.getState();
      return state.getStateString();
   }
   
   public <T extends Enum<?>> T mapControllerState(Object name, ContextStateMapper<T> mapper)
   {
      if(name == null)
         throw new IllegalArgumentException("null name");
      if(mapper == null)
         throw new IllegalArgumentException("null mapper");

      KernelController controller = kernel.getController();
      ControllerContext context = controller.getContext(name, null);
      if (context == null)
         throw new IllegalStateException("Context not installed: " + name);
      
      ControllerState requiredState = null;
      // FIXME
      if(context instanceof KernelRegistryEntry == false)
         requiredState = context.getRequiredState();
      return mapper.map(context.getState().getStateString(), requiredState != null ? requiredState.getStateString() : null);
   }

}
