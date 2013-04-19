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
package org.jboss.test.cluster.web;

import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * @author Paul Ferraro
 *
 */
public class CacheHelperClient implements CacheHelperMBean
{
   private final ObjectName name;
   private final MBeanServerConnection server;
   
   public CacheHelperClient(MBeanServerConnection server)
   {
      this.server = server;
      
      try
      {
         this.name = ObjectName.getInstance(CacheHelper.OBJECT_NAME);
      }
      catch (MalformedObjectNameException e)
      {
         throw new IllegalStateException(e);
      }
   }
   
   /**
    * {@inheritDoc}
    * @see org.jboss.test.cluster.web.CacheHelperMBean#isDistributed()
    */
   @Override
   public boolean isDistributed()
   {
      return this.getAttribute("Distributed", Boolean.class).booleanValue();
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.test.cluster.web.CacheHelperMBean#clear(java.lang.String)
    */
   @Override
   public void clear(String webapp)
   {
      this.invoke("clear", Void.class, webapp);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.test.cluster.web.CacheHelperMBean#getSessionVersion(java.lang.String, java.lang.String)
    */
   @Override
   public Integer getSessionVersion(String webapp, String sessionId)
   {
      return this.invoke("getSessionVersion", Integer.class, webapp, sessionId);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.test.cluster.web.CacheHelperMBean#getBuddySessionVersion(java.lang.String, java.lang.String)
    */
   @Override
   public Integer getBuddySessionVersion(String webapp, String sessionId) throws Exception
   {
      return this.invoke("getBuddySessionVersion", Integer.class, webapp, sessionId);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.test.cluster.web.CacheHelperMBean#getSessionIds(java.lang.String)
    */
   @SuppressWarnings("unchecked")
   @Override
   public Set<String> getSessionIds(String webapp) throws Exception
   {
      return this.invoke("getSessionIds", Set.class, webapp);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.test.cluster.web.CacheHelperMBean#getSessionIds(java.lang.String, boolean)
    */
   @SuppressWarnings("unchecked")
   @Override
   public Set<String> getSessionIds(String webapp, boolean includeBuddies) throws Exception
   {
      return this.invoke("getSessionIds", Set.class, webapp, includeBuddies);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.test.cluster.web.CacheHelperMBean#getSSOIds()
    */
   @SuppressWarnings("unchecked")
   @Override
   public Set<String> getSSOIds() throws Exception
   {
      return this.getAttribute("SSOIds", Set.class);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.test.cluster.web.CacheHelperMBean#getCacheHasSSO(java.lang.String)
    */
   @Override
   public boolean getCacheHasSSO(String ssoId) throws Exception
   {
      return this.invoke("getCacheHasSSO", Boolean.class, ssoId).booleanValue();
   }
   
   private <T> T invoke(String method, Class<T> targetClass, Object... args)
   {
      String[] types = new String[args.length];
      for (int i = 0; i < args.length; ++i)
      {
         types[i] = args[i].getClass().getName();
      }
      try
      {
         Object value = this.server.invoke(this.name, method, args, types);
         return (value != null) ? targetClass.cast(value) : null;
      }
      catch (Exception e)
      {
         throw new IllegalStateException(e);
      }
   }
   
   private <T> T getAttribute(String attribute, Class<T> targetClass)
   {
      try
      {
         Object result = this.server.getAttribute(this.name, attribute);
         return (result != null) ? targetClass.cast(result) : null;
      }
      catch (Exception e)
      {
         throw new IllegalStateException(e);
      }
   }
}
