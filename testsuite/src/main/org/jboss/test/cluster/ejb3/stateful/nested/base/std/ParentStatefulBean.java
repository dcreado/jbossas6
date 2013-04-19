/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.test.cluster.ejb3.stateful.nested.base.std;

import java.rmi.dgc.VMID;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Remove;

import org.jboss.test.cluster.ejb3.stateful.nested.base.MidLevel;
import org.jboss.test.cluster.ejb3.stateful.nested.base.PassivationActivationWatcherBean;
import org.jboss.test.cluster.ejb3.stateful.nested.base.VMTracker;

/**
 * Parent SFSB that contains nested SFSBs. We don't
 * annotate it with @Stateful or @CacheConfig so the
 * subclasses used by the clustered and non-clustered tests
 * can do that.
 *
 * NOTE: EJBTHREE-778 prevents us annotating this class 
 * @Remote(ParentStatefulRemote.class), but we would if we could. Currently
 * the subclasses need to do it.
 *
 * @author Ben Wang
 * @version $Revision: 108925 $
 */
public class ParentStatefulBean 
   extends PassivationActivationWatcherBean
   implements ParentStatefulRemote
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;
   
   private int counter = 0;
   private int localCounter = 0;
   
   @EJB(beanName="testNestedStateful")
   private NestedStateful nested;

   @EJB(beanName="testLocalNestedStateful")
   private NestedStateful localNested;

   public VMID getVMID()
   {
      return VMTracker.VMID;      
   }
   
   public int increment()
   {
      counter = nested.increment();

      log.debug("INCREMENT - counter: " + counter);
      return counter;
   }

   public int incrementLocal()
   {
      localCounter = localNested.increment();

      log.debug("INCREMENT - localCounter: " + localCounter);
      return localCounter;
   }

   /**
    * Sleep to test
    * @throws Exception
    */
   public void longRunning() throws Exception
   {
      log.debug("+++ longRunning() enter ");
      Thread.sleep(10000);
      log.debug("+++ longRunning() leave ");
   }
   
   public MidLevel getNested()
   {
      return nested;
   }
   
   public MidLevel getLocalNested()
   {
      return localNested;
   }
   
   public int getLocalNestedPostActivate()
   {
      return localNested.getPostActivate();
   }

   public int getLocalNestedPrePassivate()
   {
      return localNested.getPrePassivate();
   }

   public void reset()
   {
      super.reset();
      counter = 0;
      nested.reset();
      localNested.reset();
   }

   @Remove
   public void remove()
   {
      log.debug("Being removed");
   }

   public void setUpFailover(String failover) 
   {
      // To setup the failover property
      log.debug("Setting up failover property: " +failover);
      System.setProperty ("JBossCluster-DoFail", failover);
   }

   @PostConstruct
   public void ejbCreate()
   {
   }

   // Remote Interface implementation ----------------------------------------------

}
