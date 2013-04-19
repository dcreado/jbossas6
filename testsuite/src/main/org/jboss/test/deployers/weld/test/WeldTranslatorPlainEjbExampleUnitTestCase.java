/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.deployers.weld.test;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

/**
 * Same as WeldTranslatorExampleUnitTestCase, with the difference that this
 * test deploys an ear whose ejb jar file is a non-weld archive
 * (i.e, it lacks the META-INF/beans.xml).
 * This example asserts that Deployment.loadBeanDeploymentArchive works
 * correctly in such scenario.
 * 
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 *
 */
public class WeldTranslatorPlainEjbExampleUnitTestCase extends WeldExampleTest
{
   public WeldTranslatorPlainEjbExampleUnitTestCase(String test)
   {
      super(test);
      setTestExpected(true);
   }

    @Override
    protected URL getBaseURL() throws Exception {
        return new URL(getBaseURLString() + getWebContextName() + "/home.jsf");
    }
    
    @Override
    protected Set<String> getExpectedDeployments(String topLevelDeployment, String exampleName)
    {
       final Set<String> expected = new HashSet<String>();
       expected.add(topLevelDeployment);
       expected.add(String.format(exampleJar, "translator-without-beans"));
       expected.add(String.format(exampleWar, "translator"));
       return expected;
    }

    public static Test suite() throws Exception
   {
      return deploy(WeldTranslatorPlainEjbExampleUnitTestCase.class, false);
   }
}

