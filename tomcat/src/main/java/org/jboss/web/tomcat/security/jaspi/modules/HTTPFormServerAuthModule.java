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
package org.jboss.web.tomcat.security.jaspi.modules;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.authenticator.SavedRequest;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.util.StringManager;
import org.apache.coyote.ActionCode;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;
import org.jboss.logging.Logger;

/**
 * Server auth module for FORM authentication
 * @author Anil.Saldhana@redhat.com
 * @since Oct 7, 2008
 */
public class HTTPFormServerAuthModule extends TomcatServerAuthModule
{
   private static Logger log = Logger.getLogger(HTTPFormServerAuthModule.class);

   protected Context context; 
   
   protected boolean cache = false;
   
   protected static final StringManager sm =
      StringManager.getManager(Constants.Package);
   
   /**
    * The number of random bytes to include when generating a
    * session identifier.
    */
   protected static final int SESSION_ID_BYTES = 16;

   protected String delgatingLoginContextName = null;
   
   public HTTPFormServerAuthModule()
   { 
   }
 
   public HTTPFormServerAuthModule(String delgatingLoginContextName)
   {
      super();
      this.delgatingLoginContextName = delgatingLoginContextName;
   }

   public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject)
   throws AuthException
   {
      throw new RuntimeException("Not Applicable");
   }

   public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, 
         Subject serviceSubject) throws AuthException
   { 
      Request request = (Request) messageInfo.getRequestMessage();
      Response response = (Response) messageInfo.getResponseMessage();
     
      Principal principal;
      context = request.getContext();
      LoginConfig config = context.getLoginConfig();
       
      // References to objects we will need later
      Session session = null;

      //Lets find out if the cache is enabled or not 
      cache = (Boolean) messageInfo.getMap().get("CACHE"); 
      
      // Have we authenticated this user before but have caching disabled?
      if (!cache) {
          session = request.getSessionInternal(true);
          log.debug("Checking for reauthenticate in session " + session);
          String username =
              (String) session.getNote(Constants.SESS_USERNAME_NOTE);
          String password =
              (String) session.getNote(Constants.SESS_PASSWORD_NOTE);
          if ((username != null) && (password != null)) {
              log.debug("Reauthenticating username '" + username + "'");
              principal =
                  context.getRealm().authenticate(username, password);
              if (principal != null) {
                  session.setNote(Constants.FORM_PRINCIPAL_NOTE, principal);
                  if (!matchRequest(request)) {
                     registerWithCallbackHandler(principal, username, password);
                     
                      /*register(request, response, principal,
                               Constants.FORM_METHOD,
                               username, password);*/
                      return AuthStatus.SUCCESS;
                  }
              }
              log.trace("Reauthentication failed, proceed normally");
          }
      }

      // Is this the re-submit of the original request URI after successful
      // authentication?  If so, forward the *original* request instead.
      if (matchRequest(request)) {
          session = request.getSessionInternal(true);
          log.trace("Restore request from session '"
                        + session.getIdInternal() 
                        + "'");
          principal = (Principal)
              session.getNote(Constants.FORM_PRINCIPAL_NOTE);
          
          registerWithCallbackHandler(principal, 
                (String) session.getNote(Constants.SESS_USERNAME_NOTE), 
                (String) session.getNote(Constants.SESS_PASSWORD_NOTE));
          
          /*register(request, response, principal, Constants.FORM_METHOD,
                   (String) session.getNote(Constants.SESS_USERNAME_NOTE),
                   (String) session.getNote(Constants.SESS_PASSWORD_NOTE));*/
          // If we're caching principals we no longer need the username
          // and password in the session, so remove them
          if (cache) {
              session.removeNote(Constants.SESS_USERNAME_NOTE);
              session.removeNote(Constants.SESS_PASSWORD_NOTE);
          }
          if (restoreRequest(request, session)) {
              log.trace("Proceed to restored request");
              return (AuthStatus.SUCCESS);
          } else {
              log.trace("Restore of original request failed");
            
            try
            {
               response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
            catch (IOException e)
            {
               log.error(e.getLocalizedMessage(),e);
            }
              return AuthStatus.FAILURE;
          }
      }

      // Acquire references to objects we will need to evaluate
      MessageBytes uriMB = MessageBytes.newInstance();
      CharChunk uriCC = uriMB.getCharChunk();
      uriCC.setLimit(-1);
      String contextPath = request.getContextPath();
      String requestURI = request.getDecodedRequestURI();

      // Is this the action request from the login page?
      boolean loginAction =
          requestURI.startsWith(contextPath) &&
          requestURI.endsWith(Constants.FORM_ACTION);

      // No -- Save this request and redirect to the form login page
      if (!loginAction) {
          session = request.getSessionInternal(true);
          log.trace("Save request in session '" + session.getIdInternal() + "'");
          try {
              saveRequest(request, session);
          } catch (IOException ioe) {
              log.trace("Request body too big to save during authentication");
              try
            {
               response.sendError(HttpServletResponse.SC_FORBIDDEN,
                         sm.getString("authenticator.requestBodyTooBig"));
            }
            catch (IOException e)
            {
               log.error("Exception in Form authentication:",e);
               throw new AuthException(e.getLocalizedMessage());
            }
              return (AuthStatus.FAILURE);
          }
          forwardToLoginPage(request, response, config);
          return (AuthStatus.SEND_CONTINUE);
      }

      // Yes -- Validate the specified credentials and redirect
      // to the error page if they are not correct
      Realm realm = context.getRealm();
      String characterEncoding = request.getCharacterEncoding();
      if (characterEncoding != null) {
          try
         {
            request.setCharacterEncoding(characterEncoding);
         }
         catch (UnsupportedEncodingException e)
         {
            log.error(e.getLocalizedMessage(), e);
         }
      }
      String username = request.getParameter(Constants.FORM_USERNAME);
      String password = request.getParameter(Constants.FORM_PASSWORD);
      log.trace("Authenticating username '" + username + "'");
      principal = realm.authenticate(username, password);
      if (principal == null) {
          forwardToErrorPage(request, response, config);
          return (AuthStatus.FAILURE);
      }

      log.trace("Authentication of '" + username + "' was successful");

      if (session == null)
          session = request.getSessionInternal(false);
      if (session == null) {
          log.trace
                  ("User took so long to log on the session expired");
          try
         {
            response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT,
                                sm.getString("authenticator.sessionExpired"));
         }
         catch (IOException e)
         {
            log.error(e.getLocalizedMessage(),e);
         }
          return (AuthStatus.FAILURE);
      }

      // Save the authenticated Principal in our session
      session.setNote(Constants.FORM_PRINCIPAL_NOTE, principal);

      // Save the username and password as well
      session.setNote(Constants.SESS_USERNAME_NOTE, username);
      session.setNote(Constants.SESS_PASSWORD_NOTE, password);

      // Redirect the user to the original request URI (which will cause
      // the original request to be restored)
      requestURI = savedRequestURL(session);
      log.trace("Redirecting to original '" + requestURI + "'");
      try
      {
         if (requestURI == null)
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               sm.getString("authenticator.formlogin"));
        else
            response.sendRedirect(response.encodeRedirectURL(requestURI));  
      }
      catch(IOException ioe)
      {
         log.error(ioe.getLocalizedMessage(),ioe);
      }
      return (AuthStatus.FAILURE); 
   }
   
   /**
    * Does this request match the saved one (so that it must be the redirect
    * we signalled after successful authentication?
    *
    * @param request The request to be verified
    */
   protected boolean matchRequest(Request request) 
   { 
     // Has a session been created?
     Session session = request.getSessionInternal(false);
     if (session == null)
         return (false);

     // Is there a saved request?
     SavedRequest sreq = (SavedRequest)
         session.getNote(Constants.FORM_REQUEST_NOTE);
     if (sreq == null)
         return (false);

     // Is there a saved principal?
     if (session.getNote(Constants.FORM_PRINCIPAL_NOTE) == null)
         return (false);

     // Does the request URI match?
     String requestURI = request.getRequestURI();
     if (requestURI == null)
         return (false);
     return (requestURI.equals(sreq.getRequestURI()));

   }


   /**
    * Restore the original request from information stored in our session.
    * If the original request is no longer present (because the session
    * timed out), return <code>false</code>; otherwise, return
    * <code>true</code>.
    *
    * @param request The request to be restored
    * @param session The session containing the saved information
    */
   @SuppressWarnings("unchecked")
   protected boolean restoreRequest(Request request, Session session) 
   { 
       // Retrieve and remove the SavedRequest object from our session
       SavedRequest saved = (SavedRequest)
           session.getNote(Constants.FORM_REQUEST_NOTE);
       session.removeNote(Constants.FORM_REQUEST_NOTE);
       session.removeNote(Constants.FORM_PRINCIPAL_NOTE);
       if (saved == null)
           return (false);

       // Modify our current request to reflect the original one
       request.clearCookies();
       Iterator cookies = saved.getCookies();
       while (cookies.hasNext()) {
           request.addCookie((Cookie) cookies.next());
       }

       MimeHeaders rmh = request.getCoyoteRequest().getMimeHeaders();
       rmh.recycle();
       boolean cachable = "GET".equalsIgnoreCase(saved.getMethod()) ||
                          "HEAD".equalsIgnoreCase(saved.getMethod());
       Iterator names = saved.getHeaderNames();
       while (names.hasNext()) {
           String name = (String) names.next();
           // The browser isn't expecting this conditional response now.
           // Assuming that it can quietly recover from an unexpected 412.
           // BZ 43687
           if(!("If-Modified-Since".equalsIgnoreCase(name) ||
                (cachable && "If-None-Match".equalsIgnoreCase(name)))) {
               Iterator values = saved.getHeaderValues(name);
               while (values.hasNext()) {
                   rmh.addValue(name).setString( (String)values.next() );
               }
           }
       }
       
       request.clearLocales();
       Iterator locales = saved.getLocales();
       while (locales.hasNext()) {
           request.addLocale((Locale) locales.next());
       }
       
       request.getCoyoteRequest().getParameters().recycle();
       
       if ("POST".equalsIgnoreCase(saved.getMethod())) {
           ByteChunk body = saved.getBody();
           
           if (body != null) {
               request.getCoyoteRequest().action
                   (ActionCode.ACTION_REQ_SET_BODY_REPLAY, body);
   
               // Set content type
               MessageBytes contentType = MessageBytes.newInstance();
               
               // If no content type specified, use default for POST
               String savedContentType = saved.getContentType();
               if (savedContentType == null) {
                   savedContentType = "application/x-www-form-urlencoded";
               }

               contentType.setString(savedContentType);
               request.getCoyoteRequest().setContentType(contentType);
           }
       }
       request.getCoyoteRequest().method().setString(saved.getMethod());

       request.getCoyoteRequest().queryString().setString
           (saved.getQueryString());

       request.getCoyoteRequest().requestURI().setString
           (saved.getRequestURI());
       return (true); 
   }


   /**
    * Save the original request information into our session.
    *
    * @param request The request to be saved
    * @param session The session to contain the saved information
    * @throws IOException
    */
   @SuppressWarnings("unchecked")
   protected void saveRequest(Request request, Session session)
       throws IOException {

       // Create and populate a SavedRequest object for this request
       SavedRequest saved = new SavedRequest();
       Cookie cookies[] = request.getCookies();
       if (cookies != null) {
           for (int i = 0; i < cookies.length; i++)
               saved.addCookie(cookies[i]);
       }
       Enumeration names = request.getHeaderNames();
       while (names.hasMoreElements()) {
           String name = (String) names.nextElement();
           Enumeration values = request.getHeaders(name);
           while (values.hasMoreElements()) {
               String value = (String) values.nextElement();
               saved.addHeader(name, value);
           }
       }
       Enumeration locales = request.getLocales();
       while (locales.hasMoreElements()) {
           Locale locale = (Locale) locales.nextElement();
           saved.addLocale(locale);
       }

       if ("POST".equalsIgnoreCase(request.getMethod())) {
           ByteChunk body = new ByteChunk();
           body.setLimit(request.getConnector().getMaxSavePostSize());

           byte[] buffer = new byte[4096];
           int bytesRead;
           InputStream is = request.getInputStream();
       
           while ( (bytesRead = is.read(buffer) ) >= 0) {
               body.append(buffer, 0, bytesRead);
           }
           saved.setBody(body);
           saved.setContentType(request.getContentType());
       }

       saved.setMethod(request.getMethod());
       saved.setQueryString(request.getQueryString());
       saved.setRequestURI(request.getRequestURI());

       // Stash the SavedRequest in our session for later use
       session.setNote(Constants.FORM_REQUEST_NOTE, saved);
   }

   /**
    * Return the request URI (with the corresponding query string, if any)
    * from the saved request so that we can redirect to it.
    *
    * @param session Our current session
    */
   protected String savedRequestURL(Session session) 
   {

       SavedRequest saved =
           (SavedRequest) session.getNote(Constants.FORM_REQUEST_NOTE);
       if (saved == null)
           return (null);
       StringBuffer sb = new StringBuffer(saved.getRequestURI());
       if (saved.getQueryString() != null) {
           sb.append('?');
           sb.append(saved.getQueryString());
       }
       return (sb.toString());

   }
   
   //Forward Methods
   /**
    * Called to forward to the login page
    * 
    * @param request Request we are processing
    * @param response Response we are creating
    * @param config    Login configuration describing how authentication
    *              should be performed
    */
   protected void forwardToLoginPage(Request request, Response response, LoginConfig config) 
   {
       RequestDispatcher disp =
           context.getServletContext().getRequestDispatcher
           (config.getLoginPage());
       try {
           disp.forward(request.getRequest(), response.getResponse());
           response.finishResponse();
       } catch (Throwable t) {
           log.warn("Unexpected error forwarding to login page", t);
       }
   }


   /**
    * Called to forward to the error page
    * 
    * @param request Request we are processing
    * @param response Response we are creating
    * @param config    Login configuration describing how authentication
    *              should be performed
    */
   protected void forwardToErrorPage(Request request, Response response, LoginConfig config) 
   {
       RequestDispatcher disp =
           context.getServletContext().getRequestDispatcher
           (config.getErrorPage());
       try {
           disp.forward(request.getRequest(), response.getResponse());
       } catch (Throwable t) {
           log.warn("Unexpected error forwarding to error page", t);
       }
   }
}