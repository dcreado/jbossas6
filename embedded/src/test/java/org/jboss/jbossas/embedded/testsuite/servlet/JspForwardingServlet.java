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
package org.jboss.jbossas.embedded.testsuite.servlet;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JspForwardingServlet
 * 
 * Servlet which forwards to a JSP as requested
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class JspForwardingServlet extends HttpServlet
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(JspForwardingServlet.class.getName());

   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 1L;

   /**
    * Name of the request parameter denoting which JSP to forward
    */
   public static final String REQ_PARAM_JSP = "jsp";

   /**
    * Context root
    */
   private static final char ROOT = '/';

   /**
    * Content type to use in forwarding
    */
   private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

   //-------------------------------------------------------------------------------------||
   // Overridden Implementations ---------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Forwards the request to a JSP denoted by the request parameter "jsp", 
    * returning a status of 400/Bad Request if not specified 
    * 
    * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
         IOException
   {
      // Log
      log.info("Request: " + request);

      // Get the target JSP page
      final String jsp = request.getParameter(REQ_PARAM_JSP);

      // Handle unspecified
      if (jsp == null)
      {
         // HTTP 400 and return
         response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
         return;
      }

      // Set the content-type to text
      response.setContentType(CONTENT_TYPE_TEXT_PLAIN);

      // Forward
      final String resolvedLocation = ROOT + jsp;
      log.info("Forwarding to: " + resolvedLocation);
      final RequestDispatcher dispatcher = request.getRequestDispatcher(resolvedLocation);
      dispatcher.forward(request, response);
   }

}
