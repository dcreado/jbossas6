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
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;

import org.jboss.logging.Logger;

/**
 * <code>KeyManagerFactory</code> implementation that uses the keystore configuration
 * provided by JBossSSLConfiguration
 * 
 * @author <a href="mmoyses@redhat.com">Marcus Moyses</a>
 * @version $Revision: 1 $
 */
abstract class KeyManagerFactoryImpl extends KeyManagerFactorySpi
{
   protected KeyManagerFactory delegate;

   protected String defaultAlgorithm;

   protected static Logger log = Logger.getLogger(KeyManagerFactoryImpl.class);
   
   protected JBossSSLConfiguration sslConfiguration = JBossSSLConfiguration.getInstance(); 

   public KeyManagerFactoryImpl()
   {
      defaultAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
   }

   /**
    * Overrides the keystore configuration
    */
   protected KeyManager[] engineGetKeyManagers()
   {
      return sslConfiguration.getKeyManagers(delegate);
   }

   /**
    * Overrides the keystore configuration
    */
   protected void engineInit(KeyStore ks, char[] password) throws KeyStoreException, NoSuchAlgorithmException,
         UnrecoverableKeyException
   {
      sslConfiguration.initializeKeyManagerFactory(delegate);
   }

   /**
    * Delegates to the default <code>KeyManagerFactory</code>
    */
   protected void engineInit(ManagerFactoryParameters spec) throws InvalidAlgorithmParameterException
   {
      // Not used by the underlying implementations. Throws an exception
      delegate.init(spec);
   }

   /**
    * Implementation for Sun, JRockit and OpenJDK
    */
   public static class SunX509 extends KeyManagerFactoryImpl
   {
      public SunX509()
      {
         try
         {
            delegate = KeyManagerFactory.getInstance(defaultAlgorithm, "SunJSSE");
         }
         catch (Exception e)
         {
            log.error("Could not initialize KeyManagerFactory", e);
            throw new IllegalStateException(e);
         }
      }
   }

   /**
    * Implementation for IBM
    */
   public static class IbmX509 extends KeyManagerFactoryImpl
   {
      public IbmX509()
      {
         try
         {
            delegate = KeyManagerFactory.getInstance(defaultAlgorithm, "IBMJSSE2");
         }
         catch (Exception e)
         {
            log.error("Could not initialize KeyManagerFactory", e);
            throw new IllegalStateException(e);
         }
      }
   }

}
