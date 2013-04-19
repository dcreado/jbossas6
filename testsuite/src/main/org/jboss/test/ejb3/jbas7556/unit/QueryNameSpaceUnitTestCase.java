/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.ejb3.jbas7556.unit;

import junit.framework.Test;
import org.jboss.test.JBossTestCase;
import org.jboss.test.ejb3.jbas7556.QueryNameSpaceRemote;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class QueryNameSpaceUnitTestCase extends JBossTestCase
{
   public QueryNameSpaceUnitTestCase(String name)
   {
      super(name);
   }

   /**
    * EE.5.15
    */
   public void testModuleName() throws Exception
   {
      QueryNameSpaceRemote bean = (QueryNameSpaceRemote) getInitialContext().lookup("QueryNameSpaceBean/remote");
      String result = bean.query("java:module/ModuleName");
      assertEquals("jbas7556", result);
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(QueryNameSpaceUnitTestCase.class, "jbas7556.jar");
   }
}
