/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import junit.framework.Test;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jboss.test.JBossTestCase;
import org.jboss.test.util.web.HttpUtils;

import java.io.IOException;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class NamingServletUnitTestCase extends JBossTestCase
{
   private final HttpClient client = new HttpClient();
   
   public NamingServletUnitTestCase(String name)
   {
      super(name);
   }

   protected String get(String spec) throws IOException
   {
      HttpMethodBase request = new GetMethod(HttpUtils.getBaseURL() + spec);
      client.executeMethod(request);

      String responseBody = request.getResponseBodyAsString();
      request.releaseConnection();
      return responseBody;
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(NamingServletUnitTestCase.class, "naming.war");
   }

   /**
    * Servlet 3.0 15.2.3
    */
   public void testAppContextRoot() throws Exception
   {
      // since this is a standalone deployment, the module functions as an app (EE.5.2.2)
      String result = get("/naming/servlet?name=java:app/AppName");
      assertEquals("naming", result);
   }

   /**
    * Servlet 3.0 15.2.3
    */
   public void testGlobalContextRoot() throws Exception
   {
      // since this is a standalone deployment, no app-name is needed
      String result = get("/naming/servlet?name=java:global/naming");
      assertNotNull(result);
   }

   /**
    * JavaEE 6 EE.5.15
    */
   public void testModuleName() throws Exception
   {
      String result = get("/naming/servlet?name=java:module/ModuleName");
      assertEquals("naming", result);
   }

   /**
    * JavaEE 6 EE.5.2.2
    * In a web module, java:comp refers to the same namespace as java:module.
    */
   public void testModuleNameViaComp() throws Exception
   {
      // given the above knowledge we could also do this
      String result = get("/naming/servlet?name=java:comp/ModuleName");
      assertEquals("naming", result);
   }
}
