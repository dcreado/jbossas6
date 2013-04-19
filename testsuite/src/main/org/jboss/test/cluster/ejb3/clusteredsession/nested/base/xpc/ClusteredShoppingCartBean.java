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

package org.jboss.test.cluster.ejb3.clusteredsession.nested.base.xpc;

import java.rmi.dgc.VMID;

import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.interceptor.Interceptors;

import org.jboss.ejb3.annotation.CacheConfig;
import org.jboss.ejb3.annotation.Clustered;
import org.jboss.test.cluster.ejb3.clusteredsession.util.ExplicitFailoverInterceptor;
import org.jboss.test.cluster.ejb3.stateful.nested.base.xpc.ShoppingCart;
import org.jboss.test.cluster.ejb3.stateful.nested.base.xpc.ShoppingCartBean;

/**
 * ShoppingCartBean subclass for testing with a clustered SFSB cache.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 109544 $
 */
@Stateful(name="testShoppingCart")
@Remote(ShoppingCart.class)
@Clustered
@CacheConfig(name = "${jbosstest.cluster.sfsb.cache.config:sfsb-cache}", maxSize = 1000, idleTimeoutSeconds = 1)
public class ClusteredShoppingCartBean extends ShoppingCartBean
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
