/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.webservices.integration.invocation;

import java.lang.reflect.Method;
import java.security.Principal;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.rpc.handler.MessageContext;
import javax.xml.rpc.handler.soap.SOAPMessageContext;
import javax.xml.ws.WebServiceException;

import org.jboss.ejb.EjbModule;
import org.jboss.ejb.Interceptor;
import org.jboss.ejb.StatelessSessionContainer;
import org.jboss.invocation.InvocationKey;
import org.jboss.invocation.InvocationType;
import org.jboss.invocation.PayloadKey;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.wsf.common.ObjectNameFactory;
import org.jboss.wsf.common.integration.WSHelper;
import org.jboss.wsf.common.invocation.AbstractInvocationHandler;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.invocation.HandlerCallback;
import org.jboss.wsf.spi.invocation.Invocation;
import org.jboss.wsf.spi.invocation.SecurityAdaptor;
import org.jboss.wsf.spi.invocation.SecurityAdaptorFactory;
import org.jboss.wsf.spi.metadata.j2ee.EJBArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBMetaData;

/**
 * Handles invocations on EJB21 endpoints.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
final class InvocationHandlerEJB21 extends AbstractInvocationHandler
{
   /** EJB21 JNDI name. */
   private String jndiName;

   /** MBean server. */
   private MBeanServer server;

   /** Object name. */
   private ObjectName ejb21ContainerName;

   /**
    * Consctructor.
    */
   InvocationHandlerEJB21()
   {
      this.server = MBeanServerLocator.locateJBoss();
   }

   /**
    * Initializes EJB 21 endpoint.
    *
    * @param endpoint web service endpoint
    */
   public void init(final Endpoint endpoint)
   {
      final String ejbName = endpoint.getShortName();
      final Deployment dep = endpoint.getService().getDeployment();
      final EJBArchiveMetaData ejbArchiveMD = WSHelper.getRequiredAttachment(dep, EJBArchiveMetaData.class);
      final EJBMetaData ejbMD = (EJBMetaData) ejbArchiveMD.getBeanByEjbName(ejbName);

      if (ejbMD == null)
      {
         throw new WebServiceException("Cannot obtain ejb meta data for: " + ejbName);
      }

      // get the bean's JNDI name
      this.jndiName = ejbMD.getContainerObjectNameJndiName();

      if (this.jndiName == null)
      {
         throw new WebServiceException("Cannot obtain JNDI name for: " + ejbName);
      }
   }

   /**
    * Gets EJB 21 container name lazily.
    *
    * @param endpoint webservice endpoint
    * @return EJB21 container name
    */
   private synchronized ObjectName getEjb21ContainerName(final Endpoint endpoint)
   {
      final boolean ejb21ContainerNotInitialized = this.ejb21ContainerName == null;

      if (ejb21ContainerNotInitialized)
      {
         this.ejb21ContainerName = ObjectNameFactory.create("jboss.j2ee:jndiName=" + this.jndiName + ",service=EJB");
         final boolean ejb21NotRegistered = !this.server.isRegistered(this.ejb21ContainerName);
         if (ejb21NotRegistered)
         {
            throw new IllegalArgumentException("Cannot find service endpoint target: " + this.ejb21ContainerName);
         }

         // Inject the Service endpoint interceptor
         this.insertEJB21ServiceEndpointInterceptor(this.ejb21ContainerName, endpoint.getShortName());
      }

      return this.ejb21ContainerName;
   }

   /**
    * Invokes EJB 21 endpoint.
    *
    * @param endpoint EJB 21 endpoint
    * @param wsInvocation web service invocation
    * @throws Exception if any error occurs
    */
   public void invoke(final Endpoint endpoint, final Invocation wsInvocation) throws Exception
   {
      final ObjectName ejb21Name = this.getEjb21ContainerName(endpoint);

      try
      {
         // prepare for invocation
         final org.jboss.invocation.Invocation jbossInvocation = this.getMBeanInvocation(wsInvocation);
         final String[] signature =
         {org.jboss.invocation.Invocation.class.getName()};
         final Object[] args = new Object[]
         {jbossInvocation};

         // invoke method
         final Object retObj = this.server.invoke(ejb21Name, "invoke", args, signature);
         wsInvocation.setReturnValue(retObj);
      }
      catch (Exception e)
      {
         this.log.error("Method invocation failed with exception: " + e.getMessage(), e);
         this.handleInvocationException(e);
      }
   }

   /**
    * Returns configured EJB 21 JBoss MBean invocation.
    *
    * @param wsInvocation webservice invocation
    * @return configured JBoss invocation
    */
   private org.jboss.invocation.Invocation getMBeanInvocation(final Invocation wsInvocation)
   {
      // ensure preconditions
      final MessageContext msgContext = wsInvocation.getInvocationContext().getAttachment(MessageContext.class);
      if (msgContext == null)
      {
         throw new IllegalStateException("Cannot obtain MessageContext");
      }

      final HandlerCallback callback = wsInvocation.getInvocationContext().getAttachment(HandlerCallback.class);
      if (callback == null)
      {
         throw new IllegalStateException("Cannot obtain HandlerCallback");
      }

      // prepare security data
      final SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
      final SecurityAdaptor securityAdaptor = spiProvider.getSPI(SecurityAdaptorFactory.class).newSecurityAdapter();
      final Principal principal = securityAdaptor.getPrincipal();
      final Object credential = securityAdaptor.getCredential();

      // prepare invocation data
      final Method method = wsInvocation.getJavaMethod();
      final Object[] args = wsInvocation.getArgs();
      final org.jboss.invocation.Invocation jbossInvocation = new org.jboss.invocation.Invocation(null, method, args,
            null, principal, credential);

      // propagate values to JBoss invocation
      jbossInvocation.setValue(InvocationKey.SOAP_MESSAGE_CONTEXT, msgContext);
      jbossInvocation.setValue(InvocationKey.SOAP_MESSAGE, ((SOAPMessageContext) msgContext).getMessage());
      jbossInvocation.setType(InvocationType.SERVICE_ENDPOINT);
      jbossInvocation.setValue(HandlerCallback.class.getName(), callback, PayloadKey.TRANSIENT);
      jbossInvocation.setValue(Invocation.class.getName(), wsInvocation, PayloadKey.TRANSIENT);

      return jbossInvocation;
   }

   /**
    * This method dynamically inserts EJB 21 webservice endpoint interceptor
    * to the last but one position in EJB 21 processing chain. See [JBWS-756] for more info.
    *
    * @param objectName EJB 21 object name
    * @param ejbName EJB 21 short name
    */
   private void insertEJB21ServiceEndpointInterceptor(final ObjectName objectName, final String ejbName)
   {
      try
      {
         final EjbModule ejbModule = (EjbModule) this.server.getAttribute(objectName, "EjbModule");
         final StatelessSessionContainer container = (StatelessSessionContainer) ejbModule.getContainer(ejbName);

         Interceptor currentInterceptor = container.getInterceptor();
         while (currentInterceptor != null && currentInterceptor.getNext() != null)
         {
            final Interceptor nextInterceptor = currentInterceptor.getNext();

            if (nextInterceptor.getNext() == null)
            {
               final ServiceEndpointInterceptorEJB21 sepInterceptor = new ServiceEndpointInterceptorEJB21();
               currentInterceptor.setNext(sepInterceptor);
               sepInterceptor.setNext(nextInterceptor);
               this.log.debug("Injecting EJB 21 service endpoint interceptor after: "
                     + currentInterceptor.getClass().getName());

               return;
            }
            currentInterceptor = nextInterceptor;
         }

         this.log.warn("Cannot find EJB 21 service endpoint interceptor insert point");
      }
      catch (Exception ex)
      {
         this.log.warn("Cannot register EJB 21 service endpoint interceptor: ", ex);
      }
   }
}
