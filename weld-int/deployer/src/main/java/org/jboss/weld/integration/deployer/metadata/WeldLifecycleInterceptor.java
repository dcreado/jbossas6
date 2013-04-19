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

package org.jboss.weld.integration.deployer.metadata;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.jboss.weld.ejb.SessionBeanInterceptor;

/**
 * Handle all Weld SFSB invocations, including lifecycle.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class WeldLifecycleInterceptor extends SessionBeanInterceptor
{
   private static final long serialVersionUID = 1L;

   @PrePassivate
   public void prePassivate(InvocationContext invocationContext) throws Exception
   {
      aroundInvoke(invocationContext);
   }

   @PostActivate
   public void lifecycleCallback(InvocationContext invocation) throws Exception
   {
      aroundInvoke(invocation);
   }

   @AroundInvoke
   public Object doAroundInvoke(InvocationContext invocation) throws Exception {
      return super.aroundInvoke(invocation);
   }
}
