/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc, and individual contributors
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
 * Mediates between any {@link ContextDemandListener}s and JBoss Web, allowing 
 * JBoss Web to be made aware of the presence of on-demand applications and
 * for the listeners to be notified when a user request for an on-demand
 * application has arrived. An implementation of this interface is responsible
 * for finding and interacting with the relevant JBoss Web components, freeing
 * the listeners from that responsibility.
 *
 * @author Brian Stansberry
 * 
 * @version $Revision: 100726 $
 */
public interface OnDemandContextIntegrator
{
   /**
    * Registers a listener to receive callbacks when a request is received
    * for an on-demand web application.
    * 
    * @param listener the listener
    */
   void registerContextDemandListener(ContextDemandListener listener);

   /**
    * Removes a listener from the set of listeners receiving callbacks when a
    * request is received for an on-demand web application.
    * 
    * @param listener the listener
    */
   void removeContextDemandListener(ContextDemandListener listener);

   /**
    * Configures JBoss Web to recognize requests for the given connectors, virtual
    * host and web application context as being for an as-yet-undeployed 
    * "on-demand" web application.
    * 
    * @param serviceName the name of the JBoss Web <code>Service</code> that 
    *                    includes the connectors that will receive requests for 
    *                    the on-demand web application. Cannot be <code>null</code>.
    * @param hostName the name of the JBoss Web <code>Host</code> that is the
    *                 virtual host for on-demand web application. 
    *                 Cannot be <code>null</code>.
    * @param contextName the name of the on-demand web application context, i.e.
    *                    the context path portion of URLs that target it, with
    *                    any leading and trailing / removed
    */
   void registerOnDemandContext(String serviceName, String hostName, String contextName);

   /**
    * Configures JBoss Web to no longer recognize requests for the given 
    * connectors, virtual host and web application context.
    * 
    * @param serviceName the name of the JBoss Web <code>Service</code> that 
    *                    includes the connectors that will receive requests for 
    *                    the on-demand web application.  Cannot be <code>null</code>.
    * @param hostName the name of the JBoss Web <code>Host</code> that is the
    *                 virtual host for on-demand web application.  Cannot be <code>null</code>.
    * @param contextName the name of the on-demand web application context, i.e.
    *                    the context path portion of URLs that target it, with
    *                    any leading and trailing / removed
    */
   void removeOnDemandContext(String serviceName, String hostName, String contextName);

}