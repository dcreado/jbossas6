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
package org.jboss.test.kernel.deployment.jboss.beans.servicepojo;

import java.util.List;

/**
 * A simple pojo to represent a hypothetical service
 *
 * @author <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class POJOService extends AbstractService
{
   // Private -------------------------------------------------------
   
   private PoolService pool;
   private List addresses;
   
   // Constructor ---------------------------------------------------
   
   public POJOService()
   {
      super("POJOService");
   }
   
   // Accessors/Mutators --------------------------------------------

   public void setPoolService(PoolService pool)
   {
      log("setPoolService(" + pool + ")");      
      this.pool = pool;
   }

   public void setBindAddresses(List addresses)
   {
      log("setBindAddresses(" + addresses + ")");       
      this.addresses = addresses;
   }
   
   // Overrides -----------------------------------------------------
   
   public void create() throws Exception
   {
      if (pool == null)
         throw new IllegalAccessException("Null pool!");

      super.create();
   }
   
   public String toString()
   {
      StringBuffer sbuf = new StringBuffer();
      sbuf
      .append(getClass().getName())
      .append("[ name=").append(name)
      .append(", state=").append(state)      
      .append(", pool=").append(pool)
      .append(", addresses=").append(addresses)
      .append(" ]");
      
      return sbuf.toString();
   }
}
