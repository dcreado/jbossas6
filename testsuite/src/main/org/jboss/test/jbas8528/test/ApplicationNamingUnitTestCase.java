/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.jbas8528.test;

import junit.framework.Test;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jboss.test.JBossTestCase;
import org.jboss.test.util.web.HttpUtils;

/**
 * Tests that a deployment with application.xml with an explicitly specified
 * application-name, uses that name instead of the .ear file name for naming 
 * deployments.
 * 
 * @see https://jira.jboss.org/browse/JBAS-8528
 * 
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class ApplicationNamingUnitTestCase extends JBossTestCase
{

   private HttpClient client = new HttpClient();

   public ApplicationNamingUnitTestCase(String name)
   {
      super(name);
   }

   /**
    * Test that the application deploys successfully and is accessible.
    * 
    * @throws Exception
    */
   public void testApplicationDeployment() throws Exception
   {
      // make sure there wasn't a deployment failure
      serverFound(); // silly, method name!
      
      // now access the jsp and test the return value
      String url = HttpUtils.getBaseURL() + "/jbas8528-webapp/index.jsp";
      String response = this.getResponse(url);
      assertTrue(response.equals("JBAS-8528 Application"));
   }

   

   /**
    *  Access the passed URL and return back the response 
    */
   private String getResponse(String url) throws Exception
   {
      HttpMethodBase request = new GetMethod(url);
      client.executeMethod(request);

      String responseBody = request.getResponseBodyAsString();
      if (responseBody == null)
      {
         throw new Exception("Unable to get response from server for URL: " + url);
      }

      return responseBody;
   }

   /**
    * Deploy a .sar and .ear file, both having the same name, but the .ear has an
    * explicitly specified value for application-name in application.xml (to avoid
    * naming conflicts).
    *  
    * @return
    * @throws Exception
    */
   public static Test suite() throws Exception
   {
      return getDeploySetup(ApplicationNamingUnitTestCase.class, "jbas8528.ear,jbas8528.sar");
   }
}
