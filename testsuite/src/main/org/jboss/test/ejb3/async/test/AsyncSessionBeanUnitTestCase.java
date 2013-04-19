/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.test.ejb3.async.test;

import junit.framework.Assert;
import junit.framework.Test;
import org.jboss.logging.Logger;
import org.jboss.test.JBossTestCase;
import org.jboss.test.ejb3.async.AsyncTesterCommonBusiness;

import javax.naming.InitialContext;

/**
 * Test Cases to ensure the EJB 3.1 implementation of the @Asynchronous
 * feature is working as expected
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 */
//TODO Move this to the EJB 3.1 Integration TestSuite when it becomes available
public class AsyncSessionBeanUnitTestCase
   extends JBossTestCase
{

   private static final Logger log = Logger.getLogger(AsyncSessionBeanUnitTestCase.class);

   public static Test suite() throws Exception
   {
      return getDeploySetup(AsyncSessionBeanUnitTestCase.class, "ejb31-async.jar");
   }

   public AsyncSessionBeanUnitTestCase(String name)
   {
      super(name);
   }

   public void testRemoteAsyncInvocation()
      throws Exception
   {
      final AsyncTesterCommonBusiness bean = this.getTesterBean();
       Assert.assertTrue("Remote invocation not executed in new Thread",bean.isRemoteAsyncInvocationExecutedInNewThread());
   }

   public void testLocalAsyncInvocation()
      throws Exception
   {
      final AsyncTesterCommonBusiness bean = this.getTesterBean();
      Assert.assertTrue("Local invocation not executed in new Thread",bean.isLocalAsyncInvocationExecutedInNewThread());
   }

   public void testNointerfaceAsyncInvocation()
      throws Exception
   {
      final AsyncTesterCommonBusiness bean = this.getTesterBean();
      Assert.assertTrue("Nointerface invocation not executed in new Thread",bean.isNointerfaceAsyncInvocationExecutedInNewThread());
   }

   public void testLocalBlockingInvocation() throws Exception{
      final AsyncTesterCommonBusiness bean = this.getTesterBean();
      Assert.assertTrue("Local non-async invocation not executed in same Thread",bean.isLocalBlockingInvocationExecutedInSameThread()); 
   }

   public void testLocalCancel() throws Exception{
       final AsyncTesterCommonBusiness bean = this.getTesterBean();
       Assert.assertTrue("Local async call should be able to be cancelled",bean.cancelInterruptIfRunningWorksLocal());
   }

   public void testLocalRemote() throws Exception{
       final AsyncTesterCommonBusiness bean = this.getTesterBean();
       Assert.assertTrue("Remote async call should be able to be cancelled",bean.cancelInterruptIfRunningWorksRemote());
   }

   private AsyncTesterCommonBusiness getTesterBean() throws Exception {
      final InitialContext ctx = getInitialContext();
      final String jndiName = "AsyncTesterBean/remote";
      final Object ref = ctx.lookup(jndiName);
      final AsyncTesterCommonBusiness bean = (AsyncTesterCommonBusiness) ref;
      return bean;
   }
}