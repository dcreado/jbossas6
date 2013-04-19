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
package org.jboss.test.cluster.clusteredentity.classloader;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PreDestroy;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryVisitedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.ha.ispn.DefaultCacheContainerRegistry;
import org.jboss.logging.Logger;

/**
 * Comment
 * 
 * @author Brian Stansberry
 * @version $Revision: 60233 $
 */
@Stateful
@Remote(EntityQueryTest.class)
@RemoteBinding(jndiBinding="EntityQueryTestBean/remote")
@Listener(sync = false)
public class EntityQueryTestBean implements EntityQueryTest
{
   private static final Logger log = Logger.getLogger(EntityQueryTestBean.class);
   
   @PersistenceContext
   private EntityManager manager;
   
   private List<Cache<?, ?>> caches = new CopyOnWriteArrayList<Cache<?, ?>>();

   public EntityQueryTestBean()
   {      
   }
   
   @Override
   public void getCache(String regionPrefix)
   {
      try
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
         container.addListener(this);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }
   
   @Override
   public void updateAccountBranch(Integer id, String branch)
   {
      Account account = manager.find(Account.class, id);
      account.setBranch(branch);
   }
   
   @Override
   public int getCountForBranch(String branch, boolean useNamed, boolean useRegion)
   {
      return useNamed ? getCountForBranchViaNamedQuery(branch, useRegion) : getCountForBranchViaLocalQuery(branch, useRegion);
   }
   
   private int getCountForBranchViaLocalQuery(String branch, boolean useRegion)
   {
      Query query = manager.createQuery("select account from Account as account where account.branch = ?1");
      query.setParameter(1, branch);
      if (useRegion)
      {
         query.setHint("org.hibernate.cacheRegion", "AccountRegion");
      }
      query.setHint("org.hibernate.cacheable", Boolean.TRUE);
      return query.getResultList().size();
      
   }
   
   private int getCountForBranchViaNamedQuery(String branch, boolean useRegion)
   {
      String queryName = useRegion ? "account.bybranch.namedregion"
                                   : "account.bybranch.default";
      Query query = manager.createNamedQuery(queryName);
      query.setParameter(1, branch);
      return query.getResultList().size();      
   }
   
   /* (non-Javadoc)
    * @see org.jboss.ejb3.test.clusteredentity.EntityQueryTest#createAccount(org.jboss.ejb3.test.clusteredentity.AccountHolderPK, Integer, Integer)
    */
   @Override
   public void createAccount(AccountHolderPK pk, Integer id, Integer openingBalance, String branch)
   {
      Account account = new Account();
      account.setId(id);
      account.setAccountHolder(pk);
      account.setBalance(openingBalance);
      account.setBranch(branch);
      manager.persist(account);
   }
   
   @Override
   public void updateAccountBalance(Integer id, Integer newBalance)
   {
      Account account = manager.find(Account.class, id);
      account.setBalance(newBalance);
   }
   
   @Override
   public String getBranch(AccountHolderPK pk, boolean useNamed, boolean useRegion)
   {
      return useNamed ? getBranchViaNamedQuery(pk, useRegion) : getBranchViaLocalQuery(pk, useRegion);
   }
   
   private String getBranchViaLocalQuery(AccountHolderPK pk, boolean useRegion)
   {
      Query query = manager.createQuery("select account.branch from Account as account where account.accountHolder = ?1");
      query.setParameter(1, pk);
      if (useRegion)
      {
         query.setHint("org.hibernate.cacheRegion", "AccountRegion");
      }
      query.setHint("org.hibernate.cacheable", Boolean.TRUE);
      return (String) query.getResultList().get(0);
   }
   
   /* (non-Javadoc)
    * @see org.jboss.ejb3.test.clusteredentity.EntityQueryTest#getPostCodeViaNamedQuery(org.jboss.ejb3.test.clusteredentity.AccountHolderPK, boolean)
    */
   private String getBranchViaNamedQuery(AccountHolderPK pk, boolean useRegion)
   {
      String queryName = useRegion ? "account.branch.namedregion"
                                   : "account.branch.default";
      Query query = manager.createNamedQuery(queryName);
      query.setParameter(1, pk);
      return (String) query.getResultList().get(0);
   }
   
