/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.system.server.profileservice.deployers;

import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.client.spi.main.MainDeployer;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.profileservice.deployment.ProfileDeployerPlugin;
import org.jboss.profileservice.deployment.ProfileDeployerPluginRegistry;
import org.jboss.profileservice.spi.ProfileDeployment;
import org.jboss.profileservice.spi.ProfileKey;

/**
 * {@code ProfileDeployerPlugin} wrapping {@code MainDeployer}. 
 * 
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision$
 */
public class MainDeployerPlugin implements ProfileDeployerPlugin
{
   
   /** The deployment builder. */
   private final VDFDeploymentBuilder deploymentBuilder = VDFDeploymentBuilder.getInstance();
   
   /** The main deployer. */
   private final MainDeployer deployer;
   
   /** The deployer registry. */
   private final ProfileDeployerPluginRegistry registry;
   
   public MainDeployerPlugin(MainDeployer deployer, ProfileDeployerPluginRegistry registry)
   {
      if(deployer == null)
      {
         throw new IllegalArgumentException("null deployer");
      }
      this.deployer = deployer;
      this.registry = registry;
   }
   
   public void start()
   {
      registry.setDefaultPlugin(this);
   }
   
   public void stop()
   {
      registry.removeDefaultPlugin();
   }
   
   public void addDeployment(ProfileKey key, ProfileDeployment deployment) throws Exception
   {
      if(deployment == null)
      {
         throw new IllegalArgumentException("null profile deployment");
      }
      Deployment d = createDeployment(key, deployment);
      deployer.addDeployment(d);
   }

   public void checkComplete(String... names) throws Exception
   {
      deployer.checkComplete(names);
   }

   public void checkComplete() throws Exception
   {
      deployer.checkComplete();
   }

   public ManagedDeployment getManagedDeployment(ProfileDeployment deployment) throws Exception
   {
      if(deployment == null)
      {
         throw new IllegalArgumentException("null profile deployment");
      }
      return deployer.getManagedDeployment(deployment.getName());
   }

   public boolean isSupportRedeployment()
   {
      return true;
   }

   public void process()
   {
      this.deployer.process();
   }

   public void removeDeployment(ProfileKey key, ProfileDeployment deployment) throws Exception
   {
      if(deployment == null)
      {
         throw new IllegalArgumentException("null profile deployment");
      }
      this.deployer.removeDeployment(deployment.getName());
   }

   public void prepareShutdown()
   {
      this.deployer.prepareShutdown();
   }
   
   public void shutdown()
   {
      this.deployer.shutdown();
   }
   
   Deployment createDeployment(ProfileKey key, ProfileDeployment deployment) throws Exception
   {
      return deploymentBuilder.createDeployment(key, deployment);
   }
   
}

