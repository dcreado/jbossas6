/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc. and individual contributors
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

package org.jboss.test.web.test;

import java.net.URL;

import org.jboss.test.JBossTestCase;
import org.jboss.test.util.web.HttpUtils;

/**
 * Simple test that accessing the "on demand" webapps works.
 * 
 * This test is meant to be executed with on-demand deployment of webapps enabled
 * and disabled.
 *
 * @author Brian Stansberry
 * 
 * @version $Revision: 100830 $
 */
public class BasicOnDemandWarTestCase extends JBossTestCase
{
   private String baseURL = HttpUtils.getBaseURLNoAuth();
   
   public BasicOnDemandWarTestCase(String name)
   {
      super(name);
   }

   public void testAdminConsole() throws Exception
   {
      onDemandWarTest("admin-console");
   }
   
   public void testJmxConsole() throws Exception
   {
      onDemandWarTest("jmx-console");
   }
   
   public void testJBossWSConsole() throws Exception
   {
      onDemandWarTest("jbossws");
   }
   
   private void onDemandWarTest(String contextName) throws Exception
   {
      URL url = new URL(baseURL + contextName);
      HttpUtils.accessURL(url);
   }
}
