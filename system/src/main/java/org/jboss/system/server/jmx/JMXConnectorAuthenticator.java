/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.system.server.jmx;

import org.jboss.security.AuthenticationManager;
import org.jboss.security.SecurityContext;
import org.picketbox.factories.SecurityFactory;

import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXPrincipal;
import javax.security.auth.Subject;
import java.security.Principal;

/**
 * Handles authentication checking for JSR-160 JMXConnector (using PicketBox)
 * login-config.xml is expected to contain an application-policy for the specified
 * securityDomainName.
 *  
 * @author Scott Marlow smarlow@redhat.com
 *
 */

class JMXConnectorAuthenticator implements JMXAuthenticator {

   String securityDomainName;

   JMXConnectorAuthenticator(String securityDomainName)
   {
      this.securityDomainName = securityDomainName;
   }

   /**
    * @inheritDoc
    * 
    */
   public synchronized Subject authenticate(Object creds) {
      AuthenticationManager am;
      SecurityContext securityContext;

      // wine and complain if we don't get what we expect.
      if (creds == null) {
         throw new SecurityException(
            "JMXConnectorAuthenticator requires userid/password credentials to be passed in");
      }
      if (! (creds instanceof String[])) {
         // only support passing in array of Strings
         throw new SecurityException(
            "JMXConnectorAuthenticator can only handle authentication parameter that is array of two strings, instead got " +
            creds.getClass().getName());
      }
      String[] pair = (String[]) creds;
      if( pair.length != 2 ) {
         // only support passing userid + password
         throw new SecurityException(
            "JMXConnectorAuthenticator can only handle authentication parameter that is array of two strings, instead got " +
         pair.length +" strings");
      }

      String user, pass;
      user = pair[0];
      pass = pair[1];
      Principal principal = new JMXPrincipal(user);
      Subject subject = new Subject();

      securityContext = SecurityFactory.establishSecurityContext(securityDomainName);
      am = securityContext.getAuthenticationManager();

      boolean result = am.isValid(principal, pass , subject);
      if( result ) {
         subject.setReadOnly();
      }
      else {
         throw new SecurityException("user authentication check failed");
      }
      return subject;
   }
}