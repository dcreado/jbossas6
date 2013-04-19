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

import javax.management.Attribute;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.test.JBossTestCase;
import org.jboss.test.jmx.invoker.CustomClass;
import org.jboss.test.jmx.invoker.InvokerTestMBean;

/**
 * Tests for the jmx invoker adaptor.
 *
 * @author <a href="mailto:Adrian.Brock@HappeningTimes.com">Adrian Brock</a>
 * @author Scott.Stark@jboss.org
 * @author <a href="mailto:tom@jboss.com">Tom Elrod</a>
 * @version $Revision: 104649 $
 */
public class JMXInvokerUnitTestCase extends JBossTestCase
{
   public JMXInvokerUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      // JBAS-3605, the execution order of tests in this test case is important
      // so it must be defined explicitly when running under some JVMs
      TestSuite suite = new TestSuite();
      suite.addTest(new JMXInvokerUnitTestCase("testGetSomething"));
      suite.addTest(new JMXInvokerUnitTestCase("testGetCustom"));
      suite.addTest(new JMXInvokerUnitTestCase("testGetCustomXMBean"));
      suite.addTest(new JMXInvokerUnitTestCase("testGetXMBeanInfo"));
      suite.addTest(new JMXInvokerUnitTestCase("testXMBeanDoSomething"));
      suite.addTest(new JMXInvokerUnitTestCase("testSetCustom"));
      suite.addTest(new JMXInvokerUnitTestCase("testClassNotFoundException"));
      
      return getDeploySetup(suite, "invoker-adaptor-test.ear");
   }

   /**
    * The jmx object name name of the mbean under test
    * @return The name of the mbean under test
    * @throws MalformedObjectNameException
    */
   ObjectName getObjectName() throws MalformedObjectNameException
   {
      return InvokerTestMBean.OBJECT_NAME;
   }

   public void testGetSomething()
      throws Exception
   {
      log.info("+++ testGetSomething");
      assertEquals("something", getServer().getAttribute(getObjectName(), "Something"));
   }

   public void testGetCustom()
      throws Exception
   {
      log.info("+++ testGetCustom");
      CustomClass custom = (CustomClass) getServer().getAttribute(getObjectName(), "Custom");
      assertEquals("InitialValue", custom.getValue());
   }

   public void testGetCustomXMBean()
      throws Exception
   {
      log.info("+++ testGetCustomXMBean");
      ObjectName xmbean = new ObjectName("jboss.test:service=InvokerTest,type=XMBean");
      CustomClass custom = (CustomClass) getServer().getAttribute(xmbean, "Custom");
      assertEquals("InitialValue", custom.getValue());
   }
   public void testGetXMBeanInfo()
      throws Exception
   {
      log.info("+++ testGetXMBeanInfo");
      ObjectName xmbean = new ObjectName("jboss.test:service=InvokerTest,type=XMBean");
      MBeanInfo info = getServer().getMBeanInfo(xmbean);
      log.info("MBeanInfo: "+info);
   }
   public void testXMBeanDoSomething()
      throws Exception
   {
      log.info("+++ testXMBeanDoSomething");
      ObjectName xmbean = new ObjectName("jboss.test:service=InvokerTest,type=XMBean");
      Object[] args = {};
      String[] sig = {};
      CustomClass custom = (CustomClass) getServer().invoke(xmbean, "doSomething", args, sig);
      log.info("doSomething: "+custom);
   }

   public void testSetCustom()
      throws Exception
   {
      log.info("+++ testSetCustom");
      MBeanServerConnection server = getServer();
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
      MBeanServerConnection server = getServer();
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
}
