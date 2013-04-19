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
package org.jboss.test.jbossts.JTSContextPropagation01;

import org.jboss.test.jbossts.recovery.ASFailureMode;
import org.jboss.test.jbossts.recovery.ASFailureSpec;
import org.jboss.test.jbossts.recovery.CrashHelperRem;
import org.jboss.test.jbossts.recovery.RecoveredXid;
import org.jboss.test.jbossts.taskdefs.JUnitClientTest;
import org.jboss.test.jbossts.taskdefs.TransactionLog;
import org.jboss.test.jbossts.taskdefs.Utils;
import org.jboss.test.jbossts.txpropagation.EJBUtils;
import org.jboss.test.jbossts.txpropagation.ORBWrapper;
import org.jboss.test.jbossts.txpropagation.ejb2.TxPropagationEJB2Rem;
import org.jboss.test.jbossts.txpropagation.ejb3.TxPropagationRem;
import org.jboss.test.jbossts.crash.CrashHelper;
import org.jboss.test.jbossts.crash.TestEntity;
import org.jboss.test.jbossts.crash.TestEntityHelper;
import org.jboss.test.jbossts.crash.TestEntityHelperRem;
import org.jboss.remoting.CannotConnectException;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.apache.tools.ant.BuildException;

import com.arjuna.ats.jts.OTSManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJBTransactionRolledbackException;
import javax.transaction.UserTransaction;

/**
 * JTS context propagation tests.
 * 
 * @author <a href="istudens@redhat.com">Ivo Studensky</a>
 * @version $Revision: 1.1 $
 */
public class TestContextPropagation extends JUnitClientTest
{
   // the longest time to wait in millis before declaring a test a failed (overridable)
   private static final int MAX_TEST_TIME = 5*60 * 1000;     // 5 minutes  - allows two intervals of recovery which is 2 minutes by default

   private boolean useOTS = false;
   private boolean useEJB3 = false;
   private boolean clientTx = false;
   private boolean rollbackExpected = false;
   private boolean wipeOutTxsInDoubt = false;
   private boolean wipeOutTxsInDoubtBeforeTest = false;
   private boolean wipeOutTxsInDoubtAfterTest = false;
   private int     maxTestTime = MAX_TEST_TIME;

   private String               storeDirServer0 = null;
   private String               storeDirServer1 = null;
   private String               storeImple = "com.arjuna.ats.internal.arjuna.objectstore.HashedActionStore";
   private String               storeType = "StateManager/BasicAction/TwoPhaseCoordinator/ArjunaTransactionImple.*";
   private TransactionLog       storeServer0;
   private TransactionLog       storeServer1;
   private int                  existingUidsServer0;
   private int                  existingUidsServer1;
   private Set<RecoveredXid>    existingXidsInDoubtServer0;
   private Set<RecoveredXid>    existingXidsInDoubtServer1;
   private boolean              expectFailureNode0 = false;
   private boolean              expectFailureNode1 = false;
   private boolean              expectException = false;

   private String   serverName0 = "default";
   private String   serverName1 = "default";
   private String   host0 = "127.0.0.1";
   private String   host1 = "127.0.0.1";
   private int      jndiPort0 = 1099; 
   private int      jndiPort1 = 1199; 
   private int      corbaPort0 = 3528; 
   private int      corbaPort1 = 3628; 

   private UserTransaction tx = null;
   private static ORBWrapper orb = null;


