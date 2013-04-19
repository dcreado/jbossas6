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

import org.jboss.profileservice.management.actions.AbstractTwoPhaseModificationAction;
import org.jboss.profileservice.management.event.ProfileModificationEvent;
import org.jboss.profileservice.spi.DeploymentRepository;
import org.jboss.profileservice.spi.ProfileDeployment;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.profileservice.spi.action.ProfileModificationResponse;
import org.jboss.profileservice.spi.action.ProfileModificationType;
import org.jboss.profileservice.spi.action.deployment.DeploymentAction;
import org.jboss.profileservice.spi.action.deployment.DeploymentActionContext;
import org.jboss.profileservice.spi.deployment.ProfileDeploymentFlag;


/**
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision$
 */
public class RepositoryRemoveAction extends AbstractTwoPhaseModificationAction<DeploymentActionContext> implements DeploymentAction<DeploymentActionContext>
{

   /** The profile key. */
   private ProfileKey key;
   
   /** The deployment repository. */
   private DeploymentRepository deploymentRepository;
   
   /** The deployment name. */
   private String deploymentName;
   
   public RepositoryRemoveAction(ProfileKey key, DeploymentRepository repository, String deploymentName,
         DeploymentActionContext modificationContext)
   {
      super(modificationContext);
      this.key = key;
      this.deploymentRepository = repository;
      this.deploymentName = deploymentName;
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
         ProfileDeployment deployment = deploymentRepository.getDeployment(deploymentName);
         deployment.getDeploymentInfo().setFlag(ProfileDeploymentFlag.LOCKED);
         deploymentRepository.removeDeployment(deploymentName);
         // Notify
         response.fireModificationEvent(ProfileModificationEvent.create(ProfileModificationType.DELETE, key));
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

}

