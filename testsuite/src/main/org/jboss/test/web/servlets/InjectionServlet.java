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
package org.jboss.test.web.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.jboss.test.web.ejb3.SimpleStateful;
import org.jboss.test.web.ejb3.SimpleStateless;
import org.jboss.test.web.util.Util;

/** A servlet that accesses an EJB using injection.

 @author Remy Maucherat
 @version $Revision: 81036 $
 */
@WebServlet("/injection")
public class InjectionServlet extends HttpServlet
{
	
	@EJB(mappedName = "simpleStatefulMappedName")
	private SimpleStateful simpleStateful;
	   
	@EJB
	private SimpleStateless simpleStateless;
	
   protected void processRequest(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException
   {
	   assert simpleStateful != null : "simpleStateful is null";
	   assert simpleStateless != null : "simpleStateless is null";

	   assert simpleStateful.doSomething() == true : "simpleStateful returned false";
	   assert simpleStateless.doSomething() == true : "simpleStateless returned false";
	   
	   response.getWriter().print("Test passed. Stateless EJB is " + simpleStateless);
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
