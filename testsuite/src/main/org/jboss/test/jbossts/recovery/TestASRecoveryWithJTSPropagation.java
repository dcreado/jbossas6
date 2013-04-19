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
package org.jboss.test.jbossts.recovery;

import javax.ejb.EJBTransactionRolledbackException;
import javax.transaction.Transaction;

import org.jboss.logging.Logger;
import org.jboss.remoting.CannotConnectException;
import org.jboss.test.jbossts.txpropagation.EJBUtils;
import org.jboss.test.jbossts.txpropagation.ejb2.TxPropagationEJB2Rem;
import org.jboss.test.jbossts.txpropagation.ejb3.TxPropagationRem;

/**
 * Extends TestASRecovery class by calling an ejb bean on a remote host.
 */
public class TestASRecoveryWithJTSPropagation extends TestASRecoveryWithJPA
{
   private static Logger log = Logger.getLogger(TestASRecoveryWithJTSPropagation.class);

   private String remoteHost = null;
   private int remoteJndiPort = 1099;
   private int remoteCorbaPort = 3528;
   private ASFailureSpec[] fSpecsRemoteNode = null;
   private String testEntityPKRemoteNode = null;
   private boolean expectFailureOnRemoteNode = false;
   
   private boolean useOTS = false;
   private boolean useEJB3 = false;

   
   /**
    * Calls update of a test entity.
    */
   @Override
   protected boolean addTxResources(Transaction tx)
   {
      boolean result = super.addTxResources(tx);
      result = result && callRemoteBean();
      
      return result;
   }

   private boolean callRemoteBean()
   {
      if (remoteHost == null)
         return true;
      
      
      log.info("calling remote bean with params; remote host " + remoteHost + ", useEJB3 " + useEJB3 + ", useOTS " + useOTS);
      
      try
      {
         String result = null;
         
         if (useEJB3)
         {
            TxPropagationRem cr = EJBUtils.lookupTxPropagationBeanEJB3(remoteHost, remoteJndiPort, remoteCorbaPort, useOTS);
            result = cr.testXA(fSpecsRemoteNode, testEntityPKRemoteNode);
         }
         else 
         {
            TxPropagationEJB2Rem cr = EJBUtils.lookupTxPropagationBeanEJB2(remoteHost, remoteJndiPort, remoteCorbaPort, useOTS);
            result = cr.testXA(fSpecsRemoteNode, testEntityPKRemoteNode);
         }
         
         log.info("result = " + result);
         return "Passed".equalsIgnoreCase(result);
      }
      catch (CannotConnectException e)
      {
         if (expectFailureOnRemoteNode)
         {
            // try to recover, this failure was expected
            log.info("Failure was expected: " + e.getMessage(), e);
            throw e;
         }
         else
         {
            log.error(e);
            e.printStackTrace();
         }
      }
      catch (EJBTransactionRolledbackException re)
      {
         // try to recover, this failure was expected maybe?!
         log.info("Failure was expected (maybe): " + re.getMessage(), re);
         throw re;
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
      }

      return false;
   }

   
   public void setRemoteHost(String remoteHost)
   {
      this.remoteHost = remoteHost;
   }

   public void setfSpecsRemoteNode(ASFailureSpec[] fSpecsRemoteNode)
   {
      this.fSpecsRemoteNode = fSpecsRemoteNode;
   }

   public void setTestEntityPKRemoteNode(String testEntityPKRemoteNode)
   {
      this.testEntityPKRemoteNode = testEntityPKRemoteNode;
   }

   public void setExpectFailureOnRemoteNode(boolean expectFailureOnRemoteNode)
   {
      this.expectFailureOnRemoteNode = expectFailureOnRemoteNode;
   }

   public void setUseOTS(boolean useOTS)
   {
      this.useOTS = useOTS;
   }

   public void setUseEJB3(boolean useEJB3)
   {
      this.useEJB3 = useEJB3;
   }

   public void setRemoteJndiPort(int remoteJndiPort)
   {
      this.remoteJndiPort = remoteJndiPort;
   }
   
   public void setRemoteCorbaPort(int remoteCorbaPort)
   {
      this.remoteCorbaPort = remoteCorbaPort;
   }

}
