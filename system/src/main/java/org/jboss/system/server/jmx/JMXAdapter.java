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

import javax.management.remote.*;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RMIAdapter replacement using JMXConnector
 * @author Scott Marlow smarlow@redhat.com
 *
 */
public class JMXAdapter implements ObjectFactory {
   // cache the connection to the server so that we don't leak each new JMXConnector
   private static Map<String,javax.management.remote.JMXConnector> jmxConnectorMap = new ConcurrentHashMap<String,javax.management.remote.JMXConnector>();

   public JMXAdapter() {

   }
   
   public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
      if ( obj != null && obj instanceof Reference) {
         Reference ref = (Reference)obj;
         // The JMXServiceURL points to the jmx server
         RefAddr urlRef = ref.get("JMXServiceURL");
         String  url = (String)urlRef.getContent();
         JMXServiceURL jmxserviceURL = new JMXServiceURL(url);
         javax.management.remote.JMXConnector jmxc = null;
         jmxc = jmxConnectorMap.get(url);

         if(jmxc != null) {
            try {
               // attempt to detect that the cached connection is broken
               jmxc.getConnectionId();
            }
            catch(IOException e) {
               close(jmxc);
               jmxc = null;  // ignore the broken connection and get a new one
            }
         }

         if(jmxc == null) {
            jmxc = JMXConnectorFactory.connect(jmxserviceURL,  new HashMap());
            jmxConnectorMap.put(url, jmxc);
         }
         return jmxc.getMBeanServerConnection();
      }
      return null;
   }

   /**
    * Can be called from the client side application to close all (JMXConnector) connections to the server.
    * If new connections are obtained while current connections are being closed, the new connections will not
    * be closed.   
    */
   public static void closeAll() {
      Collection<String> keys = jmxConnectorMap.keySet();
      Collection<javax.management.remote.JMXConnector> values = jmxConnectorMap.values();
      for (javax.management.remote.JMXConnector c : values) {
         close(c);
      }
      for (String key : keys) {
         jmxConnectorMap.remove(key);
      }

   }

   private static void close(javax.management.remote.JMXConnector jmxc) {
      try {
         jmxc.close();  // clean up any local resources associated with the connection (e.g. background threads)
      }
      catch( IOException ignore)
      {
      }
   }
   
}
