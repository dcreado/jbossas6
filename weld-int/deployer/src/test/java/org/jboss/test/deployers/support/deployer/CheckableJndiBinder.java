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
package org.jboss.test.deployers.support.deployer;

import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.integration.deployer.jndi.JndiBinder;

/**
 * Mock checkable jndi binder deployer.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CheckableJndiBinder extends JndiBinder
{
   public static Context ROOT = new MockContext();

   @Override
   protected Context createContext() throws NamingException
   {
      return ROOT;
   }

   @Override
   public void bind(DeploymentUnit unit) throws DeploymentException
   {
      try
      {
         Context context = getBeanManagerContext().createSubcontext(unit.getSimpleName());
         context.bind("bootstrap", "Bootstrap Dummy");
      }
      catch (NamingException e)
      {
         new DeploymentException(e);
      }
   }

   @Override
   public void unbind(DeploymentUnit unit)
   {
      try
      {
         Context context = (Context)getBeanManagerContext().lookup(unit.getSimpleName());
         context.unbind("bootstrap");

         getBeanManagerContext().destroySubcontext(unit.getSimpleName());
      }
      catch (NamingException e)
      {
         throw new RuntimeException(e);
      }
   }

}