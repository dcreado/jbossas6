/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.invocation.http.interfaces;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jboss.ha.framework.interfaces.ClusteringTargetsRepository;
import org.jboss.ha.framework.interfaces.FamilyClusterInfo;
import org.jboss.ha.framework.interfaces.GenericClusteringException;
import org.jboss.ha.framework.interfaces.HARMIResponse;
import org.jboss.ha.framework.interfaces.LoadBalancePolicy;
import org.jboss.invocation.Invocation;
import org.jboss.invocation.InvocationException;
import org.jboss.invocation.Invoker;
import org.jboss.invocation.InvokerProxyHA;
import org.jboss.invocation.MarshalledInvocation;
import org.jboss.invocation.PayloadKey;
import org.jboss.invocation.ServiceUnavailableException;
import org.jboss.logging.Logger;
import org.jboss.tm.TransactionPropagationContextFactory;
import org.jboss.tm.TransactionPropagationContextUtil;

/** The client side Http invoker proxy that posts an invocation to the
 InvokerServlet using the HttpURLConnection created from a target url.
 This proxy handles failover using its associated LoadBalancePolicy and
 current list of URL strings. The candidate URLs are updated dynamically
 after an invocation if the cluster partitation view has changed.

* @author Scott.Stark@jboss.org
* @author <a href="mailto:galder.zamarreno@jboss.com">Galder Zamarreno</a>
* @version $Revision: 104443 $
*/
public class HttpInvokerProxyHA
   implements InvokerProxyHA, Invoker, Externalizable
{
   // Constants -----------------------------------------------------
   private static Logger log = Logger.getLogger(HttpInvokerProxyHA.class);

   /** Serial Version Identifier.
    * @since 1.1.4.5
    */
   private static final long serialVersionUID = -7081220026780794383L;
   
   public static final Map<Object, Object> txFailoverAuthorizations = Collections.synchronizedMap(new WeakHashMap<Object, Object>());

   // Attributes ----------------------------------------------------

   // URL to the remote JMX node invoker
   protected LoadBalancePolicy loadBalancePolicy;
   protected String proxyFamilyName = null;
   protected FamilyClusterInfo familyClusterInfo = null;
   /** Trace level logging flag only set when the proxy is created or read from JNDI */
   protected transient boolean trace = false;

   // Constructors --------------------------------------------------
   public HttpInvokerProxyHA()
   {
      // For externalization to work
   }

   /**
    * @param targets  the list of URLs through which clients should contact the
    * InvokerServlet.
    * @param policy  the policy for choosing among targets ClusteringTargetsRepository under
    *    which this proxy is to be stored
    * @param proxyFamilyName  the name into the 
   */
   public HttpInvokerProxyHA(List targets, long viewId, LoadBalancePolicy policy,
      String proxyFamilyName)
   {
      this.familyClusterInfo = ClusteringTargetsRepository.initTarget (proxyFamilyName, targets, viewId);
      this.loadBalancePolicy = policy;
      this.proxyFamilyName = proxyFamilyName;
      this.trace = log.isTraceEnabled();
      if( trace )
         log.trace("Init, cluterInfo: "+familyClusterInfo+", policy="+loadBalancePolicy);
   }

   // Public --------------------------------------------------------

   public void updateClusterInfo (List<?> targets, long viewId)
   {
      if (familyClusterInfo != null)
         this.familyClusterInfo.updateClusterInfo (targets, viewId);
   }

   public String getServerHostName() throws Exception
   {
      return null;
   }

   public FamilyClusterInfo getFamilyClusterInfo()
   {
      return familyClusterInfo;
   }

   public void forbidTransactionFailover(Object tpc)
   {
      txFailoverAuthorizations.put(tpc, null);
   }
   
   public String getProxyFamilyName()
   {
      return proxyFamilyName;
   }

   public Object getRemoteTarget()
   {
      return getRemoteTarget(null);
   }
   public Object getRemoteTarget(Invocation invocationBasedRouting)
   {
      Object target = loadBalancePolicy.chooseTarget(this.familyClusterInfo, invocationBasedRouting);
      if( trace )
         log.trace("Choose remoteTarget: "+target);
      return target;
   }

   public void remoteTargetHasFailed(Object target)
   {
      removeDeadTarget(target);
   }   

   protected int totalNumberOfTargets ()
   {
      int size = 0;
      if( familyClusterInfo != null )
         size = familyClusterInfo.getTargets().size();
      return size;
   }

   protected void removeDeadTarget(Object target)
   {
      if( familyClusterInfo != null )
      {
         List targets = familyClusterInfo.removeDeadTarget(target);
         if( trace )
         {
            log.trace("removeDeadTarget("+target+"), targets.size="+targets.size());
         }
      }
   }
   protected void resetView ()
   {
      familyClusterInfo.resetView();
   }

   /** This method builds a MarshalledInvocation from the invocation passed
    in and then does a post to the target URL.
   */
   public Object invoke(Invocation invocation)
      throws Exception
   {
      // we give the opportunity, to any server interceptor, to know if this a
      // first invocation to a node or if it is a failovered call
      //
      int failoverCounter = 0;

      // We are going to go through a Remote invocation, switch to a Marshalled Invocation
      MarshalledInvocation mi = new MarshalledInvocation(invocation);         
      mi.setValue("CLUSTER_VIEW_ID", new Long(familyClusterInfo.getCurrentViewId()));
      String target = (String) getRemoteTarget(invocation);
      boolean failoverAuthorized = true;
      URL externalURL = Util.resolveURL(target);
      Exception lastException = null;
      while( externalURL != null && failoverAuthorized)
      {
         boolean definitivelyRemoveNodeOnFailure = true;
         invocation.setValue("FAILOVER_COUNTER", new Integer(failoverCounter), PayloadKey.AS_IS);
         try
         {
            if( trace )
               log.trace("Invoking on target="+externalURL);
            Object rtn = Util.invoke(externalURL, mi);
            HARMIResponse rsp = (HARMIResponse) rtn;

            if (rsp.newReplicants != null)
               updateClusterInfo(rsp.newReplicants, rsp.currentViewId);
            
            invocationHasReachedAServer (invocation);
            
            return rsp.response;
         }
         catch(GenericClusteringException e)
         {
            definitivelyRemoveNodeOnFailure = handleGenericClusteringException(invocation, failoverCounter,
                  definitivelyRemoveNodeOnFailure, e);
         }
         catch(InvocationException e)
         {
            // Handle application declared exceptions
            Throwable cause = e.getTargetException();
            if (cause instanceof GenericClusteringException)
            {
               GenericClusteringException gce = (GenericClusteringException) cause;
               definitivelyRemoveNodeOnFailure = handleGenericClusteringException(invocation, failoverCounter,
                     definitivelyRemoveNodeOnFailure, gce);
            }
            else
            {
               invocationHasReachedAServer(invocation);
               if( cause instanceof Exception )
                  throw (Exception) cause;
               else if (cause instanceof Error)
                  throw (Error) cause;
               throw new InvocationTargetException(cause);
            }
         }
         catch (java.net.ConnectException e)
         {
            lastException = e;
         }
         catch (java.net.UnknownHostException e)
         {
            lastException = e;
         }
         catch(Exception e)
         {
            if( trace )
               log.trace("Invoke failed, target="+externalURL, e);
            invocationHasReachedAServer(invocation);
            // Rethrow for the application to handle
            throw e;
         }

         // If we reach here, this means that we must fail-over
         remoteTargetHasFailed(target);
         if( definitivelyRemoveNodeOnFailure )
            resetView();
         failoverAuthorized = txContextAllowsFailover (invocation);            
         target = (String) getRemoteTarget(invocation);
         externalURL = Util.resolveURL(target);
         failoverCounter ++;
      }
      
      // if we get here this means list was exhausted
      String msg = failoverAuthorized ? "Service unavailable." : "Service unavailable (failover not possible inside a user transaction).";
      throw new ServiceUnavailableException(msg, lastException);
   }

   private boolean handleGenericClusteringException(Invocation invocation, int failoverCounter,
         boolean definitivlyRemoveNodeOnFailure, GenericClusteringException gce) throws ServerException
   {
      // this is a generic clustering exception that contain the
      // completion status: usefull to determine if we are authorized
      // to re-issue a query to another node
      //               
      if( gce.getCompletionStatus() == GenericClusteringException.COMPLETED_NO )
      {
            // we don't want to remove the node from the list of targets 
            // UNLESS there is a risk to loop
            if (totalNumberOfTargets() >= failoverCounter)
            {
               if( gce.isDefinitive() == false )
                  definitivlyRemoveNodeOnFailure = false;                     
            }
      }
      else
      {
         invocationHasReachedAServer(invocation);
         throw new ServerException("Clustering error", gce);
      }
      return definitivlyRemoveNodeOnFailure;
   }
   
   public void invocationHasReachedAServer (Invocation invocation)
   {
      Object tpc = getTransactionPropagationContext();
      if (tpc != null)
      {
         forbidTransactionFailover(tpc);
      }
   }


   /**
    * Overriden method to rethrow any potential SystemException arising from it.
    * Looking at the parent implementation, none of the methods called actually 
    * throw SystemException.
    */
   public Object getTransactionPropagationContext()
   {
      TransactionPropagationContextFactory tpcFactory = TransactionPropagationContextUtil.getTPCFactoryClientSide();
      return (tpcFactory == null) ? null : tpcFactory.getTransactionPropagationContext();
   }  
   
   public boolean txContextAllowsFailover (Invocation invocation)
   {
      Object tpc = getTransactionPropagationContext();
      if (tpc != null)
      {
         if (trace)
         {
            log.trace("Checking tx failover authorisation map with tpc " + tpc);
         }
         
         /* If the map contains the tpc, then we can't allow a failover */
         return ! txFailoverAuthorizations.containsKey(tpc);            
      }

      return true;
   } 

   /** Externalize this instance.
   */
   @SuppressWarnings("unchecked")
   public void writeExternal(final ObjectOutput out)
      throws IOException
   { 
      // JBAS-2071 - sync on FCI to ensure targets and vid are consistent
      ArrayList currentTargets = null;
      long vid = 0;
      synchronized (this.familyClusterInfo)
      {
         // JBAS-6345 -- write an ArrayList for compatibility with AS 3.x/4.x clients
         currentTargets = new ArrayList(this.familyClusterInfo.getTargets());
         vid = this.familyClusterInfo.getCurrentViewId ();
      }
      out.writeObject(currentTargets);
      out.writeLong(vid);
      out.writeObject(this.loadBalancePolicy);
      out.writeObject(this.proxyFamilyName);
   }

   /** Un-externalize this instance.
   */
   public void readExternal(final ObjectInput in)
      throws IOException, ClassNotFoundException
   {
      List targets = (List) in.readObject();
      long vid = in.readLong ();
      this.loadBalancePolicy = (LoadBalancePolicy) in.readObject();
      this.proxyFamilyName = (String)in.readObject();
      this.trace = log.isTraceEnabled();

      // keep a reference on our family object
      this.familyClusterInfo = ClusteringTargetsRepository.initTarget(this.proxyFamilyName, targets, vid);
      if( trace )
         log.trace("Init, clusterInfo: "+familyClusterInfo+", policy="+loadBalancePolicy);
   }
}

