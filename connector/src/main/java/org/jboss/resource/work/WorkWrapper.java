/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.resource.work;

import java.util.concurrent.CountDownLatch;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkRejectedException;
import javax.transaction.xa.Xid;

import org.jboss.logging.Logger;

/**
 * Wraps the resource adapter's work.
 *
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @version $Revision: 100711 $
 */
public class WorkWrapper implements Runnable
{
   /** The log */
   private static Logger log = Logger.getLogger(WorkWrapper.class);

   /** Whether we are tracing */
   private static boolean trace = log.isTraceEnabled();
   
   /** The work */
   private Work work;

   /** The execution context */
   private ExecutionContext executionContext;
   
   /** the work listener */
   private WorkListener workListener;   

   /** The work manager */
   private JBossWorkManager workManager;

   /** The blocked time */
   private long blockedTime;

   /** Any exception */
   private WorkException exception;

   /** Started latch */
   private CountDownLatch startedLatch;

   /** Completed latch */
   private CountDownLatch completedLatch;

   /**
    * Create a new WorkWrapper
    *
    * @param workManager the work manager
    * @param work the work
    * @param waitType the waitType
    * @param executionContext the execution context
    * @param workListener the WorkListener
    * @throws IllegalArgumentException for null work, execution context or a negative start timeout
    */
   public WorkWrapper(JBossWorkManager workManager, 
                      Work work, 
                      ExecutionContext executionContext, 
                      WorkListener workListener,
                      CountDownLatch startedLatch,
                      CountDownLatch completedLatch)
   {
      super();

      if (work == null)
         throw new IllegalArgumentException("Null work");
      if (executionContext == null)
         throw new IllegalArgumentException("Null execution context");

      this.workManager = workManager;
      this.work = work;
      this.executionContext = executionContext;
      this.workListener = workListener;
      this.startedLatch = startedLatch;
      this.completedLatch = completedLatch;
   }
   
   /**
    * Get the work manager
    *
    * @return the work manager
    */
   public JBossWorkManager getWorkManager()
   {
      return workManager;
   }

   /**
    * Retrieve the work
    *
    * @return the work
    */
   public Work getWork()
   {
      return work;
   }

   /**
    * Retrieve the exection context
    *
    * @return the execution context
    */
   public ExecutionContext getExecutionContext()
   {
      return executionContext;
   }

   /**
    * Retrieve the work listener
    *
    * @return the WorkListener
    */
   public WorkListener getWorkListener()
   {
      return workListener;
   }
   
   /**
    * Get any exception
    * 
    * @return the exception or null if there is none
    */
   public WorkException getWorkException()
   {
      return exception;
   }

   /**
    * Run
    */
   public void run()
   {
      if (trace)
         log.trace("Starting work " + this);  

      try
      {
         start();

         if (startedLatch != null)
            startedLatch.countDown();

         work.run();

         end();
      }
      catch (Exception e)
      {
         exception = new WorkCompletedException(e);

         cancel();
      } 
      finally
      {
         work.release();

         if (workListener != null)
         {
            WorkEvent event = new WorkEvent(workManager, WorkEvent.WORK_COMPLETED, work, exception);
            workListener.workCompleted(event);
         }

         if (startedLatch != null)
         {
            while (startedLatch.getCount() != 0)
               startedLatch.countDown();
         }

         if (completedLatch != null)
            completedLatch.countDown();

         if (trace)
            log.trace("Executed work " + this);  
      }
   }

   /**
    * Start
    * @throws WorkException for any error 
    */
   protected void start() throws WorkException
   {
      if (trace)
      {
         log.trace("Starting work " + this);  
      }

      ExecutionContext ctx = getExecutionContext();
      
      if (ctx != null)
      {
         Xid xid = ctx.getXid();
         if (xid != null)
         {
            //JBAS-4002 base value is in seconds as per the API, here we convert to millis
            long timeout = (ctx.getTransactionTimeout() * 1000);
            workManager.getXATerminator().registerWork(work, xid, timeout);
         }
      }
      
      if (ctx != null)
      {
         Xid xid = ctx.getXid();
         if (xid != null)
         {
            workManager.getXATerminator().startWork(work, xid);
         }
      }

      if (workListener != null)
      {
         WorkEvent event = new WorkEvent(workManager, WorkEvent.WORK_STARTED, work, null);
         workListener.workStarted(event);
      }

      if (trace)
      {
         log.trace("Started work " + this);  
      }
   }

   /**
    * End
    */
   protected void end()
   {
      if (trace)
      {
         log.trace("Ending work " + this);  
      }

      ExecutionContext ctx = getExecutionContext();

      if (ctx != null)
      {
         Xid xid = ctx.getXid();
         if (xid != null)
         {
            workManager.getXATerminator().endWork(work, xid);
         }
      }

      if (trace)
      {
         log.trace("Ended work " + this);  
      }
   }

   /**
    * Cancel
    */
   protected void cancel()
   {
      if (trace)
         log.trace("Cancel work " + this);  

      ExecutionContext ctx = getExecutionContext();

      if (ctx != null)
      {
         Xid xid = ctx.getXid();
         if (xid != null)
         {
            workManager.getXATerminator().cancelWork(work, xid);
         }
      }

      if (trace)
         log.trace("Canceled work " + this);  
   }
   
   /**
    * String representation
    * @return The string
    */
   public String toString()
   {
      StringBuilder buffer = new StringBuilder(100);
      buffer.append("WorkWrapper@").append(Integer.toHexString(System.identityHashCode(this)));
      buffer.append("[workManger=").append(workManager);
      buffer.append(" work=").append(work);

      if (executionContext != null && executionContext.getXid() != null)
      {
         buffer.append(" xid=").append(executionContext.getXid());
         buffer.append(" txTimeout=").append(executionContext.getTransactionTimeout());
      }

      if (workListener != null)
         buffer.append(" workListener=").append(workListener);
      if (exception != null)
         buffer.append(" exception=").append(exception);
      buffer.append("]");
      return buffer.toString();
   }
}
