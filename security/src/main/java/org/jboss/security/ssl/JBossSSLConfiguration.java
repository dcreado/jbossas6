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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import org.jboss.beans.metadata.api.annotations.FactoryMethod;
import org.jboss.logging.Logger;
import org.jboss.security.Util;
import org.jboss.security.plugins.SecurityKeyManager;

/**
 * A MC bean that installs a <code>Provider</code> so we can override the default
 * implementations for <code>KeyManagerFactory</code> and <code>TrustManagerFactory</code>.
 * 
 * @author <a href="mmoyses@redhat.com">Marcus Moyses</a>
 * @version $Revision: 1 $
 */
public class JBossSSLConfiguration
{
   private Provider provider;

   private String keyStoreType;

   private URL keyStoreURL;

   private char[] keyStorePass;

   private String keyStoreAlias;

   private String keyStoreProvider;

   private String keyStoreProviderArgument;

   private KeyStore keyStore;

   private String trustStoreType;

   private URL trustStoreURL;

   private char[] trustStorePass;

   private String trustStoreProvider;

   private String trustStoreProviderArgument;

   private KeyStore trustStore;
   
   private static JBossSSLConfiguration singleton;
   
   private static Logger log = Logger.getLogger(JBossSSLConfiguration.class);
   
   private JBossSSLConfiguration()
   {
   }
   
   @FactoryMethod
   public static JBossSSLConfiguration getInstance()
   {
      if (singleton == null)
         singleton = new JBossSSLConfiguration();
      return singleton;
   }

   public String getKeyStoreType()
   {
      return keyStoreType;
   }

   public void setKeyStoreType(String keyStoreType)
   {
      this.keyStoreType = keyStoreType;
   }

   public String getKeyStoreURL()
   {
      String url = null;
      if (keyStoreURL != null)
         url = keyStoreURL.toExternalForm();
      return url;
   }

   public void setKeyStoreURL(String keyStoreURL) throws IOException
   {
      this.keyStoreURL = validateStoreURL(keyStoreURL);
   }

   public void setKeyStorePassword(String keyStorePassword) throws Exception
   {
      keyStorePass = Util.loadPassword(keyStorePassword);
   }

   public String getKeyStoreAlias()
   {
      return keyStoreAlias;
   }

   public void setKeyStoreAlias(String alias)
   {
      keyStoreAlias = alias;
   }

   public String getKeyStoreProvider()
   {
      return keyStoreProvider;
   }

   public void setKeyStoreProvider(String keyStoreProvider)
   {
      this.keyStoreProvider = keyStoreProvider;
   }

   public String getKeyStoreProviderArgument()
   {
      return keyStoreProviderArgument;
   }

   public void setKeyStoreProviderArgument(String keyStoreProviderArgument)
   {
      this.keyStoreProviderArgument = keyStoreProviderArgument;
   }

   public String getTrustStoreType()
   {
      return trustStoreType;
   }

   public void setTrustStoreType(String trustStoreType)
   {
      this.trustStoreType = trustStoreType;
   }

   public String getTrustStoreURL()
   {
      String url = null;
      if (trustStoreURL != null)
         url = trustStoreURL.toExternalForm();
      return url;
   }

   public void setTrustStoreURL(String trustStoreURL) throws IOException
   {
      this.trustStoreURL = validateStoreURL(trustStoreURL);
   }

   public void setTrustStorePassword(String trustStorePassword) throws Exception
   {
      trustStorePass = Util.loadPassword(trustStorePassword);
   }

   public String getTrustStoreProvider()
   {
      return trustStoreProvider;
   }

   public void setTrustStoreProvider(String trustStoreProvider)
   {
      this.trustStoreProvider = trustStoreProvider;
   }

   public String getTrustStoreProviderArgument()
   {
      return trustStoreProviderArgument;
   }

   public void setTrustStoreProviderArgument(String trustStoreProviderArgument)
   {
      this.trustStoreProviderArgument = trustStoreProviderArgument;
   }

