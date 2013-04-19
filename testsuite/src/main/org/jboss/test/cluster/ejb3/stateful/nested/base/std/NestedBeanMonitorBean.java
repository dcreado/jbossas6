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

package org.jboss.test.cluster.ejb3.stateful.nested.base.std;

import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.test.cluster.ejb3.stateful.nested.base.BeanMonitorBean;
import org.jboss.test.cluster.ejb3.stateful.nested.base.TopLevel;

/**
 * A NestedBeanMonitorBean.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 108925 $
 */
@Stateful
@Remote(NestedBeanMonitor.class)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class NestedBeanMonitorBean
   extends BeanMonitorBean
   implements NestedBeanMonitor
{
   public void monitor(TopLevel topLevel)
   {
      // Just do a cast to confirm the type
      super.monitor((ParentStatefulRemote) topLevel);
   }

   public int incrementParent()
   {
      try
      {
         return ((ParentStatefulRemote) parent).increment();
      }
      catch (Exception e)
      {
         log.debug("incrementParent() " + e.getLocalizedMessage());
         return -1;
      }
   }

   public int incrementNested()
   {
      try
      {
         return ((NestedStateful) nested).increment();
      }
      catch (Exception e)
      {
         log.debug("incrementNested() " + e.getLocalizedMessage());
         return -1;
      }
   }

   public int incrementLocalNested()
   {
      try
      {
         return ((NestedStateful) localNested).increment();
      }
      catch (Exception e)
      {
         log.debug("incrementLocalNested() " + e.getLocalizedMessage());
         return -1;
      }
   }
}
