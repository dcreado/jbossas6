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
package org.jboss.webservices.integration.deployers;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.jboss.deployers.vfs.plugins.classloader.UrlIntegrationDeployer;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.util.StringPropertyReplacer;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.wsf.spi.management.ServerConfig;

/**
 * An abstract deployer that properly set the classpath for JAX-RPC deployments.
 * This is performed adding a reference to an integration lib from the jaxrpc
 * deployers to the deployment unit's classpath.
 *
 * @author alessio.soldano@jboss.com
 * @since 02-Feb-2010
 */
public abstract class JAXRPCIntegrationClassPathDeployer<T> extends UrlIntegrationDeployer<T>
{
   private Set<String> libs;
   private boolean integrationLibsFound = false;
   private ServerConfig wsServerConfig;
   private boolean stackRequiresIntegration;

   public JAXRPCIntegrationClassPathDeployer(Class<T> input)
   {
      super(input);
   }

   protected abstract boolean isClassPathChangeRequired(VFSDeploymentUnit unit);

   @Override
   protected boolean isIntegrationDeployment(VFSDeploymentUnit unit)
   {
      return stackRequiresIntegration && integrationLibsFound && isClassPathChangeRequired(unit);
   }

   @Override
   public void start()
   {
      //NOOP
   }

   protected Set<URL> getJBossWSIntegrationUrls()
   {
      Set<URL> result = new HashSet<URL>();
      try
      {
         for (String file : libs)
         {
            String url = getServerHome() + file;
            url = StringPropertyReplacer.replaceProperties(url);
            VirtualFile integrationLib = VFS.getChild(new URL(url));

            if (integrationLib != null && integrationLib.exists())
            {
               integrationLibsFound = true;
               result.add(integrationLib.toURL());
            }
            else
            {
               log.debug("Could not find JAX-RPC integration lib: " + url);
            }
         }
      }
      catch (Exception e)
      {
         throw new IllegalArgumentException("Unexpected error: " + e);
      }
      return result;
   }

   protected String getServerHome()
   {
      return "${jboss.server.home.url}";
   }

   public Set<String> getLibs()
   {
      return libs;
   }

   public void setLibs(Set<String> libs)
   {
      this.libs = libs;
      setIntegrationURLs(getJBossWSIntegrationUrls());
   }

   public ServerConfig getWsServerConfig()
   {
      return wsServerConfig;
   }

   public void setWsServerConfig(ServerConfig wsServerConfig)
   {
      this.wsServerConfig = wsServerConfig;
      //the Native stack does not requires the JAXRPC additional integration 
      stackRequiresIntegration = !wsServerConfig.getImplementationTitle().toLowerCase().contains("native");
   }
}