   /**
    * Callback method that initializes the keystore and truststore
    * and adds <code>JBossProvider</code> as the first in the list.
    * 
    * @throws Exception if an error happens during initialization
    */
   public void start() throws Exception
   {
      // add provider as the first
      provider = new JBossProvider();
      addProvider(provider);
      
      // initialize keystore and truststore
      if (keyStorePass != null)
      {
         if (keyStoreType == null)
            keyStoreType = KeyStore.getDefaultType();
         if (keyStoreProvider != null)
         {
            if (keyStoreProviderArgument != null)
            {
               ClassLoader loader = getContextClassLoader();
               Class<?> clazz = loader.loadClass(keyStoreProvider);
               Class<?>[] ctorSig = {String.class};
               Constructor<?> ctor = clazz.getConstructor(ctorSig);
               Object[] ctorArgs = {keyStoreProviderArgument};
               Provider provider = (Provider) ctor.newInstance(ctorArgs);
               keyStore = KeyStore.getInstance(keyStoreType, provider);
            }
            else
               keyStore = KeyStore.getInstance(keyStoreType, keyStoreProvider);
         }
         else
            keyStore = KeyStore.getInstance(keyStoreType);
         InputStream is = null;
         if ((!"PKCS11".equalsIgnoreCase(keyStoreType) || !"PKCS11IMPLKS".equalsIgnoreCase(keyStoreType)) && keyStoreURL != null)
         {
            is = keyStoreURL.openStream();
         }
         keyStore.load(is, keyStorePass);
         if (keyStoreAlias != null && !keyStore.isKeyEntry(keyStoreAlias))
         {
            throw new IOException("Cannot find key entry with alias " + keyStoreAlias + " in the keyStore");
         }
      }
      if (trustStorePass != null)
      {
         if (trustStoreType == null)
            trustStoreType = KeyStore.getDefaultType();
         if (trustStoreProvider != null)
         {
            if (trustStoreProviderArgument != null)
            {
               ClassLoader loader = getContextClassLoader();
               Class<?> clazz = loader.loadClass(trustStoreProvider);
               Class<?>[] ctorSig = {String.class};
               Constructor<?> ctor = clazz.getConstructor(ctorSig);
               Object[] ctorArgs = {trustStoreProviderArgument};
               Provider provider = (Provider) ctor.newInstance(ctorArgs);
               trustStore = KeyStore.getInstance(trustStoreType, provider);
            }
            else
               trustStore = KeyStore.getInstance(trustStoreType, trustStoreProvider);
         }
         else
            trustStore = KeyStore.getInstance(trustStoreType);
         InputStream is = null;
         if ((!"PKCS11".equalsIgnoreCase(trustStoreType) || !"PKCS11IMPLKS".equalsIgnoreCase(trustStoreType)) && trustStoreURL != null)
         {
            is = trustStoreURL.openStream();
         }
         trustStore.load(is, trustStorePass);
      }
   }

   /**
    * Removes <code>JBossProvider</code> from the <code>Provider</code>s list.
    */
   public void stop()
   {
      if (provider != null)
         removeProvider(provider);
   }
   
   /**
    * Overrides the keystore using the configuration set in the bean.
    * 
    * @param delegate <code>KeyManagerFactory</code>implementation
    * @throws KeyStoreException
    * @throws UnrecoverableKeyException
    * @throws NoSuchAlgorithmException
    */
   public void initializeKeyManagerFactory(KeyManagerFactory delegate) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException
   {
      if (keyStore == null)
         throw new KeyStoreException("Global keystore is not correctly initialized");
      if (log.isTraceEnabled())
         log.trace("Using global keystore configuration");
      delegate.init(keyStore, keyStorePass);
   }
   
   /**
    * Overrides the <code>KeyManager>s if an alias in provided in the configuration.
    * 
    * @param delegate <code>KeyManagerFactory</code> implementation
    * @return
    */
   public KeyManager[] getKeyManagers(KeyManagerFactory delegate)
   {
      KeyManager[] keyManagers = delegate.getKeyManagers();
      if (keyStoreAlias != null)
      {
         for (int i = 0; i < keyManagers.length; i++)
         {
            keyManagers[i] = new SecurityKeyManager((X509KeyManager) keyManagers[i], keyStoreAlias, null);
         }
      }
      return keyManagers;
   }
   