   public void testAction()
   {
      if (config == null || params == null)
         throw new UnsupportedOperationException("The test has not been initiated yet. Call the init() method first.");

      StringBuilder sb = new StringBuilder();
      ASFailureSpec[] fSpecsNode0 = null;
      ASFailureSpec[] fSpecsNode1 = null;

      for (Map.Entry<String, String> me : params.entrySet())
      {
         String key = me.getKey().trim();
         String val = me.getValue().trim();

         if ("name".equals(key))
            setName(val);
         else if ("ots".equals(key))
            useOTS = val.equalsIgnoreCase("true");
         else if ("ejb3".equals(key))
            useEJB3 = val.equalsIgnoreCase("true");
         else if ("serverName0".equals(key))
            serverName0 = val;
         else if ("serverName1".equals(key))
            serverName1 = val;
         else if ("host0".equals(key))
            host0 = val;
         else if ("host1".equals(key))
            host1 = val;
         else if ("jndiPort0".equals(key))
            jndiPort0 = Integer.parseInt(val);
         else if ("jndiPort1".equals(key))
            jndiPort1 = Integer.parseInt(val);
         else if ("corbaPort0".equals(key))
            corbaPort0 = Integer.parseInt(val);
         else if ("corbaPort1".equals(key))
            corbaPort1 = Integer.parseInt(val);
         else if ("storeType".equals(key))
            storeType = val;
         else if ("storeDirServer0".equals(key))
            storeDirServer0 = val;
         else if ("storeDirServer1".equals(key))
            storeDirServer1 = val;
         else if ("clientTx".equals(key))
            clientTx = val.equalsIgnoreCase("true");
         else if ("storeImple".equals(key))
            storeImple = val;
         else if ("testTime".equals(key))
            maxTestTime = Utils.parseInt(val, "parameter testTime should represent a number of miliseconds: ");
         else if ("specs0".equals(key))
            fSpecsNode0 = parseSpecs(val, sb, key);
         else if ("specs1".equals(key))
            fSpecsNode1 = parseSpecs(val, sb, key);
         else if ("wait".equals(key))
            suspendFor(Integer.parseInt(val));
         else if ("rollbackExpected".equals(key))
            rollbackExpected = val.equalsIgnoreCase("true");
         else if ("wipeOutTxsInDoubt".equals(key))
            wipeOutTxsInDoubt = val.equalsIgnoreCase("true");
         else if ("wipeOutTxsInDoubtBeforeTest".equals(key))
            wipeOutTxsInDoubtBeforeTest = val.equalsIgnoreCase("true");
         else if ("wipeOutTxsInDoubtAfterTest".equals(key))
            wipeOutTxsInDoubtAfterTest = val.equalsIgnoreCase("true");
      }

      sb.insert(0, ":\n").insert(0, getName()).insert(0, "Executing test ");

      System.out.println(sb);

      try 
      {
         // get a handle to the transaction logs
         storeServer0 = initStore(serverName0, storeDirServer0);
         storeServer1 = initStore(serverName1, storeDirServer1);

         // this test may halt the VM so make sure the transaction log is empty
         // before starting the test - then the pass/fail check is simply to
         // test whether or not the log is empty (see recoverUids() below).
         if (expectFailureNode0)
         {
            clearXidsOnServer0();
         }
         if (expectFailureNode1)
         {
            clearXidsOnServer1();
         }
         
         existingUidsServer0 = getPendingUids(storeServer0);
         existingUidsServer1 = getPendingUids(storeServer1);

         if (wipeOutTxsInDoubtBeforeTest)
         {
            wipeOutTxsInDoubt(serverName0);
            wipeOutTxsInDoubt(serverName1);
         }

         existingXidsInDoubtServer0 = lookupCrashHelper(serverName0).checkXidsInDoubt();
         existingXidsInDoubtServer1 = lookupCrashHelper(serverName1).checkXidsInDoubt();
         if (isDebug) 
         {
            print(existingXidsInDoubtServer0.size() + " txs in doubt in database on server0 before test run");
            print(existingXidsInDoubtServer1.size() + " txs in doubt in database on server1 before test run");
         }
         
         // name of this test will be the primary key for test record in DB
         String testEntityPKServer0 = getName() + "_" + serverName0;
         String testEntityPKServer1 = getName() + "_" + serverName1;
         
         // initialize databases
         TestEntity initEntityServer0 = initDatabase(serverName0, testEntityPKServer0);
         TestEntity initEntityServer1 = initDatabase(serverName1, testEntityPKServer1);

         // run the crash test
         boolean result = propagationTest(fSpecsNode0, fSpecsNode1, testEntityPKServer0, testEntityPKServer1);
         print("test result: " + result);

         // checking the state of DB after recovering
         boolean dbChangedServer0 = true;
         boolean dbChangedServer1 = true;
         if (result)
         {
            dbChangedServer0 = checkDatabase(serverName0, testEntityPKServer0, initEntityServer0);
            dbChangedServer1 = checkDatabase(serverName1, testEntityPKServer1, initEntityServer1);
            print("checkDatabase result for server0: " + dbChangedServer0);
            print("checkDatabase result for server1: " + dbChangedServer1);
         }

         Set<RecoveredXid> xidsInDoubtAfterTestServer0 = lookupCrashHelper(serverName0).checkXidsInDoubt();
         Set<RecoveredXid> xidsInDoubtAfterTestServer1 = lookupCrashHelper(serverName1).checkXidsInDoubt();
         if (wipeOutTxsInDoubt || wipeOutTxsInDoubtAfterTest)
         {
            wipeOutTxsInDoubt(serverName0, existingXidsInDoubtServer0, xidsInDoubtAfterTestServer0);
            wipeOutTxsInDoubt(serverName1, existingXidsInDoubtServer1, xidsInDoubtAfterTestServer1);
         }


         assertTrue("Propagation test failed.", result);
         assertTrue("Incorrect data in database on server0.", dbChangedServer0);
         assertTrue("Incorrect data in database on server1.", dbChangedServer1);
         assertEquals("There are still unrecovered txs in database on server0 after the test.", existingXidsInDoubtServer0.size(), xidsInDoubtAfterTestServer0.size());
         assertEquals("There are still unrecovered txs in database on server0 after the test.", existingXidsInDoubtServer1.size(), xidsInDoubtAfterTestServer1.size());
      }
      catch (Exception e)
      {
         if (isDebug)
            e.printStackTrace();

         throw new BuildException(e);
      }
   }

