/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.services.binding;

import org.jboss.naming.Util;
import org.jboss.system.ServiceMBeanSupport;

/**
 * Register JNDI alias for original.
 * e.g. new AS7 default DS --> legacy default DS
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AliasJndiService extends ServiceMBeanSupport
   implements AliasJndiServiceMBean
{
   private String alias;
   private String original;

   @Override
   protected void startService() throws Exception
   {
      Util.createLinkRef(alias, original);
   }

   @Override
   protected void stopService() throws Exception
   {
      Util.removeLinkRef(alias);
   }

   public String getAlias()
   {
      return alias;
   }

   public void setAlias(String alias)
   {
      this.alias = alias;
   }

   public String getOriginal()
   {
      return original;
   }

   public void setOriginal(String original)
   {
      this.original = original;
   }
}
