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
package org.jboss.ejb;

import javax.ejb.TimerService;
import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.invocation.Invocation;

import java.util.Map;

/**
 * MBean interface.
 */
public interface ContainerMBean extends org.jboss.system.ServiceMBean
{

  /**
   * Gets the deployment name of the deployment unit that contains this container.
   */
  String getDeploymentName();

  /**
   * Gets the type of bean (Session, Entity, etc)
   *
   * @return type of bean
   */
  String getBeanTypeName();

   /**
    * @return Gets the application deployment unit for this container. All the bean containers within the same application unit share the same instance.
    */
   EjbModule getEjbModule();

   /**
    * @return Gets the number of create invocations that have been made
    */
   long getCreateCount();

  /**
   * Converts the method invocation stats into a detyped nested map structure.
   * The format is:
   *
   * {methodName => {statisticTypeName => longValue}}
   *
   * @return A map indexed by method name with map values indexed by statistic type
   */
  Map<String, Map<String, Long>> getDetypedInvocationStatistics();

  /**
   * Get current pool size of the pool associated with this container,
   * also known as the method ready count
   *
   * @throws UnsupportedOperationException if the container type does not support an instance pool
   */
  int getCurrentPoolSize();

  /**
   * Gets the max pool size of the pool associated with this container
   *
   * @throws UnsupportedOperationException if the container type does not support an instance pool
   */
  int getMaxPoolSize();

  /**
   * Resets the current invocation stats
   */
  void resetInvocationStats();

   /**
    * @return Gets the number of remove invocations that have been made
    */
   long getRemoveCount();

   /**
    * @return Gets the invocation statistics collection
    */
   org.jboss.invocation.InvocationStatistics getInvokeStats();

   /**
    * Get the components environment context
    * @return Environment Context
    * @throws NamingException for any error
    */
   Context getEnvContext() throws NamingException;

   /**
    * Returns the metadata of this container.
    * @return metaData;
    */
   org.jboss.metadata.BeanMetaData getBeanMetaData();

   /**
    * Creates the single Timer Servic for this container if not already created
    * @param pKey Bean id
    * @return Container Timer Service
    * @throws IllegalStateException If the type of EJB is not allowed to use the timer service
    * @see javax.ejb.EJBContext#getTimerService
    */
   TimerService getTimerService(Object pKey) throws IllegalStateException;

   /**
    * Removes Timer Servic for this container
    * @param pKey Bean id
    * @throws IllegalStateException If the type of EJB is not allowed to use the timer service
    */
   void removeTimerService(Object pKey) throws IllegalStateException;

   /**
    * The detached invoker operation.
    * @param mi - the method invocation context
    * @return the value of the ejb invocation
    * @throws Exception on error    */
   Object invoke(Invocation mi) throws Exception;
}
