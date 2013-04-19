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

package org.jboss.test.cluster.defaultcfg.profileservice.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Properties;

import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.metatype.api.values.SimpleValue;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.profileservice.spi.ProfileService;
import org.jboss.test.JBossClusteredTestCase;

/**
 * Test for JBAS-7102. Creates a war w/ 150MB of content in the farm
 * folder; verifies that it is farmed and deployed correctly.
 * @author Brian Stansberry
 *
 */
public class FarmedClusterHugeDeploymentUnitTestCase extends JBossClusteredTestCase
{  
   /** We use the default profile, defined by DeploymentManager to deploy apps. */
   public static final ProfileKey farmProfile = new ProfileKey("farm");
   public static final String SCANNER_ONAME = "jboss.deployment:flavor=URL,type=DeploymentScanner";
   private ManagementView activeView;
   private File farm0Dir;
   private File farm1Dir;
   
   /**
    * Create a new FarmedClusterHotDeployUnitTestCase.
    * 
    * @param name
    */
   public FarmedClusterHugeDeploymentUnitTestCase(String name)
   {
      super(name);
   }
   
   @Override
   protected void setUp() throws Exception
   {
      super.setUp(); 
      
      ManagementView mgtView = getManagementView(getNamingContext(0));
      ComponentType type = new ComponentType("MCBean", "ServerConfig");
      ManagedComponent mc = mgtView.getComponent("jboss.system:type=ServerConfig", type);
      if (mc == null)
      {
         throw new IllegalStateException("No ServerConfig for node0 available");
      }
      String homeDir = (String) ((SimpleValue) mc.getProperty("serverHomeDir").getValue()).getValue();
      if (homeDir == null)
      {
         throw new IllegalStateException("No serverHomeDir for node0 available");
      }
      
      this.farm0Dir = new File(homeDir, "farm");    
      
      mgtView = getManagementView(getNamingContext(1));
      mc = mgtView.getComponent("jboss.system:type=ServerConfig", type);
      if (mc == null)
      {
         throw new IllegalStateException("No ServerConfig for node1 available");
      }
      homeDir = (String) ((SimpleValue) mc.getProperty("serverHomeDir").getValue()).getValue();
      if (homeDir == null)
      {
         throw new IllegalStateException("No serverHomeDir for node1 available");
      }
      
      this.farm1Dir = new File(homeDir, "farm");    
   }


   @Override
   protected void tearDown() throws Exception
   {
      try
      {
      super.tearDown();
      }
      finally
      {
         // Be a good boy and delete the huge files.
         try
         {
            deleteHugeFile(farm0Dir);
         }
         finally
         {
            deleteHugeFile(farm1Dir);
         }
      }
   }

   public void testFarmHotDeployment() throws Exception
   {       
      assertTrue(farm0Dir + " exists", farm0Dir.exists());
      
      ObjectName scanner = new ObjectName(SCANNER_ONAME);      
      try
      {
         getAdaptors()[0].invoke(scanner, "stop", new Object[]{}, new String[]{});
         validateState(false);
         performModifications();
      }
      finally
      {
         getAdaptors()[0].invoke(scanner, "start", new Object[]{}, new String[]{});
      }      

      validateState(true);
   }

   private void performModifications() throws Exception
   {
      // Create a war
      File dir = new File(farm0Dir, "huge.war");
      dir.mkdir();
      
      // Add a file we can request to verify deployment
      copyFile(dir, "farm-huge-index.html", "index.html");
      
      // Add a 150MB file to it
      File huge = new File(dir, "huge.bin");
      FileOutputStream fos = new FileOutputStream(huge);
      BufferedOutputStream bos = new BufferedOutputStream(fos);
      int count = 150 * 1024 * 1024 / 4096;
      for (int i = 0; i < count; i++)
    	  bos.write(new byte[4096]);
      bos.close();
      
      // Add the deployment descriptor
      dir = new File(dir, "WEB-INF");
      dir.mkdir();
      copyFile(dir, "farm-huge-web.xml", "web.xml");
   }
   
   private void validateState(boolean expectContent) throws Exception
   {
	  boolean node0ok = false;
	  boolean node1ok = false;
	  
	  long start = System.nanoTime();
	  long timeout = expectContent ? 65000000000L : 0;
	  
	  while (true)
	  {
	      // Create an instance of HttpClient.
	      HttpClient client = new HttpClient();
	
	      if (!node0ok)
	      {	  
	         node0ok = makeGet(client, getHttpURLs()[0] + "/huge/index.html", expectContent);
	      }
	      
	      if (node0ok && !node1ok)
	      {
	    	  client = new HttpClient();
	      
	    	  node1ok = makeGet(client, getHttpURLs()[1] + "/huge/index.html", expectContent);
	      }
	      
	      if ((node0ok && node1ok) || (System.nanoTime() - start > timeout))
	      {
	    	  break;
	      }
	      else
	      {
	    	  Thread.sleep(1000);
	      }
	  }
	  
	  assertTrue("node 0 ok", node0ok);
	  assertTrue("node 1 ok", node1ok);
   }
   
   private boolean makeGet(HttpClient client, String url, boolean expectSuccess)
      throws IOException
   {
      getLog().debug("makeGet(): trying to get from url " +url);

      GetMethod method = new GetMethod(url);
      int responseCode = 0;
      try
      {
         responseCode = client.executeMethod(method);
      } catch (IOException e)
      {
         e.printStackTrace();
         fail("HttpClient executeMethod fails." +e.toString());
      }
      boolean ok = false;
      if (expectSuccess)
      {
	      ok = responseCode == HttpURLConnection.HTTP_OK;
      }
      else
      {
    	  ok = responseCode != HttpURLConnection.HTTP_OK;
      }
      
      // Read the response body.
      byte[] responseBody = method.getResponseBody();

      // Release the connection.
//      method.releaseConnection();

      // Deal with the response.
      // Use caution: ensure correct character encoding and is not binary data
      return ok;
   }

   /**
    * Obtain the ProfileService.ManagementView
    * @return
    * @throws Exception
    */
   private ManagementView getManagementView(Context ctx)
      throws Exception
   {
      if( activeView == null )
      {
         ProfileService ps = (ProfileService) ctx.lookup("ProfileService");
         activeView = ps.getViewManager();
      }
      // Reload
      activeView.load();
      return activeView;
   }
   
   private Context getNamingContext(int nodeIndex) throws Exception
   {
      // Connect to the server0 JNDI
      String[] urls = getNamingURLs();
      Properties env1 = new Properties();
      env1.setProperty(Context.INITIAL_CONTEXT_FACTORY,
         "org.jnp.interfaces.NamingContextFactory");
      env1.setProperty(Context.PROVIDER_URL, urls[nodeIndex]);
      return new InitialContext(env1);
   }

   private void copyFile(File dir, String sourceName, String targetName) throws Exception
   {
      InputStream is = getDeployURL(sourceName).openStream();
      try
      {
         File output = new File(dir, targetName);
         FileOutputStream fos = new FileOutputStream(output);
         try
         {
            byte[] tmp = new byte[1024];
            int read;
            while((read = is.read(tmp)) > 0)
            {
               fos.write(tmp, 0, read);
            }
            fos.flush();
         }
         finally
         {
            fos.close();
         }         
      }
      finally
      {
         is.close();
      }
   }


   private void deleteHugeFile(File farmDir)
   {
      if (farmDir.exists())
      {
         File war = new File(farmDir, "huge.war");
         if (war.exists())
         {
            File huge = new File(war, "huge.bin");
            if (huge.exists() && !huge.delete())
            {
               huge.deleteOnExit();
            }
         }
      }      
   }

}
