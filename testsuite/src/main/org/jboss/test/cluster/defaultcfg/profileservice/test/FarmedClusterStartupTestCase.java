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

package org.jboss.test.cluster.defaultcfg.profileservice.test;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jboss.test.JBossClusteredTestCase;

/**
 * Tests that a pair of servers started with farming configured end
 * up with the expected set of farm deployments.
 *
 * @author Brian Stansberry
 * 
 * @version $Revision: $
 */
public class FarmedClusterStartupTestCase extends JBossClusteredTestCase
{

   /**
    * Create a new FarmedClusterStartupTestCase.
    * 
    * @param name
    */
   public FarmedClusterStartupTestCase(String name)
   {
      super(name);
   }

   public void testNode0FarmTestThreadPool() throws Exception
   {
      MBeanServerConnection[] adaptors = getAdaptors();
      ObjectName oname = new ObjectName("jboss.system:service=Node0FarmTestThreadPool");
      assertEquals("Node0FarmThreadPool", adaptors[0].getAttribute(oname, "Name"));
      assertEquals("Node0FarmThreadPool", adaptors[1].getAttribute(oname, "Name"));
   }

   public void testNode1FarmTestThreadPool() throws Exception
   {
      MBeanServerConnection[] adaptors = getAdaptors();
      ObjectName oname = new ObjectName("jboss.system:service=Node1FarmTestThreadPool");
      assertEquals("Node1FarmThreadPool", adaptors[0].getAttribute(oname, "Name"));
      assertEquals("Node1FarmThreadPool", adaptors[1].getAttribute(oname, "Name"));      
   }
   
   /**
    * Confirms an exploded war replicated from node0 to node1
    * 
    * @throws Exception
    */
   public void testFarmAWar() throws Exception
   {
      checkAvailable("/farmA/index.html");
   }
   
   /**
    * Confirms an exploded war replicated from node1 to node0
    * 
    * @throws Exception
    */
   public void testFarmBWar() throws Exception
   {
      checkAvailable("/farmB/index.html");      
   }
   
   public void testFarmCWar() throws Exception
   {
      checkAvailable("/farmC/index.html"); 
      checkAvailable("/farmC/node0.html"); 
      checkAvailable("/farmC/node1.html"); 
   }
   
   public void testFarmDWar() throws Exception
   {
      String index = "/farmD/index.html";

      HttpClient client = new HttpClient();

      GetMethod get = new GetMethod(getHttpURLs()[0] +index);
      assertEquals("farmD is unavailable on node0", HttpURLConnection.HTTP_NOT_FOUND, client.executeMethod(get));
      get = new GetMethod(getHttpURLs()[1] +index);
      assertEquals("farmD is unavailable on node1", HttpURLConnection.HTTP_NOT_FOUND, client.executeMethod(get));
   }
   
   /**
    * Confirms a zipped war replicated from node0 to node1
    * 
    * @throws Exception
    */
   public void testFarmEWar() throws Exception
   {
      checkAvailable("/farmE/index.html");
   }
   
   /**
    * Confirms a zipped war replicated from node1 to node0
    * 
    * @throws Exception
    */
   public void testFarmFWar() throws Exception
   {
      checkAvailable("/farmF/index.html");
   }

   private void checkAvailable(String url) throws Exception, IOException, HttpException
   {
      HttpClient client = new HttpClient();

      GetMethod get = new GetMethod(getHttpURLs()[0] +url);
      assertEquals(url + " is available on node0", HttpURLConnection.HTTP_OK, client.executeMethod(get));
      get = new GetMethod(getHttpURLs()[1] +url);
      assertEquals(url + " is available on node1", HttpURLConnection.HTTP_OK, client.executeMethod(get));
   }
}
