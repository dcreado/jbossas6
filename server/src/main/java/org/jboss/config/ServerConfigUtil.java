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
package org.jboss.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jboss.bootstrap.api.as.config.JBossASServerConfig;
import org.jboss.bootstrap.spi.as.config.JBossASBasedConfigurationInitializer;

/**
 * ServerConfigUtil
 * 
 * Utilities for accessing server configuration.  Moved from 
 * legacy bootstrap implementation and preserved to protect
 * legacy, working code from being disturbed.
 *
 * @author <a href="mailto:adrian@jboss.org">Adrian Brock</a>
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a> Maintain only
 * @author <a href="mailto:rachmatowicz@jboss.org">Richard Achmatowicz</a> 
 * @version $Revision: $
 */
public final class ServerConfigUtil
{
   /**
    * Private constructor
    */
   private ServerConfigUtil()
   {
      // No external instances
   }

   /**
    * Retrieve the default bind address, but only if it is specific
    * 
    * @return the specific bind address
    */
   public static String getSpecificBindAddress()
   {
      String address = System.getProperty(JBossASServerConfig.PROP_KEY_JBOSSAS_BIND_ADDRESS);
      if (address == null || address.equals(JBossASBasedConfigurationInitializer.VALUE_BIND_ADDRESS_ANY))
         return null;
      return address;
   }

   /**
    * Fix the remote inet address.
    * 
    * If we pass the address to the client we don't want to
    * tell it to connect to 0.0.0.0, use our host name instead
    * @param address the passed address
    * @return the fixed address
    */
   public static InetAddress fixRemoteAddress(InetAddress address)
   {
      try
      {
         if (address == null
               || InetAddress.getByName(JBossASBasedConfigurationInitializer.VALUE_BIND_ADDRESS_ANY).equals(address))
            return InetAddress.getLocalHost();
      }
      catch (UnknownHostException ignored)
      {
      }
      return address;
   }

   /**
    * Fix the remote address.
    * 
    * If we pass the address to the client we don't want to
    * tell it to connect to 0.0.0.0, use our host name instead
    * @param address the passed address
    * @return the fixed address
    */
   public static String fixRemoteAddress(String address)
   {
      try
      {
         if (address == null || JBossASBasedConfigurationInitializer.VALUE_BIND_ADDRESS_ANY.equals(address))
            return InetAddress.getLocalHost().getHostName();
      }
      catch (UnknownHostException ignored)
      {
      }
      return address;
   }
   
   /**
    * Utility to get a shortened url relative to the server home if possible
    * 
    * @param longUrl
    * @return the short url
    */
   public static String shortUrlFromServerHome(String longUrl)
   {
      String serverHomeUrl = System.getProperty(JBossASServerConfig.PROP_KEY_JBOSSAS_HOME_URL);

      if (longUrl == null || serverHomeUrl == null)
          return longUrl;

      if (longUrl.startsWith(serverHomeUrl))
        return ".../" + longUrl.substring(serverHomeUrl.length());
      else
      {
         String jarServerHomeUrl = "jar:" + serverHomeUrl;
         if (longUrl.startsWith(jarServerHomeUrl))
            return ".../" + longUrl.substring(jarServerHomeUrl.length());
         else
            return longUrl;
      }
   }
   
   /**
    * Utility to check if a hostname is an IPv6 literal and is so, add surrounding brackets
    * for use in URLs (see JBAS-8540)
    * 
    * @param hostname
    * @return hostname suitable for use in URLs
    */
   public static String fixHostnameForURL(String host)
   {
       if (host == null)
    	   return host ;

       // if the hostname is an IPv6 literal, enclose it in brackets
       if (host.indexOf(':') != -1)
    	   return "[" + host + "]" ;
       else 
    	   return host ;
   }
}
