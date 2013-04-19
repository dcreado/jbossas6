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
package org.jboss.as.ejb3.naming;

import org.jboss.ejb3.EJBContainer;
import org.jboss.ejb3.EjbEncFactory;
import org.jboss.logging.Logger;
import org.jboss.reloaded.naming.CurrentComponent;
import org.jboss.reloaded.naming.spi.JavaEEComponent;

import javax.naming.Context;

/**
 * As opposed to the DefaultEjbEncFactory, the NamingComponentEjbEncFactory has a one-one association
 * with the container.
 * 
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class NamingComponentEjbEncFactory implements EjbEncFactory
{
   private static final Logger log = Logger.getLogger(NamingComponentEjbEncFactory.class);

   private JavaEEComponent component;

   public NamingComponentEjbEncFactory(JavaEEComponent component)
   {
      this.component = component;
   }

   public Context getEnc(final EJBContainer container)
   {
      return component.getContext();
   }

   public void pushEnc(EJBContainer container)
   {
      CurrentComponent.push(component);
   }

   public void popEnc(EJBContainer container)
   {
      JavaEEComponent previous = CurrentComponent.pop();
      assert previous == component;
   }

   public void cleanupEnc(EJBContainer container)
   {
      // do nothing, naming-deployers will take care of it
   }
}
