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

import java.rmi.dgc.VMID;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.jboss.test.cluster.ejb3.stateful.nested.base.MidLevel;
import org.jboss.test.cluster.ejb3.stateful.nested.base.PassivationActivationWatcherBean;
import org.jboss.test.cluster.ejb3.stateful.nested.base.VMTracker;

/**
 * Base SFSB for use in ExtendedPersistenceContext testing.
 *
 * @author Ben Wang
 */
public class ShoppingCartBean
   extends PassivationActivationWatcherBean
   implements ShoppingCart
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   @PersistenceContext(unitName="tempdb",type=PersistenceContextType.EXTENDED) 
   private EntityManager em;

   private Customer customer;

   @EJB(beanName="testShoppingCartContained") 
   private Contained contained;

   @EJB(beanName="testLocalShoppingCartContained") 
   private Contained localContained;
   
   @EJB
   private NestedStatelessLocal stateless;

   public VMID getVMID()
   {
      return VMTracker.VMID;
   }
   
   public MidLevel getLocalNested()
   {
      return localContained;
   }

   public MidLevel getNested()
   {
      return contained;
   }

   public long createCustomer()
   {
      customer = new Customer();
      customer.setName("William");
      em.persist(customer);
      System.out.println("********* created *****");
      return customer.getId();
   }

   public void setCustomer(long id)
   {
      customer = find(id);
   }

   public void setContainedCustomer()
   {
      localContained.setCustomer(customer.getId());
   }

   public boolean checkContainedCustomer()
   {
      return (localContained.getCustomer() == customer);
   }

   public void updateContained()
   {
      localContained.updateCustomer();
   }

   public void update()
   {
      System.out.println("********* update() *****");
      customer.setName("Bill");
   }
   public void update2()
   {
      customer.setName("Billy");
   }

   public void update3()
   {
      stateless.update(customer);
   }

   public void findAndUpdateStateless()
   {
      stateless.findAndUpdate(customer.getId());
      if (!customer.getName().equals("stateless modified")) 
         throw new RuntimeException("stateless didn't get propagated pc");
   }

   public Customer find(long id)
   {
      return em.find(Customer.class, id);
   }

   @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
   public void never()
   {
      customer.setName("Bob");
   }

   public void reset()
   {
      super.reset();
      contained.reset();
      localContained.reset();
   }


   @Remove
   public void remove() 
   {      
   }

   public void setUpFailover(String failover) 
   {
      // To setup the failover property
      log.debug("Setting up failover property: " +failover);
      System.setProperty ("JBossCluster-DoFail", failover);
   }
}
