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

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.test.cluster.ejb3.stateful.nested.base.VMTrackerBean;

/**
 * SLSB that is meant to be nested in a parent SFSB and share
 * and ExtendedPersistenceContext with it.
 *
 * @author Ben Wang
 * @author Brian Stansberry
 */
@Stateless
@Local(NestedStatelessLocal.class)
public class NestedStatelessSessionBean 
   extends VMTrackerBean
   implements NestedStatelessLocal
{
   @PersistenceContext(unitName="tempdb") 
   private EntityManager em;
   
   public void update(Customer c)
   {
      c.setName("Bill Jr.");
   }

   public void findAndUpdate(long id)
   {
      Customer cust = find(id);
      cust.setName("stateless modified");
   }

   public Customer find(long id)
   {
      return em.find(Customer.class, id);
   }
}
