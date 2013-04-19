/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.cluster.ejb3.clusteredservice.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.test.cluster.ejb3.clusteredservice.ServiceRemote;
import org.jboss.logging.Logger;

/**
 * @version <tt>$Revision: 108925 $</tt>
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public class EJBServlet extends HttpServlet
{
   private static final Logger log = Logger.getLogger(EJBServlet.class);
   
   protected void processRequest(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException
   {
      try
      {
         log.info("EJBServlet.processRequest invoked");
         ServiceRemote test = (ServiceRemote) getInitialContext().lookup("ServiceBean/remote");
         log.info("looked up remote bean ServiceBean/remote, about to invoke remoteMethod() on bean");
         test.remoteMethod();
         log.info("invoked remoteMethod() on bean");
  /*       MBeanServer server = org.jboss.mx.util.MBeanServerLocator.locateJBoss();
         ObjectName testerName = new ObjectName("default:service=service");
         Object[] params = {};
         String[] sig = {};
         server.invoke(testerName, "", params, sig);*/
      }
      catch (Exception e)
      {
         log("error trying to invoke ServiceBean/remote remoteMethod ", e);
         throw new ServletException("Failed to call ServiceBean through remote or local interfaces", e);
      }
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      out.println("<html>");
      out.println("<head><title>EJBServlet</title></head>");
      out.println("<body>Tests passed<br></body>");
      out.println("</html>");
      out.close();
   }
   
   protected InitialContext getInitialContext() throws Exception
   {
      Properties p = new Properties();
      p.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
      p.put(Context.URL_PKG_PREFIXES, "jboss.naming:org.jnp.interfaces");
      String local = System.getProperty("jboss.bind.address");
      p.put(Context.PROVIDER_URL, local + ":1100");
      return new InitialContext(p);
   }

   protected void doGet(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException
   {
      processRequest(request, response);
   }

   protected void doPost(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException
   {
      processRequest(request, response);
   }
}
