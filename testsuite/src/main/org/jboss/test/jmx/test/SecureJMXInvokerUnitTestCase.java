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
package org.jboss.test.jmx.test;

import java.util.HashMap;

import javax.management.Attribute;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.test.JBossTestCase;
import org.jboss.test.jmx.invoker.CustomClass;

/** Tests for the jmx invoker adaptor with a secured xmbean.
 *
 * @author Scott.Stark@jboss.org
 * @version $Revision: 109975 $
 */
public class SecureJMXInvokerUnitTestCase extends JBossTestCase
{
   public SecureJMXInvokerUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      // JBAS-3605, the execution order of tests in this test case is important
      // so it must be defined explicitly when running under some JVMs
      TestSuite suite = new TestSuite();
      suite.addTest(new SecureJMXInvokerUnitTestCase("testGetSomething"));
      suite.addTest(new SecureJMXInvokerUnitTestCase("testGetCustom"));
      suite.addTest(new SecureJMXInvokerUnitTestCase("testGetCustomXMBean"));
      suite.addTest(new SecureJMXInvokerUnitTestCase("testGetXMBeanInfo"));
      suite.addTest(new SecureJMXInvokerUnitTestCase("testXMBeanDoSomething"));
      suite.addTest(new SecureJMXInvokerUnitTestCase("testSetCustom"));
      suite.addTest(new SecureJMXInvokerUnitTestCase("testClassNotFoundException"));
      
      return getDeploySetup(suite, "invoker-adaptor-test.ear");
   }

   /**
    * The jmx object name name of the mbean under test
    * @return The name of the mbean under test
    * @throws MalformedObjectNameException
    */
   ObjectName getObjectName() throws MalformedObjectNameException
   {
      return new ObjectName("jboss.test:service=InvokerTest,secured=true");
   }
   // JBAS-8540
   // use JBossTestCase.getServerHost() instead
   // static final String TARGET_SERVER_FOR_URL = System.getProperty("jbosstest.server.host.url", "localhost");
   private MBeanServerConnection getJMXServer() throws Exception
   {
      HashMap env = new HashMap();
      String username = "admin";
      String password = "admin";

      if (username != null && password != null)
      {
         String[] creds = new String[2];
         creds[0] = username;
         creds[1] = password;
         env.put(JMXConnector.CREDENTIALS, creds);
      }

      JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"+getServerHostForURL()+":1090/jmxrmi");
      JMXConnector jmxc = JMXConnectorFactory.connect(url, env);
      MBeanServerConnection adaptor = jmxc.getMBeanServerConnection();
      return adaptor;
   }

   public void testGetSomething()
      throws Exception
   {
      log.info("+++ testGetSomething");
      assertEquals("something", getJMXServer().getAttribute(getObjectName(), "Something"));
   }

   public void testGetCustom()
      throws Exception
   {
      log.info("+++ testGetCustom");
      CustomClass custom = (CustomClass) getJMXServer().getAttribute(getObjectName(), "Custom");
      assertEquals("InitialValue", custom.getValue());
   }

   public void testGetCustomXMBean()
      throws Exception
   {
      log.info("+++ testGetCustomXMBean");
      ObjectName xmbean = new ObjectName("jboss.test:service=InvokerTest,type=XMBean");
      CustomClass custom = (CustomClass) getJMXServer().getAttribute(xmbean, "Custom");
      assertEquals("InitialValue", custom.getValue());
   }
   public void testGetXMBeanInfo()
      throws Exception
   {
      log.info("+++ testGetXMBeanInfo");
      ObjectName xmbean = new ObjectName("jboss.test:service=InvokerTest,type=XMBean");
      MBeanInfo info = getJMXServer().getMBeanInfo(xmbean);
      log.info("MBeanInfo: "+info);
   }
   public void testXMBeanDoSomething()
      throws Exception
   {
      log.info("+++ testXMBeanDoSomething");
      ObjectName xmbean = new ObjectName("jboss.test:service=InvokerTest,type=XMBean");
      Object[] args = {};
      String[] sig = {};
      CustomClass custom = (CustomClass) getJMXServer().invoke(xmbean, "doSomething", args, sig);
      log.info("doSomething: "+custom);
   }

   public void testSetCustom()
      throws Exception
   {
      log.info("+++ testSetCustom");
      MBeanServerConnection server = getJMXServer();
      server.setAttribute(getObjectName(), new Attribute("Custom", new CustomClass("changed")));
      CustomClass custom = (CustomClass) server.getAttribute(getObjectName(), "Custom");
      assertEquals("changed", custom.getValue());
   }

   /**
    * Create an mbean whose class does not exist to test that the exception
    * seen from the adaptor is a ClassNotFoundException wrapped in a
    * ReflectionException
    * @throws Exception
    */
   public void testClassNotFoundException() throws Exception
   {
      log.info("+++ testClassNotFoundException");
      MBeanServerConnection server = getJMXServer();
      ObjectName name = new ObjectName("jboss.test:test=testClassNotFoundException");
      try
      {
         server.createMBean("org.jboss.text.jmx.DoesNotExist", name);
         fail("Was able to create org.jboss.text.jmx.DoesNotExist mbean");
      }
      catch (ReflectionException e)
      {
         Exception ex = e.getTargetException();
         assertTrue("ReflectionException.target is ClassNotFoundException",
            ex instanceof ClassNotFoundException);
      }
   }

   protected void setUp() throws Exception
   {
      super.setUp();
   }
   
   protected void tearDown() throws Exception
   {
      super.tearDown();
   }
}
