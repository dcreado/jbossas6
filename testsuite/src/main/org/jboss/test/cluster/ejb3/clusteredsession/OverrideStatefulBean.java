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

import javax.ejb.Remote;
import javax.ejb.Stateful;

import org.jboss.ejb3.annotation.CacheConfig;
import org.jboss.ejb3.annotation.Clustered;
import org.jboss.logging.Logger;

/**
 * @version <tt>$Revision: 109544 $</tt>
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateful(name="OverrideStateful")
@Clustered(partition="BogusPartition", loadBalancePolicy="BogusPolicy")
@CacheConfig(name = "${jbosstest.cluster.sfsb.cache.config:sfsb-cache}", maxSize=1000, idleTimeoutSeconds=2)
@Remote(OverrideStatefulRemote.class)
public class OverrideStatefulBean implements OverrideStatefulRemote
{
   private static final Logger log = Logger.getLogger(OverrideStatefulBean.class);
   
   private int state = 0;

   public int increment()
   {
      log.info("INCREMENT - counter: " + (state++));
      return state;
   }
   
   public String getHostAddress()
   {
      return System.getProperty("jboss.bind.address");
   }
}
