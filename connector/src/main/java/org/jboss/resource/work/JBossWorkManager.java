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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkRejectedException;
import javax.transaction.xa.Xid;

import org.jboss.logging.Logger;
import org.jboss.threads.BlockingExecutor;
import org.jboss.threads.ExecutionTimedOutException;
import org.jboss.tm.JBossXATerminator;
import org.jboss.system.ServiceMBeanSupport;

/**
 * The work manager implementation
 *
 * @author <a href="mailto:adrian@jboss.org">Adrian Brock</a>
 * @version $Revision: 100756 $
 */
public class JBossWorkManager extends ServiceMBeanSupport implements WorkManager, JBossWorkManagerMBean
{
   /** The logger */
   private static Logger log = Logger.getLogger(JBossWorkManager.class);
   
   /** Whether trace is enabled */
   private static boolean trace = log.isTraceEnabled();

   /** The executor */
   private BlockingExecutor executor;

   /** The XA terminator */
   private JBossXATerminator xaTerminator;

   /**
    * Constructor
    */
   public JBossWorkManager()
   {
   }
   
   /**
    * Retrieve the executor
    * @return The executor
    */
   public BlockingExecutor getExecutor()
   {
      return executor;
   }

   /**
    * Set the executor
    * @param executor The executor
    */
   public void setExecutor(BlockingExecutor executor)
   {
      this.executor = executor;
   }

   /**
    * Get the XATerminator
    * @return The XA terminator
    */
   public JBossXATerminator getXATerminator()
   {
      return xaTerminator;
   }

   /**
    * Set the XATerminator
    * @param xaTerminator The XA terminator
    */
   public void setXATerminator(JBossXATerminator xaTerminator)
   {
      this.xaTerminator = xaTerminator;
   }

   /**
    * Get the work manager instance
    * @return The instance
    */
   public WorkManager getInstance()
   {
      return this;
   }

   /**
    * {@inheritDoc}
    */
   public void doWork(Work work) throws WorkException
   {
      doWork(work, WorkManager.INDEFINITE, null, null);
   }
   
   /**
    * {@inheritDoc}
    */
   public void doWork(Work work,
                      long startTimeout, 
                      ExecutionContext execContext, 
                      WorkListener workListener) 
      throws WorkException
   {
      WorkException exception = null;
      WorkWrapper wrapper = null;
      try
      {
         if (work == null)
            throw new WorkRejectedException("Work is null");

         if (startTimeout < 0)
            throw new WorkRejectedException("StartTimeout is negative: " + startTimeout);

         if (execContext == null)
         {
            execContext = new ExecutionContext();  
         }

         final CountDownLatch completedLatch = new CountDownLatch(1);

         wrapper = new WorkWrapper(this, work, execContext, workListener, null, completedLatch);

         if (workListener != null)
         {
            WorkEvent event = new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null);
            workListener.workAccepted(event);
         }

         BlockingExecutor executor = getExecutor(work);

         if (startTimeout == WorkManager.INDEFINITE)
         {
            executor.executeBlocking(wrapper);
         }
         else
         {
            executor.executeBlocking(wrapper, startTimeout, TimeUnit.MILLISECONDS);
         }

         completedLatch.await();
      }
      catch (ExecutionTimedOutException etoe)
      {
         exception = new WorkRejectedException(etoe);
         exception.setErrorCode(WorkRejectedException.START_TIMED_OUT);  
      }
      catch (RejectedExecutionException ree)
      {
         exception = new WorkRejectedException(ree);
      }
      catch (WorkException we)
      {
         exception = we;
      }
      catch (InterruptedException ie)
      {
         Thread.currentThread().interrupt();
         exception = new WorkRejectedException("Interrupted while requesting permit");
      }
      finally
      {
         if (exception != null)
         {
            if (workListener != null)
            {
               WorkEvent event = new WorkEvent(this, WorkEvent.WORK_REJECTED, work, exception);
               workListener.workRejected(event);
            }

            throw exception;
         }

         checkWorkCompletionException(wrapper);
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public long startWork(Work work) throws WorkException
   {
      return startWork(work, WorkManager.INDEFINITE, null, null);
   }
   
   /**
    * {@inheritDoc}
    */
   public long startWork(Work work, 
                         long startTimeout, 
                         ExecutionContext execContext, 
                         WorkListener workListener) 
      throws WorkException
   {
      WorkException exception = null;
      WorkWrapper wrapper = null;
      try
      {
         if (work == null)
            throw new WorkRejectedException("Work is null");

         if (startTimeout < 0)
            throw new WorkRejectedException("StartTimeout is negative: " + startTimeout);

         long started = System.currentTimeMillis();

         if (execContext == null)
         {
            execContext = new ExecutionContext();  
         }

         final CountDownLatch startedLatch = new CountDownLatch(1);

         wrapper = new WorkWrapper(this, work, execContext, workListener, startedLatch, null);

         if (workListener != null)
         {
            WorkEvent event = new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null);
            workListener.workAccepted(event);
         }

         BlockingExecutor executor = getExecutor(work);

         if (startTimeout == WorkManager.INDEFINITE)
         {
            executor.executeBlocking(wrapper);
         }
         else
         {
            executor.executeBlocking(wrapper, startTimeout, TimeUnit.MILLISECONDS);
         }

         startedLatch.await();

         return System.currentTimeMillis() - started;
      }
      catch (ExecutionTimedOutException etoe)
      {
         exception = new WorkRejectedException(etoe);
         exception.setErrorCode(WorkRejectedException.START_TIMED_OUT);  
      }
      catch (RejectedExecutionException ree)
      {
         exception = new WorkRejectedException(ree);
      }
      catch (WorkException we)
      {
         exception = we;
      }
      catch (InterruptedException ie)
      {
         Thread.currentThread().interrupt();
         exception = new WorkRejectedException("Interrupted while requesting permit");
      }
      finally
      {
         if (exception != null)
         {
            if (workListener != null)
            {
               WorkEvent event = new WorkEvent(this, WorkEvent.WORK_REJECTED, work, exception);
               workListener.workRejected(event);
            }

            throw exception;
         }

         checkWorkCompletionException(wrapper);
      }

      return WorkManager.UNKNOWN;
   }
   