   @Override
   public int getTotalBalance(AccountHolderPK pk, boolean useNamed, boolean useRegion)
   {
      return useNamed ? getTotalBalanceViaNamedQuery(pk, useRegion) : getTotalBalanceViaLocalQuery(pk, useRegion);
   }
   
   private int getTotalBalanceViaLocalQuery(AccountHolderPK pk, boolean useRegion)
   {
      Query query = manager.createQuery("select account.balance from Account as account where account.accountHolder = ?1");
      query.setParameter(1, pk);
      query.setHint("org.hibernate.cacheable", Boolean.TRUE);
      return totalBalances(query);
   }
   
   private int getTotalBalanceViaNamedQuery(AccountHolderPK pk, boolean useRegion)
   {
      String queryName = useRegion ? "account.totalbalance.namedregion"
                                   : "account.totalbalance.default";
      Query query = manager.createNamedQuery(queryName);
      query.setParameter(1, pk);
      return totalBalances(query);
   }
   
   private int totalBalances(Query balanceQuery)
   {
      List<?> results = balanceQuery.getResultList();
      int total = 0;
      for (Object result: results)
      {
         total += ((Integer) result).intValue();
         System.out.println("Total = " + total);
      }
      return total;      
   }
   
   @Override
   public boolean getSawRegionModification(String regionName)
   {
      return this.modified.remove(regionName);
   }
   
   @Override
   public boolean getSawRegionAccess(String regionName)
   {
      return this.accessed.remove(regionName);
   }
   
   @Override
   public void cleanup()
   {
      internalCleanup();
   }
   
   private void internalCleanup()
   {  
      if (manager != null)
      {
         Query query = manager.createQuery("select account from Account as account");
         List<?> accts = query.getResultList();
         for (Object acct: accts)
         {
            try
            {
               log.info("Removing " + acct);
               manager.remove(acct);
            }
            catch (Exception ignored) {}
         }
      }      
   }
   
   @Override
   @PreDestroy
   @Remove
   public void remove(boolean removeEntities)
   {
      if (removeEntities)
      {
         try
         {
            internalCleanup();
         }
         catch (Exception e)
         {
            log.error("Caught exception in remove", e);
         }
      }
      
      try
      {
         this.clear();
         for (Cache<?, ?> cache: this.caches)
         {
            cache.removeListener(this);
         }
         this.caches.clear();
         DefaultCacheContainerRegistry.getInstance().getCacheContainer("hibernate").removeListener(this);
      }
      catch (Exception e)
      {
        log.error("Caught exception in remove", e);
      }
   }

   private Set<String> modified = new ConcurrentSkipListSet<String>();
   private Set<String> accessed = new ConcurrentSkipListSet<String>();
   
   public void clear()
   {
      modified.clear();
      accessed.clear();
   }
   
   @CacheStarted
   public void cacheStarted(CacheStartedEvent event)
   {
      Cache<?, ?> cache = event.getCacheManager().getCache(event.getCacheName());
      System.out.println("Adding cache listener for " + event.getCacheName());
      cache.addListener(this);
      this.caches.add(cache);
   }
   
   @CacheEntryModified
   public void entryModified(CacheEntryModifiedEvent event)
   {
      if (!event.isPre())
      {
         String region = event.getCache().getName();
         System.out.println("MyListener - Modified cache entry " + region);
         modified.add(region);
      }
   }

   @CacheEntryCreated
   public void entryCreated(CacheEntryCreatedEvent event)
   {  
      if (!event.isPre())
      {
         String region = event.getCache().getName();
         System.out.println("MyListener - Created cache entry " + region);
         modified.add(region);
      }
   }

   @CacheEntryVisited
   public void entryVisited(CacheEntryVisitedEvent event)
   {  
      if (!event.isPre())
      {
         String region = event.getCache().getName();
         System.out.println("MyListener - Visited cache entry " + region);
         accessed.add(region);
      }
   }
}
