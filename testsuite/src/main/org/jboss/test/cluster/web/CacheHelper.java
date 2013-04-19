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
package org.jboss.test.cluster.web;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.ha.ispn.CacheContainerRegistry;
import org.jboss.ha.ispn.atomic.AtomicMapCache;
import org.jboss.system.ServiceMBeanSupport;
import org.jboss.web.tomcat.service.session.distributedcache.ispn.SessionMapEntry;
import org.jboss.web.tomcat.service.sso.ispn.CredentialKey;
import org.jboss.web.tomcat.service.sso.ispn.SSOKey;

/**
 * Helper class to locate and invoke methods on the cache mbeans used by JBossWeb.
 *
 * TODO. Update the DistributedCacheManager SPI to provide the data we use
 * here and use a factory to create the SPI impl rather than directly accessing
 * the cache.
 * 
 * @author Ben Wang  Date: Aug 16, 2005
 * @author Brian Stansberry
 * 
 * @version $Id: CacheHelper.java 109050 2010-11-01 17:27:24Z pferraro $
 */
public class CacheHelper 
   extends ServiceMBeanSupport
   implements CacheHelperMBean
{
   public static final String CACHE_CONFIG_PROP = "jbosstest.cluster.web.cache.config";
   
   public static final String OBJECT_NAME = "jboss.test:service=WebTestCacheHelper";
   
   private final CacheContainer container;
   private final String cacheName;
   
   public CacheHelper(CacheContainerRegistry registry, String config)
   {
      String containerName = config;
      String cacheName = null;
      
      String[] parts = config.split("/");
      if (parts.length == 2)
      {
         containerName = parts[0];
         cacheName = parts[1];
      }
      
      this.container = registry.getCacheContainer(containerName);
      this.cacheName = cacheName;
   }
   
   @Override
   public boolean isDistributed()
   {
      EmbeddedCacheManager container = (EmbeddedCacheManager) this.container;
      Configuration config = (this.cacheName != null) ? container.defineConfiguration(this.cacheName, new Configuration()) : container.getDefaultConfiguration();
      return config.getCacheMode().isDistributed();
   }
   
   /**
    * {@inheritDoc}
    * @see org.jboss.test.cluster.web.CacheHelperMBean#clear()
    */
   @Override
   public void clear(String webapp)
   {
      this.container.getCache(webapp).getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL).clear();
   }

   @Override
   public Integer getSessionVersion(String webapp, String sessionId)
   {
      Cache<String, Map<Object, Object>> cache = this.container.getCache(webapp);
      Map<Object, Object> map = new AtomicMapCache<String, Object, Object>(cache).get(sessionId);
      return (map != null) ? SessionMapEntry.VERSION.<Integer>get(map) : null;
   }
   
   @Override
   public Integer getBuddySessionVersion(String webapp, String sessionId) throws Exception
   {
      return this.getSessionVersion(webapp, sessionId);
   }
   
   @Override
   public Set<String> getSessionIds(String webapp) throws Exception
   {
      Cache<Object, Map<Object, Object>> cache = this.container.getCache(webapp);
      Set<String> sessions = new HashSet<String>();
      for (Object key: cache.keySet())
      {
         if (key instanceof String)
         {
            sessions.add((String) key);
         }
      }
      return sessions;
   }
   
   @Override
   public Set<String> getSessionIds(String webapp, boolean includeBuddies) throws Exception
   {
      return this.getSessionIds(webapp);
   }
   
   @Override
   public Set<String> getSSOIds() throws Exception
   {
      Cache<SSOKey, ?> cache = (this.cacheName != null) ? this.container.<SSOKey, Object>getCache(this.cacheName) : this.container.<SSOKey, Object>getCache();
      Set<String> result = new HashSet<String>();
      
      for (SSOKey key: cache.keySet())
      {
         if (key instanceof CredentialKey)
         {
            result.add(key.getId());
         }
      }
      return result;
   }
   
   @Override
   public boolean getCacheHasSSO(String ssoId) throws Exception
   {
      Cache<SSOKey, ?> cache = (this.cacheName != null) ? this.container.<SSOKey, Object>getCache(this.cacheName) : this.container.<SSOKey, Object>getCache();
      return cache.containsKey(ssoId);
   }
}
