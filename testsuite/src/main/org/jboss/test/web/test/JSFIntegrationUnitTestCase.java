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
package org.jboss.test.web.test;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.test.JBossTestCase;
import org.jboss.test.JBossTestSetup;
import org.jboss.test.util.web.HttpUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.SubmitButton;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/** Tests of JSF integration into the JBoss server. This test
 requires than a web container and JSF implementation be integrated 
 into the JBoss server. The tests currently do NOT use the 
 java.net.HttpURLConnection and associated http client and these do 
 not return valid HTTP error codes so if a failure occurs it is best 
 to connect the webserver using a browser to look for additional error
 info. 
 
 @author Stan.Silvert@jboss.org
 @version $Revision: 104943 $
 */
public class JSFIntegrationUnitTestCase extends JBossTestCase
{

   public JSFIntegrationUnitTestCase(String name)
   {
      super(name);
   }
   
   /** Access the http://localhost/jbosstest-jsf/index.jsf.
    */
   public void testJSFIntegrated() throws Exception
   {
      String responseBody = getResponseBody("jbosstest-jsf");

      assertTrue(responseBody.contains("@PostConstruct was called."));
      assertTrue(responseBody.contains("@PreDestroy was called."));
      assertTrue(responseBody.contains("Datasource was injected."));

      // Tests JSF/JSTL integration
      assertTrue(responseBody.contains("number one"));
      assertTrue(responseBody.contains("number two"));
      assertTrue(responseBody.contains("number three"));

      // Tests enum support 
      assertTrue(responseBody.contains("JBoss Color selection is PURPLE"));

   }   

   public void testJSFAppWithBundledMyFaces() throws Exception
   {
      String baseURL = HttpUtils.getBaseURL();
      WebConversation webConversation = new WebConversation();
      
      // Initial JSF request
      WebRequest req = new GetMethodWebRequest(baseURL + "bundled-myfaces-hellojsf/index.faces");
      WebResponse webResponse = webConversation.getResponse(req);
      assertTrue(webResponse.getText().contains("Enter your name"));

      // submit data
      WebForm form = webResponse.getFormWithID("form1");
      form.setParameter("form1:input_foo_text", "Stan");
      SubmitButton submitButton = form.getSubmitButtonWithID("form1:submit_button");
      webResponse = form.submit(submitButton);
      assertTrue(webResponse.getText().contains("Hello Stan"));
   }
   
   public void testBeanValidationIntegratedWithJSF() throws Exception
   {
      WebConversation wc = new WebConversation();
      String url = makeRequestString("jbosstest-jsf", "/beanvalidation.jsf");
      WebResponse response = wc.getResponse(url);
      WebForm form = response.getFormWithID("form1");
      form.setParameter("form1:input_name", "a");
      SubmitButton submitButton = form.getSubmitButtonWithID("form1:submit_button");
      response = form.submit(submitButton);
      assertTrue(response.getText().contains("size must be between 2 and"));
   }

   private String getResponseBody(String warName) throws Exception
   {
      HttpClient client = new HttpClient();
      client.executeMethod(makeRequest(warName));
      
      HttpMethodBase result = makeRequest(warName);

      // need to hit it twice with the same session for test to pass
      client.executeMethod(result);

      String responseBody = result.getResponseBodyAsString();
      if (responseBody == null) {
         throw new Exception("Unable to get response from server.");
      }
      
      return responseBody;
   }
   
   // makes default GetMethod request for index.jsf
   private GetMethod makeRequest(String warName) 
   {
      return new GetMethod(makeRequestString(warName, "index.jsf"));
   }
   
   private String makeRequestString(String warName, String jsfPage)
   {
      String baseURL = HttpUtils.getBaseURL();
      return  baseURL + warName + "/" + jsfPage;
   }

   public static Test suite() throws Exception
   {
      TestSuite suite = new TestSuite();
      suite.addTest(new TestSuite(JSFIntegrationUnitTestCase.class));

      // Create an initializer for the test suite
      Test wrapper = new JBossTestSetup(suite)
      {
         protected void setUp() throws Exception
         {
            super.setUp();
            deploy("jbosstest-jsf.war");   
            deploy("bundled-myfaces-hellojsf.war");
         }
         protected void tearDown() throws Exception
         {
            undeploy("bundled-myfaces-hellojsf.war");
            undeploy("jbosstest-jsf.war");
            super.tearDown();            
         }
      };
      return wrapper;
   }
   

}
