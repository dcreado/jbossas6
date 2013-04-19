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
package org.jboss.system.server;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.bootstrap.api.as.config.JBossASBasedServerConfig;
import org.jboss.bootstrap.api.descriptor.BootstrapDescriptor;

/**
 * An mbean wrapper for the BaseServerConfig that exposes the config as the
 * legacy ServerConfigImplMBean.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision: 101358 $
 */
public class ServerConfigImpl<T extends JBossASBasedServerConfig<T>> implements ServerConfigImplMBean<T>
{
   private T config;

   /**
    * Construct a ServerConfigImpl with the ServerConfig pojo which will be used
    * as the delegate for the ServerConfigImplMBean ops.
    *
    * @param config - the ServerConfig pojo to expose as a ServerConfigImplMBean
    */
   public ServerConfigImpl(T config)
   {
      this.config = config;
   }

   public T getConfig()
   {
      return config;
   }

   public boolean equals(Object obj)
   {
      return config.equals(obj);
   }

   /*
    * All methods below this marker delegate to the underlying config 
    */

   public T bindAddress(String arg0)
   {
      config.bindAddress(arg0);
      return this.covarientReturn();
   }

   public T bootLibraryLocation(String arg0) throws IllegalArgumentException
   {
      config.bootLibraryLocation(arg0);
      return this.covarientReturn();
   }

   public T bootLibraryLocation(URL arg0)
   {
      config.bootLibraryLocation(arg0);
      return this.covarientReturn();
   }

   public T commonBaseLocation(String arg0) throws IllegalArgumentException
   {
      config.commonBaseLocation(arg0);
      return this.covarientReturn();
   }

   public T commonBaseLocation(URL arg0)
   {
      config.commonBaseLocation(arg0);
      return this.covarientReturn();
   }

   public T commonLibLocation(String arg0) throws IllegalArgumentException
   {
      config.commonLibLocation(arg0);
      return this.covarientReturn();
   }

   public T commonLibLocation(URL arg0)
   {
      config.commonLibLocation(arg0);
      return this.covarientReturn();
   }

   public String getBindAddress()
   {
      return config.getBindAddress();
   }

   public URL getBootLibraryLocation()
   {
      return config.getBootLibraryLocation();
   }

   public URL getCommonBaseLocation()
   {
      return config.getCommonBaseLocation();
   }

   public URL getCommonLibLocation()
   {
      return config.getCommonLibLocation();
   }

   public URL getJBossHome()
   {
      return config.getJBossHome();
   }

   public URL getNativeLibraryLocation()
   {
      return config.getNativeLibraryLocation();
   }

   public String getPartitionName()
   {
      return config.getPartitionName();
   }

   public URL getServerBaseLocation()
   {
      return config.getServerBaseLocation();
   }

   public URL getServerConfLocation()
   {
      return config.getServerConfLocation();
   }

   public URL getServerDataLocation()
   {
      return config.getServerDataLocation();
   }

   public URL getServerHomeLocation()
   {
      return config.getServerHomeLocation();
   }

   public URL getServerLibLocation()
   {
      return config.getServerLibLocation();
   }

   public URL getServerLogLocation()
   {
      return config.getServerLibLocation();
   }

   public String getServerName()
   {
      return config.getServerName();
   }

   public URL getServerTempLocation()
   {
      return config.getServerTempLocation();
   }

   public String getUdpGroup()
   {
      return config.getUdpGroup();
   }

   public Integer getUdpPort()
   {
      return config.getUdpPort();
   }

   public Boolean isLoadNative()
   {
      return config.isLoadNative();
   }

   public T jbossHome(String arg0) throws IllegalArgumentException
   {
      config.jbossHome(arg0);
      return this.covarientReturn();
   }

   public T jbossHome(URL arg0)
   {
      config.jbossHome(arg0);
      return this.covarientReturn();
   }

   public T loadNative(Boolean arg0)
   {
      config.loadNative(arg0);
      return this.covarientReturn();
   }

   public T nativeLibraryLocation(String arg0) throws IllegalArgumentException
   {
      config.nativeLibraryLocation(arg0);
      return this.covarientReturn();
   }

   public T nativeLibraryLocation(URL arg0)
   {
      config.nativeLibraryLocation(arg0);
      return this.covarientReturn();
   }

   public T partitionName(String arg0)
   {
      config.partitionName(arg0);
      return this.covarientReturn();
   }

   public T serverBaseLocation(String arg0) throws IllegalArgumentException
   {
      config.serverBaseLocation(arg0);
      return this.covarientReturn();
   }

   public T serverBaseLocation(URL arg0)
   {
      config.serverBaseLocation(arg0);
      return this.covarientReturn();
   }

