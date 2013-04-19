/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.cluster.ejb3.stateful.nested.base.xpc;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue; import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Company customer
 *
 * @author Ben Wang
 */
@Entity
public class Customer implements java.io.Serializable
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;
   
   long id;
   String name;

   public
   Customer()
   {
   }

   @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
   public
   long getId()
   {
      return id;
   }

   public
   String getName()
   {
      return name;
   }

   public
   void setId(long long1)
   {
      id = long1;
   }

   public
   void setName(String string)
   {
      name = string;
   }
   
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      
      if (obj instanceof Customer)
      {
         Customer other = (Customer) obj;
         String oName = other.name == null ? "" : other.name;
         return (this.id == other.id)
                  && (oName.equals(this.name));
         
      }
      
      return false;
   }

}

