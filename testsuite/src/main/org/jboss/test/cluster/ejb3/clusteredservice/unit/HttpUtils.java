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

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.lang.System;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.TraceMethod;
import org.jboss.logging.Logger;
import org.jboss.test.JBossTestUtil;

/** Utilities for client http requests
 *
 * @author Scott.Stark@jboss.org
 * @version $Revision: 109975 $
 */
public class HttpUtils
{
   private static Logger log = Logger.getLogger(HttpUtils.class);
   private static String baseURL = "http://jduke:theduke@" + getServerHostForURL() + ":" + Integer.getInteger("web.port", 8080) + "/";
   private static String baseURLNoAuth = "http://" + getServerHostForURL() + ":" + Integer.getInteger("web.port", 8080) + "/";

   public static final int GET = 1;
   public static final int POST = 2;
   public static final int HEAD = 3;
   public static final int OPTIONS = 4;
   public static final int PUT = 5;
   public static final int DELETE = 6;
   public static final int TRACE = 7;
   
   // JBAS-8540
   private static String getServerHost() {
	   String hostname = System.getProperty("jboss.bind.address", "localhost") ;
	   if (log.isDebugEnabled())
		   log.debug("getServerHost(): using hostname = " + hostname) ;
	   return hostname;
   }
   
   // JBAS-8540
   private static String getServerHostForURL()
   {
      String hostname = getServerHost() ;
      
      if (hostname == null)
    	  return hostname;	
      
      String hostnameForURL = JBossTestUtil.fixHostnameForURL(hostname) ;
      if (log.isDebugEnabled())
		   log.debug("getServerHostForURL(): using hostname for url = " + hostnameForURL) ;
      
      return hostnameForURL;
   }      
   
   public static String getBaseURL()
   {
      return baseURL;
   }
   public static String getBaseURL(String username, String password)
   {
      String url = "http://"+username+":"+password+"@"+ getServerHostForURL() + ":"
         + Integer.getInteger("web.port", 8080) + "/";
      return url;
   }
   public static String getBaseURLNoAuth()
   {
      return baseURLNoAuth;
   }

   /** Perform a get on the indicated URL and assert an HTTP_OK response code
    *
    * @param url
    * @return int http response code
    * @throws Exception on any failure
    */
   public static int accessURL(URL url) throws Exception
   {
      HttpClient httpConn = new HttpClient();
      HttpMethodBase request = createMethod(url, GET);

      int hdrCount = null != null ? ((Header[]) null).length : 0;
      for(int n = 0; n < hdrCount; n ++)
         request.addRequestHeader(((Header[]) null)[n]);
      try
      {
         log.info("Connecting to: "+ url);
         String userInfo = url.getUserInfo();

         if( userInfo != null )
         {
            UsernamePasswordCredentials auth = new UsernamePasswordCredentials(userInfo);
            httpConn.getState().setCredentials("JBossTest Servlets", url.getHost(), auth);
         }
         log.info("RequestURI: "+request.getURI());
         int responseCode = httpConn.executeMethod(request);
         String response = request.getStatusText();
         log.info("responseCode="+responseCode+", response="+response);
         String content = request.getResponseBodyAsString();
         log.info(content);
         return responseCode;
         // Validate that we are seeing the requested response code
      }
      catch(IOException e)
      {
         throw e;
      }

   }

   public static HttpMethodBase accessURL(URL url, String realm,
      int expectedHttpCode, int type)
      throws Exception
   {
      HttpClient httpConn = new HttpClient();
      HttpMethodBase request = createMethod(url, type);

      int hdrCount = null != null ? ((Header[]) null).length : 0;
      for(int n = 0; n < hdrCount; n ++)
         request.addRequestHeader(((Header[]) null)[n]);
      try
      {
         log.info("Connecting to: "+ url);
         String userInfo = url.getUserInfo();

         if( userInfo != null )
         {
            UsernamePasswordCredentials auth = new UsernamePasswordCredentials(userInfo);
            httpConn.getState().setCredentials(realm, url.getHost(), auth);
         }
         log.info("RequestURI: "+request.getURI());
         int responseCode = httpConn.executeMethod(request);
         String response = request.getStatusText();
         log.info("responseCode="+responseCode+", response="+response);
         String content = request.getResponseBodyAsString();
         log.info(content);
         // Validate that we are seeing the requested response code
         if( responseCode != expectedHttpCode)
         {
            throw new IOException("Expected reply code:"+ expectedHttpCode
               +", actual="+responseCode);
         }
      }
      catch(IOException e)
      {
         throw e;
      }
      return request;
   }

   public static HttpMethodBase createMethod(URL url, int type)
   {
      HttpMethodBase request = null;
      switch( type )
      {
         case GET:
            request = new GetMethod(url.toString());
            break;
         case POST:
            request = new PostMethod(url.toString());
            break;
         case HEAD:
            request = new HeadMethod(url.toString());
            break;
         case OPTIONS:
            request = new OptionsMethod(url.toString());
            break;
         case PUT:
            request = new PutMethod(url.toString());
            break;
         case DELETE:
            request = new DeleteMethod(url.toString());
            break;
         case TRACE:
            request = new TraceMethod(url.toString());
            break;
      }
      return request;
   }
}
