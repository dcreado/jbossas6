/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.test.cluster.ejb3.clusteredsession.nested.base.std;

import java.rmi.dgc.VMID;

import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.interceptor.Interceptors;

import org.jboss.ejb3.annotation.CacheConfig;
import org.jboss.ejb3.annotation.Clustered;
import org.jboss.test.cluster.ejb3.clusteredsession.util.ExplicitFailoverInterceptor;
import org.jboss.test.cluster.ejb3.stateful.nested.base.std.ParentStatefulBean;
import org.jboss.test.cluster.ejb3.stateful.nested.base.std.ParentStatefulRemote;

/**
 * Parent SFSB that contains nested SFSB.
 *
 * @author Ben Wang
 * @author Brian Stansberry
 * @version $Revision: 109544 $
 */
@Clustered
@Stateful(name="testParentStateful")
@CacheConfig(name = "${jbosstest.cluster.sfsb.cache.config:sfsb-cache}", maxSize=1000, idleTimeoutSeconds=1)   // this will get evicted the second time eviction thread wakes up
@Remote(ParentStatefulRemote.class)
public class ClusteredParentStatefulBean extends ParentStatefulBean
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;
   
   // Mimic explict failover
   @Interceptors({ExplicitFailoverInterceptor.class})
   public VMID getVMID()
   {
      return super.getVMID();
   }
}
