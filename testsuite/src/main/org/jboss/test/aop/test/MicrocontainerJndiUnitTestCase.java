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
package org.jboss.test.aop.test;

import javax.naming.Context;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.test.JBossTestCase;
import org.jboss.test.aop.mcjndi.AnnotatedPojo;
import org.jboss.test.aop.mcjndi.PojoInterface;

/**
 * MicrocontainerJndiUnitTestCase.
 * 
 * @author <a href="mailto:galder.zamarreno@jboss.com">Galder Zamarreno</a>
 */
public class MicrocontainerJndiUnitTestCase extends JBossTestCase
{
   public MicrocontainerJndiUnitTestCase(String name)
   {
      super(name);
   }

   public void testAnnotated() throws Exception
   {
      Context ctx = getInitialContext();
      PojoInterface pojo = (PojoInterface)ctx.lookup(AnnotatedPojo.NAME);
      pojo.setProperty(39);
      assertEquals(39, pojo.getProperty());
      
      Object ret = pojo.someAction();
      assertEquals("JNDI-Annotated-39", ret);
   }
   
   public void testXml() throws Exception
   {
      Context ctx = getInitialContext();
      PojoInterface pojo = (PojoInterface)ctx.lookup("pojo/XmlBean");
      pojo.setProperty(49);
      assertEquals(49, pojo.getProperty());
      
      Object ret = pojo.someAction();
      assertEquals("JNDI-XML-49", ret);      
   }
   
   public static Test suite() throws Exception
   {
      TestSuite suite = new TestSuite();
      suite.addTest(new TestSuite(MicrocontainerJndiUnitTestCase.class));
      AOPTestSetup setup = new AOPTestSetup(suite, "aop-mc-jnditest.jar");
      return setup;
   }
}
