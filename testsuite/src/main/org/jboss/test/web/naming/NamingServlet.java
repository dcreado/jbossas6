/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.web.naming;

import org.jboss.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
@WebServlet("/servlet")
public class NamingServlet extends HttpServlet
{
   private static final Logger log = Logger.getLogger(NamingServlet.class);
   
   private InitialContext ctx;
   
   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
   {
      String name = req.getParameter("name");
      if(name == null)
      {
         resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
         print(resp, "name is unspecified");
         return;
      }

      try
      {
         String value = String.valueOf(ctx.lookup(name));
         print(resp, value);
      }
      catch(NamingException e)
      {
         log.warn("Failed to lookup " + name, e);
         resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
         print(resp, e.getMessage());
      }
   }

   @Override
   public void init() throws ServletException
   {
      super.init();

      try
      {
         this.ctx = new InitialContext();
      }
      catch(NamingException e)
      {
         throw new ServletException(e);
      }
   }

   protected static void print(HttpServletResponse resp, String s) throws IOException
   {
      resp.setContentType("text/plain");
      PrintWriter out = resp.getWriter();
      out.print(s);
   }
}
