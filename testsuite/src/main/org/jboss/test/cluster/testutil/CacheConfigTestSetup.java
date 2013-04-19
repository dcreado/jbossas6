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

package org.jboss.test.cluster.testutil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.infinispan.manager.CacheContainer;
import org.jboss.test.JBossTestSetup;

/**
 * @author Brian Stansberry
 *
 */
public class CacheConfigTestSetup extends JBossTestSetup
{
   public static Test getTestSetup(Class<?> clazz, final CacheContainer[] cacheContainers, final boolean local, final String passivationDir, final Boolean totalReplication) throws Exception
   {
      final TestSuite suite = new TestSuite();
      suite.addTest(new TestSuite(clazz));
      return new CacheConfigTestSetup(suite, cacheContainers, local, passivationDir, totalReplication);
   }
   
   private CacheContainer[] cacheContainers;
   private String passivationDir;
   private boolean local;
   private Boolean totalReplication;
   private final List<String> cleanupPaths = new ArrayList<String>();
   
   /**
    * Create a new CacheConfigTestSetup.
    * 
    * @param test
    * @throws Exception
    */
   public CacheConfigTestSetup(Test test, CacheContainer[] cacheContainers, boolean local, String passivationDir, Boolean totalReplication) throws Exception
   {
      super(test);
      this.cacheContainers = cacheContainers;
      this.passivationDir = passivationDir;
      this.local = local;
      this.totalReplication = totalReplication;
   }
   
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      if (cacheContainers == null) return;
      JGroupsSystemPropertySupport jgSupport = new JGroupsSystemPropertySupport();
      
      try
      {
         jgSupport.setUpProperties();
         
         if (passivationDir != null)
         {
            File base = new File(passivationDir);
            if (!base.exists())
            {
               if (!base.mkdir())
               {
                  throw new RuntimeException("Cannot create base passivation dir " + passivationDir);
               }

               cleanupPaths.add(base.getAbsolutePath());
            }
         }
         long now = System.currentTimeMillis();
         boolean totalReplication = (this.totalReplication != null) ? this.totalReplication.booleanValue() : !System.getProperty(SessionTestUtil.CACHE_CONFIG_PROP, "standard-session-cache").endsWith("dist");
         for (int i = 0; i < cacheContainers.length; i++)
         {            
            String cacheStore = (passivationDir == null ? null : new File(passivationDir, String.valueOf( now + i)).getAbsolutePath());
            if (cacheStore != null)
            {
               cleanupPaths.add(0, cacheStore);
            }
            cacheContainers[i] = SessionTestUtil.createCacheContainer(local, cacheStore, totalReplication, true);
            cacheContainers[i].start();
            // Request default cache, thereby making sure channel is started
            cacheContainers[i].getCache();
         }
      }
      finally
      {
         jgSupport.restoreProperties();
      }
      
      // wait a few seconds so that the cluster stabilize
      synchronized (this)
      {
         wait(2000);
      }
   }

   @Override
   protected void tearDown() throws Exception
   {
      for (CacheContainer cacheContainer: cacheContainers)
      {
         try
         {
            cacheContainer.stop();
         }
         catch (Exception e)
         {
            e.printStackTrace(System.err);
         }
      }
      
      for (String path : cleanupPaths)
      {
         SessionTestUtil.cleanFilesystem(path);
      }
      super.tearDown();
   }
}
