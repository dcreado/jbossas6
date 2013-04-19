/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.jpa.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.jboss.logging.Logger;
import org.jboss.test.jpa.support.ITestEntity;
import org.jboss.test.jpa.support.TestEntity;
import org.jboss.test.jpa.support.TestEntityCopy;

/**
 * MultipleServlet.
 *
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 */
public class MultipleServlet extends HttpServlet
{
   /**
    * The serialVersionUID
    */
   private static final long serialVersionUID = -5539196377086639503L;

   private Logger log = Logger.getLogger(MultipleServlet.class);

   @Resource
   private UserTransaction ut;

   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
   {
      try
      {
         StringBuilder out = new StringBuilder();

         handleJPA(req, "java:comp/env/persistence/em0", "Wo", out, TestEntity.class);
         handleJPA(req, "java:comp/env/persistence/em1", "rld", out, TestEntityCopy.class);

         if (out.length() > 0)
         {
            resp.setContentType("text/plain");
            PrintWriter writer = resp.getWriter();
            writer.print(out.toString());
            writer.close();
         }
      }
      catch (Exception e)
      {
         throw new ServletException("Error", e);
      }
   }

   protected void handleJPA(HttpServletRequest req, String name, String msg, StringBuilder out, Class<? extends ITestEntity> clazz) throws Exception
   {
      ut.begin();
      try
      {
         InitialContext ctx = new InitialContext();

         EntityManager em = (EntityManager) ctx.lookup(name);
         handleRequest(req, em, msg, out, clazz);
      }
      catch (Exception e)
      {
         log.error("Error in servlet", e);
         ut.setRollbackOnly();
         throw e;
      }
      finally
      {
         ut.commit();
      }
   }

   protected void handleRequest(HttpServletRequest req, EntityManager em, String text, StringBuilder out, Class<? extends ITestEntity> clazz) throws Exception
   {
      String mode = req.getParameter("mode");
      if ("Write".equals(mode))
      {
         Constructor ctor = clazz.getConstructor(String.class, String.class);
         ITestEntity test = (ITestEntity) ctor.newInstance("Hello", text);
         em.persist(test);
      }
      else
      {
         ITestEntity test = em.find(clazz, "Hello");
         out.append(test.getDescription());
      }
   }
}
