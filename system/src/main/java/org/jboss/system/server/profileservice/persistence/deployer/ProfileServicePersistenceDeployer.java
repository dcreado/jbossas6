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
package org.jboss.system.server.profileservice.persistence.deployer;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.profileservice.persistence.PersistenceFactory;
import org.jboss.profileservice.persistence.repository.PersistenceRepository;
import org.jboss.profileservice.persistence.repository.metadata.AttachmentMetaData;
import org.jboss.profileservice.persistence.repository.metadata.RepositoryAttachmentMetaData;
import org.jboss.profileservice.persistence.xml.PersistenceRoot;

/**
 * The ProfileService Persistence Deployer. This deployer applies the
 * persisted changes to an attachment.
 * 
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision: 104372 $
 */
public class ProfileServicePersistenceDeployer extends AbstractRealDeployer
{

   /** The managed prefix. */
   public static final String PERSISTED_ATTACHMENT_PREFIX = "PERISTED";
   
   /** The persistence repository. */
   private PersistenceRepository persistenceRepository;
   
   /** The Logger. */
   private static final Logger log = Logger.getLogger(ProfileServicePersistenceDeployer.class);
   
   public ProfileServicePersistenceDeployer()
   {
      super();
      setAllInputs(true);
      setStage(DeploymentStages.PRE_REAL);
   }

   public PersistenceRepository getPersistenceRepository()
   {
	   return persistenceRepository;
   }
   
   public void setPersistenceRepository(PersistenceRepository persistenceRepository)
   {
	   this.persistenceRepository = persistenceRepository;
   }

   @Override
   protected void internalDeploy(DeploymentUnit unit) throws DeploymentException
   {
      if(unit == null || unit instanceof VFSDeploymentUnit == false)
         return;
      try
      {
         applyPersistentChanges(VFSDeploymentUnit.class.cast(unit));
      }
      catch(Throwable e)
      {
         log.warn("Failed to update the persisted attachment information", e);
      }
   }
   
   protected void applyPersistentChanges(VFSDeploymentUnit unit) throws Throwable
   {
      String deploymentName = unit.getName();
      String simpleName = unit.getSimpleName();
      RepositoryAttachmentMetaData metaData = getPersistenceRepository().loadMetaData(deploymentName, simpleName);
      if(metaData == null)
         return;      
      
      // Check if the deployment was modified
      if(PersistenceModificationChecker.hasBeenModified(unit, metaData.getLastModified()))
      {
         log.debug("Deployment was modified, not applying persisted information : " + unit);
         return;
      }
      //
      if(metaData.getAttachments() != null && metaData.getAttachments().isEmpty() == false)
      {
         for(AttachmentMetaData attachment: metaData.getAttachments())
         {
            Object instance = unit.getAttachment(attachment.getName());
            if(instance != null)
            {
               PersistenceRoot root = getPersistenceRepository().loadAttachment(deploymentName, simpleName, attachment);
               if(root == null)
               {
                  log.warn("Null persisted information for deployment: " + deploymentName);
               }
               // update ...
               getPersistenceFactory().restorePersistenceRoot(root, instance, unit.getClassLoader());
            }
            else
            {
               log.warn("Could not apply changes, failed to find attachment: " + attachment.getName());
            }
         }         
      }      
   }

   private PersistenceFactory getPersistenceFactory()
   {
      return getPersistenceRepository().getPersistenceFactory();
   }
   
   
   

}
