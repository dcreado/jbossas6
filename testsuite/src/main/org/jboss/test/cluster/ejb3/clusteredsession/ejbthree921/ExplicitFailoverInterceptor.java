/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.test.cluster.ejb3.clusteredsession.ejbthree921;

import org.jboss.ha.framework.interfaces.GenericClusteringException;
import org.jboss.logging.Logger;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 * Used for testing clustering: allows to explicitly makes a call to node fail
 * This will mimic a dead server. This is used as a ejb3 interceptor now.
 * @author Ben Wang
 *
 */
public class ExplicitFailoverInterceptor
{
   private Logger log = Logger.getLogger(ExplicitFailoverInterceptor.class);

   @AroundInvoke
   public Object invoke(InvocationContext ctx)
      throws Exception
   {
      checkFailoverNeed (ctx);
      return ctx.proceed();
   }

   protected void checkFailoverNeed (InvocationContext ctx)
      throws Exception
   {
      if(ctx.getMethod().getName().equals("setUpFailover"))
      {
         return;
      }

      String failover = (String)System.getProperty ("JBossCluster-DoFail");
      boolean doFail = false;

      if (failover != null)
      {
         String strFailover = failover;
         if (strFailover.equalsIgnoreCase ("true"))
         {
            doFail = true;
         }
         else if (strFailover.equalsIgnoreCase ("once"))
         {
            doFail = true;
            System.setProperty ("JBossCluster-DoFail", "false");
         }
      }

      if (doFail)
      {
         GenericClusteringException e = new GenericClusteringException
         (GenericClusteringException.COMPLETED_NO, "Test failover from ejb interceptor", false);
         
         log.debug ("WE FAILOVER IN EJB INTERCEPTOR (explicit failover)!", e);

         throw e;
      }
   }
}
