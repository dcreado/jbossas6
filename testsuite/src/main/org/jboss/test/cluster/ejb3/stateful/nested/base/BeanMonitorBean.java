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

import java.rmi.dgc.VMID;

import javax.ejb.Remove;

import org.jboss.logging.Logger;

/**
 * A BeanMonitorBean.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 108925 $
 */
public class BeanMonitorBean implements BeanMonitor
{
   protected Logger log = Logger.getLogger(getClass());
   
   protected TopLevel parent;
   protected MidLevel nested;
   protected MidLevel localNested;
   protected DeepNestedStateful deepNested;
   protected DeepNestedStateful localDeepNested;
   
   public VMID getVMID()
   {
      return VMTracker.VMID;
   }

   public void monitor(TopLevel topLevel)
   {
      this.parent = topLevel;
      log.debug("parent is " + parent);
      this.nested = parent.getNested();
      log.debug("nested is " + nested);
      this.localNested = parent.getLocalNested();
      log.debug("localNested is " + localNested);
      this.deepNested = nested.getDeepNestedStateful();
      log.debug("deepNested is " + deepNested);
      this.localDeepNested = localNested.getDeepNestedStateful();
      log.debug("localDeepNested is " + localDeepNested);
   }

   public String getDeepNestedId()
   {
      try
      {
         return deepNested.getInternalId();
      }
      catch (Exception e)
      {
         log.warn("getDeepNestedId() " + e.getLocalizedMessage(), e);
         return "ERROR";
      }
   }

   public String getLocalDeepNestedId()
   {
      try
      {
         return localDeepNested.getInternalId();
      }
      catch (Exception e)
      {
         log.warn("getLocalDeepNestedId() " + e.getLocalizedMessage(), e);
         return "ERROR";
      }
   }

   public int getParentPassivations()
   {
      try
      {
         return parent.getPrePassivate();
      }
      catch (Exception e)
      {
         log.warn("getParentPassivations() " + e.getLocalizedMessage(), e);
         return -1;
      }
   }

   public int getNestedPassivations()
   {
      try
      {
         return nested.getPrePassivate();
      }
      catch (Exception e)
      {
         log.warn("getNestedPassivations() " + e.getLocalizedMessage(), e);
         return -1;
      }
   }

   public int getLocalNestedPassivations()
   {
      try
      {
         return localNested.getPrePassivate();
      }
      catch (Exception e)
      {
         log.warn("getLocalNestedPassivations() " + e.getLocalizedMessage(), e);
         return -1;
      }
   }

   public int getDeepNestedPassivations()
   {
      try
      {
         return deepNested.getPrePassivate();
      }
      catch (Exception e)
      {
         log.warn("getDeepNestedPassivations() " + e.getLocalizedMessage(), e);
         return -1;
      }
   }

   public int getLocalDeepNestedPassivations()
   {
      try
      {
         return localDeepNested.getPrePassivate();
      }
      catch (Exception e)
      {
         log.warn("getLocalDeepNestedPassivations() " + e.getLocalizedMessage(), e);
         return -1;
      }
   }

   public int getParentActivations()
   {
      try
      {
         return parent.getPostActivate();
      }
      catch (Exception e)
      {
         log.warn("getParentActivations() " + e.getLocalizedMessage(), e);
         return -1;
      }
   }

   public int getNestedActivations()
   {
      try
      {
         return nested.getPostActivate();
      }
      catch (Exception e)
      {
         log.debug("getNestedActivations() " + e.getLocalizedMessage());
         return -1;
      }
   }

   public int getLocalNestedActivations()
   {
      try
      {
         return localNested.getPostActivate();
      }
      catch (Exception e)
      {
         log.warn("getLocalNestedActivations() " + e.getLocalizedMessage(), e);
         return -1;
      }
   }

   public int getDeepNestedActivations()
   {
      try
      {
         return deepNested.getPostActivate();
      }
      catch (Exception e)
      {
         log.warn("getDeepNestedActivations() " + e.getLocalizedMessage(), e);
         return -1;
      }
   }

   public int getLocalDeepNestedActivations()
   {
      try
      {
         return localDeepNested.getPostActivate();
      }
      catch (Exception e)
      {
         log.debug("getLocalDeepNestedActivations() " + e.getLocalizedMessage());
         return -1;
      }
   }

   public boolean removeParent()
   {
      try
      {
         parent.remove();
         return true;
      }
      catch (Exception e)
      {
         log.warn("removeParent() " + e.getLocalizedMessage(), e);
         return false;
      }
   }

   public boolean removeNested()
   {
      try
      {
         nested.remove();
         return true;
      }
      catch (Exception e)
      {
         log.debug("removeNested() " + e.getLocalizedMessage());
         return false;
      }
   }

   public boolean removeLocalNested()
   {
      try
      {
         localNested.remove();
         return true;
      }
      catch (Exception e)
      {
         log.warn("removeLocalNested() " + e.getLocalizedMessage(), e);
         return false;
      }
   }

   public boolean removeDeepNested()
   {
      try
      {
         deepNested.remove();
         return true;
      }
      catch (Exception e)
      {
         log.debug("removeDeepNested() " + e.getLocalizedMessage());
         return false;
      }
   }

   public boolean removeLocalDeepNested()
   {
      try
      {
         localDeepNested.remove();
         return true;
      }
      catch (Exception e)
      {
         log.warn("removeLocalDeepNested() " + e.getLocalizedMessage(), e);
         return false;
      }
   }

   @Remove
   public void remove()
   {
      log.debug("Being removed");
      removeParent();
      removeNested();
      removeLocalNested();
      removeDeepNested();
      removeLocalDeepNested();
   }

}
