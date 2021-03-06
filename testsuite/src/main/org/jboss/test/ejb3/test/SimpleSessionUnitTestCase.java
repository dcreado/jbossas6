/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.test.ejb3.test;

import javax.naming.InitialContext;

import junit.framework.Test;

import org.jboss.test.JBossTestCase;
import org.jboss.test.ejb3.basic.SimpleSession;

/**
 * @author Scott.Stark@jboss.org
 * @version $Revision: 102506 $
 */
public class SimpleSessionUnitTestCase
   extends JBossTestCase
{
   public static Test suite() throws Exception
   {
      Test t1 = getDeploySetup(SimpleSessionUnitTestCase.class, "simple-session.jar");
      return t1;
   }

   public SimpleSessionUnitTestCase(String name)
   {
      super(name);
   }

   public void testSession()
      throws Exception
   {
      InitialContext ctx = getInitialContext();
      String jndiName = "ejb3/basic/SimpleSessionBean";
      Object ref = ctx.lookup(jndiName);
      SimpleSession test = (SimpleSession) ref;
      test.ping();
   }
}
