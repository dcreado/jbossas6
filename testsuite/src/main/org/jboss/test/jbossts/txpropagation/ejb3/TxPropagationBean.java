/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2008,
 * @author JBoss Inc.
 */
package org.jboss.test.jbossts.txpropagation.ejb3;

import org.jboss.logging.Logger;
import org.jboss.test.jbossts.recovery.ASFailureSpec;
import org.jboss.test.jbossts.recovery.TestASRecoveryWithJTSPropagation;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transaction;

@Stateless
public class TxPropagationBean implements TxPropagationRem 
{
   private static Logger log = Logger.getLogger(TxPropagationBean.class);


   // Dedicated for the first call of the propagation tests.
   @TransactionAttribute(TransactionAttributeType.REQUIRED)
   public String testXA(ASFailureSpec[] fSpecsNode, ASFailureSpec[] fSpecsRemoteNode, String testEntityPK, String testEntityPKRemoteNode, String remoteHost, boolean expectFailureOnRemoteNode, boolean useOTS, int remoteJndiPort, int remoteCorbaPort)
   {
      log.info("CMT testXA called with " + fSpecsNode.length + "(this node) and " + fSpecsRemoteNode.length + "(remote node) specs and testEntityPK=" + testEntityPK + ", testEntityPKRemoteNode=" + testEntityPKRemoteNode + ", remoteHost=" + remoteHost + ", expectFailureOnRemoteNode=" + expectFailureOnRemoteNode + ", useOTS=" + useOTS);

      TestASRecoveryWithJTSPropagation xatest = new TestASRecoveryWithJTSPropagation();
      Transaction tx;

      try
      {
         tx = com.arjuna.ats.jta.TransactionManager.transactionManager().getTransaction();
      }
      catch (javax.transaction.SystemException e)
      {
         tx = null;
      }

      if (tx == null)
      {
         log.error("CMT method called without a transaction");

         return "Failed";
      }
      else
      {
         xatest.setTestEntityPK(testEntityPK);
         xatest.setRemoteHost(remoteHost);
         xatest.setfSpecsRemoteNode(fSpecsRemoteNode);
         xatest.setTestEntityPKRemoteNode(testEntityPKRemoteNode);
         xatest.setExpectFailureOnRemoteNode(expectFailureOnRemoteNode);
         xatest.setRemoteJndiPort(remoteJndiPort);
         xatest.setRemoteCorbaPort(remoteCorbaPort);
         xatest.setUseEJB3(true);
         xatest.setUseOTS(useOTS);      

         for (ASFailureSpec spec : fSpecsNode)
            xatest.addResource(spec);

         return xatest.startTest(tx) ? "Passed" : "Failed";
      }
   }

   // Dedicated for the second call (remote side) of the propagation tests, i.e. it expects to be run within an existing transaction. 
   @TransactionAttribute(TransactionAttributeType.MANDATORY)
   public String testXA(ASFailureSpec[] fSpecsNode, String testEntityPK)
   {
      log.info("CMT testXA called with " + fSpecsNode.length + " specs and testEntityPK=" + testEntityPK);

      TestASRecoveryWithJTSPropagation xatest = new TestASRecoveryWithJTSPropagation();
      Transaction tx;

      try
      {
         tx = com.arjuna.ats.jta.TransactionManager.transactionManager().getTransaction();
      }
      catch (javax.transaction.SystemException e)
      {
         tx = null;
      }

      if (tx == null)
      {
         log.error("CMT testXA called without a transaction");

         return "Failed";
      }
      else
      {
         xatest.setTestEntityPK(testEntityPK);

         for (ASFailureSpec spec : fSpecsNode)
            xatest.addResource(spec);

         return xatest.startTest(tx) ? "Passed" : "Failed";
      }
   }
}
