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
package org.jboss.executor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Creates a ThreadPoolExecutor that can be configured as an MC bean.
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ThreadPoolExecutorFactory
{
   final static ThreadFactory DEFAULT_THREAD_FACTORY = Executors.defaultThreadFactory();
   
   final static RejectedExecutionHandler DEFAULT_REJECTED_EXECUTION_HANDLER = new ThreadPoolExecutor.AbortPolicy();

   final static int DEFAULT_KEEP_ALIVE_TIME = 30;

   final static TimeUnit DEFAULT_KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
   
   float corePoolSizeBase;

   float corePoolSizePerCpu;
   
   float maxPoolSizeBase;
   
   float maxPoolSizePerCpu;
   
   int keepAliveTime;
   
   TimeUnit keepAliveTimeUnit = TimeUnit.SECONDS;
   
   BlockingQueue<Runnable> workQueue;
   
   ThreadFactory threadFactory;
   
   RejectedExecutionHandler rejectedExecutionHandler;

   Executor executor;
   
   /**
    * Gets the keep alive time to be used in the created pool.
    * @return the keep alive time
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public int getKeepAliveTime()
   {
      return keepAliveTime;
   }

   /**
    * Sets the keep alive time to be used in the created pool.
    * @param keepAliveTime The time
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public void setKeepAliveTime(int keepAliveTime)
   {
      this.keepAliveTime = keepAliveTime;
   }

   /**
    * Gets the time unit for keep alive time to be used in the created pool.
    * @return The keep alive time unit
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public TimeUnit getKeepAliveTimeUnit()
   {
      return keepAliveTimeUnit;
   }

   /**
    * Sets the time unit for keep alive time to be used in the created pool. The default is TimeUnit.SECONDS
    * @param keepAliveTimeUnit The time unit
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public void setKeepAliveTimeUnit(TimeUnit keepAliveTimeUnit)
   {
      this.keepAliveTimeUnit = keepAliveTimeUnit;
   }

   /**
    * Gets work queue to be used in the created pool.
    * @return The work queue
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public BlockingQueue<Runnable> getWorkQueue()
   {
      return workQueue;
   }

   /**
    * Sets the work queue to be used in the created pool. By default it will create an instance of {@link LinkedBlockingQueue}
    * @param workQueue The work queue
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public void setWorkQueue(BlockingQueue<Runnable> workQueue)
   {
      this.workQueue = workQueue;
   }

   /**
    * Gets the thread factory to be used in the created pool.
    * @return The thread factory
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public ThreadFactory getThreadFactory()
   {
      return threadFactory;
   }

   /**
    * Sets the thread factory to be used in the created pool. By default it will use {@link Executors#defaultThreadFactory()}
    * @return The thread factory
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public void setThreadFactory(ThreadFactory threadFactory)
   {
      this.threadFactory = threadFactory;
   }

   /**
    * Gets the base value for the calculation of the core pool size to be used as the corePoolSize argument in the created pool.
    * @return the base core pool size
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public float getCorePoolSizeBase()
   {
      return corePoolSizeBase;
   }

   /**
    * Sets the base value for the calculation of the core pool size to be used as the corePoolSize argument in the created pool.
    * The calculation is <code>corePoolSizeBase + (corePoolSizePerCpu * Runtime.availableProcessors())</code>
    * @param the base core pool size
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public void setCorePoolSizeBase(float corePoolSizeBase)
   {
      this.corePoolSizeBase = corePoolSizeBase;
   }

   /**
    * Gets the per cpu value for the calculation of the core pool size to be used as the corePoolSize argument in the created pool.
    * @return the per cpu core pool size
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public float getCorePoolSizePerCpu()
   {
      return corePoolSizePerCpu;
   }

   /**
    * Sets the per cpu value for the calculation of the core pool size to be used as the corePoolSize argument in the created pool.
    * The calculation is <code>corePoolSizeBase + (corePoolSizePerCpu * Runtime.availableProcessors())</code>
    * @param the per cpu core pool size
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public void setCorePoolSizePerCpu(float corePoolSizePerCpu)
   {
      this.corePoolSizePerCpu = corePoolSizePerCpu;
   }

   /**
    * Gets the base value for the calculation of the max pool size to be used as the maxPoolSize in the created pool.
    * @return the base max pool size
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public float getMaxPoolSizeBase()
   {
      return maxPoolSizeBase;
   }

   /**
    * Sets the base value for the calculation of the max pool size to be used as the maxPoolSize argument in the created pool.
    * The calculation is <code>maxPoolSizeBase + (maxPoolSizePerCpu * Runtime.availableProcessors())</code>
    * @param the base max pool size
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public void setMaxPoolSizeBase(float maxPoolSizeBase)
   {
      this.maxPoolSizeBase = maxPoolSizeBase;
   }

   /**
    * Gets the per cpu value for the calculation of the max pool size to be used as the maxPoolSize argument in the created pool.
    * @return the per cpu max pool size
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public float getMaxPoolSizePerCpu()
   {
      return maxPoolSizePerCpu;
   }

   /**
    * Sets the per cpu value for the calculation of the max pool size to be used as the maxPoolSize argument in the created pool.
    * The calculation is <code>maxPoolSizeBase + (maxPoolSizePerCpu * Runtime.availableProcessors())</code>
    * @param the per cpu max pool size
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public void setMaxPoolSizePerCpu(float maxPoolSizePerCpu)
   {
      this.maxPoolSizePerCpu = maxPoolSizePerCpu;
   }

   /**
    * Gets the rejected execution handler to be used in the created pool.
    * @return The rejected execution handler
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public RejectedExecutionHandler getRejectedExecutionHandler()
   {
      return rejectedExecutionHandler;
   }

   /**
    * Sets the rejected execution handler to be used in the created pool. The default is to create an instance of {@link ThreadPoolExecutor.AbortPolicy}
    * @param rejectedExecutionHandler The rejected execution handler
    * @See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
    */
   public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler)
   {
      this.rejectedExecutionHandler = rejectedExecutionHandler;
   }

   /**
    * Creates and returns an instance of the ThreadPoolExecutor configured by this bean. Subsequent invocations
    * will return the same instance.
    * @return The created executor
    */
   public Executor getExecutor()
   {
      synchronized(this)
      {
         if (executor == null)
         {
            executor = createExecutor(); 
         }
      }
      return executor;
   }
   
   private ThreadPoolExecutor createExecutor()
   {
      int coreSize = Math.max(calcPoolSize(corePoolSizeBase, corePoolSizePerCpu), 0);
      if (coreSize == 0)
      {
         throw new IllegalStateException("Core size was calculated to 0");
      }
      int maxSize = Math.max(calcPoolSize(maxPoolSizeBase, maxPoolSizePerCpu), 0);
      if (maxSize == 0)
      {
         throw new IllegalStateException("Max size was calculated to 0");
      }
      BlockingQueue<Runnable> queue = workQueue == null ? new LinkedBlockingQueue<Runnable>() : workQueue;
      ThreadFactory factory = threadFactory == null ? DEFAULT_THREAD_FACTORY : threadFactory;
      RejectedExecutionHandler handler = rejectedExecutionHandler == null ? DEFAULT_REJECTED_EXECUTION_HANDLER : rejectedExecutionHandler;
      
      return new ThreadPoolExecutor(coreSize, maxSize, keepAliveTime, keepAliveTimeUnit, queue, factory, handler);
   }

   private static int calcPoolSize(float count, float perCpu) 
   {
      if (Float.isNaN(count) || Float.isInfinite(count) || count < 0.0f) 
      {
          count = 0.0f;
      }
      if (Float.isNaN(perCpu) || Float.isInfinite(perCpu) || perCpu < 0.0f) 
      {
          perCpu = 0.0f;
      }
      return Math.round(count + ((float) Runtime.getRuntime().availableProcessors()) * perCpu);
  }
}
