/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.test.classloader.leak.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Registers the relevant classloaders to monitor leaks.  Uses at least a session context
 * bean to force use of the session context and management infrastructure in Weld.
 * 
 * @author David Allen
 *
 */
public class CdiIntrospectSession extends HttpServlet
{
   private static final long serialVersionUID = 1L;

   @Inject
   private SimpleSessionBean simpleSessionBean;

   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
   {
      org.jboss.test.classloader.leak.clstore.ClassLoaderStore.getInstance().storeClassLoader("SERVLET", getClass().getClassLoader());
      org.jboss.test.classloader.leak.clstore.ClassLoaderStore.getInstance().storeClassLoader("SERVLET_TCCL", Thread.currentThread().getContextClassLoader());
      org.jboss.test.classloader.leak.clstore.ClassLoaderStore.getInstance().storeClassLoader("WELD", simpleSessionBean.getClass().getClassLoader());
      
      Log log = LogFactory.getLog("WEBAPP");
      log.info("Logging from " + getClass().getName());
      
      simpleSessionBean.hashCode();
      resp.setContentType("text/text");
      PrintWriter writer = resp.getWriter();
      writer.println("WEBAPP");
      writer.flush();
   }

}
