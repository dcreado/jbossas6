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
package org.jboss.security.ssl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;

/**
 * A <code>Provider</code> that overrides the default <code>KeyManagerFactory</code> and
 * <code>TrustManagerFactory</code> implementations.
 * 
 * @author <a href="mmoyses@redhat.com">Marcus Moyses</a>
 * @version $Revision: 1 $
 */
public class JBossProvider extends Provider
{
   private static final long serialVersionUID = -6211291745955454828L;

   private static String INFO = "JBoss (X509 key/trust manager factories)";
   
   private static String NAME = "JBoss";

   public JBossProvider()
   {
      super(NAME, 1.0, INFO);

      // override only the default algorithms
      AccessController.doPrivileged(new PrivilegedAction<Object>()
      {
         public Object run()
         {
            // Sun, OpenJDK and JRockit KeyManagerFactory
            put("KeyManagerFactory.SunX509", "org.jboss.security.ssl.KeyManagerFactoryImpl$SunX509");
            
            // IBM KeyManagerFactory
            put("KeyManagerFactory.IbmX509", "org.jboss.security.ssl.KeyManagerFactoryImpl$IbmX509");
            
            // put the correct TrustManagerFactory
            Provider[] providers = Security.getProviders("TrustManagerFactory.PKIX");
            for (int i = 0; i < providers.length; i++)
            {
               String name = providers[i].getName(); 
               if (name.equals("SunJSSE"))
               {
                  put("TrustManagerFactory.PKIX", "org.jboss.security.ssl.TrustManagerFactoryImpl$SunPKIX");
                  break;
               }
               else if (name.equals("IBMJSSE2"))
               {
                  put("TrustManagerFactory.PKIX", "org.jboss.security.ssl.TrustManagerFactoryImpl$IbmPKIX");
                  break;
               }
            }
            return null;
         }
      });
   }
}
