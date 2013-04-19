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

import java.util.HashSet;
import java.util.Set;

import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.test.cluster.ejb3.stateful.nested.base.BeanMonitorBean;

/**
 * A NestedXPCMonitorBean.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 108925 $
 */
@Stateful
@Remote(NestedXPCMonitor.class)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class NestedXPCMonitorBean 
   extends BeanMonitorBean 
   implements NestedXPCMonitor
{
   @PersistenceContext(unitName="tempdb")
   private EntityManager em;
   
   private Set<Long> ids = new HashSet<Long>();
   
   @TransactionAttribute(TransactionAttributeType.REQUIRED)
   public Customer find(long id)
   {
      ids.add(new Long(id));
      return em.find(Customer.class, id);
   }

   public boolean compareLocalNestedToLocalDeepNested(long id)
   {
      try
      {
         Contained midLevel = (Contained) localNested;
         midLevel.setCustomer(id);
         midLevel.setContainedCustomer();
         return midLevel.checkContainedCustomer(); 
      }
      catch (Exception e)
      {
         log.debug("compareLocalNestedToDeepNested(): " + e.getLocalizedMessage());
      }
      return false;
   }

   public boolean compareNestedToDeepNested(long id)
   {
      try
      {
         Contained midLevel = (Contained) nested;
         midLevel.setCustomer(id);
         midLevel.setContainedCustomer();
         return midLevel.checkContainedCustomer();         
      }
      catch (Exception e)
      {
         log.debug("compareNestedToDeepNested(): " + e.getLocalizedMessage());
      }
      return false;
   }

   public boolean compareTopToLocalNested(long id)
   {
      try
      {
         ShoppingCart cart = (ShoppingCart) parent;
         cart.setCustomer(id);
         cart.setContainedCustomer();
         return cart.checkContainedCustomer();         
      }
      catch (Exception e)
      {
         log.debug("compareTopToLocalNested(): " + e.getLocalizedMessage());
      }
      return false;
   }

   public boolean compareTopToNested(long id)
   {
      try
      {
         Customer mid = findNested(id);
         Customer top = findParent(id);
         
         return (mid != null && mid != top && mid.equals(top)) ;          
      }
      catch (Exception e)
      {
         log.debug("compareTopToNested(): " + e.getLocalizedMessage());
      }
      return false;
   }

   public boolean compareNestedToLocalNested(long id)
   {
      try
      {
         Customer remote = findNested(id);
         Customer local  = findLocalNested(id);
         
         return (remote != null && remote.equals(local));         
      }
      catch (Exception e)
      {
         log.debug("compareTopToLocalNested(): " + e.getLocalizedMessage());
      }
      return false;
   }

   @TransactionAttribute(TransactionAttributeType.SUPPORTS)
   public Customer findDeepNested(long id)
   {
      try
      {
         return ((DeepNestedContained) deepNested).find(id);
      }
      catch (Exception e)
      {
         log.debug("findDeepNested(): " + e.getLocalizedMessage());
      }
      return null;
   }

   @TransactionAttribute(TransactionAttributeType.SUPPORTS)
   public Customer findLocalDeepNested(long id)
   {
      try
      {
         return ((DeepNestedContained) localDeepNested).find(id);
      }
      catch (Exception e)
      {
         log.debug("findLocalDeepNested(): " + e.getLocalizedMessage());
      }
      return null;
   }

   @TransactionAttribute(TransactionAttributeType.SUPPORTS)
   public Customer findLocalNested(long id)
   {
      try
      {
         return ((Contained) localNested).find(id);
      }
      catch (Exception e)
      {
         log.debug("findLocalNested(): " + e.getLocalizedMessage());
      }
      return null;
   }

   @TransactionAttribute(TransactionAttributeType.SUPPORTS)
   public Customer findNested(long id)
   {
      try
      {
         return ((Contained) nested).find(id);
      }
      catch (Exception e)
      {
         log.debug("findNested(): " + e.getLocalizedMessage());
      }
      return null;
   }

   @TransactionAttribute(TransactionAttributeType.SUPPORTS)
   public Customer findParent(long id)
   {
      try
      {
         return ((ShoppingCart) parent).find(id);
      }
      catch (Exception e)
      {
         log.debug("findParent(): " + e.getLocalizedMessage());
      }
      return null;
   }

   @Override
   @TransactionAttribute(TransactionAttributeType.REQUIRED)   
   public void remove()
   {
      super.remove();
      
      for (Long id : ids)
      {
         try
         {
            Customer c = em.find(Customer.class, id.longValue());
            if (c != null)
               em.remove(c);
         }
         catch (Exception e) 
         {
            log.debug("remove(): Problem removing " + id, e);
         }
      }
   }
   
   
}
