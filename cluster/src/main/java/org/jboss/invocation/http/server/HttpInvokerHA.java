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
package org.jboss.invocation.http.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.jboss.mx.util.JMXExceptionDecoder;
import org.jboss.ha.framework.interfaces.HARMIResponse;
import org.jboss.ha.framework.server.HATarget;
import org.jboss.ha.framework.interfaces.LoadBalancePolicy;
import org.jboss.ha.framework.interfaces.GenericClusteringException;
import org.jboss.invocation.http.interfaces.HttpInvokerProxyHA;
import org.jboss.invocation.Invocation;
import org.jboss.invocation.Invoker;
import org.jboss.invocation.InvokerHA;
import org.jboss.system.Registry;

/** An extension of the HttpInvoker and supports clustering of HTTP invokers.
 *
 * @author <a href="mailto:scott.stark@jboss.org>Scott Stark</a>
 * @version $Revision: 105943 $
 */
public class HttpInvokerHA extends HttpInvoker
   implements InvokerHA
{
   protected HashMap<Integer, HATarget> targetMap = new HashMap<Integer, HATarget>();

   // Public --------------------------------------------------------

   protected void startService()
      throws Exception
   {
      // Export the Invoker interface
      ObjectName name = super.getServiceName();
      Registry.bind(name, this);
      // Make sure the invoker URL is valid
      super.checkInvokerURL();
      log.debug("Bound HttpHA invoker for JMX node");
   }

   protected void stopService()
   {
      // Unxport the Invoker interface
      ObjectName name = super.getServiceName();
      Registry.unbind(name);
      log.debug("Unbound HttpHA invoker for JMX node");
   }

   protected void destroyService()
   {
      // Export references to the bean
      Registry.unbind(serviceName);
   }

   public void registerBean(ObjectName targetName, HATarget target) throws Exception
   {
      Integer hash = new Integer(targetName.hashCode());
      log.debug("Registered targetName("+targetName+"), hash="+hash
         + ", target="+target);
      if (targetMap.containsKey(hash))
      {
         throw new IllegalStateException("Duplicate targetName("+targetName
            + ") hashCode: "+hash);
      }
      targetMap.put(hash, target);
   }

   public void unregisterBean(ObjectName targetName) throws Exception
   {
      Integer hash = new Integer(targetName.hashCode());
      targetMap.remove(hash);
      log.debug("Unregistered targetName("+targetName+"), hash="+hash);
   }

   public Invoker createProxy(ObjectName targetName, LoadBalancePolicy policy,
                              String proxyFamilyName)
      throws Exception
   {
      Integer hash = new Integer(targetName.hashCode());
      HATarget target = (HATarget) targetMap.get(hash);
      if (target == null)
      {
         throw new IllegalStateException("The targetName("+targetName
            + "), hashCode("+hash+") not found");
      }
      Invoker proxy = new HttpInvokerProxyHA(target.getReplicantList(), target.getCurrentViewId (),
                                             policy, proxyFamilyName);
      return proxy;
   }

   public Serializable getStub()
   {
      return super.getInvokerURL();
   }

   /**
    * Invoke a Remote interface method.
    */
   public Object invoke(Invocation invocation)
      throws Exception
   {
      ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
      try
      {
         // Extract the ObjectName, the rest is still marshalled
         ObjectName mbean = (ObjectName) Registry.lookup(invocation.getObjectName());
         long clientViewId = ((Long)invocation.getValue("CLUSTER_VIEW_ID")).longValue();

         HATarget target = targetMap.get(invocation.getObjectName());
         if (target == null) 
         {
            // We could throw IllegalStateException but we have a race condition that could occur:
            // when we undeploy a bean, the cluster takes some time to converge
            // and to recalculate a new viewId and list of replicant for each HATarget.
            // Consequently, a client could own an up-to-date list of the replicants
            // (before the cluster has converged) and try to perform an invocation
            // on this node where the HATarget no more exist, thus receiving a
            // wrong exception and no failover is performed with an IllegalStateException
            //
            throw new GenericClusteringException(GenericClusteringException.COMPLETED_NO, 
                                                 "target is not/no more registered on this node");            
         }
         
         if (!target.invocationsAllowed ())
            throw new GenericClusteringException(GenericClusteringException.COMPLETED_NO, 
                                        "invocations are currently not allowed on this target");            

         // The cl on the thread should be set in another interceptor
         Object[] args = {invocation};
         String[] sig = {"org.jboss.invocation.Invocation"};
         Object rtn = super.getServer().invoke(mbean, 
            "invoke", args, sig);

         // Update the targets list if the client view is out of date
         HARMIResponse rsp = new HARMIResponse();
         if (clientViewId != target.getCurrentViewId())
         {
            rsp.newReplicants = new ArrayList<Object>(target.getReplicantList());
            rsp.currentViewId = target.getCurrentViewId();
         }
         rsp.response = rtn;

         // Return the raw object and let the http layer marshall it
         return rsp;
      }
      catch (InstanceNotFoundException e)
      {
         throw new GenericClusteringException(GenericClusteringException.COMPLETED_NO, e);
      }
      catch (ReflectionException e)
      {
         throw new GenericClusteringException(GenericClusteringException.COMPLETED_NO, e);
      }      
      catch(Exception e)
      {
         JMXExceptionDecoder.rethrow(e);

         // the compiler does not know an exception is thrown by the above
         throw new org.jboss.util.UnreachableStatementException();
      }
      finally
      {
         Thread.currentThread().setContextClassLoader(oldCl);
      }
   }
}
