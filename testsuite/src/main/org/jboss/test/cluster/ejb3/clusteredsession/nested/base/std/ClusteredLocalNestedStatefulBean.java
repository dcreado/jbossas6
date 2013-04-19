/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.test.cluster.ejb3.clusteredsession.nested.base.std;

import java.rmi.dgc.VMID;

import javax.ejb.Local;
import javax.ejb.Stateful;

import org.jboss.ejb3.annotation.CacheConfig;
import org.jboss.ejb3.annotation.Clustered;
import org.jboss.test.cluster.ejb3.stateful.nested.base.VMTracker;
import org.jboss.test.cluster.ejb3.stateful.nested.base.std.NestedStateful;
import org.jboss.test.cluster.ejb3.stateful.nested.base.std.NestedStatefulBean;

/**
 * Nested SFSB with only a local interface.
 *
 * @author Ben Wang
 * @author Brian Stansberry
 * 
 * @version $Revision: 109544 $
 */
@Clustered
@Stateful(name="testLocalNestedStateful")
@CacheConfig(name = "${jbosstest.cluster.sfsb.cache.config:sfsb-cache}", maxSize=10000, idleTimeoutSeconds=1) 
@Local(NestedStateful.class)
public class ClusteredLocalNestedStatefulBean extends NestedStatefulBean
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;
   
   public VMID getVMID()
   {
      return VMTracker.VMID;
   }
}
