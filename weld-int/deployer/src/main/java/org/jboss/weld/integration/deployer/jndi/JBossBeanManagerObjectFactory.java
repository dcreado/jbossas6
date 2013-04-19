/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.weld.integration.deployer.jndi;

import javax.naming.*;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;
import java.util.Map;

import org.jboss.weld.Container;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.integration.util.IdFactory;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * @author Marius Bogoevici
 */
public class JBossBeanManagerObjectFactory implements ObjectFactory
{
   public Object getObjectInstance(Object o, Name name, Context context, Hashtable<?, ?> hashtable) throws Exception
   {
      Reference reference = (Reference) o;
      StringRefAddr refAddr = (StringRefAddr) reference.get(JndiBinder.REFADDR_ID);
      String beanManagerId = (String)refAddr.getContent();
      if (Container.available())
      {
         Container container = Container.instance();

         for (Map.Entry<BeanDeploymentArchive, BeanManagerImpl> mapElement : container.beanDeploymentArchives().entrySet())
         {
            if (mapElement.getKey().getId().equals(beanManagerId))
            {
               return mapElement.getValue().getCurrent();
            }
         }
      }
      else
      {
         throw new NamingException("Cannot resolve BeanManager: container not available");
      }
      throw new NamingException("Cannot resolve BeanManager for id " + beanManagerId);
   }
}
