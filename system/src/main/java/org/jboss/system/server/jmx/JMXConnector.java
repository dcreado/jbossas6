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

import org.jboss.logging.Logger;
import org.jboss.net.sockets.DefaultClientSocketFactory;
import org.jboss.net.sockets.DefaultSocketFactory;
import org.jboss.util.naming.Util;
import org.picketbox.factories.SecurityFactory;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;


/**
 * setup JSR-160 JMXConnector
 * @author Scott Marlow smarlow@redhat.com
 *
 */
public class JMXConnector {

   /* start of configurable settings */
   private int rmiRegistryPort=1090;  // create a RMI registry at this port
   private int rmiServerPort=1091;    // rmi server will listen on this port
   private String hostname= "localhost";
   private static final String RMI_BIND_NAME =  "jmxrmi";
   private static final String LEGACY_RMI_BIND_NAME =  "jmxconnector";
   private static final String JNDI_BIND_NAME = "jmx/invoker/RMIAdaptor";
   private static final String LEGACY_BIND_NAME = "jmx/rmi/RMIAdaptor";
   private MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
   private InitialContext context;

   private String securityDomain;

   /* end of configurable settings */
   private RMIConnectorServer adapter;
   private RMIJRMPServerImpl rmiServer;
   private Registry registry;
   private static final Logger log = Logger.getLogger(JMXConnector.class);


   public JMXConnector() {

   }

   public InitialContext getContext() {
      return context;
   }

   public void setContext(InitialContext context) {
      this.context = context;
   }

   public String getSecurityDomain() {
      return securityDomain;
   }

   public void setSecurityDomain(String securityDomain) {
      this.securityDomain = securityDomain;
   }

   public MBeanServer getMbeanServer() {
      return mbeanServer;
   }

   public void setMbeanServer(MBeanServer mbeanServer) {
      this.mbeanServer = mbeanServer;
   }

   public int getRmiRegistryPort() {
      return rmiRegistryPort;
   }

   public void setRmiRegistryPort(int rmiRegistryPort) {
      this.rmiRegistryPort = rmiRegistryPort;
   }

   public int getRmiServerPort() {
      return rmiServerPort;
   }

   public void setRmiServerPort(int rmiServerPort) {
      this.rmiServerPort = rmiServerPort;
   }

   public String getHostname() {
      return hostname;
   }

   public void setHostname(String hostname) {
      this.hostname = hostname;
   }

   public void start()  {
      try {
         if(log.isInfoEnabled()) {
            log.info("starting JMXConnector on host " + hostname + ":" + rmiRegistryPort);
         }
         DefaultClientSocketFactory clientSocketFactory = new DefaultClientSocketFactory();
         clientSocketFactory.setBindAddress(hostname);
         DefaultSocketFactory serverSocketFactory = new DefaultSocketFactory();
         serverSocketFactory.setBindAddress(hostname);
         registry = LocateRegistry.createRegistry(rmiRegistryPort, clientSocketFactory, serverSocketFactory);
         HashMap env = new HashMap();
         if( securityDomain != null)
            env.put(RMIConnectorServer.AUTHENTICATOR, new JMXConnectorAuthenticator( securityDomain) );
         // note:  don't pass clientSocketFactory to RMIJRMPServerImpl ctor or JBAS-7933 regression will occur. 
         rmiServer = new RMIJRMPServerImpl(rmiServerPort, null, serverSocketFactory, env);
         JMXServiceURL url = buildJMXServiceURL();
         adapter = new RMIConnectorServer(url, env, rmiServer, wrapMBeanServer(mbeanServer));
         adapter.start();
         url = adapter.getAddress();
         registry.rebind(RMI_BIND_NAME, rmiServer.toStub());
         registry.rebind(LEGACY_RMI_BIND_NAME, rmiServer.toStub());

         if(log.isDebugEnabled()) {
            log.debug("started JMXConnector (" + url.toString() + ")" +
               (securityDomain!=null ? " domain=" + securityDomain : "") );
         }

         // For legacy access, bind a JMXAdapter to the JNDI names
         Reference reference = new Reference(MBeanServerConnection.class.getName(), JMXAdapter.class.getName(), null);
         reference.add(new StringRefAddr("JMXServiceURL", url.toString()));
         Util.rebind(context, JNDI_BIND_NAME, reference);
         Util.rebind(context, LEGACY_BIND_NAME, reference);
         //Object test = Util.lookup(context, JNDI_BIND_NAME, MBeanServerConnection.class);
         //log.info("test = " + test);
         SecurityFactory.prepare();
      } catch (IOException e) {
         log.error("Could not start JMXConnector", e);
      } catch (NamingException e) {
         log.error("couldn't bind legacy adaptor (" +JNDI_BIND_NAME + " or " + LEGACY_BIND_NAME + ")", e);
      }
   }

   /**
    * build URL and handle IPV6 literal address case.
    * TODO:  share IPV6 logic like this via a shared library.  
    */
   private JMXServiceURL buildJMXServiceURL() throws MalformedURLException {
      String host = hostname;
      if(host.indexOf(':') != -1) {  // is this a IPV6 literal address? if yes, surround with square brackets
                                     // as per rfc2732.
                                     // IPV6 literal addresses have one or more colons
                                     // IPV4 addresses/hostnames have no colons
         host = "[" + host + "]";
         
      }
      JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://" + host);
      return url;
   }

   public void stop()  {
      try {
         registry.unbind(RMI_BIND_NAME);
         registry.unbind(LEGACY_RMI_BIND_NAME);
         Util.unbind(context, JNDI_BIND_NAME);
         Util.unbind(context, LEGACY_BIND_NAME);
         UnicastRemoteObject.unexportObject(registry, true);
         log.info(this.getClass().getSimpleName() + " stopped");
      } catch (NotBoundException e) {
         log.error("connector was not bound ("+RMI_BIND_NAME+" or "+LEGACY_RMI_BIND_NAME+") to registry", e);
      } catch (IOException e) {
         log.error("couldn't unbind ("+RMI_BIND_NAME+" or "+LEGACY_RMI_BIND_NAME+") connector", e);
      } catch (NamingException e) {
         log.error("couldn't unbind legacy adaptor (" +JNDI_BIND_NAME + " or " + LEGACY_BIND_NAME + ")", e);
      }
      finally {
         try {
            SecurityFactory.release();
            adapter.stop();
         } catch (IOException e) {
            log.error("Could not stop connector", e);
         }
      }
   }

   private MBeanServer wrapMBeanServer(MBeanServer mbServer) {
      return new MBeanServerWrapper(mbServer);
   }
}