/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.refs.ejbs;

import java.util.Properties;

import javax.ejb.Remote;
import javax.ejb.Stateful;

/**
 * @author Scott.Stark@jboss.org
 * @version $Revision: 82920 $
 */
@Stateful(name="StatefulBean", mappedName="refs/ejbs/StatefulBean")
@Remote({StatefulIF.class})
public class StatefulBean implements StatefulIF
{
   public void init(Properties prop) throws Exception
   {
   }
   public void test1() throws Exception
   {
      
   }
   public void test2() throws Exception
   {
      
   }
}