   private TransactionLog initStore(String serverName, String storeDir)
   {
      String serverPath = config.getServerPath(serverName);

      if (storeDir == null)
         storeDir = serverPath + "data/tx-object-store";
      else
         storeDir = serverPath + storeDir;
      
      TransactionLog store = new TransactionLog(storeDir, storeImple);
      
      return store;
   }

   private boolean propagationTest(ASFailureSpec[] fSpecsNode0, ASFailureSpec[] fSpecsNode1, String testEntityPK0, String testEntityPK1) throws Exception
   {
      try
      {
         String res = null;

         if (useOTS)
            initORB();
            
         if (clientTx)
            startTx(host0, jndiPort0);

         if (useEJB3)
         {
            TxPropagationRem cr = EJBUtils.lookupTxPropagationBeanEJB3(host0, jndiPort0, corbaPort0, useOTS);
            res = cr.testXA(fSpecsNode0, fSpecsNode1, testEntityPK0, testEntityPK1, host1, expectFailureNode1, useOTS, jndiPort1, corbaPort1);
         }
         else
         {
            TxPropagationEJB2Rem cr = EJBUtils.lookupTxPropagationBeanEJB2(host0, jndiPort0, corbaPort0, useOTS);
            res = cr.testXA(fSpecsNode0, fSpecsNode1, testEntityPK0, testEntityPK1, host1, expectFailureNode1, useOTS, jndiPort1, corbaPort1);
         }
         
         // in case of commit_halt on node1 there is no thrown exception, though that we need to pass through recoverUids()
         return "Passed".equalsIgnoreCase(res) && recoverUids();
      }
      catch (CannotConnectException e)
      {
         if (expectFailureNode0 || expectFailureNode1)
         {
            print("Failure was expected: " + e.getMessage());

            return recoverUids();
         }
         else
         {
            print("propagationTest: Caught[1] " + e);

            e.printStackTrace();
         }
      }
      catch (Throwable t)
      {
         if (expectException ||
             (t instanceof EJBTransactionRolledbackException
               || t instanceof java.rmi.ServerException
               || t instanceof org.jboss.tm.JBossTransactionRolledbackException
               || t instanceof javax.transaction.TransactionRolledbackException))
         {
            // try to recover, this failure was expected (maybe?!)
            print("Failure was expected (maybe?): " + t.getMessage());
            
            return recoverUids();
         } 
         else
         {
            print("propagationTest: Caught[2] " + t);
            
            t.printStackTrace();
         }
      }
      finally 
      {
         try 
         {
            if (clientTx)
               endTx();
         }
         finally
         {
            if (orb != null)
               orb.stop();
         }
      }

      return false;
   }

