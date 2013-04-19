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
package org.jboss.test.managed.bean.test;

import junit.framework.Assert;
import junit.framework.Test;

import org.jboss.test.JBossTestCase;
import org.jboss.test.managed.bean.DelegateSLSB;
import org.jboss.test.managed.bean.Echo;

/**
 * Tests Managed bean deployments
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class ManagedBeanUnitTestCase extends JBossTestCase
{

   public ManagedBeanUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(ManagedBeanUnitTestCase.class, "javaee6-managed-bean.jar");
   }

   /**
    * Tests that invocation on a Managed bean works without issues.
    * @throws Exception
    */
   public void testManagedBeanInvocation() throws Exception
   {
      // get hold of the bean which internally invokes on the managed bean
      Echo bean = (Echo) this.getInitialContext().lookup(DelegateSLSB.JNDI_NAME);
      String message = "No new message!";
      String echoedMessage = bean.echo(message);
      Assert.assertEquals("Unexpected message returned", message, echoedMessage);
   }
}
