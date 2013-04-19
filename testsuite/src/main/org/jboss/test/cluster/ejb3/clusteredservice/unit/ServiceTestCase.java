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
package org.jboss.test.cluster.ejb3.clusteredservice.unit;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.management.MBeanServerConnection;

import junit.framework.Test;

import junit.framework.TestSuite;
import org.jboss.test.JBossClusteredTestCase;

/** 
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 * @version <tt>$Revision: 108925 $</tt>
 */
public class ServiceTestCase extends JBossClusteredTestCase
{
   private static final String USE_JBOSS = "UseJBossWebLoader";
   
   public ServiceTestCase(String name)
   {
      super(name);
   }
   
   public void testEJBServlet() throws Exception
   {
      String serverUrls[] = getHttpURLs();
      log.info("serverUrls[0] = " + serverUrls[0]);  // http://127.0.0.2:8080

      String node1URL = serverUrls[0];
      String node2URL = serverUrls[1];
      node1URL = addUserInfo(node1URL, "jduke:theduke@");
      node2URL = addUserInfo(node2URL, "jduke:theduke@");
      URL url=null;
      try
      {
         url = new URL(node1URL+"/clusteredservice/EJBServlet");
         log.info("about to access " + url);
         assertEquals(HttpURLConnection.HTTP_OK,HttpUtils.accessURL(url));
         assertEquals(HttpURLConnection.HTTP_OK,HttpUtils.accessURL(url));
      } catch (Exception e)
      {
         log.error("error accessing url = " + url, e);
      }

      try
      {
         url = new URL(node2URL+"/clusteredservice/EJBServlet");
         log.info("about to access " + url);
         assertEquals(HttpURLConnection.HTTP_OK,HttpUtils.accessURL(url));
         assertEquals(HttpURLConnection.HTTP_OK,HttpUtils.accessURL(url));
      } catch (Exception e)
      {
         log.error("error accessing url = " + url, e);
      }
   }

   private String addUserInfo(String url, String userinfo)
   {
      String result = url;
      // url will look like http://127.0.0.2:8080
      // change to http://jduke:theduke@127.0.0.2:8080
      if(url.startsWith("http://"))
      {
         result = "http://" + userinfo + url.substring("http://".length());
         log.info("addUserInfo url=" + result);
      }
      return result;

   }

   /**
    * Setup the test suite.
    */
   public static Test suite() throws Exception
   {
      TestSuite suite = new TestSuite();
      suite.addTest(new ServiceTestCase("testEJBServlet"));
       return JBossClusteredTestCase.getDeploySetup(suite, "clusteredservice.war clusteredserviceejb.jar");
   }

    // NOTE: these variables must be static as apparently a separate instance
   // of this class is created for each test
   private static boolean deployed0 = false;
   private static boolean deployed1 = false;

   private MBeanServerConnection[] adaptors = null;
   protected void setUp() throws Exception
   {
      super.setUp();

      log.debug("deployed0 = " + deployed0);
      log.debug("deployed1 = " + deployed1);

      adaptors = getAdaptors();
      if (!deployed0)
      {
         deploy(adaptors[0], "clusteredserviceejb.jar");
         deploy(adaptors[0], "clusteredservice.war");
         deployed0 = true;
      }

      if (!deployed1)
      {
         deploy(adaptors[1], "clusteredserviceejb.jar");
         deploy(adaptors[1], "clusteredservice.war");
         deployed1 = true;
      }
   }

   protected void tearDown() throws Exception
   {
      super.tearDown();

      log.debug("deployed0 = " + deployed0);
      log.debug("deployed1 = " + deployed1);
   }
   
}
