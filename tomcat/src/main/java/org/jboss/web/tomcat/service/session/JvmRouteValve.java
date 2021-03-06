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
package org.jboss.web.tomcat.service.session;

import java.io.IOException;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.apache.catalina.*;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.valves.ValveBase;
import org.jboss.logging.Logger;
import org.jboss.web.tomcat.service.session.distributedcache.spi.OutgoingDistributableSessionData;

/**
 * Web request valve to specifically handle Tomcat jvmRoute using mod_jk(2)
 * module. We assume that the session id has a format of id.jvmRoute where 
 * jvmRoute is used by JK module to determine sticky session during 
 * load balancing. Checks for failover by matching session and request jvmRoute
 * to the session manager's, updating the session and session cookie if a 
 * failover is detected.
 *
 * This valve is inserted in the pipeline only when mod_jk is configured.
 * 
 * @author Ben Wang
 * @author Brian Stansberry
 * 
 * @version $Revision: 108925 $
 */
public class JvmRouteValve extends ValveBase implements Lifecycle
{
   // The info string for this Valve
   private static final String info = "JvmRouteValve/1.0";

   protected static Logger log_ = Logger.getLogger(JvmRouteValve.class);

   // Valve-lifecycle_ helper object
   protected LifecycleSupport support = new LifecycleSupport(this);

   protected AbstractJBossManager manager_;

   /**
    * Create a new Valve.
    *
    */
   public JvmRouteValve(AbstractJBossManager manager)
   {
      super();
      manager_ = manager;
   }

   /**
    * Get information about this Valve.
    */
   public String getInfo()
   {
      return info;
   }

   public void invoke(Request request, Response response) throws IOException, ServletException
   {
      // Need to check it before let request through.
      checkJvmRoute(request, response);

      // let the servlet invocation go through
      getNext().invoke(request, response);
   }

   public void checkJvmRoute(Request req, Response res)
      throws IOException, ServletException
   {
      String requestedId = req.getRequestedSessionId();
      HttpSession session = req.getSession(false);
      if (session != null)
      {
         String sessionId = session.getId();
         String jvmRoute = manager_.getJvmRoute();
         
         if (log_.isTraceEnabled())
         {
            log_.trace("checkJvmRoute(): check if need to re-route based on JvmRoute. Session id: " +
               sessionId + " jvmRoute: " + jvmRoute);
         }
         
         if (jvmRoute != null)
         {
            // Check if incoming session id has JvmRoute appended. If not, append it.
            boolean setCookie = !req.isRequestedSessionIdFromURL();
            handleJvmRoute(requestedId, sessionId, jvmRoute, res, setCookie);
         }
      }
   }

   protected void handleJvmRoute(String requestedId,
                                 String sessionId, 
                                 String jvmRoute, 
                                 HttpServletResponse response,
                                 boolean setCookie)
         throws IOException
   {      
      // The new id we'll give the session if we detect a failover
      String newId = null;
      
      // First, check if the session object's jvmRoute matches ours      
      
      // TODO. The current format is assumed to be id.jvmRoute. Can be generalized later.
      String sessionJvmRoute = null;
      int index = sessionId.indexOf('.', 0);
      if (index > -1 && index < sessionId.length() -1)
      {
         sessionJvmRoute = sessionId.substring(index + 1, sessionId.length());
      }
      
      if (!jvmRoute.equals(sessionJvmRoute))
      {
         if (index < 0)
         {
            // If this valve is turned on, we assume we have an appendix of jvmRoute. 
            // So this request is new.
            newId = new StringBuilder(sessionId).append('.').append(jvmRoute).toString();
         }         
         else 
         {
            // We just had a failover since jvmRoute does not match. 
            // We will replace the old one with the new one.         
            if (log_.isTraceEnabled())
            {
               log_.trace("handleJvmRoute(): We have detected a failover with different jvmRoute." +
                  " old one: " + sessionJvmRoute + " new one: " + jvmRoute + ". Will reset the session id.");
            }
            
            String base = sessionId.substring(0, index);
            
            newId = base + "." + this.manager_.locate(base);
         }
         
         // Fix the session's id
         resetSessionId(sessionId, newId);
      }
      
      // Now we know the session object has a correct id
      // Also need to ensure any session cookie is correct
      if (setCookie)
      {
         // Check if the jvmRoute of the requested session id matches ours.
         // Only bother if we haven't already spotted a mismatch above
         if (newId == null)
         {
            String requestedJvmRoute = null;
            if (requestedId != null)
            {
               int reqIndex = requestedId.indexOf('.', 0);
               if (reqIndex > -1 && reqIndex < requestedId.length() - 1)
               {
                  requestedJvmRoute = requestedId.substring(reqIndex + 1, requestedId.length());
               }
            }
            
            if (!jvmRoute.equals(requestedJvmRoute))
            {
               if (log_.isTraceEnabled())
               {
                  log_.trace("handleJvmRoute(): We have detected a failover with different jvmRoute." +
                     " received one: " + requestedJvmRoute + " new one: " + jvmRoute + ". Will reset the session id.");
               }
               String base = index > -1 ? sessionId.substring(0, index) : sessionId;
               newId = new StringBuilder(base).append('.').append(this.manager_.locate(base)).toString();
            }
         }

         /* Change the sessionid cookie if needed */
         if (newId != null)
            manager_.setNewSessionCookie(newId, response);
      }
   }
   
   /** 
    * Update the id of the given session
    * 
    *  @param oldId id of the session to change
    *  @param newId new session id the session object should have
    */
   private void resetSessionId(String oldId, String newId)
         throws IOException
   {
      ClusteredSession<? extends OutgoingDistributableSessionData> session = (ClusteredSession<?>) manager_.findSession(oldId);
      // change session id with the new one using local jvmRoute.
      if( session != null )
      {
         // Note this will trigger a session remove from the super Tomcat class.
         session.resetIdWithRouteInfo(newId);
         if (log_.isTraceEnabled())
         {
            log_.trace("resetSessionId(): changed catalina session to= [" + newId + "] old one= [" + oldId + "]");
         }
      }
      else if (log_.isTraceEnabled())
      {
         log_.trace("resetSessionId(): no session with id " + newId + " found");
      }
   }

   // Lifecycle Interface
   public void addLifecycleListener(LifecycleListener listener)
   {
      support.addLifecycleListener(listener);
   }

   public void removeLifecycleListener(LifecycleListener listener)
   {
      support.removeLifecycleListener(listener);
   }

   public LifecycleListener[] findLifecycleListeners()
   {
      return support.findLifecycleListeners();
   }

   public void start() throws LifecycleException
   {
      support.fireLifecycleEvent(START_EVENT, this);
   }

   public void stop() throws LifecycleException
   {
      support.fireLifecycleEvent(STOP_EVENT, this);
   }

}
