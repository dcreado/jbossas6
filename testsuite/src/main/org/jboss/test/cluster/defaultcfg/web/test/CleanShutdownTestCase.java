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
package org.jboss.test.cluster.defaultcfg.web.test;

import java.net.InetAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import junit.framework.Assert;
import junit.framework.Test;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.jboss.test.JBossClusteredTestCase;

/**
 * A CleanShutdownTestCase.
 * 
 * @author Paul Ferraro
 */
public class CleanShutdownTestCase extends JBossClusteredTestCase
{
   private static final String SERVER_NAME = "jboss.web.deployment:war=/http-sr";
   private static final String SHUTDOWN_METHOD = "stop";
   private static final String URL = "%s/http-sr/sleep.jsp?sleep=%d";
   private static final String COOKIE_NAME = "sleep";
   private static final int MAX_THREADS = 2;
   private static final int REQUEST_DURATION = 10000;
   
   ObjectName name;
   HttpClient client;
   
   private MultiThreadedHttpConnectionManager manager;
   private ExecutorService executor;
   
   public static Test suite() throws Exception
   {
      return JBossClusteredTestCase.getDeploySetup(CleanShutdownTestCase.class, "http-sr.war");
   }
   
   /**
    * Create a new CleanShutdownTestCase.
    * 
    * @param name
    * @throws MalformedObjectNameException 
    */
   public CleanShutdownTestCase(String name) throws MalformedObjectNameException
   {
      super(name);
   }

   /**
    * @see org.jboss.test.JBossClusteredTestCase#setUp()
    */
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      this.name = ObjectName.getInstance(SERVER_NAME);

      this.manager = new MultiThreadedHttpConnectionManager();
      
      HttpConnectionManagerParams params = new HttpConnectionManagerParams();
      params.setDefaultMaxConnectionsPerHost(MAX_THREADS);
      params.setMaxTotalConnections(MAX_THREADS);
      
      this.manager.setParams(params);

      this.client = new HttpClient();
      this.executor = Executors.newFixedThreadPool(MAX_THREADS);
   }

   /**
    * @see org.jboss.test.JBossTestCase#tearDown()
    */
   @Override
   protected void tearDown() throws Exception
   {
      this.manager.shutdown();
      this.executor.shutdownNow();
      
      try
      {
         super.tearDown();
      }
      catch (Exception e)
      {
         // Webapp undeploy failed because server has shutdown
      }
   }

   public void testShutdown() throws Exception
   {
      // Make sure a normal request will succeed
      Assert.assertEquals(200, new RequestTask(0, 0).call().intValue());
      
      System.out.println("After initial request");
      for (Cookie cookie: this.client.getState().getCookies())
      {
         System.out.println(cookie.getDomain() + "\t" + cookie.getName() + "\t" + cookie.getPath() + "\t" + cookie.getValue());
      }
      
      Assert.assertNull(this.findCookie(0, COOKIE_NAME));
      
      // Send a long request - in parallel
      Future<Integer> future = executor.submit(new RequestTask(0, REQUEST_DURATION));

      // Make sure long request has started
      Thread.sleep(1000);

      // Shutdown server
      this.getAdaptors()[0].invoke(this.name, SHUTDOWN_METHOD, null, null);

      // Get result of long request
      // This request should succeed since it initiated before server shutdown
      ExecutionException exception = null;
      try
      {
         Assert.assertEquals(200, future.get().intValue());
      }
      catch (ExecutionException e)
      {
         e.printStackTrace(System.err);
         exception = e;
      }

      Assert.assertNull(exception);
      
      System.out.println("After request initiated prior to shutdown");
      for (Cookie cookie: this.client.getState().getCookies())
      {
         System.out.println(cookie.getDomain() + "\t" + cookie.getName() + "\t" + cookie.getPath() + "\t" + cookie.getValue());
      }
      
      Cookie cookie = this.findCookie(0, COOKIE_NAME);
      Assert.assertNotNull(cookie);
      Assert.assertEquals("0", cookie.getValue());
      
      // Subsequent request should return 404
      Assert.assertEquals(404, new RequestTask(0, 0).call().intValue());
      
      System.out.println("After request to shutdown server");
      for (Cookie c: this.client.getState().getCookies())
      {
         System.out.println(c.getDomain() + "\t" + c.getName() + "\t" + c.getPath() + "\t" + c.getValue());
      }
      
      String server0 = this.getServers()[0];
      String server1 = this.getServers()[1];
      
      // Copy cookies from server[0] to server[1]
      for (Cookie c: this.client.getState().getCookies())
      {
         String domain = c.getDomain();
         if (InetAddress.getByName(domain).equals(InetAddress.getByName(server0)) && c.getName().equals("JSESSIONID"))
         {
            // TODO Replace this with JBossTestUtil.fixHostnameForURL(), when ready
            boolean wrap = domain.startsWith("[") && !server1.startsWith("[");
            StringBuilder builder = new StringBuilder();
            if (wrap)
            {
               builder.append('[');
            }
            builder.append(server1);
            if (wrap)
            {
               builder.append(']');
            }
            this.client.getState().addCookie(new Cookie(builder.toString(), c.getName(), c.getValue(), c.getPath(), c.getExpiryDate(), c.getSecure()));
         }
      }

      System.out.println("After cookie copy to failover server");
      for (Cookie c: this.client.getState().getCookies())
      {
         System.out.println(c.getDomain() + "\t" + c.getName() + "\t" + c.getPath() + "\t" + c.getValue());
      }
      
      Assert.assertEquals(200, new RequestTask(1, 0).call().intValue());

      System.out.println("After request on new server");
      for (Cookie c: this.client.getState().getCookies())
      {
         System.out.println(c.getDomain() + "\t" + c.getName() + "\t" + c.getPath() + "\t" + c.getValue());
      }
      
      // If old session had replicated successfully, cookie value should equal old sleep duration
      cookie = this.findCookie(1, COOKIE_NAME);
      Assert.assertNotNull(cookie);
      Assert.assertEquals(String.valueOf(REQUEST_DURATION), cookie.getValue());
   }
   
   private Cookie findCookie(int serverIndex, String name) throws Exception
   {
      InetAddress address = InetAddress.getByName(this.getServers()[serverIndex]);
      
      for (Cookie cookie: this.client.getState().getCookies())
      {
         if (InetAddress.getByName(cookie.getDomain()).equals(address) && cookie.getName().equals(name))
         {
            return cookie;
         }
      }
      
      return null;
   }
   
   private class RequestTask implements Callable<Integer>
   {
      private final int server;
      private final int sleep;
      
      RequestTask(int server, int sleep)
      {
         this.server = server;
         this.sleep = sleep;
      }
      
      @Override
      public Integer call() throws Exception
      {
         GetMethod method = new GetMethod(String.format(URL, CleanShutdownTestCase.this.getHttpURLs()[this.server], this.sleep));
         
         try
         {
            return Integer.valueOf(CleanShutdownTestCase.this.client.executeMethod(method));
         }
         finally
         {
            method.releaseConnection();
         }
      }
   }
}
