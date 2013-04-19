/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.mdbdefaultraname.test;


import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.test.mdbdefaultraname.bean.TestStatus;
import org.jboss.test.mdbdefaultraname.bean.TestStatusHome;

import org.jboss.test.JBossJMSTestCase;

/**
 * A MDBDefaultRANameUnitTestCase.
 * 
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 * @version $Revision: 1.1 $
 */
public class MDBDefaultRANameUnitTestCase extends JBossJMSTestCase
{
   public MDBDefaultRANameUnitTestCase(String name)
   {
      super(name);
   }
   
   public void testMDBDefaultRAName() throws Exception
   {
      TestStatusHome testHome = (TestStatusHome) new InitialContext().lookup("TestStatus");
      TestStatus test = testHome.create();
      Map<String, String> map = new HashMap<String, String>();
      map.put("StandardMDB", "jms-ra.rar");
      map.put("MDBTestMessageListener", "jcainflow.rar");
      assertEquals(map, test.getResourceAdapterNames());
   }
   
   public static Test suite() throws Exception
   {
      Test t = getDeploySetup(MDBDefaultRANameUnitTestCase.class, "mdbdefaultraname.jar");
      t = getDeploySetup(t, "jcainflow.rar");
      return t;
   }
}