   /**
    * {@inheritDoc}
    */
   public void scheduleWork(Work work) throws WorkException
   {
      scheduleWork(work, WorkManager.INDEFINITE, null, null);
   }
   
   /**
    * {@inheritDoc}
    */
   public void scheduleWork(Work work,
                            long startTimeout, 
                            ExecutionContext execContext, 
                            WorkListener workListener) 
      throws WorkException
   {
      WorkException exception = null;
      WorkWrapper wrapper = null;
      try
      {
         if (work == null)
            throw new WorkRejectedException("Work is null");

         if (startTimeout < 0)
            throw new WorkRejectedException("StartTimeout is negative: " + startTimeout);

         if (execContext == null)
         {
            execContext = new ExecutionContext();  
         }

         wrapper = new WorkWrapper(this, work, execContext, workListener, null, null);

         if (workListener != null)
         {
            WorkEvent event = new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null);
            workListener.workAccepted(event);
         }

         BlockingExecutor executor = getExecutor(work);

         if (startTimeout == WorkManager.INDEFINITE)
         {
            executor.executeBlocking(wrapper);
         }
         else
         {
            executor.executeBlocking(wrapper, startTimeout, TimeUnit.MILLISECONDS);
         }
      }
      catch (ExecutionTimedOutException etoe)
      {
         exception = new WorkRejectedException(etoe);
         exception.setErrorCode(WorkRejectedException.START_TIMED_OUT);  
      }
      catch (RejectedExecutionException ree)
      {
         exception = new WorkRejectedException(ree);
      }
      catch (WorkException we)
      {
         exception = we;
      }
      catch (InterruptedException ie)
      {
         Thread.currentThread().interrupt();
         exception = new WorkRejectedException("Interrupted while requesting permit");
      }
      finally
      {
         if (exception != null)
         {
            if (workListener != null)
            {
               WorkEvent event = new WorkEvent(this, WorkEvent.WORK_REJECTED, work, exception);
               workListener.workRejected(event);
            }

            throw exception;
         }

         checkWorkCompletionException(wrapper);
      }
   }

   /**
    * Get the executor
    * @param work The work instance
    * @return The executor
    */
   private BlockingExecutor getExecutor(Work work)
   {
      return executor;
   }
   
   /**
    * Checks work completed status. 
    * @param wrapper work wrapper instance
    * @throws {@link WorkException} if work is completed with an exception
    */
   private void checkWorkCompletionException(WorkWrapper wrapper) throws WorkException
   {
      if (wrapper.getWorkException() != null)
      {
         throw wrapper.getWorkException();  
      }      
   }
}