   public T serverConfLocation(String arg0) throws IllegalArgumentException
   {
      config.serverConfLocation(arg0);
      return this.covarientReturn();
   }

   public T serverConfLocation(URL arg0)
   {
      config.serverConfLocation(arg0);
      return this.covarientReturn();
   }

   public T serverDataLocation(String arg0) throws IllegalArgumentException
   {
      config.serverDataLocation(arg0);
      return this.covarientReturn();
   }

   public T serverDataLocation(URL arg0)
   {
      config.serverDataLocation(arg0);
      return this.covarientReturn();
   }

   public T serverHomeLocation(String arg0) throws IllegalArgumentException
   {
      config.serverHomeLocation(arg0);
      return this.covarientReturn();
   }

   public T serverHomeLocation(URL arg0)
   {
      config.serverHomeLocation(arg0);
      return this.covarientReturn();
   }

   public T serverLibLocation(String arg0) throws IllegalArgumentException
   {
      config.serverLibLocation(arg0);
      return this.covarientReturn();
   }

   public T serverLibLocation(URL arg0)
   {
      config.serverLibLocation(arg0);
      return this.covarientReturn();
   }

   public T serverLogLocation(String arg0) throws IllegalArgumentException
   {
      config.serverLogLocation(arg0);
      return this.covarientReturn();
   }

   public T serverLogLocation(URL arg0)
   {
      config.serverLogLocation(arg0);
      return this.covarientReturn();
   }

   public T serverName(String arg0)
   {
      config.serverName(arg0);
      return this.covarientReturn();
   }

   public T serverTempLocation(String arg0) throws IllegalArgumentException
   {
      config.serverTempLocation(arg0);
      return this.covarientReturn();
   }

   public T serverTempLocation(URL arg0)
   {
      config.serverTempLocation(arg0);
      return this.covarientReturn();
   }

   public T udpGroup(String arg0)
   {
      config.udpGroup(arg0);
      return this.covarientReturn();
   }

   public T udpPort(Integer arg0)
   {
      config.udpPort(arg0);
      return this.covarientReturn();
   }

   public T bootstrapHome(String arg0) throws IllegalArgumentException, IllegalStateException
   {
      config.bootstrapHome(arg0);
      return this.covarientReturn();
   }

   public T bootstrapHome(URL arg0) throws IllegalArgumentException, IllegalStateException
   {
      config.bootstrapHome(arg0);
      return this.covarientReturn();
   }

   public T bootstrapName(String arg0) throws IllegalArgumentException, IllegalStateException
   {
      config.bootstrapName(arg0);
      return this.covarientReturn();
   }

   public T bootstrapUrl(String arg0) throws IllegalArgumentException, IllegalStateException
   {
      config.bootstrapUrl(arg0);
      return this.covarientReturn();
   }

   public T bootstrapUrl(URL arg0) throws IllegalArgumentException, IllegalStateException
   {
      config.bootstrapUrl(arg0);
      return this.covarientReturn();
   }

   public void freeze() throws IllegalStateException
   {
      config.freeze();

   }

   public URL getBootstrapHome()
   {
      return config.getBootstrapHome();
   }

   public String getBootstrapName()
   {
      return config.getBootstrapName();
   }

   public URL getBootstrapUrl()
   {
      return config.getBootstrapUrl();
   }

   public Map<String, String> getProperties()
   {
      return config.getProperties();
   }

   public boolean isFrozen()
   {
      return config.isFrozen();
   }

   public T property(String arg0, String arg1) throws IllegalArgumentException, IllegalStateException
   {
      config.property(arg0, arg1);
      return this.covarientReturn();
   }

   public Boolean isUsePlatformMBeanServer()
   {
      return config.isUsePlatformMBeanServer();
   }

   public T usePlatformMBeanServer(Boolean arg0)
   {
      config.usePlatformMBeanServer(arg0);
      return this.covarientReturn();
   }

   public String getProperty(String arg0) throws IllegalArgumentException
   {
      return config.getProperty(arg0);
   }

   public T properties(Map<String, String> arg0) throws IllegalArgumentException, IllegalStateException
   {
      config.properties(arg0);
      return this.covarientReturn();
   }

   public T properties(Properties arg0) throws IllegalArgumentException, IllegalStateException
   {
      config.properties(arg0);
      return this.covarientReturn();
   }

   public List<BootstrapDescriptor> getBootstrapDescriptors()
   {
      return config.getBootstrapDescriptors();
   }

   /**
    * Returns this reference represented as T
    * @return
    */
   private T covarientReturn()
   {
      @SuppressWarnings("unchecked")
      final T thisRef = (T) this;
      return thisRef;
   }

}
