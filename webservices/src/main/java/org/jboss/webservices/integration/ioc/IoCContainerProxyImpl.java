/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.webservices.integration.ioc;

import org.jboss.dependency.spi.ControllerContext;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.wsf.spi.ioc.IoCContainerProxy;

/**
 * @see org.jboss.wsf.spi.ioc.IoCContainerProxy
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class IoCContainerProxyImpl implements IoCContainerProxy
{
   /** Singleton. */
   private static final IoCContainerProxy SINGLETON = new IoCContainerProxyImpl();
   /** JBoss MC kernel. */
   private static Kernel kernel;

   /**
    * Constructor.
    */
   public IoCContainerProxyImpl()
   {
      super();
   }

   /**
    * Returns container proxy instance.
    *
    * @return container proxy instance
    */
   static IoCContainerProxy getInstance()
   {
      return IoCContainerProxyImpl.SINGLETON;
   }

   /**
    * Sets JBoss kernel - invoked via MC injection.
    *
    * @param kernel JBoss kernel
    */
   public void setKernel(final Kernel kernel)
   {
      IoCContainerProxyImpl.kernel = kernel;
   }

   /**
    * @see org.jboss.wsf.spi.ioc.IoCContainerProxy#getBean(java.lang.String, java.lang.Class)
    *
    * @param <T> bean type
    * @param beanName bean name inside IoC registry
    * @param clazz bean type class
    * @return bean instance
    * @throws IllegalArgumentException if bean is not found 
    */
   @SuppressWarnings("unchecked")
   public <T> T getBean(final String beanName, final Class<T> clazz)
   {
      final KernelController controller = IoCContainerProxyImpl.kernel.getController();
      final ControllerContext ctx = controller.getInstalledContext(beanName);

      return (T)ctx.getTarget();
   }
}