   /**
    * Overrides the truststore using the configuration set in the bean.
    * 
    * @param delegate <code>TrustManagerFactory</code> implementation
    * @param ks truststore
    * @throws KeyStoreException
    */
   public void initializeTrustManagerFactory(TrustManagerFactory delegate, KeyStore ks) throws KeyStoreException
   {
      if (trustStore == null)
      {
         if (log.isTraceEnabled())
            log.trace("Global truststore is not correctly initialized. Using local truststore configuration");
         delegate.init(ks);
      }
      else
      {
         if (log.isTraceEnabled())
            log.trace("Using global truststore configuration");
         delegate.init(trustStore);
      }
   }
   
   /**
    * Overrides the truststore using the configuration set in the bean.
    * 
    * @param delegate <code>TrustManagerFactory</code> implementation
    * @param spec <code>ManagerFactoryParameters</code> parameters
    * @throws InvalidAlgorithmParameterException
    */
   public void initializeTrustManagerFactory(TrustManagerFactory delegate, ManagerFactoryParameters spec) throws InvalidAlgorithmParameterException
   {
      if (trustStore == null)
      {
         if (log.isTraceEnabled())
            log.trace("Global truststore is not correctly initialized. Using local truststore configuration");
         delegate.init(spec);
      }
      else
      {
         CertPathTrustManagerParameters parameters = (CertPathTrustManagerParameters) spec;
         PKIXBuilderParameters oldParams = (PKIXBuilderParameters) parameters.getParameters();

         PKIXBuilderParameters xparams = null;
         try
         {
            xparams = new PKIXBuilderParameters(trustStore, new X509CertSelector());
            xparams.setAnyPolicyInhibited(oldParams.isAnyPolicyInhibited());
            xparams.setCertPathCheckers(oldParams.getCertPathCheckers());
            xparams.setCertStores(oldParams.getCertStores());
            xparams.setDate(oldParams.getDate());
            xparams.setExplicitPolicyRequired(oldParams.isExplicitPolicyRequired());
            xparams.setInitialPolicies(oldParams.getInitialPolicies());
            xparams.setMaxPathLength(oldParams.getMaxPathLength());
            xparams.setPolicyMappingInhibited(oldParams.isPolicyMappingInhibited());
            xparams.setPolicyQualifiersRejected(oldParams.getPolicyQualifiersRejected());
            xparams.setRevocationEnabled(oldParams.isRevocationEnabled());
            xparams.setSigProvider(oldParams.getSigProvider());
         }
         catch (KeyStoreException ke)
         {
            log.error("Error initializing TrustManagerFactory", ke);
         }
         if (log.isTraceEnabled())
            log.trace("Using global truststore configuration");
         ManagerFactoryParameters mfp = new CertPathTrustManagerParameters(xparams);
         delegate.init(mfp);
      }
   }

   /**
    * Loads a key/trust store
    */
   private URL validateStoreURL(String storeURL) throws IOException
   {
      URL url = null;
      // First see if this is a URL
      try
      {
         url = new URL(storeURL);
      }
      catch (MalformedURLException e)
      {
         // Not a URL or a protocol without a handler
      }

      // Next try to locate this as file path
      if (url == null)
      {
         File tst = new File(storeURL);
         if (tst.exists() == true)
            url = tst.toURI().toURL();
      }

      // Last try to locate this as a classpath resource
      if (url == null)
      {
         ClassLoader loader = getContextClassLoader();
         url = loader.getResource(storeURL);
      }

      // Fail if no valid key store was located
      if (url == null)
      {
         String msg = "Failed to find url=" + storeURL + " as a URL, file or resource";
         throw new MalformedURLException(msg);
      }
      return url;
   }

   private static ClassLoader getContextClassLoader()
   {
      return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
      {
         public ClassLoader run()
         {
            return Thread.currentThread().getContextClassLoader();
         }
      });
   }

   private static Object addProvider(final Provider provider)
   {
      return AccessController.doPrivileged(new PrivilegedAction<Object>()
      {
         public Object run()
         {
            return Security.insertProviderAt(provider, 1);
         }
      });
   }

   private static Object removeProvider(final Provider provider)
   {
      return AccessController.doPrivileged(new PrivilegedAction<Object>()
      {
         public Object run()
         {
            Security.removeProvider(provider.getName());
            return null;
         }
      });
   }
}
