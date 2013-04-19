/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.webservices.integration.config;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import javax.management.JMException;
import javax.management.ObjectName;

import org.jboss.wsf.common.management.AbstractServerConfig;
import org.jboss.wsf.common.management.AbstractServerConfigMBean;

/**
 * AS specific ServerConfig.
 *
 * @author <a href="mailto:asoldano@redhat.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
public final class ServerConfigImpl extends AbstractServerConfig implements AbstractServerConfigMBean
{
   /**
    * Constructor.
    */
   public ServerConfigImpl()
   {
      super();
   }

   /**
    * Gets server temp directory.
    *
    * @return temp directory
    */
   public File getServerTempDir()
   {
      return this.getDirFromServerConfig("ServerTempLocation");
   }

   /**
    * Gets server home directory.
    *
    * @return home directory
    */
   public File getHomeDir()
   {
      return this.getDirFromServerConfig("JBossHome");
   }

   /**
    * Gets server data directory.
    *
    * @return data directory
    */
   public File getServerDataDir()
   {
      return this.getDirFromServerConfig("ServerDataLocation");
   }

   /**
    * Obtains the requested directory from the server configuration.
    *
    * @param attributeName directory attribute name
    * @return requested directory
    */
   private File getDirFromServerConfig(final String attributeName)
   {
      // Define the ON to invoke upon
      final ObjectName on = OBJECT_NAME_SERVER_CONFIG;

      // Get the URL location
      URL location = null;
      try
      {
         location = (URL) this.getMbeanServer().getAttribute(on, attributeName);
      }
      catch (final JMException e)
      {
         throw new RuntimeException("Could not obtain attribute " + attributeName + " from " + on, e);
      }

      // Represent as a File
      File dir = null;
      try
      {
         dir = new File(location.toURI());
      }
      catch (final URISyntaxException urise)
      {
         throw new RuntimeException("Could not desired directory from URL: " + location, urise);
      }

      return dir;
   }
}
