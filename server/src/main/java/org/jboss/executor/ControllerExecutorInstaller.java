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

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.dependency.plugins.AbstractController;
import org.jboss.dependency.spi.ControllerContext;

/**
 * Executor implementation which installs itself in the controller to be used for asynchronous deployments.
 * It wraps two executors. One used during bootstrap ({@link #setBootstrapExecutor(Executor)}),
 * and one which is deployed later ({@link #setMainExecutor(Executor)}).
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ControllerExecutorInstaller implements Executor
{
   ControllerContext context;

   volatile Executor bootstrapExecutor;
   
   volatile Executor mainExecutor;

   /**
    * Contextual injection
    * 
    * @return My ControllerContext
    */
   public ControllerContext getContext()
   {
      return context;
   }
   
   /**
    * Contextual injection
    * 
    * @param context My ControllerContext
    */
   public void setContext(ControllerContext context)
   {
      this.context = context;
   }
   
   /**
    * Get the bootstrap executor to be used until the main executor is deployed.
    * 
    * @return The bootstrap executor
    */
   public Executor getBootstrapExecutor()
   {
      return bootstrapExecutor;
   }
   
   /**
    * Set the bootstrap executor to be used until the main executor is deployed.
    * 
    * @param mainExecutor The bootstrap executor.
    */
   public void setBootstrapExecutor(Executor executor)
   {
      this.bootstrapExecutor = executor;
   }
   
   /**
    * Set the main executor once that is deployed
    * @param mainExecutor The system executor
    */
   public Executor getMainExecutor()
   {
      return mainExecutor;
   }

   /**
    * Set the main executor once that is deployed
    * @param mainExecutor The system executor
    */
   public void setMainExecutor(Executor mainExecutor)
   {
      this.mainExecutor = mainExecutor;
   }

   /**
    * Set myself as the executor in the controller
    */
   public void start()
   {
      ((AbstractController)context.getController()).setExecutor(this);
   }
   
   /**
    * Unset myself as the executor in the controller
    */
   public void stop()
   {
      ((AbstractController)context.getController()).setExecutor(null);
   }

   /**
    * Execute the command, using the mainExecutor if available. If there
    * is no mainExecutor, use the bootstrapExecutor.
    * @param command the command to execute
    * @throws java.util.concurrent.RejectedExecutionException if there is no mainExecutor or bootstrapExecutor 
    * @see Executor#execute(Runnable)
    */
   public void execute(Runnable command)
   {
      Executor exec = mainExecutor;
      if (exec != null)
      {
         exec.execute(command);
         return;
      }

      exec = bootstrapExecutor;
      if (exec != null)
      {
         exec.execute(command);
         return;
      }
      
      throw new RejectedExecutionException("No executor available in " + this.getClass().getName());
   }
}
