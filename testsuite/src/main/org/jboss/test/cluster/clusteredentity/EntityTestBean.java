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
package org.jboss.test.cluster.clusteredentity;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.CacheEntryVisitedEvent;
import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.ha.ispn.DefaultCacheContainerRegistry;
import org.jboss.logging.Logger;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 109050 $
 */
@Stateful
@Remote(EntityTest.class)
@RemoteBinding(jndiBinding="EntityTestBean/remote")
@Listener
public class EntityTestBean implements EntityTest
{
   private static final Logger log = Logger.getLogger(EntityTestBean.class);
   
   @PersistenceContext
   private EntityManager manager;
   
   private List<Cache<?, ?>> caches = new LinkedList<Cache<?, ?>>();
   
   @Override
   public void getCache(String regionPrefix)
   {
      EmbeddedCacheManager container = DefaultCacheContainerRegistry.getInstance().getCacheContainer("hibernate");
      for (String cacheName: container.getCacheNames())
      {
         if (cacheName.startsWith(regionPrefix))
         {
            Cache<?, ?> cache = container.getCache(cacheName);
            cache.addListener(this);
            caches.add(cache);
         }
      }
   }
   
   @Override
   public Customer createCustomer()
   {
      System.out.println("CREATE CUSTOMER");
      try
      {
         this.clear();
         
         Customer customer = new Customer();
         customer.setId(new Integer(1));
         customer.setName("JBoss");
         Set<Contact> contacts = new HashSet<Contact>();
         
         Contact kabir = new Contact();
         kabir.setId(new Integer(1));
         kabir.setCustomer(customer);
         kabir.setName("Kabir");
         kabir.setTlf("1111");
         contacts.add(kabir);
         
         Contact bill = new Contact();
         bill.setId(new Integer(2));
         bill.setCustomer(customer);
         bill.setName("Bill");
         bill.setTlf("2222");
         contacts.add(bill);

         customer.setContacts(contacts);

         manager.persist(customer);
         return customer;
      }
      catch (RuntimeException e)
      {
         throw e;
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
      finally
      {
         System.out.println("CREATE CUSTOMER -  END");         
      }
   }

   @Override
   public Customer findByCustomerId(Integer id)
   {
      System.out.println("FIND CUSTOMER");         
      this.clear();
      try
      {
         Customer customer = manager.find(Customer.class, id);
         return customer;
      }
      catch (RuntimeException e)
      {
         throw e;
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
      finally
      {
         System.out.println("FIND CUSTOMER -  END");         
      }
   }
   
   @Override
   public String loadedFromCache()
   {
      System.out.println("CHECK CACHE");         
      try
      {
         System.out.println("Visited: " + this.visited);
         if (!this.visited.contains("Customer#1"))
            return "Customer#1 was not in cache";
         if (!this.visited.contains("Contact#1"))
            return "Contact#1 was not in cache";
         if (!this.visited.contains("Contact#2"))
            return "Contact#2 was not in cache";
         if (!this.visited.contains("Customer.contacts#1"))
            return "Customer.contacts#1 was not in cache";
         return null;
      }
      finally
      {
         System.out.println("CHECK CACHE -  END");         
      }
      
   }
   
   @Override
   @Remove
   public void cleanup()
   {
      for (Cache<?, ?> cache: caches)
      {
         cache.removeListener(this);
      }
      
      try
      {
         if (manager != null)
         {
            Customer c = findByCustomerId(new Integer(1));
            if (c != null)
            {
               for (Contact contact: c.getContacts())
               {
                  manager.remove(contact);
               }
               c.setContacts(null);
               manager.remove(c);
            }
         }
      }
      catch (Exception e)
      {
         log.error("Caught exception in cleanup", e);
      }
   }

   private Set<String> visited = new ConcurrentSkipListSet<String>(); 
   
   public void clear()
   {
      visited.clear();
   }
   
   @CacheEntryVisited
   public void entryVisited(CacheEntryVisitedEvent event)
   {
      if (!event.isPre())
      {
         String key = event.getKey().toString();
         System.out.println("MyListener - Visiting node " + key);
         String token = ".clusteredentity.";
         int index = key.indexOf(token);
         if (index > -1)
         {
            index += token.length();
            String name = key.substring(index);
            System.out.println("MyListener - recording visit to " + name);
            visited.add(name);
         }
      }
   }
}
