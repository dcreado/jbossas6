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
package org.jboss.test.managed.bean;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.logging.Logger;

/**
 * DelegateSLSB
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Stateless
@Remote(Echo.class)
@RemoteBinding(jndiBinding = DelegateSLSB.JNDI_NAME)
public class DelegateSLSB implements Echo
{

   public static final String JNDI_NAME = "DelegateSLSBToManagedBean";
   
   private static Logger logger = Logger.getLogger(DelegateSLSB.class);
   
   @Override
   public String echo(String msg)
   {
      Context ctx;
      try
      {
         ctx = new InitialContext();
         String jndiName = "java:module/" + SimpleManagedBean.class.getSimpleName();
         logger.info("Looking up Managed bean at: " + jndiName);
         // lookup the managed bean
         SimpleManagedBean managedBean = (SimpleManagedBean) ctx.lookup(jndiName);
         // invoke on the managed bean
         return managedBean.echo(msg);
      }
      catch (NamingException ne)
      {
         throw new RuntimeException(ne);
      }

   }

}
