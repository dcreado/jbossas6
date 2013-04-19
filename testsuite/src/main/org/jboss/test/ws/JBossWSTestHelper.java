/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.test.ws;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.management.MBeanServerConnection;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.Service;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.soap.SOAPBinding;

/**
 * A JBossWS test helper that deals with test deployment/undeployment, etc.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 14-Oct-2004
 */
public class JBossWSTestHelper
{
   private static String implVendor;
   private static String implTitle;
   private static String implVersion;
	
   /** Deploy the given archive
    */
   public void deploy(String archive) throws Exception
   {
      URL url = getArchiveURL(archive);
      getDeployer().deploy(url);
   }

   /** Undeploy the given archive
    */
   public void undeploy(String archive) throws Exception
   {
      URL url = getArchiveURL(archive);
      getDeployer().undeploy(url);
   }

   /** True, if -Djbossws.target.server=jboss */
   public boolean isTargetServerJBoss()
   {
      String targetServer = System.getProperty("jbossws.target.server");
      if( targetServer == null )
         return ! isTargetServerTomcat();
      return "jboss".equals(targetServer);
   }

   /** True, if -Djbossws.target.server=tomcat */
   public boolean isTargetServerTomcat()
   {
      String targetServer = System.getProperty("jbossws.target.server");
      return "tomcat".equals(targetServer);
   }

   public MBeanServerConnection getServer() throws NamingException
   {
      InitialContext iniCtx = new InitialContext();
      MBeanServerConnection server = (MBeanServerConnection)iniCtx.lookup("jmx/invoker/RMIAdaptor");
      return server;
   }

   private JBossWSTestDeployer getDeployer()
   {
      return new JBossTestDeployer();
   }

   /** Try to discover the URL for the deployment archive */
   public URL getArchiveURL(String archive) throws MalformedURLException
   {
      URL url = null;
      try
      {
         url = new URL(archive);
      }
      catch (MalformedURLException ignore)
      {
         // ignore
      }

      if (url == null)
      {
         File file = new File(archive);
         if (file.exists())
            url = file.toURL();
      }

      if (url == null)
      {
         File file = new File("lib/" + archive);
         if (file.exists())
            url = file.toURL();
      }

      if (url == null)
         throw new IllegalArgumentException("Cannot obtain URL for: " + archive+", cwd="+new File(".").getAbsolutePath());

      return url;
   }

   public static boolean isIntegrationNative()
   {
      String vendor = getImplementationVendor();
      return vendor.toLowerCase().indexOf("jboss") != -1;
   }

   public static boolean isIntegrationMetro()
   {
      String vendor = getImplementationVendor();
      return vendor.toLowerCase().indexOf("sun") != -1;
   }

   public static boolean isIntegrationCXF()
   {
      String vendor = getImplementationVendor();
      return vendor.toLowerCase().indexOf("apache") != -1;
   }

   private static String getImplementationVendor()
   {
      if (implVendor == null)
      {
         Object obj = getImplementationObject();
         implVendor = obj.getClass().getPackage().getImplementationVendor();
         if (implVendor == null)
            implVendor = getImplementationPackage();

         implTitle = obj.getClass().getPackage().getImplementationTitle();
         implVersion = obj.getClass().getPackage().getImplementationVersion();

         System.out.println(implVendor + ", " + implTitle + ", " + implVersion);
      }
      return implVendor;
   }

   private static Object getImplementationObject()
   {
      Service service = Service.create(new QName("dummyService"));
      Object obj = service.getHandlerResolver();
      if (obj == null)
      {
         service.addPort(new QName("dummyPort"), SOAPBinding.SOAP11HTTP_BINDING, "http://dummy-address");
         obj = service.createDispatch(new QName("dummyPort"), Source.class, Mode.PAYLOAD);
      }
      return obj;
   }

   private static String getImplementationPackage()
   {
      return getImplementationObject().getClass().getPackage().getName();
   }
}
