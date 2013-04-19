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
package org.jboss.web.tomcat.service;

import org.apache.catalina.InstanceEvent;
import org.apache.catalina.InstanceListener;
import org.jboss.logging.Logger;
import org.jboss.reloaded.naming.CurrentComponent;
import org.jboss.reloaded.naming.spi.JavaEEComponent;

/**
 * An InstanceListener used to push/pop the application naming context.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision: 81037 $
 */
public class NamingListener implements InstanceListener
{
   private static Logger log = Logger.getLogger(NamingListener.class);
   public static ThreadLocal<JavaEEComponent> idLocal = new ThreadLocal<JavaEEComponent>();

   private JavaEEComponent id;

   public NamingListener()
   {
      id = idLocal.get();
      assert id != null : "id is null";
   }

   public Object getId()
   {
      return id;
   }

   public void setId(JavaEEComponent id)
   {
      this.id = id;
   }

   public void instanceEvent(InstanceEvent event)
   {
      String type = event.getType();
      // Push the identity on the before init/destroy
      if ( type.equals(InstanceEvent.BEFORE_DISPATCH_EVENT)
         || type.equals(InstanceEvent.BEFORE_DESTROY_EVENT)
         || type.equals(InstanceEvent.BEFORE_INIT_EVENT) )
      {
         // Push naming id
         CurrentComponent.push(id);
      }
      // Pop the identity on the after init/destroy
      else if ( type.equals(InstanceEvent.AFTER_DISPATCH_EVENT)
         || type.equals(InstanceEvent.AFTER_DESTROY_EVENT)
         || type.equals(InstanceEvent.AFTER_INIT_EVENT) )
      {
         // Pop naming id
         CurrentComponent.pop();
      }
   }
   
}
