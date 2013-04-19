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

package org.jboss.web.tomcat.service.ondemand;

/**
 * Interface implemented by listeners that wish to be notified of requests
 * for as-yet-undeployed "on-demand" web applications.  It is expected that the
 * typical implementation of this interface would trigger deployment of the web
 * application.
 *
 * @author Brian Stansberry
 * 
 * @version $Revision: 100726 $
 */
public interface ContextDemandListener
{
   /** 
    * Notification that a request has been received for a web application
    * previously {@link OnDemandContextIntegrator#registerContextDemandListener(ContextDemandListener)
    * registered with JBoss Web} as eligible for on-demand deployment.
    * <p>
    * Implementors of this interface should assume that the notification will be
    * received before the web server has handled the request and that any handling
    * of the request is either being done by the calling thread or will block 
    * until the <code>contextDemanded</code> call has returned.
    * </p>
    *
    * 
    * @param serviceName the name of the JBoss Web <code>Service</code> that 
    *                    includes the connector that received the request.
    *                    Will not be <code>null</code>.
    * @param hostName the name of the JBoss Web <code>Host</code> that is the
    *                 virtual host associated with the request.
    *                 Will not be <code>null</code>.
    * @param contextName the name of the on-demand web application context, i.e.
    *                    the context path portion of the request URL, with
    *                    any leading and trailing / removed.
    *                    Will not be <code>null</code>.
    */
   void contextDemanded(String serviceName, String hostName, String contextName);
}
