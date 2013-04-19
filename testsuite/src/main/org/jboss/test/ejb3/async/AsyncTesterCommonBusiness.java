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
package org.jboss.test.ejb3.async;

import java.util.concurrent.Future;

/**
 * Business view of a bean which tests @Asynchronous calls
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 */
public interface AsyncTesterCommonBusiness
{
   // --------------------------------------------------------------------------------||
   // Contracts ----------------------------------------------------------------------||
   // --------------------------------------------------------------------------------||

   /**
    * Returns whether or not a local invocation is executed in a new Thread
    */
   boolean isLocalAsyncInvocationExecutedInNewThread();

   /**
    * Returns whether or not a remote invocation is executed in a new Thread
    */
   boolean isRemoteAsyncInvocationExecutedInNewThread();

   /**
    * Returns whether or not a remote invocation is executed in a new Thread
    */
   boolean isNointerfaceAsyncInvocationExecutedInNewThread();

    /**
     * Returns whether a Future return type on a non-Asynchronous
     * method is executed within the same Thread
     * @return
     */
   boolean isLocalBlockingInvocationExecutedInSameThread();

    /**
     * 
     * @return
     */
   boolean cancelInterruptIfRunningWorksLocal();

    /**
     * 
     * @return
     */
   boolean cancelInterruptIfRunningWorksRemote();
}