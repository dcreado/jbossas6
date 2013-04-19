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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Test;

import org.jboss.test.AbstractTestDelegate;
import org.jboss.test.JBossTestClusteredServices;
import org.jboss.test.cluster.testutil.DelegatingClusteredTestCase;
import org.jboss.test.cluster.testutil.SessionTestUtil;
import org.jboss.test.cluster.testutil.TestSetupDelegate;

/**
 * @author Brian Stansberry
 *
 */
public class JBossClusteredWebTestCase extends DelegatingClusteredTestCase
{

   public static AbstractTestDelegate getDelegate(Class<?> clazz)
       throws Exception
   {
       return new JBossTestClusteredServices(clazz);
   }
   
   public JBossClusteredWebTestCase(String name)
   {
      super(name);
   } 

   public static Test getDeploySetup(Test test, String jarNames)
       throws Exception
   {
      return getDeploySetup(test, jarNames, null);
   }

   public static Test getDeploySetup(Class<?> clazz, String jarNames)
       throws Exception
   {
       return getDeploySetup(clazz, jarNames, null);
   }

   public static Test getDeploySetup(Test test, String jarNames, List<TestSetupDelegate> delegates)
       throws Exception
   {
      return DelegatingClusteredTestCase.getDeploySetup(test, getJarNamesWithHelper(jarNames), ensureJBCConfigDelegate(delegates));
   }

   public static Test getDeploySetup(Class<?> clazz, String jarNames, List<TestSetupDelegate> delegates)
       throws Exception
   {
       return DelegatingClusteredTestCase.getDeploySetup(clazz, getJarNamesWithHelper(jarNames), ensureJBCConfigDelegate(delegates));
   }
   
   private static List<TestSetupDelegate> ensureJBCConfigDelegate(List<TestSetupDelegate> delegates)
   {
      List<TestSetupDelegate> result = delegates;
      if (result == null)
      {
         result = Arrays.asList(new TestSetupDelegate[]{new JBossCacheConfigTestSetupDelegate()});
      }
      else
      {
         boolean hasJBCConfig = false;
         for (TestSetupDelegate setup : delegates)
         {
            if (setup instanceof JBossCacheConfigTestSetupDelegate)
            {
               hasJBCConfig = true;
               break;
            }
         }
         
         if (!hasJBCConfig)
         {
            result = new ArrayList<TestSetupDelegate>(delegates);
            result.add(0, new JBossCacheConfigTestSetupDelegate());
         }
      }
      
      return result;
   }
   
   
   private static String getJarNamesWithHelper(String jarNames)
   {
      if (jarNames == null || jarNames.length() == 0)
         return "jbosscache-helper.sar";
      else
         return "jbosscache-helper.sar, " + jarNames;
   }

}