   /**
    * Wait for any pending transactions to recover by restarting both of the AS.
    * @return true if all pending branches have been recovered
    * @throws IOException if the server cannot be started
    */
   private boolean recoverUids() throws IOException
   {
      int retryPeriod = 60 * 1000;    // 1 minute 
      int maxWait = maxTestTime;

      // wait for the server to start up the first time through, we will need it for later database checking

      Set<RecoveredXid> xidsInDoubtAfterTest0;
      Set<RecoveredXid> xidsInDoubtAfterTest1;
      int pendingUids0;
      int pendingUids1;
      int pendingXidsInDoubt0;
      int pendingXidsInDoubt1;
      int existingXidsInDoubt0Cnt = existingXidsInDoubtServer0.size();
      int existingXidsInDoubt1Cnt = existingXidsInDoubtServer1.size();

      
      if (expectFailureNode0 || expectFailureNode1)
      {
         suspendFor(2 * 1000);   // a little waiting is needed sometimes in order to be able to start server again, 2 secs
      
         if (expectFailureNode0)
            config.startServer(serverName0);
         if (expectFailureNode1)
            config.startServer(serverName1);
      }

      do
      {
         pendingUids0 = getPendingUids(storeServer0);
         pendingUids1 = getPendingUids(storeServer1);
         try 
         {
            xidsInDoubtAfterTest0 = lookupCrashHelper(serverName0).checkXidsInDoubt();
            xidsInDoubtAfterTest1 = lookupCrashHelper(serverName1).checkXidsInDoubt();
         }
         catch (Exception e)
         {
            return false;
         }
         pendingXidsInDoubt0 = xidsInDoubtAfterTest0.size();
         pendingXidsInDoubt1 = xidsInDoubtAfterTest1.size();

         if (pendingUids0 == -1 || pendingUids1 == -1)
            return false;   // object store error

         if (pendingUids0 <= existingUidsServer0 && pendingXidsInDoubt0 <= existingXidsInDoubt0Cnt
               && pendingUids1 <= existingUidsServer1 && pendingXidsInDoubt1 <= existingXidsInDoubt1Cnt)
            return true;    // all uids in AS and txs in doubt in DB have been recovered

         pendingUids0 -= existingUidsServer0;
         pendingUids1 -= existingUidsServer1;
         pendingXidsInDoubt0 -= existingXidsInDoubt0Cnt;
         pendingXidsInDoubt1 -= existingXidsInDoubt1Cnt;

         print("waiting for " + pendingUids0 + " branches on server0");
         print("waiting for " + pendingXidsInDoubt0 + " txs in doubt on server0");
         print("waiting for " + pendingUids1 + " branches on server1");
         print("waiting for " + pendingXidsInDoubt1 + " txs in doubt on server1\n");

         suspendFor(retryPeriod);
         maxWait -= retryPeriod;

      } while (maxWait > 0);

      // the test failed to recover some uids - clear them out ready for the next test
      if (pendingUids0 > 0)
      {
         clearXidsOnServer0();
      }
      if (pendingUids1 > 0)
      {
         clearXidsOnServer1();
      }

      if (pendingXidsInDoubt0 > 0) 
      {
         print(pendingXidsInDoubt0 + " new txs in doubt in database on server0 after the test");

         if (wipeOutTxsInDoubt || wipeOutTxsInDoubtAfterTest)
         {
            wipeOutTxsInDoubt(serverName0, existingXidsInDoubtServer0, xidsInDoubtAfterTest0);
         }
      }
      if (pendingXidsInDoubt1 > 0) 
      {
         print(pendingXidsInDoubt1 + " new txs in doubt in database on server1 after the test");

         if (wipeOutTxsInDoubt || wipeOutTxsInDoubtAfterTest)
         {
            wipeOutTxsInDoubt(serverName1, existingXidsInDoubtServer1, xidsInDoubtAfterTest1);
         }
      }

      return false;
   }

   private TestEntity initDatabase(String serverName, String entityPK) throws Exception
   {
      TestEntityHelperRem hlp = (TestEntityHelperRem) config.getNamingContext(serverName).lookup(TestEntityHelper.REMOTE_JNDI_NAME);

      TestEntity initEntity = hlp.initTestEntity(entityPK);
//      if (isDebug)
//         print("TestContextPropagation#initDatabase(serverName=" + serverName + "): initEntity = " + initEntity);

      return initEntity;
   }

