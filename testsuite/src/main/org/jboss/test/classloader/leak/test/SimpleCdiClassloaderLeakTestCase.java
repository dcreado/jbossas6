/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc. and/or its affiliates, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.test.classloader.leak.test;

import junit.framework.Test;

/**
 * <p>
 * Test for classloader leaks following deployment, use and undeployment of a
 * simple war using Weld.
 * <p/>
 * <p>
 * If these tests are run with JBoss Profiler's jbossAgent (.dll or .so) on the
 * path and the AS is started with -agentlib:jbossAgent, in case of classloader
 * leakage an extensive report will be logged to the server log, showing the
 * path to root of all references to the classloader.
 * </p>
 * 
 * @author David Allen
 */
public class SimpleCdiClassloaderLeakTestCase extends J2EEClassloaderLeakTestBase
{

   private static final String   SIMPLE_CDI_WAR = "classloader-leak-simple-cdi.war";
   private static final String   WELD           = "WELD";
   private static final String[] CLASS_LOADERS  = { SERVLET, SERVLET_TCCL, WELD };

   public SimpleCdiClassloaderLeakTestCase(String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(SimpleCdiClassloaderLeakTestCase.class, "classloader-leak-test.sar");
   }

   public void testSimpleCdiWar() throws Exception
   {
      // Ensure we are starting with a clean slate
      checkCleanKeys(CLASS_LOADERS);

      deployComponent(SIMPLE_CDI_WAR);

      makeWebRequest(baseURL + "IntrospectCDISession", WEBAPP);
      makeWebRequest(baseURL + "InvalidateCDISession", WEBAPP);

      // Make sure the expected registrations were done
      checkKeyRegistration(CLASS_LOADERS);

      // This sleep is a workaround to JBAS-4060
      sleep(500);

      undeployComponent(SIMPLE_CDI_WAR, true);

      // TODO - probably not needed anymore; remove
      flushSecurityCache("HsqlDbRealm");

      sleep(500);

      // Confirm the classloaders were released
      String unregistered = checkClassLoaderRelease(CLASS_LOADERS);

      if (unregistered.trim().length() > 0)
      {
         fail("Classloaders unregistered: " + unregistered);
      }
   }
}
