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
package org.jboss.invocation.http.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.management.ObjectName;
import javax.naming.InitialContext;

import org.jboss.invocation.Invoker;
import org.jboss.invocation.InvokerInterceptor;
import org.jboss.invocation.http.interfaces.HttpInvokerProxy;
import org.jboss.invocation.http.interfaces.ClientMethodInterceptor;
import org.jboss.naming.Util;
import org.jboss.proxy.GenericProxyFactory;
import org.jboss.system.Registry;
import org.jboss.system.ServiceMBeanSupport;
import org.jboss.util.StringPropertyReplacer;
import org.jboss.metadata.MetaData;
import org.jboss.config.ServerConfigUtil;
import org.w3c.dom.Element;

/** Create an interface proxy that uses HTTP to communicate with the server
 * side object that exposes the corresponding JMX invoke operation. Any request
 * to this servlet receives a serialized object stream containing a
 * MarshalledValue with the Naming proxy as its content.
 *
 * @author Scott.Stark@jboss.org
 * @version $Revision: 109975 $
 */
public class HttpProxyFactory extends ServiceMBeanSupport
   implements HttpProxyFactoryMBean
{
   /** The server side mbean that exposes the invoke operation for the
    exported interface */
   private ObjectName jmxInvokerName;
   /** The Proxy object which uses the HttpInvokerProxy as its handler */
   private Object theProxy;
   /** The http URL to the InvokerServlet */
   private String invokerURL;
   /** The alternative prefix used to build the invokerURL */
   private String invokerURLPrefix = "http://";
   /** The alternative suffix used to build the invokerURL */
   private String invokerURLSuffix = ":8080/invoker/JMXInvokerServlet";
   /** The alternative host or ip flag used to build the invokerURL */
   private boolean useHostName = false;
   /** The JNDI name under which the HttpInvokerProxy will be bound */
   private String jndiName;
   /** The interface that the HttpInvokerProxy implements */
   private Class exportedInterface;
   private Element interceptorConfig;
   private ArrayList interceptorClasses;

   public HttpProxyFactory()
   {
   }

   public ObjectName getInvokerName()
   {
      return jmxInvokerName;
   }
   public void setInvokerName(ObjectName jmxInvokerName)
   {
      this.jmxInvokerName = jmxInvokerName;
   }

   public String getJndiName()
   {
      return jndiName;
   }
   public void setJndiName(String jndiName)
   {
      this.jndiName = jndiName;
   }

   public String getInvokerURL()
   {
      return invokerURL;
   }
   public void setInvokerURL(String invokerURL)
   {
      // Replace any system properties in the URL
      String tmp = StringPropertyReplacer.replaceProperties(invokerURL);
      this.invokerURL = tmp;
      log.debug("Set invokerURL to "+this.invokerURL);
   }

   public String getInvokerURLPrefix()
   {
      return invokerURLPrefix;
   }
   public void setInvokerURLPrefix(String invokerURLPrefix)
   {
      this.invokerURLPrefix = invokerURLPrefix;
   }

   public String getInvokerURLSuffix()
   {
      return invokerURLSuffix;
   }
   public void setInvokerURLSuffix(String invokerURLSuffix)
   {
      this.invokerURLSuffix = invokerURLSuffix;
   }

   public boolean getUseHostName()
   {
      return useHostName;
   }
   public void setUseHostName(boolean flag)
   {
      this.useHostName = flag;
   }

   public Class getExportedInterface()
   {
      return exportedInterface;
   }
   public void setExportedInterface(Class exportedInterface)
   {
      this.exportedInterface = exportedInterface;
   }

   public Element getClientInterceptors()
   {
      return interceptorConfig;
   }
   public void setClientInterceptors(Element config) throws Exception
   {
      this.interceptorConfig = config;
      Iterator interceptorElements = MetaData.getChildrenByTagName(interceptorConfig, "interceptor");
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      if( interceptorClasses != null )
         interceptorClasses.clear();
      else
         interceptorClasses = new ArrayList();
      while( interceptorElements != null && interceptorElements.hasNext() )
      {
         Element ielement = (Element) interceptorElements.next();
         String className = null;
         className = MetaData.getElementContent(ielement);
         Class clazz = loader.loadClass(className);
         interceptorClasses.add(clazz);
      }
   }

   public Object getProxy()
   {
      return theProxy;
   }

   public Object getProxy(Object id)
   {
      Class[] ifaces = {exportedInterface};
      ArrayList interceptorClasses = null; //defineInterceptors();
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      GenericProxyFactory proxyFactory = new GenericProxyFactory();
      Object newProxy = null;
      /*
      Object newProxy = proxyFactory.createProxy(id, jmxInvokerName,
         null, null, null, interceptorClasses, loader, ifaces);
         */
      return newProxy;
   }

   /** Initializes the servlet.
    */
   protected void startService() throws Exception
   {
      /** Create an HttpInvokerProxy that posts invocations to the
       externalURL. This proxy will be associated with a naming JMX invoker
       given by the jmxInvokerName.
       */
      Invoker delegateInvoker = createInvoker();
      Integer nameHash = new Integer(jmxInvokerName.hashCode());
      log.debug("Bound delegate: "+delegateInvoker
         +" for invoker="+jmxInvokerName);
      /* Create a binding betweeh the invoker name hash and the jmx name
      This is used by the HttpInvoker to map from the Invocation ObjectName
      hash value to the target JMX ObjectName.
      */
      Registry.bind(nameHash, jmxInvokerName);

      Object cacheID = null;
      String proxyBindingName = null;
      Class[] ifaces = {exportedInterface};
      /* Initialize interceptorClasses with default client interceptor list
         if no client interceptor configuration was provided */
      if( interceptorClasses == null )
         interceptorClasses = defineDefaultInterceptors();
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      GenericProxyFactory proxyFactory = new GenericProxyFactory();
      theProxy = proxyFactory.createProxy(cacheID, jmxInvokerName,
         delegateInvoker, jndiName, proxyBindingName, interceptorClasses,
         loader, ifaces);
      log.debug("Created HttpInvokerProxy for invoker="+jmxInvokerName
         +", nameHash="+nameHash);

      if( jndiName != null )
      {
         InitialContext iniCtx = new InitialContext();
         Util.bind(iniCtx, jndiName, theProxy);
         log.debug("Bound proxy under jndiName="+jndiName);
      }
   }

   protected void stopService() throws Exception
   {
      Integer nameHash = new Integer(jmxInvokerName.hashCode());
      Registry.unbind(jmxInvokerName);
      Registry.unbind(nameHash);
      if( jndiName != null )
      {
         InitialContext iniCtx = new InitialContext();
         Util.unbind(iniCtx, jndiName);
      }
   }

   /** Build the default interceptor list. This consists of:
    * ClientMethodInterceptor
    * InvokerInterceptor
    */
   protected ArrayList defineDefaultInterceptors()
   {
      ArrayList tmp = new ArrayList();
      tmp.add(ClientMethodInterceptor.class);
      tmp.add(InvokerInterceptor.class);
      return tmp;
   }

   /** Create the Invoker
    */
   protected Invoker createInvoker() throws Exception
   {
      checkInvokerURL();
      HttpInvokerProxy delegateInvoker = new HttpInvokerProxy(invokerURL);
      return delegateInvoker;
   }

   /** Validate that the invokerURL is set, and if not build it from
    * the invokerURLPrefix + host + invokerURLSuffix. The host value will be
    * taken from the jboss.bind.address system property if its a valid
    * address, InetAddress.getLocalHost otherwise.
    */
   protected void checkInvokerURL() throws UnknownHostException
   {
      if( invokerURL == null )
      {
         // First check for a global bind address
         String host = ServerConfigUtil.getSpecificBindAddress();
         if( host == null )
         {
            InetAddress addr = InetAddress.getLocalHost();
            host = useHostName ? addr.getHostName() : addr.getHostAddress();
         }
         // JBAS-8540
         String url = invokerURLPrefix + ServerConfigUtil.fixHostnameForURL(host) + invokerURLSuffix;
         setInvokerURL(url);
      }
   }

}