   private boolean checkDatabase(String serverName, String entityPK, TestEntity initEntity)
   {
      try
      {
         TestEntityHelperRem hlp = (TestEntityHelperRem) config.getNamingContext(serverName).lookup(TestEntityHelper.REMOTE_JNDI_NAME);
         TestEntity checkedEntity = hlp.getTestEntity(entityPK);

         if (checkedEntity != null)
         {
//            if (isDebug)
//            {
//               print("TestContextPropagation#checkDatabase(serverName=" + serverName + "): initEntity = " + initEntity);
//               print("TestContextPropagation#checkDatabase(serverName=" + serverName + "): checkedEntity = " + checkedEntity);
//            }

            return (rollbackExpected) ? checkedEntity.getA() == initEntity.getA()
                  : checkedEntity.getA() != initEntity.getA();
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      return false;
   }

   protected CrashHelperRem lookupCrashHelper(String serverName) throws Exception
   {
      return (CrashHelperRem) config.getNamingContext(serverName).lookup(CrashHelper.REMOTE_JNDI_NAME);
   }

   protected TestEntityHelperRem lookupTestEntityHelper(String serverName) throws Exception
   {
      return (TestEntityHelperRem) config.getNamingContext(serverName).lookup(TestEntityHelper.REMOTE_JNDI_NAME);
   }

   protected void startTx(String host, int jndiPort) throws Exception
   {
      if (isDebug)
         print("startTx at host " + host + ", jndiPort " + jndiPort + " and useOTS " + useOTS);
      
      if (useOTS)
      {
         OTSManager.get_current().begin();
      }
      else
      {
         tx = (UserTransaction) EJBUtils.lookupObject(host, jndiPort, "UserTransaction");
         tx.begin();
      }
   }

   protected void endTx()
   {
      if (isDebug)
         print("endTx useOTS = " + useOTS);
      
      try 
      {
         if (useOTS)
         {
            if (OTSManager.get_current().get_status().value() == org.omg.CosTransactions.Status._StatusMarkedRollback)
               OTSManager.get_current().rollback();
            else
               OTSManager.get_current().commit(true);
         }
         else
         {
            if (tx.getStatus() == javax.transaction.Status.STATUS_MARKED_ROLLBACK)
               tx.rollback();
            else
               tx.commit();
         }
      }
      catch (Throwable e)
      {
         print("User tx commit/rollback failure: " + e.getMessage());
//         e.printStackTrace();
      } 
   }

   /**
    * Wipes out all the txs in doubt from the database on a given server.
    * 
    * @return true in success, fail otherwise
    */
   private boolean wipeOutTxsInDoubt(String serverName)
   {
      // wipes out all txs in doubt from DB
      return wipeOutTxsInDoubt(serverName, null);
   }

   /**
    * Wipes out only new txs in doubt from the database on a given server after test run.
    *
    * @param xidsInDoubtBeforeTest txs in doubt in database before test run
    * @param xidsInDoubtBeforeTest txs in doubt in database after test run
    * @return true in success, fail otherwise
    */
   private boolean wipeOutTxsInDoubt(String serverName, Set<RecoveredXid> xidsInDoubtBeforeTest, Set<RecoveredXid> xidsInDoubtAfterTest)
   {
      Set<RecoveredXid> xidsToRecover = new HashSet<RecoveredXid>(xidsInDoubtAfterTest);
      xidsToRecover.removeAll(xidsInDoubtBeforeTest);

      if (xidsToRecover.isEmpty())
         return true;

      return wipeOutTxsInDoubt(serverName, xidsToRecover);
   }

   /**
    * Wipes out txs in doubt from the database on a given server according to a xidsToRecover list.
    * 
    * @param xidsToRecover list of xids to recover
    * @return true in success, fail otherwise
    */
   private boolean wipeOutTxsInDoubt(String serverName, Set<RecoveredXid> xidsToRecover)
   {
      print("wiping out txs in doubt");
      try
      {
         lookupCrashHelper(serverName).wipeOutTxsInDoubt(xidsToRecover);
      }  
      catch (Exception e)
      {
         e.printStackTrace();
      }
      return false;
   }


   /**
    * Count how many pending transaction branches there are in the transaction log.
    * 
    * @return number of pending transaction branches
    */
   private int getPendingUids(TransactionLog store)
   {
      try
      {
         return store.getIds(storeType).size();
      }
      catch (Exception e)
      {
         e.printStackTrace();

         return -1;
      }
   }

   private void clearXidsOnServer0()
   {
      clearXidsOnServerX(storeServer0);
   }
   
   private void clearXidsOnServer1()
   {
      clearXidsOnServerX(storeServer1);
   }
   
   private void clearXidsOnServerX(TransactionLog store)
   {
      try
      {
         store.clearXids(storeType);
      }
      catch (Exception ignore)
      {
         ignore.printStackTrace();
      }
   }
   
   private ASFailureSpec[] parseSpecs(String specArg, StringBuilder sb, String specKey)
   {
      ASFailureSpec[] fspecs = config.parseSpecs(specArg);

      for (ASFailureSpec spec: fspecs)
      {
         String name = (spec == null ? "INVALID" : spec.getName());

         if (spec != null && spec.willTerminateVM())
         {
            if ("specs0".equals(specKey))
               expectFailureNode0 = true;
            else if ("specs1".equals(specKey))
               expectFailureNode1 = true;
         }
         else if (spec != null && ASFailureMode.EJBEXCEPTION.equals(spec.getMode()))
         {
            expectException = true;
         }

         sb.append("\t").append(name).append('\n');
      }

      return fspecs;
   }

   public static synchronized void initORB() throws InvalidName
   {
      if (orb == null)
      {
         orb = new ORBWrapper();
         orb.start();
      }
   }

}