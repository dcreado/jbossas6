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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.jboss.deployers.spi.management.deploy.DeploymentID;
import org.jboss.profileservice.management.event.ProfileModificationEvent;
import org.jboss.profileservice.plugins.deploy.actions.AbstractDeploymentAddAction;
import org.jboss.profileservice.spi.DeploymentRepository;
import org.jboss.profileservice.spi.MutableProfile;
import org.jboss.profileservice.spi.ProfileDeployment;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.profileservice.spi.action.ProfileModificationResponse;
import org.jboss.profileservice.spi.action.ProfileModificationType;
import org.jboss.profileservice.spi.action.deployment.DeploymentAction;
import org.jboss.profileservice.spi.action.deployment.DeploymentActionContext;
import org.jboss.profileservice.spi.deployment.ProfileDeploymentFlag;
import org.jboss.system.server.profileservice.repository.LegacyProfileDeploymentFactory;
import org.jboss.vfs.VirtualFile;

/**
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision$
 */
public class RepositoryDistributeAction extends AbstractDeploymentAddAction implements DeploymentAction<DeploymentActionContext>
{

   /** The deployment repository. */
   private final DeploymentRepository deploymentRepository;
   
   /** The deployment content IS. */
   private final InputStream contentIS;
   
   /** The deployment factory */
   private static final LegacyProfileDeploymentFactory deploymentFactory = LegacyProfileDeploymentFactory.getInstance(); 
   
   public RepositoryDistributeAction(DeploymentID dtID, InputStream is, MutableProfile profile, DeploymentRepository repository,
         DeploymentActionContext modificationContext)
   {
      super(dtID, profile, modificationContext);
      this.deploymentRepository = repository;
      this.contentIS = is;
   }

   protected void doCancel()
   {
      // 
   }

   protected void doCommit(ProfileModificationResponse response)
   {
      // 
   }

   protected void doComplete(ProfileModificationResponse response) throws Exception
   {
      // 
   }

   protected boolean doPrepare(ProfileModificationResponse response)
   {
      try
      {
         String deploymentName = getDeploymentName();
         // in case there is already a deployment, lock the contents.
         // TODO this might should not be done here ?
         if(getProfile().hasDeployment(deploymentName))
         {
            ProfileDeployment deployment = getProfile().getDeployment(deploymentName);
            deployment.getDeploymentInfo().setFlag(ProfileDeploymentFlag.LOCKED);
         }
         
         String repositoryName = deploymentRepository.addDeploymentContent(deploymentName, contentIS, getDeploymentID().getDeploymentOptions());
         
         // FIXME make deployment visible to management view
         VirtualFile vf = deploymentRepository.getDeploymentContent(repositoryName);
         
         // FIXME
         ProfileKey key = getProfile().getKey();
         String profileName = key.getName();
         String repoDeploymentName = createDeploymentName(vf);
         deploymentRepository.lockDeploymentContent(repoDeploymentName);
         // Don't try to re-mount
         if(deploymentRepository.getDeploymentNames().contains(repoDeploymentName) == false)
         {
            ProfileDeployment deployment = createDeployment(profileName, repoDeploymentName, vf);
            // Mark it as locked, so we can add contents after
            deployment.getDeploymentInfo().setFlag(ProfileDeploymentFlag.LOCKED);
            // Add deployment
            deploymentRepository.addDeployment(deployment.getName(), deployment);
         }
         
         // Notify
         response.fireModificationEvent(ProfileModificationEvent.create(ProfileModificationType.ADD, getProfile().getKey()));
      }
      catch(Exception e)
      {
         response.setFailure(e);
         return false;
      }
      return true;
   }

   protected void doRollbackFromActive()
   {
      // 
   }

   protected void doRollbackFromCancelled()
   {
      // 
   }

   protected void doRollbackFromComplete()
   {
      // 
   }

   protected void doRollbackFromPrepared()
   {
      // 
   }

   protected void doRollbackFromRollbackOnly()
   {
      //
   }

   /**
   * Create a profile deployment.
   *
   * @param file the root file
   * @return the deployment
   */
   protected ProfileDeployment createDeployment(String profileName, String deploymentName, VirtualFile file) throws IOException
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");
   
      return deploymentFactory.createProfileDeployment(profileName, deploymentName, file);
   }
   
   @SuppressWarnings("static-access")
   protected String createDeploymentName(VirtualFile vf) throws URISyntaxException
   {
      return deploymentFactory.createDeploymentName(vf);
   } 
   
}

