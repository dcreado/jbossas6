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

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import junit.framework.Test;

import org.jboss.test.JBossTestCase;

/**
 * A test that deploys app sars.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 82920 $
 */
public class ValueFactoryInjectionUnitTestCase extends JBossTestCase
{
   private static final String valueFactoryDeployment = "testjmx-valuefactory.sar";
   private static final String targetObject = "jboss.test:service=ValueFactoryInjectionTestTarget";
   
   public ValueFactoryInjectionUnitTestCase(String test)
   {
      super(test);
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(ValueFactoryInjectionUnitTestCase.class, valueFactoryDeployment);
   }
  
   public void testValueFactoryInjection() throws Exception
   {      
      ObjectName oname = new ObjectName(targetObject);
      MBeanServerConnection adaptor = this.getServer();
      Integer val = (Integer) adaptor.getAttribute(oname, "FromPojo");
      assertNotNull(val);
      assertEquals(3, val.intValue());
      val = (Integer) adaptor.getAttribute(oname, "FromMBean");
      assertNotNull(val);
      assertEquals(7, val.intValue());
      val = (Integer) adaptor.getAttribute(oname, "FromDefault");
      assertNotNull(val);
      assertEquals(5, val.intValue());
   }
}
