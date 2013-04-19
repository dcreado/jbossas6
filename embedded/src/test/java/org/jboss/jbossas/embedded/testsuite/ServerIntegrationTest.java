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
package org.jboss.jbossas.embedded.testsuite;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.bootstrap.api.lifecycle.LifecycleState;
import org.jboss.bootstrap.api.mc.server.MCBasedServer;
import org.jboss.ejb3.embedded.api.JBossEJBContainer;
import org.jboss.ejb3.proxy.impl.handler.session.SessionProxyInvocationHandlerBase;
import org.jboss.embedded.api.server.JBossASEmbeddedServer;
import org.jboss.embedded.api.server.JBossASEmbeddedServerFactory;
import org.jboss.jbossas.embedded.testsuite.ejb3.async.AsyncBean;
import org.jboss.jbossas.embedded.testsuite.ejb3.async.AsyncLocalBusiness;
import org.jboss.jbossas.embedded.testsuite.ejb3.entity.Jbossian;
import org.jboss.jbossas.embedded.testsuite.ejb3.entity.JbossianRegistrarBean;
import org.jboss.jbossas.embedded.testsuite.ejb3.entity.JbossianRegistrarLocalBusiness;
import org.jboss.jbossas.embedded.testsuite.ejb3.mdb.MessageStoringMdb;
import org.jboss.jbossas.embedded.testsuite.ejb3.slsb.OutputBean;
import org.jboss.jbossas.embedded.testsuite.ejb3.slsb.OutputLocalBusiness;
import org.jboss.jbossas.embedded.testsuite.mc.StateReportingBean;
import org.jboss.jbossas.embedded.testsuite.servlet.EmbeddedEjbCallingServlet;
import org.jboss.jbossas.embedded.testsuite.servlet.JspForwardingServlet;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * End-user view of the Embedded Server.  Tests here should include lifecycle
 * to start/stop the server, and checks to ensure that
 * subsystems are working as expected.
 * 
 * This is a high-level integration test for the Application
 * Server running in Embedded mode
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class ServerIntegrationTest
{

   //-------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------||
   //-------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(ServerIntegrationTest.class.getName());

   /**
    * The server instance
    */
   private static JBossASEmbeddedServer server;

   /**
    * Path, relative to the resources base, of the directory containing web.xml descriptor for tests
    */
   private static final String PATH_RESOURCE_WEB_XML = "webxml/";

   /**
    * Path, relative to the resources base, of a test web.xml for a Servlet which forwards control to a JSP
    */
   private static final String PATH_ACTUAL_SERVLET_FORWARDING_TO_JSP_WEB_XML = PATH_RESOURCE_WEB_XML
         + "servletForwardingToJsp.xml";

   /**
    * Path, relative to the resources base, of a test web.xml for a Servlet which uses the {@link JBossEJBContainer} API
    */
   private static final String PATH_ACTUAL_SERVLET_EMBEDDED_EJB_WEB_XML = PATH_RESOURCE_WEB_XML
         + "ejbCallingServlet.xml";

   /**
    * Filename of a test queue *-service.xml
    */
   private static final String FILENAME_QUEUE_SERVICE_XML = "hornetq-jms.xml";

   /**
    * Path, relative to the resources base, of a test queue *-service.xml
    */
   private static final String PATH_QUEUE_SERVICE_XML = "queues/" + FILENAME_QUEUE_SERVICE_XML;

   /**
    * Path, relative to the resources base, of a test JSP
    */
   private static final String PATH_JSP = "jsp/requestParamEcho.jsp";

   /**
    * Path, relative to the resources base, of a persistence.xml file for Embedded DataSource
    */
   private static final String PATH_RESOURCE_PERSISTENCE_XML_EMBEDDED = "persistence/persistenceEmbedded.xml";

   /**
    * Path, relative to the deployment root, of a persistence.xml file
    */
   private static final String PATH_DESTINATION_PERSISTENCE_XML = "persistence.xml";

   /**
    * Path, relative to the resources base, of the directory containing DataSource resources
    */
   private static final String PATH_RESOURCE_DS_XMLs = "datasources/";

   /**
    * Filename of the Embedded DataSource deployment XML
    */
   private static final String FILENAME_EMBEDDED_DS = "embedded-ds.xml";

   /**
    * Path, relative to the resources base, of the directory containing DataSource resources
    */
   private static final String PATH_RESOURCE_DS_XML_EMBEDDED = PATH_RESOURCE_DS_XMLs + FILENAME_EMBEDDED_DS;

   /**
    * Separator character within archives
    */
   private static final char SEPARATOR = '/';

   /**
    * The JNDI Context
    */
   private static Context NAMING_CONTEXT;

   /**
    * Name of the Queue Connection Factory in JNDI
    */
   private static final String JNDI_NAME_CONNECTION_FACTORY = "ConnectionFactory";

   /**
    * JNDI name suffix appended to local business EJB3 views
    */
   private static final String JNDI_SUFFIX_LOCAL_BUSINESS = "/local";

   /**
    * Name of the server configuration to use
    */
   private static final String NAME_SERVER_CONFIG = "all";

   /**
    * Path, relative to test resources of web.xml used to deploy a werservice war
    */
   private static final String PATH_ACTUAL_WEB_XML_WS = PATH_RESOURCE_WEB_XML + "webservice-web.xml";

   private static final String WS_REQUEST_PARAMETER = "jboss embedded user";

   private static final String WS_RESPONSE = "hello " + WS_REQUEST_PARAMETER;

   /**
    * Relative path to the MC Descriptor for the State Reporting Bean 
    */
   private static final String PATH_STATEREPORTING_BEAN = "mc/statereporter-jboss-beans.xml";

   /**
    * MC Name (Context) of the State Reporting Bean
    */
   private static final String MC_NAME_STATEREPORTING_BEAN = "org.jboss.jbossas.embedded.test.StateReporter";

   //-------------------------------------------------------------------------------||
   // Lifecycle --------------------------------------------------------------------||
   //-------------------------------------------------------------------------------||

   /**
    * Starts up the Application Server.  Relies upon either Environment
    * Variable "JBOSS_HOME" or System Property "jboss.home" being set, with 
    * precedence to the system property
    */
   @BeforeClass
   public static void startEmbedddedASAndSetNamingContext() throws Exception
   {
      // Make Server (will pull JBOSS_HOME from env var or sys prop)
      server = JBossASEmbeddedServerFactory.createServer();
      log.info("Created: " + server);

      // Start
      log.info("Starting Server: " + server);
      server.getConfiguration().serverName(NAME_SERVER_CONFIG);
      server.start();
      log.info("...started.");

      // Set Naming Context
      NAMING_CONTEXT = new InitialContext();
   }

   /**
    * Shuts down the Application Server after the suite ends
    */
   @AfterClass
   public static void stopEmbeddedAS() throws Exception
   {
      // If exists and started
      if (server != null && server.getState().equals(LifecycleState.STARTED))
      {
         // Shutdown
         log.info("Shutting down server: " + server);
         server.shutdown();
      }

   }

   //-------------------------------------------------------------------------------||
   // Tests ------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------||

   /**
    * Tests EJB3 Stateless Session Beans via a virtual archive
    * deployment
    */
   @Test
   public void testSlsb() throws Exception
   {
      // Log
      log.info("testSlsb");

      // Make a deployment
      final String name = "slsb.jar";
      final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, name).addClasses(OutputBean.class,
            OutputLocalBusiness.class);
      log.info(archive.toString(true));
      // Deploy
      server.deploy(archive);

      // Test
      try
      {
         final OutputLocalBusiness bean = (OutputLocalBusiness) NAMING_CONTEXT.lookup(OutputLocalBusiness.JNDI_NAME);
         final String output = bean.getOutput();
         log.info("Got output: " + output);

         Assert.assertEquals(OutputLocalBusiness.OUTPUT, output);
      }
      finally
      {
         // Undeploy
         server.undeploy(archive);
      }

   }

   /**
    * Tests EJB3 3.1 @Asynchronous support
    */
   @Test
   @Ignore // temporarily disabled till Async interceptor is enabled back in EJB3 release
   public void testEjb31Async() throws Exception
   {
      // Log
      log.info("testEjb31Async");

      // Make a deployment
      final String name = "ejb31async.jar";
      final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, name).addPackage(
            AsyncLocalBusiness.class.getPackage());
      log.info(archive.toString(true));
      // Deploy
      server.deploy(archive);

      // Test
      try
      {
         final AsyncLocalBusiness bean = (AsyncLocalBusiness) NAMING_CONTEXT.lookup(AsyncBean.class.getSimpleName()
               + JNDI_SUFFIX_LOCAL_BUSINESS);
         log.info(bean.toString());
         log.info(bean.getClass().toString());
         final SessionProxyInvocationHandlerBase handler = (SessionProxyInvocationHandlerBase) Proxy
               .getInvocationHandler(bean);
         log.info("INTERCEPTORS: " + Arrays.asList(handler.getInterceptors()).toString());

         final Future<Thread> invocation = bean.getThreadOfExecution();

         // Block and test
         final Thread beanThread = invocation.get(3, TimeUnit.SECONDS);
         final Thread ourThread = Thread.currentThread();
         log.info("Got: " + invocation);
         log.info("Invocation Thread: " + beanThread);
         log.info("Out Thread: " + Thread.currentThread());
         Assert.assertFalse("Bean invocation should not take place in the caller's Thread",
               beanThread.equals(ourThread));
         Assert.assertTrue("First invocation did not report as completed", invocation.isDone());
         Assert.assertFalse("Invocation should not report as cancelled", invocation.isCancelled());
      }
      catch (final TimeoutException te)
      {
         Assert.fail("Timed out waiting for invocation:" + te);
      }
      finally
      {
         // Undeploy
         server.undeploy(archive);
      }

   }

   /**
    * Tests deployment of a virtual WAR containing a servlet 
    * and JSP.
    * 
    * The Servlet will forward to the JSP path denoted by request param
    * "jsp".  The JSP will echo the value of request param "echo".
    * 
    * @throws Exception
    */
   @Test
   public void testWarServletJsp() throws Exception
   {
      // Log
      log.info("testWarServletJsp");

      // Make a deployment
      final String appName = "testServletJsp";
      final String name = appName + ".war";
      final Class<?> servletClass = JspForwardingServlet.class;
      final WebArchive archive = ShrinkWrap.create(WebArchive.class, name);
      final ArchivePath targetPathWebXml = ArchivePaths.create("web.xml");
      archive.addWebResource(PATH_ACTUAL_SERVLET_FORWARDING_TO_JSP_WEB_XML, targetPathWebXml).addResource(PATH_JSP)
            .addClass(servletClass);
      log.info(archive.toString(true));

      // Deploy
      server.deploy(archive);

      try
      {
         // Get an HTTP Client
         final HttpClient client = new DefaultHttpClient();

         // Make an HTTP Request
         final String echoValue = "EmbeddedBiatch";
         final List<NameValuePair> params = new ArrayList<NameValuePair>();
         params.add(new BasicNameValuePair("jsp", PATH_JSP));
         params.add(new BasicNameValuePair("echo", echoValue));
         final URI uri = URIUtils.createURI("http", "localhost", 8080,
               appName + SEPARATOR + servletClass.getSimpleName(), URLEncodedUtils.format(params, "UTF-8"), null);
         final HttpGet request = new HttpGet(uri);

         // Execute the request
         log.info("Executing request to: " + request.getURI());
         final HttpResponse response = client.execute(request);
         final HttpEntity entity = response.getEntity();
         if (entity == null)
         {
            TestCase.fail("Request returned no entity");
         }

         // Read the result, ensure it's what we're expecting (should be the value of request param "echo")
         final BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
         final String line = reader.readLine();
         log.info("Got response: " + line);
         Assert.assertEquals(echoValue, line);
      }
      finally
      {
         // Undeploy
         server.undeploy(archive);
      }
   }

   /**
    * Tests deployment of a virtual WAR which contains a servlet 
    * that will use the {@link JBossEJBContainer} API to deploy
    * and invoke upon an EJB.
    * 
    * @throws Exception
    */
   @Test
   @Ignore // we're running embedded, calling a servlet, which starts embedded, which starts standalone, which starts embedded... This requires CL magic
   public void testServletUsingEmbeddedEJB() throws Exception
   {
      // Log
      log.info("testServletUsingEmbeddedEJB");

      // Make a deployment
      final String appName = "embeddedEjbCallingServlet";
      final String name = appName + ".war";
      final Class<?> servletClass = EmbeddedEjbCallingServlet.class;
      final WebArchive archive = ShrinkWrap.create(WebArchive.class, name);
      final ArchivePath targetPathWebXml = ArchivePaths.create("web.xml");
      archive.addWebResource(PATH_ACTUAL_SERVLET_EMBEDDED_EJB_WEB_XML, targetPathWebXml).addClass(servletClass);
      log.info(archive.toString(true));

      // Deploy
      server.deploy(archive);

      try
      {
         // Get an HTTP Client
         final HttpClient client = new DefaultHttpClient();

         // Make an HTTP Request
         final URI uri = URIUtils.createURI("http", "localhost", 8080,
               appName + SEPARATOR + servletClass.getSimpleName(), null, null);
         final HttpGet request = new HttpGet(uri);

         // Execute the request
         log.info("Executing request to: " + request.getURI());
         final HttpResponse response = client.execute(request);
         final HttpEntity entity = response.getEntity();
         if (entity == null)
         {
            TestCase.fail("Request returned no entity");
         }

         // Read the result, ensure it's what we're expecting
         final BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
         final String line = reader.readLine();
         log.info("Got response: " + line);
         Assert.assertEquals(OutputLocalBusiness.OUTPUT, line);
      }
      finally
      {
         // Undeploy
         server.undeploy(archive);
      }
   }

   /**
    * Tests deployment of a JMS Queue with EJB3 MDB Listener
    * 
    * The MDB will simply store the text of the message in a publicly-accessible
    * static field
    * 
    * @throws Exception
    */
   @Test
   public void testJmsAndMdb() throws Exception
   {
      // Log
      log.info("testJmsAndMdb");

      // Create a virtual archive for the MDB deployment
      final String name = "jms-mdb-test.jar";
      final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, name);
      final ArchivePath queuesTargetPath = ArchivePaths.create(FILENAME_QUEUE_SERVICE_XML);
      archive.addClass(MessageStoringMdb.class).addResource(PATH_QUEUE_SERVICE_XML, queuesTargetPath);

      // Deploy
      log.info(archive.toString(true));
      server.deploy(archive);

      try
      {
         // Define a String message to send
         final String message = "From in-JVM Test Message";

         // Send the message
         this.sendTextMessageToQueue(message, MessageStoringMdb.NAME_QUEUE);

         // Wait on the MDB to process
         try
         {
            MessageStoringMdb.BARRIER.await(10, TimeUnit.SECONDS);
         }
         catch (final InterruptedException e)
         {
            // Clear the flag
            Thread.interrupted();
            // Throw up
            throw e;
         }
         catch (final TimeoutException e)
         {
            TestCase.fail("The MDB did not process the message in the allotted time");
         }

         // Get the contents of the String from the MDB
         final String received = MessageStoringMdb.LAST_MESSAGE_CONTENTS;

         // Ensure equal
         Assert.assertEquals("The test message received was not as expected", message, received);
      }
      finally
      {
         // Undeploy
         server.undeploy(archive);
      }

   }

   /**
    * Tests deployment of a JCA DataSource and EJB3 Entity Bean (JPA)
    * 
    * A test SLSB will also be used to create a few rows in the DB, 
    * then obtain all.
    * 
    * @throws Exception
    */
   @Test
   public void testDataSourceAndEntity() throws Exception
   {
      // Log
      log.info("testDataSourceAndEntity");

      // Create a virtual archive for DS, persistence.xml, Entity, and SLSB
      final String name = "datasource-entity-test.jar";
      final ArchivePath targetDsPath = ArchivePaths.create(FILENAME_EMBEDDED_DS);
      final ArchivePath targetPersistencePath = ArchivePaths.create(PATH_DESTINATION_PERSISTENCE_XML);
      final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, name);
      archive.addClasses(Jbossian.class, JbossianRegistrarLocalBusiness.class, JbossianRegistrarBean.class)
            .addResource(PATH_RESOURCE_DS_XML_EMBEDDED, targetDsPath)
            .addManifestResource(PATH_RESOURCE_PERSISTENCE_XML_EMBEDDED, targetPersistencePath);

      // Deploy
      log.info(archive.toString(true));
      server.deploy(archive);

      try
      {
         // Make some JBossians
         final Jbossian jgreene = new Jbossian("Jason T. Greene", "AS Hole", 12);
         final Jbossian jpederse = new Jbossian("Jesper Pedersen", "Professional Tattletale", 21);
         final Jbossian dmlloyd = new Jbossian("David M. Lloyd", "???????", 15);
         final Jbossian wolfc = new Jbossian("Carlo de Wolf", "Superlead", 13);
         final Jbossian alr = new Jbossian("Andew Lee Rubinger", "The New Fluery", 58);
         final Jbossian asaldhan = new Jbossian("Anil Saldhana", "Karma Police", 23);

         // Get an SLSB to interact w/ the DB
         final JbossianRegistrarLocalBusiness slsb = (JbossianRegistrarLocalBusiness) NAMING_CONTEXT
               .lookup(JbossianRegistrarBean.class.getSimpleName() + JNDI_SUFFIX_LOCAL_BUSINESS);

         // Add the JBossians
         slsb.add(jgreene);
         slsb.add(jpederse);
         slsb.add(dmlloyd);
         slsb.add(wolfc);
         slsb.add(alr);
         slsb.add(asaldhan);

         // Get all
         final Collection<Jbossian> jbossians = slsb.getAllJbossians();
         log.info("Got all JBossians: " + jbossians);

         // Test
         Assert.assertEquals(6, jbossians.size());
      }
      finally
      {
         // Undeploy
         server.undeploy(archive);
      }
   }

   /**
    * Ensures WS is working as expected
    * 
    * EMB-61
    *  
    * @throws Exception
    */
   @Test
   public void testWs() throws Exception
   {

      // Make a deployment
      final String appName = "webservice";
      final String name = appName + ".war";
      final WebArchive archive = ShrinkWrap.create(WebArchive.class, name);
      final String targetPathWebXml = "web.xml";
      archive.addWebResource(PATH_ACTUAL_WEB_XML_WS, targetPathWebXml).addClass(
            org.jboss.jbossas.embedded.testsuite.ws.EmbeddedWs.class);
      // Deploy
      log.info(archive.toString(true));
      server.deploy(archive);
      try
      {
         // consume the webservice
         org.jboss.jbossas.embedded.testsuite.wsclient.Embedded embeddedService = new org.jboss.jbossas.embedded.testsuite.wsclient.Embedded();
         log.info("Create Web Service client...");
         org.jboss.jbossas.embedded.testsuite.wsclient.EmbeddedWs port = embeddedService.getEmbeddedWsPort();
         log.info("Call Web Service Operation...");
         String wsResponse = port.hello("jboss embedded user");
         log.info("Web service response: " + wsResponse);
         Assert.assertEquals(WS_RESPONSE, wsResponse);
      }
      finally
      {
         // Undeploy
         server.undeploy(archive);
      }
   }

   /**
    * Ensures that a URL may be deployed and undeployed
    * 
    * @throws Exception
    */
   @Test
   public void testUrlDeployment() throws Exception
   {
      // Get a URL
      final URL url = this.getMcBeanDescriptor();

      // Deploy it
      server.deploy(url);

      StateReportingBean bean = null;
      try
      {
         // Lookup the bean
         final String context = MC_NAME_STATEREPORTING_BEAN;
         bean = (StateReportingBean) ((MCBasedServer<?, ?>) server).getKernel().getController()
               .getInstalledContext(context).getTarget();
         TestCase.assertNotNull("Bean was not found installed in expected context: " + context, bean);

         // Ensure started
         TestCase.assertEquals("Bean should be started after installation", StateReportingBean.State.STARTED,
               bean.getState());
      }
      finally
      {
         // Undeploy
         server.undeploy(url);

         // Ensure stopped
         TestCase.assertEquals("Bean should be stopped after undeploy", StateReportingBean.State.STOPPED,
               bean.getState());
      }
   }

   /**
    * Ensures that a File may be deployed and undeployed
    * 
    * @throws Exception
    */
   @Test
   public void testFileDeployment() throws Exception
   {
      // Get a File
      final File file = new File(this.getMcBeanDescriptor().toURI());

      // Deploy it
      server.deploy(file);

      StateReportingBean bean = null;
      try
      {
         // Lookup the bean
         final String context = MC_NAME_STATEREPORTING_BEAN;
         bean = (StateReportingBean) ((MCBasedServer<?, ?>) server).getKernel().getController()
               .getInstalledContext(context).getTarget();
         TestCase.assertNotNull("Bean was not found installed in expected context: " + context, bean);

         // Ensure started
         TestCase.assertEquals("Bean should be started after installation", StateReportingBean.State.STARTED,
               bean.getState());
      }
      finally
      {
         // Undeploy
         server.undeploy(file);

         // Ensure stopped
         TestCase.assertEquals("Bean should be stopped after undeploy", StateReportingBean.State.STOPPED,
               bean.getState());
      }
   }

   //-------------------------------------------------------------------------------------||
   // Internal Helper Methods ------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Obtains the URL of the MC Bean Descriptor for
    * the State Reporting Bean
    */
   private URL getMcBeanDescriptor()
   {
      return this.getClass().getClassLoader().getResource(PATH_STATEREPORTING_BEAN);
   }

   /**
    * Sends a JMS {@link TextMessage} containing the specified contents to the 
    * queue of the specified name  
    * 
    * @param contents
    * @param queueName
    * @throws Exception
    * @throws IllegalArgumentException If either argument is not provided
    */
   private void sendTextMessageToQueue(final String contents, final String queueName) throws Exception,
         IllegalArgumentException
   {
      // Precondition check
      if (contents == null || contents.length() == 0)
      {
         throw new IllegalArgumentException("contents must be provided");
      }
      if (queueName == null || queueName.length() == 0)
      {
         throw new IllegalArgumentException("queueName must be provided");
      }

      // Get the queue from JNDI
      final Queue queue = (Queue) NAMING_CONTEXT.lookup(queueName);

      // Get the ConnectionFactory from JNDI
      final QueueConnectionFactory factory = (QueueConnectionFactory) NAMING_CONTEXT
            .lookup(JNDI_NAME_CONNECTION_FACTORY);

      // Make a Connection
      final QueueConnection connection = factory.createQueueConnection();
      final QueueSession sendSession = connection.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);

      // Make the message
      final TextMessage message = sendSession.createTextMessage(contents);

      // Send the message
      final QueueSender sender = sendSession.createSender(queue);
      sender.send(message);
      log.info("Sent message " + message + " with contents: " + contents);

      // Clean up
      sendSession.close();
      connection.close();
   }
}
