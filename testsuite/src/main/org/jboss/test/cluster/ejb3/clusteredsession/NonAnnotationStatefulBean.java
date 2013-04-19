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
package org.jboss.test.cluster.ejb3.clusteredsession;

import java.rmi.dgc.VMID;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.interceptor.Interceptors;

import org.jboss.logging.Logger;
import org.jboss.test.cluster.ejb3.clusteredsession.util.ExplicitFailoverInterceptor;
import org.jboss.test.cluster.ejb3.stateful.nested.base.VMTracker;

/**
 * Base class for various configs of a SFSB.  No class-level clustering 
 * annotations applied so it can serve as a bean class that doesn't use 
 * annotations, or be subclassed by a bean that adds annotations. 
 *
 * @see StatefulBean
 * 
 * @author Ben Wang
 * @author Brian Stansberry
 * 
 * @version $Revision: 108940 $
 */
public class NonAnnotationStatefulBean implements java.io.Serializable, StatefulRemote
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;
   
   private Logger log = Logger.getLogger(getClass());
   private int counter = 0;
   private String state;
   public static final VMID myId = VMTracker.VMID;
   public String name;

   private int postActivateCalled = 0;
   private int prePassivateCalled = 0;

   public int increment()
   {
      System.out.println("INCREMENT - counter: " + (counter++));
      return counter;
   }

   public String getHostAddress()
   {
      return System.getProperty("jboss.bind.address");
   }

   /**
    * Sleep to test
    * @throws Exception
    */
   public void longRunning() throws Exception
   {
      log.debug("+++ longRunning() enter ");
      Thread.sleep(20000); // 20000 will break the passivation test now.
      log.debug("+++ longRunning() leave ");
   }

   public int getPostActivate()
   {
      return postActivateCalled;
   }

   public int getPrePassivate()
   {
      return prePassivateCalled;
   }

   public void setState(String state)
   {
      this.state = state;
   }

   public String getState()
   {
      log.debug("getState(): entering ...");
      return this.state;
   }

   public void reset()
   {
      state = null;
      postActivateCalled = 0;
      prePassivateCalled = 0;
   }

   public void resetActivationCounter() 
   {
      postActivateCalled = 0;
      prePassivateCalled = 0;
   }

   @PostActivate
   public void postActivate()
   {
      postActivateCalled++;
      log.debug("Activate. My ID: " + myId + " name: " + this.name);
   }

   @PrePassivate
   public void prePassivate()
   {
      prePassivateCalled++;
      log.debug("Passivate. My ID: " + myId + " name: " + this.name);
   }

   @Remove
   public void remove()
   {
   }

   @PostConstruct
   public void ejbCreate()
   {
      log.debug("My ID: " + myId);
   }

   // Remote Interface implementation ----------------------------------------------
   
   // Mimic explict failover
   @Interceptors({ExplicitFailoverInterceptor.class})
   public NodeAnswer getNodeState()
   {
      NodeAnswer state = new NodeAnswer(myId, this.name);
      log.debug("getNodeState, " + state);
      return state;
   }

   public void setName(String name)
   {
      this.name = name;
      log.debug("Name set to " + name);
   }

   public void setNameOnlyOnNode(String name, VMID node)
   {
      if (node.equals(myId))
         this.setName(name);
      else
         throw new EJBException("Trying to assign value on node " + myId + " but this node expected: " + node);
   }

   public void setUpFailover(String failover) {
      // To setup the failover property
      log.debug("Setting up failover property: " +failover);
      System.setProperty ("JBossCluster-DoFail", failover);
   }

}
