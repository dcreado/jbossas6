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
package org.jboss.webservices.integration.weld;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.weld.integration.injection.NonContextualObjectInjectionHelper;
import org.jboss.weld.manager.api.WeldManager;
import org.jboss.wsf.common.invocation.AbstractInvocationHandlerJSE;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.invocation.Invocation;
import org.jboss.wsf.spi.invocation.InvocationHandler;

/**
 * Weld invocation handler.
 *
 * @author <a href="maito:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public final class WeldInvocationHandler extends AbstractInvocationHandlerJSE
{

   private static final String BEAN_MANAGER_JNDI_NAME = "java:comp/BeanManager";

   private final InvocationHandler delegate;

   public WeldInvocationHandler(final InvocationHandler delegate)
   {
      this.delegate = delegate;
   }

   @Override
   public void onEndpointInstantiated(final Endpoint endpoint, final Invocation invocation) throws Exception
   {
      // handle Weld injections first
      this.handleWeldInjection(invocation.getInvocationContext().getTargetBean());
      // handle JBossWS injections last and call @PostConstruct annotated methods
      this.delegate.onEndpointInstantiated(endpoint, invocation);
   }

   @Override
   public void onBeforeInvocation(final Invocation invocation) throws Exception
   {
      this.delegate.onBeforeInvocation(invocation);
   }

   @Override
   public void onAfterInvocation(final Invocation invocation) throws Exception
   {
      this.delegate.onAfterInvocation(invocation);
   }

   /**
    * Handles weld injection.
    *
    * @param instance to operate upon
    */
   private void handleWeldInjection(final Object instance)
   {
       try {
           WeldManager beanManager = (WeldManager) new InitialContext().lookup(BEAN_MANAGER_JNDI_NAME);
           NonContextualObjectInjectionHelper.injectNonContextualInstance(instance, beanManager);
       } catch (NamingException e) {
           throw new IllegalStateException("Unable to locate BeanManager");
       }
   }

}
