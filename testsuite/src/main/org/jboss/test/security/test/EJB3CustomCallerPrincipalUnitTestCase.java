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
package org.jboss.test.security.test;

import java.security.Principal;

import javax.ejb.EJBAccessException;
import javax.rmi.PortableRemoteObject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.test.JBossTestCase;
import org.jboss.test.JBossTestSetup;
import org.jboss.test.security.ejb3.EJB3CustomPrincipalImpl;
import org.jboss.test.security.ejb3.SimpleSession;
import org.jboss.test.util.AppCallbackHandler;

/**
 * <p>
 * This {@code TestCase} validates the usage of custom principal class with EJB3.
 * </p>
 * 
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class EJB3CustomCallerPrincipalUnitTestCase extends JBossTestCase
{

   private LoginContext loginContext;

   /**
    * <p>
    * Creates an instance of {@code EJB3CustomCallerPrincipalUnitTestCase} with the specified name.
    * </p>
    * 
    * @param name a {@code String} that represents the name of the test case.
    */
   public EJB3CustomCallerPrincipalUnitTestCase(String name)
   {
      super(name);
      // set the login config file if it hasn't been set yet.
      if (System.getProperty("java.security.auth.login.config") == null)
         System.setProperty("java.security.auth.login.config", "output/resources/security/auth.conf");
   }

   /**
    * <p>
    * Tests accessing protected methods using a client that has the {@code RegularUser} role.
    * </p>
    * 
    * @throws Exception if an error occurs while running the test.
    */
   public void testRegularUserMethodAccess() throws Exception
   {
      // login with a user that has the RegularUser role.
      this.login("UserA", "PassA".toCharArray());

      // get a reference to the remote protected stateless session bean.
      Object obj = getInitialContext().lookup("SimpleStatelessSessionBean/remote");
      SimpleSession session = (SimpleSession) PortableRemoteObject.narrow(obj, SimpleSession.class);

      Principal principal = null;
      try
      {
         principal = session.invokeRegularMethod();
         if (!(principal instanceof EJB3CustomPrincipalImpl))
            fail("Custom principal is not the caller principal");
      }
      catch (EJBAccessException eae)
      {
         fail("UserA should be able to invoke the regular method");
      }

      this.logout();
   }

   /**
    * <p>
    * Authenticates the client identified by the given {@code username} using the specified {@code password}.
    * </p>
    * 
    * @param username a {@code String} that identifies the client that is being logged in.
    * @param password a {@code char[]} that contains the password that asserts the client's identity.
    * @throws LoginException if an error occurs while authenticating the client.
    */
   private void login(String username, char[] password) throws LoginException
   {
      // get the conf name from a system property - default is spec-test.
      String confName = System.getProperty("conf.name", "spec-test");
      AppCallbackHandler handler = new AppCallbackHandler(username, password);
      this.loginContext = new LoginContext(confName, handler);
      this.loginContext.login();
   }

   /**
    * <p>
    * Perform a logout of the current user.
    * </p>
    * 
    * @throws LoginException if an error occurs while logging the user out.
    */
   private void logout() throws LoginException
   {
      this.loginContext.logout();
   }

   /**
    * <p>
    * Sets up the test suite.
    * </p>
    * 
    * @return a {@code TestSuite} that contains this test case.
    * @throws Exception if an error occurs while setting up the {@code TestSuite}.
    */
   public static Test suite() throws Exception
   {
      TestSuite suite = new TestSuite();
      suite.addTest(new TestSuite(EJB3CustomCallerPrincipalUnitTestCase.class));

      TestSetup wrapper = new JBossTestSetup(suite)
      {
         /*
          * (non-Javadoc)
          * 
          * @see org.jboss.test.JBossTestSetup#setUp()
          */
         @Override
         protected void setUp() throws Exception
         {
            super.setUp();
            // deploy the ejb3 test application.
            super.deploy("security-ejb3-caller-principal.jar");
         }

         /*
          * (non-Javadoc)
          * 
          * @see org.jboss.test.JBossTestSetup#tearDown()
          */
         @Override
         protected void tearDown() throws Exception
         {
            // undeploy the ejb3 test application.
            super.undeploy("security-ejb3-caller-principal.jar");
            // flush the authentication cache of the test domain.
            super.flushAuthCache("security-ejb3-test");
            super.tearDown();
         }
      };
      return wrapper;
   }
}
