/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.test.cts.ejb;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.ejb.plugins.AbstractInterceptor;
import org.jboss.invocation.Invocation;

/**
 * Used by StatefulSessionUnitTestCase.testBadUserTx() to
 * confirm that the user tx is properly rolled back
 *
 * @author Brian Stansberry
 * 
 * @version $Revision: 99138 $
 */
public class BadUserTxInterceptor extends AbstractInterceptor
{
   /**
    * Local reference to the container's TransactionManager.
    */
   private TransactionManager tm;
   
   public void create() throws Exception
   {
      super.create();
      tm = getContainer().getTransactionManager();
   }

   @Override
   public Object invoke(Invocation mi) throws Exception
   {
      try
      {
         return super.invoke(mi);
      }
      finally
      {   
         // The bean started a transaction; we register a 
         // synchronization to monitor that another interceptor
         // rolls it back
         if ("testBadUserTx".equals(mi.getMethod().getName()))
         {
            Transaction tx = tm.getTransaction();
            if (tx != null)
            {
               tx.registerSynchronization(new BadTxSynchronization());
               log.debug("Registered BdTxSynchronization");
            }
            else
            {
               log.error("No transaction registered!!");
            }
         }
         else
         {
            log.trace("ignoring method " + mi.getMethod().getName());
         }
      }
   }
   
   private class BadTxSynchronization implements Synchronization
   {
      public void beforeCompletion()
      {
         log.debug("BdTxSynchronization received beforeCompletion() callback");
      }

      public void afterCompletion(int status)
      {
         // Store the status in JNDI so the test driver can read it. Hack-a-licious!
         try
         {
            new InitialContext().bind("testBadUserTx", Integer.valueOf(status));
            log.debug("BdTxSynchronization afterCompletion() status = " + status);
         }
         catch (NamingException e)
         {
            log.error("Cannot bind bad user tx status in JNDI", e);
         }         
      }
      
   }

}
