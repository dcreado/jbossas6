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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.ejb3.embedded.api.shrinkwrap.ShrinkWrapEJBContainer;
import org.jboss.ejb3.embedded.impl.shrinkwrap.ShrinkWrapEJBContainerImpl;
import org.jboss.ejb3.embedded.spi.JBossEJBContainerProvider;
import org.jboss.jbossas.embedded.testsuite.ejb3.slsb.OutputBean;
import org.jboss.jbossas.embedded.testsuite.ejb3.slsb.OutputLocalBusiness;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Servlet which forwards calls upon an EJB
 * via the {@link EJBContainer} API.
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class EmbeddedEjbCallingServlet extends HttpServlet
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(EmbeddedEjbCallingServlet.class.getName());

   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 1L;

   /**
    * Content type to use in forwarding
    */
   private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

   //-------------------------------------------------------------------------------------||
   // Overridden Implementations ---------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Returns the value {@link OutputLocalBusiness#OUTPUT} by invoking via an EJB
    * 
    * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
         IOException
   {
      // Log
      log.info("Request: " + request);

      // Set the content-type to text
      response.setContentType(CONTENT_TYPE_TEXT_PLAIN);

      // Create the EJB Container
      final Map<String, String> ejbContainerProps = new HashMap<String, String>();
      ejbContainerProps.put(EJBContainer.MODULES, ""); // Deploy no modules and do no scanning by default
      final JBossEJBContainerProvider ejbContainer = (JBossEJBContainerProvider) EJBContainer
            .createEJBContainer(ejbContainerProps);
      final ShrinkWrapEJBContainer shrinkwrapEjbContainer = new ShrinkWrapEJBContainerImpl(ejbContainer);

      // Define the EJB JAR
      final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "outputSlsb.jar").addClasses(OutputBean.class,
            OutputLocalBusiness.class);

      // Deploy the JAR
      shrinkwrapEjbContainer.deploy(archive);

      // Look up the EJB
      final Context context = ejbContainer.getContext();
      final OutputLocalBusiness bean;
      try
      {
         bean = (OutputLocalBusiness) context.lookup(OutputLocalBusiness.JNDI_NAME);
      }
      catch (final NamingException e)
      {
         throw new RuntimeException("Could not find bean proxy at " + OutputLocalBusiness.JNDI_NAME, e);
      }

      // Invoke
      final String value = bean.getOutput();

      // Undeploy
      shrinkwrapEjbContainer.undeploy(archive);

      // Shut down EJBContainer
      ejbContainer.close();

      // Write out
      log.info("Got value from EJB: " + value);
      response.getWriter().write(value);
   }
}
