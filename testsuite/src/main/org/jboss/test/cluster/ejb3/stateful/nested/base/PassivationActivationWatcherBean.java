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
package org.jboss.test.cluster.ejb3.stateful.nested.base;

import java.io.Serializable;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;

import org.jboss.logging.Logger;

/**
 * Superclass for SFSBs that monitor calls to @PrePassivate and @PostActivate.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 108925 $
 */
public abstract class PassivationActivationWatcherBean
      implements Serializable, PassivationActivationWatcher
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;
   
   protected Logger log = Logger.getLogger(getClass());
   private int postActivateCalled = 0;
   private int prePassivateCalled = 0;

   public PassivationActivationWatcherBean()
   {
      super();
   }

   @PostActivate
   public void postActivate()
   {
      postActivateCalled++;
      int sysHC = System.identityHashCode(this);
      log.debug(sysHC + ": Activated -- activate count = " + postActivateCalled );
   }

   @PrePassivate
   public void prePassivate()
   {
      prePassivateCalled++;
      int sysHC = System.identityHashCode(this);
      log.debug(sysHC + ": Passivated -- passivate count = " + prePassivateCalled );
   }

   public int getPostActivate()
   {
      int sysHC = System.identityHashCode(this);
      log.debug(sysHC + ": getPostActivate() -- activate count = " + 
                postActivateCalled );
      return postActivateCalled;
   }

   public int getPrePassivate()
   {
      int sysHC = System.identityHashCode(this);
      log.debug(sysHC + ": getPrePassivate() -- passivate count = " + 
                prePassivateCalled );
      return prePassivateCalled;
   }
   
   public void reset()
   {
      int sysHC = System.identityHashCode(this);
      log.debug(sysHC + "Being reset");
      prePassivateCalled = 0;
      postActivateCalled = 0;
   }

}