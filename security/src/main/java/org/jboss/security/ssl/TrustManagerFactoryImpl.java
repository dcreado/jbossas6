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

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;

import org.jboss.logging.Logger;

/**
 * <code>TrustManagerFactory</code> implementation that uses the truststore configuration
 * provided by the JBossSSLConfiguration.
 * 
 * @author <a href="mmoyses@redhat.com">Marcus Moyses</a>
 * @version $Revision: 1 $
 */
abstract class TrustManagerFactoryImpl extends TrustManagerFactorySpi
{
   protected String defaultAlgorithm;

   protected TrustManagerFactory delegate;

   protected Logger log = Logger.getLogger(TrustManagerFactoryImpl.class);
   
   protected JBossSSLConfiguration sslConfiguration = JBossSSLConfiguration.getInstance();

   public TrustManagerFactoryImpl()
   {
      defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
   }

   /**
    * Delegates to the default <code>TrustManagerFactory</code>
    */
   protected TrustManager[] engineGetTrustManagers()
   {
      return delegate.getTrustManagers();
   }

   /**
    * Overrides the truststore configuration
    */
   protected void engineInit(KeyStore ks) throws KeyStoreException
   {
      sslConfiguration.initializeTrustManagerFactory(delegate, ks);
   }

   /**
    * Overrides the truststore configuration
    */
   protected void engineInit(ManagerFactoryParameters spec) throws InvalidAlgorithmParameterException
   {
      sslConfiguration.initializeTrustManagerFactory(delegate, spec);
   }

   /**
    * Implementation for Sun, JRockit and OpenJDK
    */
   public static class SunPKIX extends TrustManagerFactoryImpl
   {
      public SunPKIX()
      {
         try
         {
            delegate = TrustManagerFactory.getInstance(defaultAlgorithm, "SunJSSE");
         }
         catch (Exception e)
         {
            log.error("Could not initialize TrustManagerFactory", e);
            throw new IllegalStateException(e);
         }
      }
   }

   /**
    * Implementation for IBM
    */
   public static class IbmPKIX extends TrustManagerFactoryImpl
   {
      public IbmPKIX()
      {
         try
         {
            delegate = TrustManagerFactory.getInstance(defaultAlgorithm, "IBMJSSE2");
         }
         catch (Exception e)
         {
            log.error("Could not initialize TrustManagerFactory", e);
            throw new IllegalStateException(e);
         }
      }
   }
}
