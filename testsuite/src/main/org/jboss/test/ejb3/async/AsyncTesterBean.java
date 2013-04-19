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
package org.jboss.test.ejb3.async;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * EJB Used to test @Asynchronous support by comparing the name
 * of the current Thread with the one used to execute the
 * invocation.
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 */
@Stateless
@Remote(AsyncTesterCommonBusiness.class)
public class AsyncTesterBean implements AsyncTesterCommonBusiness
{
   // --------------------------------------------------------------------------------||
   // Class Members ------------------------------------------------------------------||
   // --------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(AsyncTesterBean.class.getName());

   // --------------------------------------------------------------------------------||
   // Instance Members ---------------------------------------------------------------||
   // --------------------------------------------------------------------------------||

   @EJB
   private AsyncLocalBusiness local;

   @EJB
   private AsyncRemoteBusiness remote;

   @EJB
   private AsyncBean nointerface;

   @EJB
   private StatusCommonBusiness status;

   // --------------------------------------------------------------------------------||
   // Required Implementations -------------------------------------------------------||
   // --------------------------------------------------------------------------------||

    /**
     *
     * @return
     */
    public boolean isLocalAsyncInvocationExecutedInNewThread() {
        log.info("Local @Asynchronous Invocation");
        return isAsyncInvocationExecutedInNewThread(local);
    }

    /**
     * 
     * @return
     */
    public boolean isRemoteAsyncInvocationExecutedInNewThread() {
        log.info("Remote @Asynchronous Invocation");
        return isAsyncInvocationExecutedInNewThread(remote);
    }

    /**
     * 
     * @return
     */
    public boolean isNointerfaceAsyncInvocationExecutedInNewThread() {
        log.info("No-interface @Asynchronous Invocation");
        return isAsyncInvocationExecutedInNewThread(nointerface);
    }

    private boolean isAsyncInvocationExecutedInNewThread(final AsyncCommonBusiness bean)
    {
       final String current = Thread.currentThread().getName();
        final String used;
        try{
            used = bean.getExecutingThreadNameAsync().get();
        }
        catch(final Exception e){
            throw new RuntimeException(e);
        }
        log.info("@Asynchronous Invocation - Current:" + current + ", Used: " + used);
        return !current.equals(used);
    }

    public boolean isLocalBlockingInvocationExecutedInSameThread(){
                 final String current = Thread.currentThread().getName();
        final String used;
        try{
            used = local.getExecutingThreadNameBlocking().get();
        }
        catch(final Exception e){
            throw new RuntimeException(e);
        }
        log.info("Local non-@Asynchronous Invocation - Current:" + current + ", Used: " + used);
        return current.equals(used);
    }


    public boolean cancelInterruptIfRunningWorksLocal() {
        return this.cancelInterruptIfRunningWorks(local);
    }

    public boolean cancelInterruptIfRunningWorksRemote() {
        return this.cancelInterruptIfRunningWorks(remote);
    }

    private boolean cancelInterruptIfRunningWorks(final AsyncCommonBusiness bean)
    {   final Future<Void> future = bean.waitTenSeconds();

        // At least let the request get sent along
        try
        {
            Thread.sleep(300);
        }
        catch(final InterruptedException ie)
        {
            Thread.interrupted();
            throw new RuntimeException("Should not be interrupted");
        }

        final ExecutorService es =Executors.newSingleThreadExecutor();
        es.submit(new Callable<Void>() {

            public Void call() throws Exception {
                Thread.sleep(1000);
                future.cancel(true);
                return null;
            }
        });
        es.shutdown();

        log.info("Submitted cancel request");
        try {
            // Block
            log.info("Blocking on result now");
            future.get(20, TimeUnit.SECONDS);
            log.info("Done? " +future.isDone());
            log.info("Got result");
        } catch (InterruptedException e) {
            Thread.interrupted();
            log.info(e.getMessage());
           throw new RuntimeException("Should not be interrupted");
        } catch (ExecutionException e) {
            log.info(e.getMessage());
            throw new RuntimeException("Should not receive execution exception");
        }
        catch(final TimeoutException te)
        {
            log.info("TIMEOUT");
            throw new RuntimeException("Should not have timed out",te);
        }
        catch(final RuntimeException re){
            log.info("RuntimeException: " + re);
        }
        log.info("Returning");

        return status.wasCancelled();
    }

}