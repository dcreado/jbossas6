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
package org.jboss.security.ssl;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.naming.InitialContext;
import javax.net.ServerSocketFactory;

import org.jboss.logging.Logger;
import org.jboss.security.SecurityDomain;

/**
 * A <code>ServerSocketFactory</code> that uses a <code>SecurityDomain</code>
 * to create <code>SSLServerSocket</code>s.
 * The security domain name is set as system property returned by the
 * getSystemPropertyName() method.
 * 
 * @author <a href="mmoyses@redhat.com">Marcus Moyses</a>
 * @version $Revision: 1 $
 */
public class JaasSecurityDomainServerSocketFactory extends DomainServerSocketFactory implements JaasSecurityDomainServerSocketFactoryMBean
{
   private static Logger log = Logger.getLogger(JaasSecurityDomainServerSocketFactory.class);
   
   private String securityDomainName;

   /**
    * Default constructor.
    */
   public JaasSecurityDomainServerSocketFactory()
   {
      super();
      if (log.isTraceEnabled())
         log.trace("Creating socket factory: " + this.getClass().getName());
      SecurityDomain sd = getJaasSecurityDomain();
      setSecurityDomain(sd);
   }

   /**
    * Static method required.
    * 
    * @return an instance of <code>JaasSecurityDomainServerSocketFactory</code>
    *  or <code>null</code> if the security domain is null.
    */
   public static ServerSocketFactory getDefault()
   {
      JaasSecurityDomainServerSocketFactory jsdssf = new JaasSecurityDomainServerSocketFactory();
      return jsdssf;
   }

   /**
    * Constructs a <code>SecurityDomain</code> based on the
    * system property defined in getSystemPropertyName().
    * 
    * @return an instance of <code>SecurityDomain</code>
    *  or <code>null</code> if an error occurred.
    */
   protected SecurityDomain getJaasSecurityDomain()
   {
      final String name = getSystemPropertyName();
      String secDomain = null;
      if (securityDomainName != null)
         secDomain = securityDomainName;
      else
      {
         secDomain = (String) AccessController.doPrivileged(new PrivilegedAction()
         {
            public Object run()
            {
               return System.getProperty(name);
            }
         });
      }
      if (secDomain != null)
      {
         if (!secDomain.startsWith("java:/jaas/") || !secDomain.startsWith("java:jaas/"))
            secDomain = "java:/jaas/" + secDomain;
         try
         {
            InitialContext iniCtx = new InitialContext();
            SecurityDomain sd = (SecurityDomain) iniCtx.lookup(secDomain);
            if (log.isDebugEnabled())
               log.debug("Created Security Domain object from " + secDomain + ":" + sd.toString());
            return sd;
         }
         catch (Exception e)
         {
            log.error("Failed to create Security Domain '" + secDomain + "'", e);
         }
      }
      return null;
   }

   /**
    * Name of the system property with the security domain name.
    * By default "org.jboss.security.ssl.server.domain.name".
    * Override this method if you want different <code>SocketFactory</code>s
    * each using a different security domain. Need to overwrite the
    * static method getDefault() as well.
    * 
    * @return a <code>String</code> if the property name
    */
   protected String getSystemPropertyName()
   {
      return "org.jboss.security.ssl.server.domain.name";
   }

   public void create() throws Exception
   {
      //NOOP
   }

   public void destroy() throws Exception
   {
      //NOOP
   }

   public void start() throws Exception
   {
      SecurityDomain sd = getJaasSecurityDomain();
      setSecurityDomain(sd);
   }

   public void stop() throws Exception
   {
      //NOOP
   }
   
   public String getSecurityDomainName()
   {
      return securityDomainName;
   }
   
   public void setSecurityDomainName(String securityDomainName)
   {
      this.securityDomainName = securityDomainName;
   }

}
