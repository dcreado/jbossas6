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
package org.jboss.test.cluster.ejb3.clusteredsession.ejbthree1346;

import java.rmi.dgc.VMID;

import javax.interceptor.Interceptors;

import org.jboss.logging.Logger;
import org.jboss.test.cluster.ejb3.clusteredsession.NodeAnswer;
import org.jboss.test.cluster.ejb3.clusteredsession.util.ExplicitFailoverInterceptor;

/** 
 * @author Brian Stansberry
 * 
 * @version $Revision: 57207 $
 */
public class DisableClusteredAnnotationBase implements java.io.Serializable, DisableClusteredAnnotationRemote
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;
   
   private Logger log = Logger.getLogger(getClass());
   public static final NodeAnswer myId = new NodeAnswer(new VMID(), "answer");

   // Remote Interface implementation ----------------------------------------------
   
   // Mimic explict failover
   @Interceptors({ExplicitFailoverInterceptor.class})
   public NodeAnswer getNodeState()
   {
      log.info("getNodeState, " + myId);
      return myId;
   }

   public void setUpFailover(String failover) {
      // To setup the failover property
      log.info("Setting up failover property: " +failover);
      System.setProperty ("JBossCluster-DoFail", failover);
   }

}
