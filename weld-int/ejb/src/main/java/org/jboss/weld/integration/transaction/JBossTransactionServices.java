/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.weld.integration.transaction;

import static javax.transaction.Status.STATUS_ACTIVE;

import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import com.arjuna.ats.jbossatx.jta.TransactionManagerService;
import org.jboss.weld.transaction.spi.TransactionServices;

/**
 * JBoss AS implementation of TransactionServices. The transaction manager for
 * the application server is injected directly into this bean and used to
 * provide the services.
 *
 * @author David Allen
 * @author ales.justin@jboss.org
 *
 */
public class JBossTransactionServices implements TransactionServices
{

   /** The TM */
   private final TransactionManagerService transactionManager;

   public JBossTransactionServices(TransactionManagerService transactionManager)
   {
      this.transactionManager = transactionManager;
   }

   public boolean isTransactionActive()
   {
      try
      {
         return getUserTransaction().getStatus() == STATUS_ACTIVE;
      }
      catch (SystemException e)
      {
         throw new RuntimeException("Failed to determine transaction status", e);
      }
   }

   public TransactionManagerService getTransactionManager()
   {
      return transactionManager;
   }

   public void registerSynchronization(Synchronization synchronizedObserver)
   {
      try
      {
         getTransactionManager().getTransactionManager().getTransaction().registerSynchronization(synchronizedObserver);
      }
      catch (Exception e)
      {
         throw new RuntimeException("Failed to register synchronization " + synchronizedObserver + " for current transaction", e);
      }
   }

   public UserTransaction getUserTransaction()
   {
      return transactionManager.getUserTransaction();
   }

   public void cleanup() {}
}