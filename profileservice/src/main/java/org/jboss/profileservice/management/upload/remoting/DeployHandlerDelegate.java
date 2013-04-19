/*
* JBoss, Home of Professional Open Source
* Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.profileservice.management.upload.remoting;

import java.io.InputStream;

import org.jboss.deployers.spi.management.deploy.DeploymentID;
import org.jboss.profileservice.plugins.deploy.AbstractDeployHandler;
import org.jboss.profileservice.spi.DeploymentRepository;
import org.jboss.profileservice.spi.MutableProfile;
import org.jboss.profileservice.spi.NoSuchDeploymentException;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.profileservice.spi.ProfileRepository;
import org.jboss.profileservice.spi.action.deployment.DeploymentAction;
import org.jboss.profileservice.spi.action.deployment.DeploymentActionContext;
import org.jboss.profileservice.spi.managed.ManagedProfile;

/**
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision$
 */
public class DeployHandlerDelegate extends AbstractDeployHandler
{
   
   /** The profile repository. */
   private ProfileRepository profileRepository;

   public ProfileRepository getProfileRepository()
   {
      return profileRepository;
   }
   
   public void setProfileRepository(ProfileRepository profileRepository)
   {
      this.profileRepository = profileRepository;
   }
   
   public String[] resolveDeploymentNames(String... names) throws NoSuchDeploymentException
   {
      if(names == null)
      {
         throw new IllegalArgumentException("null deployment names");
      }
      return super.resolveDeploymentNames(names);
   }
   
   public String[] distribute(DeploymentID dtID, InputStream contentIS) throws Exception
   {
      super.addDeployment(dtID, contentIS);
      return resolveDeploymentNames(dtID.getNames());
   }
   
   public void startDeployments(String... deploymentNames) throws Exception
   {
      super.startDeployments(deploymentNames);
   }
   
   protected void stopDeployments(String... deploymentNames) throws Exception
   {
      super.stopDeployments(deploymentNames);
   }
   
   protected void removeDeployments(String... deploymentNames) throws Exception
   {
      super.removeDeployments(deploymentNames);
   }
 
   
   protected DeploymentAction<? extends DeploymentActionContext> createWoraroundDistributeAction(ProfileKey key,
         DeploymentID dtID, InputStream contentIS, MutableProfile profile) throws Exception
   {
      if(profileRepository.getProfileKeys().contains(key))
      {
         DeploymentRepository repository = profileRepository.getProfileDeploymentRepository(key);
         return new RepositoryDistributeAction(dtID, contentIS, profile, repository, null);
      }
      return null;
   }
   
   protected DeploymentAction<? extends DeploymentActionContext> createWoraroundRemoveAction(ProfileKey key,
         ManagedProfile profile, String name) throws Exception
   {
      if(profileRepository.getProfileKeys().contains(key))
      {
         DeploymentRepository repository = profileRepository.getProfileDeploymentRepository(key);
         return new RepositoryRemoveAction(key, repository, name, null);
      }
      return null;
   }
}

