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

import javax.xml.rpc.handler.soap.SOAPMessageContext;

import org.jboss.ejb.plugins.AbstractInterceptor;
import org.jboss.invocation.InvocationKey;
import org.jboss.wsf.spi.invocation.HandlerCallback;
import org.jboss.wsf.spi.invocation.Invocation;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData.HandlerType;

/**
 * This Interceptor does the ws4ee handler processing on EJB 21 endpoints.
 *
 * According to the ws4ee spec the handler logic must be invoked after the container
 * applied method level security to the invocation.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
final class ServiceEndpointInterceptorEJB21 extends AbstractInterceptor
{
   /**
    * Constructor.
    */
   ServiceEndpointInterceptorEJB21()
   {
      super();
   }

   /**
    * Before and after we call the EJB 21 service endpoint bean, we process the handler chains.
    *
    * @param jbossInvocation jboss invocation
    * @return ws invocation return value
    * @throws Exception if any error occurs
    */
   @Override
   public Object invoke(final org.jboss.invocation.Invocation jbossInvocation) throws Exception
   {
      final SOAPMessageContext msgContext = (SOAPMessageContext) jbossInvocation
            .getPayloadValue(InvocationKey.SOAP_MESSAGE_CONTEXT);
      if (msgContext == null)
      {
         // not for us
         return this.getNext().invoke(jbossInvocation);
      }

      final Invocation wsInvocation = (Invocation) jbossInvocation.getValue(Invocation.class.getName());
      final HandlerCallback callback = (HandlerCallback) jbossInvocation.getValue(HandlerCallback.class.getName());

      if (callback == null || wsInvocation == null)
      {
         log.warn("Handler callback not available");
         return this.getNext().invoke(jbossInvocation);
      }

      // Handlers need to be Tx. Therefore we must invoke the handler chain after the TransactionInterceptor.
      try
      {
         // call the request handlers
         boolean handlersPass = callback.callRequestHandlerChain(wsInvocation, HandlerType.ENDPOINT);
         handlersPass = handlersPass && callback.callRequestHandlerChain(wsInvocation, HandlerType.POST);

         // Call the next interceptor in the chain
         if (handlersPass)
         {
            // The SOAPContentElements stored in the EndpointInvocation might have changed after
            // handler processing. Get the updated request payload. This should be a noop if request
            // handlers did not modify the incomming SOAP message.
            final Object[] reqParams = wsInvocation.getArgs();
            jbossInvocation.setArguments(reqParams);
            final Object resObj = this.getNext().invoke(jbossInvocation);

            // Setting the message to null should trigger binding of the response message
            msgContext.setMessage(null);
            wsInvocation.setReturnValue(resObj);
         }

         // call the response handlers
         handlersPass = callback.callResponseHandlerChain(wsInvocation, HandlerType.POST);
         handlersPass = handlersPass && callback.callResponseHandlerChain(wsInvocation, HandlerType.ENDPOINT);

         // update the return value after response handler processing
         return wsInvocation.getReturnValue();
      }
      catch (Exception ex)
      {
         try
         {
            // call the fault handlers
            boolean handlersPass = callback.callFaultHandlerChain(wsInvocation, HandlerType.POST, ex);
            handlersPass = handlersPass && callback.callFaultHandlerChain(wsInvocation, HandlerType.ENDPOINT, ex);
         }
         catch (Exception e)
         {
            log.warn("Cannot process handlerChain.handleFault, ignoring: ", e);
         }
         throw ex;
      }
   }
}
