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
package org.jboss.test.refs.ejblink;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.SessionContext;
import javax.annotation.Resource;

import org.jboss.test.refs.common.Constants;
import org.jboss.test.refs.common.EjbLinkBeanBase;
import org.jboss.test.refs.common.EjbLinkIF;
import org.jboss.test.refs.common.ServiceLocator;

/**
 * @author Scott.Stark@jboss.org
 * @version $Revision: 82920 $
 */
@Stateless(name = "EjbLink2Bean")
@Remote( { EjbLinkIF.class })
public class EjbLink2Bean extends EjbLinkBeanBase
   implements EjbLinkIF, Constants
{
   @Resource
   private SessionContext sessionContext;

   public EjbLink2Bean()
   {
   }

   public void remove()
   {
   }

   public void callTwo() throws Exception
   {
      throw new IllegalStateException("Cannot call bean2 from bean2");
   }

   public void callOne() throws Exception
   {
      Object obj = ServiceLocator.lookup(BEAN1_REF_NAME);
      EjbLinkIF bean1 = (EjbLinkIF) obj;
      bean1.call();
   }

   public void callThree() throws Exception
   {
      Object obj = ServiceLocator.lookup(BEAN3_REF_NAME);
      EjbLinkIF bean3 = (EjbLinkIF) obj;
      bean3.call();
   }
}
