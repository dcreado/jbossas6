/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.test.cluster.ejb3.clusteredsession.ejbthree1136;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.aop.microcontainer.aspects.jmx.JMX;
import org.jboss.ha.ispn.CacheContainerRegistry;

/**
 * MBean that stores a key/value in a cache during start
 * and then allows a caller to check if the value is still there.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1.1 $
 */
@JMX(name = "jboss.test:service=Ejb3SFSBCacheManipulator", exposedInterface = SFSBCacheManipulatorMBean.class)
public class SFSBCacheManipulator implements SFSBCacheManipulatorMBean
{
   public static final String KEY = "key";
   public static final String VALUE = "value";
   
   private CacheContainerRegistry registry;
   private String cacheContainerName;
   private String cacheName;
   
   public void setCacheContainerRegistry(CacheContainerRegistry registry)
   {
      this.registry = registry;
   }

   public void setCacheContainerName(String cacheContainerName)
   {
      this.cacheContainerName = cacheContainerName;
   }

   public void setCacheName(String cacheName)
   {
      this.cacheName = cacheName;
   }

   @Override
   public String lookup()
   {
      Cache<String, String> cache = this.registry.getCacheContainer(this.cacheContainerName).getCache(this.cacheName);
      String value = cache.get(KEY);
      cache.evict(KEY);
      cache.stop();
      return value;
   }

   public void start()
   {
      Cache<String, String> cache = this.registry.getCacheContainer(this.cacheContainerName).getCache(this.cacheName);
      cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL).put(KEY, VALUE);
      cache.evict(KEY);
   }
   
   public void stop()
   {
      Cache<String, String> cache = this.registry.getCacheContainer(this.cacheContainerName).getCache(this.cacheName);
      cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL).remove(KEY);
      cache.stop();
   }
}
