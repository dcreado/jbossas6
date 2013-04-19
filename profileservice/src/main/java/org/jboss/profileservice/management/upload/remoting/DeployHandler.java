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
package org.jboss.profileservice.management.upload.remoting;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import javax.management.MBeanServer;

import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.InvocationResponse;
import org.jboss.aspects.remoting.AOPRemotingInvocationHandler;
import org.jboss.deployers.spi.management.deploy.DeploymentID;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.logging.Logger;
import org.jboss.profileservice.management.client.upload.SerializableDeploymentID;
import org.jboss.profileservice.remoting.SecurityContainer;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.stream.StreamInvocationHandler;
import org.jboss.security.ISecurityManagement;
import org.jboss.security.SecurityContext;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * A profile service deploy subsystem handling transient deployments. 
 * The AbstractDeployHandler takes care of the profile deployments.
 *
 * @author Scott.Stark@jboss.org
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision: 109157 $
 */
public class DeployHandler extends AOPRemotingInvocationHandler
   implements StreamInvocationHandler
{
   /** The logger. */
   static final Logger log = Logger.getLogger(DeployHandler.class);

   /** The profile service security domain name */
   private String securityDomain = "jmx-console";
   /** The security management layer to use in the security context setup */
   private ISecurityManagement securityManagement;

   private DeployHandlerDelegate delegate;
   
   public DeployHandler(DeployHandlerDelegate delegate)
   {
      this.delegate = delegate;
   }
   
   public String getSecurityDomain()
   {
      return securityDomain;
   }
   public void setSecurityDomain(String securityDomain)
   {
      this.securityDomain = securityDomain;
   }

   public ISecurityManagement getSecurityManagement()
   {
      return securityManagement;
   }
   public void setSecurityManagement(ISecurityManagement securityManagement)
   {
      this.securityManagement = securityManagement;
   }

   public void addListener(InvokerCallbackHandler arg0)
   {
   }
   
   public void removeListener(InvokerCallbackHandler arg0)
   {
   }

   public void setInvoker(ServerInvoker arg0)
   {
   }

   public void setMBeanServer(MBeanServer arg0)
   {
   }
   
   /**
    * Handle a DeploymentManager invocation other than distribute
    * 
    * @param request - the remoting invocation
    * @return the result of the invocation
    */
   public Object invoke(InvocationRequest request) throws Throwable
   {
      // Create a security context for the invocation
      establishSecurityContext(request);
      Object parameter = request.getParameter();
      
      Object returnValue = null;

      if(parameter instanceof Invocation)
      {
         Invocation inv =(Invocation) parameter;
         SecurityContainer.setInvocation(inv);
         returnValue = super.invoke(request);
      }
      else
      {
         Map<?, ?> payload = request.getRequestPayload();
         DeploymentID dtID = (DeploymentID) payload.get("DeploymentTargetID");
         if(dtID == null)
            throw new IllegalStateException("Null deployment target ID.");
         
         log.info("invoke, payload: "+payload+", parameter: "+parameter);
         try
         {
            if( parameter.equals("getRepositoryNames"))
            {
               returnValue = delegate.resolveDeploymentNames(dtID.getNames());
            }
            else if( parameter.equals("distribute") )
            {
               final URL url = dtID.getContentURL();
               final VirtualFile vf = VFS.getChild(url);
               if(vf == null || vf.exists() == false) {
                  throw new IllegalStateException(String.format("file (%s) does not exist. Use 'copyContent = true' to copy the deployment", url));
               }
               returnValue = delegate.distribute(dtID, null);
            }
            else if( parameter.equals("prepare"))
            {
               // TODO
            }
            else if( parameter.equals("start") )
            {
               delegate.startDeployments(dtID.getNames());
            }
            else if( parameter.equals("stop") )
            {
               delegate.stopDeployments(dtID.getNames());
            }
            else if( parameter.equals("remove"))
            {
               delegate.removeDeployments(dtID.getNames());
            }
            // Keep for backward compatibility
            else if( parameter.equals("undeploy") )
            {
               delegate.removeDeployments(dtID.getNames());
            }
            else if (parameter.equals("redeploy"))
            {
               String[] names = dtID.getNames();
               delegate.stopDeployments(names);
               delegate.startDeployments(names);
            }
         }
         catch(Exception e)
         {
            // Server side logging
            log.warn("Failed to complete command: ["+ parameter +"] for deployment: " + dtID, e);
            throw e;
         }

      }
      return returnValue;
   }
   
   /**
    * Handle a DeploymentManager distribute invocation for copyContent == true
    * 
    * @see DeploymentManager#distribute(String, java.net.URL, boolean)
    * @param request - the remoting invocation
    */
   public InvocationResponse handleStream(InputStream contentIS, InvocationRequest request) throws Throwable
   {
      // Get the deployment repository for this deploymentID
      SerializableDeploymentID deploymentTarget = (SerializableDeploymentID) request.getParameter();

      String[] names = delegate.distribute(deploymentTarget, contentIS);
      deploymentTarget.setRepositoryNames(names);
     
      return new InvocationResponse(names[0]);
   }
   
   private void establishSecurityContext(InvocationRequest invocation) throws Exception
   { 
      SecurityContext newSC = SecurityActions.createAndSetSecurityContext(securityDomain);  

      // Set the SecurityManagement on the context
      SecurityActions.setSecurityManagement(newSC, securityManagement);
      log.trace("establishSecurityIdentity:SecCtx="+SecurityActions.trace(newSC));
   }
}
