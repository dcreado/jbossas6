/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.test.cluster.ejb3.clusteredsession;

import java.rmi.dgc.VMID;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.Clustered;

/**
 * Simple stateless bean
 *
 * @author Ben Wang
 */
@Stateless(name="clusteredStateless")
@Clustered(loadBalancePolicy = "RoundRobin")
@Remote(ClusteredStatelessRemote.class)
public class ClusteredStatelessSessionBean implements ClusteredStatelessRemote
{
   public static final VMID myId = new VMID();
   
   public NodeAnswer getNodeState() {
      return new NodeAnswer(myId, "test");
   }
}
