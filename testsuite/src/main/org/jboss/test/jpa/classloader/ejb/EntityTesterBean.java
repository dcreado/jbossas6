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
package org.jboss.test.jpa.classloader.ejb;

import java.util.Iterator;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.logging.Logger;
import org.jboss.test.jpa.classloader.model.Account;
import org.jboss.test.jpa.classloader.model.AccountHolder;

/**
 * Comment
 * 
 * @author Brian Stansberry
 * @version $Revision: 60233 $
 */
@Stateful
@Remote(EntityTester.class)
@RemoteBinding(jndiBinding="EntityTesterBean/remote")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class EntityTesterBean implements EntityTester
{
   private static final Logger log = Logger.getLogger(EntityTesterBean.class);
   
   @PersistenceContext
   private EntityManager manager;

   public EntityTesterBean()
   {      
   }
   
   public Account getAccount(Integer id)
   {
      return (Account) manager.find(Account.class, id);
   }
   
   public void createAccount(AccountHolder holder, Integer id, Integer openingBalance, String branch)
   {
      Account account = new Account();
      account.setId(id);
      account.setAccountHolder(holder);
      account.setBalance(openingBalance);
      account.setBranch(branch);
      manager.persist(account);
      log.info("Persisted Account " + id);
   }
   
   public void cleanup()
   {
      internalCleanup();
   }
   
   private void internalCleanup()
   {  
      if (manager != null)
      {
         Query query = manager.createQuery("select account from Account as account");
         List accts = query.getResultList();
         if (accts != null)
         {
            for (Iterator it = accts.iterator(); it.hasNext();)
            {
               try
               {
                  Account acct = (Account) it.next();
                  log.info("Removing " + acct);
                  manager.remove(acct);
               }
               catch (Exception ignored) {}
            }
         }
      }      
   }
   
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
   }
}
