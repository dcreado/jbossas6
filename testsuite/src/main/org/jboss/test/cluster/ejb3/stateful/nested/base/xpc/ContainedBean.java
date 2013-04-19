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

import java.io.Serializable;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.jboss.test.cluster.ejb3.stateful.nested.base.DeepNestedStateful;
import org.jboss.test.cluster.ejb3.stateful.nested.base.PassivationActivationWatcherBean;

/**
 * Comment
 *
 * @author Ben Wang
 * @version $Revision: 108925 $
 */
public class ContainedBean
   extends PassivationActivationWatcherBean
   implements Contained, Serializable
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   @PersistenceContext(unitName="tempdb",type=PersistenceContextType.EXTENDED) 
   private EntityManager em;

   @EJB(beanName="testDeepNestedContained")
   private DeepNestedContained deepNestedContained;
   
   private Customer customer;

   public Customer find(long id)
   {
      Customer c = em.find(Customer.class, id);
      log.trace("customer " + id + " = " + c);
      return c;
   }

   public void setCustomer(long id)
   {
      customer = find(id);
   }

   public Customer getCustomer()
   {
      return customer;
   }

   public void updateCustomer()
   {
      customer.setName("contained modified");
   }

   public void setContainedCustomer()
   {
      deepNestedContained.setCustomer(customer.getId());
   }

   public boolean checkContainedCustomer()
   {
      return (deepNestedContained.getCustomer() == customer);
   }

   @Remove
   public void remove()
   {
      
   }

   public DeepNestedStateful getDeepNestedStateful()
   {
      return deepNestedContained;
   }

   public void reset()
   {
      super.reset();
      deepNestedContained.reset();
   }
   
   
}
