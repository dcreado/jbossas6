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

package org.jboss.test.cluster.ejb3.stateful.nested.base.xpc;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.jboss.test.cluster.ejb3.stateful.nested.base.DeepNestedStatefulBean;

/**
 * A DeepNestedContainedBean.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 108925 $
 */
public class DeepNestedContainedBean 
   extends DeepNestedStatefulBean 
   implements DeepNestedContained
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   @PersistenceContext(unitName="tempdb",type=PersistenceContextType.EXTENDED) 
   private EntityManager em;

   private Customer customer;

   public Customer find(long id)
   {
      return em.find(Customer.class, id);
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
      customer.setName("modified by " + getInternalId());
   }

}
