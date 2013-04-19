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
package org.jboss.test.cluster.ejb3.stateful.unit;

import java.util.HashSet;
import java.util.Set;

import javax.ejb.NoSuchEJBException;
import javax.naming.InitialContext;

import org.jboss.logging.Logger;
import org.jboss.test.cluster.ejb3.stateful.nested.base.Removable;

/**
 * Base class for test runners that can share test functionality between
 * JBossTestCase subclasses and JBossClusteredTestCase subclasses.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 108925 $
 */
public class BaseTestRunner
{
   protected InitialContext initialContext;
   protected Set<Removable> removables = new HashSet<Removable>();
   
   private Logger log;
   private long sleepTime = 11100L;
   private int passivationPerInvocation = 0;
   private int passivationPerSleep = 1;
   
   public BaseTestRunner(InitialContext context, Logger log)
   {
      this.initialContext = context;
      this.log = log;
   }

   public void setUp() throws Exception
   {      
   }

   public void tearDown() throws Exception
   {
      // Remove any EJBs so they don't spuriously passivate and
      // throw off the passivation counts
      for (Removable removable : removables)
      {
         try
         {
            removable.remove();
         }
         catch (Exception ignored) {}
      }
      
      removables.clear();
   }

   protected InitialContext getInitialContext()
   {
      return initialContext;
   }
   
   protected Logger getLog()
   {
      return log;
   }

   public long getSleepTime()
   {
      return sleepTime;
   }

   public void setSleepTime(long sleepTime)
   {
      this.sleepTime = sleepTime;
   }

   public void sleep(long time)
   {
      try {
         Thread.sleep(time);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }
   
   public void sleep()
   {
      sleep(getSleepTime());
   }

   public int getPassivationPerInvocation()
   {
      return passivationPerInvocation;
   }

   public void setPassivationPerInvocation(int passivationPerInvocation)
   {
      this.passivationPerInvocation = passivationPerInvocation;
   }

   public int getPassivationPerSleep()
   {
      return passivationPerSleep;
   }

   public void setPassivationPerSleep(int passivationPerSleep)
   {
      this.passivationPerSleep = passivationPerSleep;
   }
   
   public int getExpectedPassivations(int sleepCycles, int invocations)
   {
      return (sleepCycles * passivationPerSleep) + 
             (invocations * passivationPerInvocation);
   }
   
   public void addRemovable(Removable bean)
   {
      removables.add(bean);
   }

   public void removeBean(Removable bean)
   {
      try
      {
         bean.remove();
      }
      catch (NoSuchEJBException ignored) 
      {
         // this is OK; it was already removed
      }
      removables.remove(bean);
   }

}