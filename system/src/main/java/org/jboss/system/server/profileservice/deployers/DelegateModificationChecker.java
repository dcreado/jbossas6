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
package org.jboss.system.server.profileservice.deployers;

import org.jboss.deployers.vfs.spi.structure.modified.StructureModificationChecker;
import org.jboss.logging.Logger;
import org.jboss.profileservice.deployment.hotdeploy.ProfileDeploymentModificationChecker;
import org.jboss.profileservice.spi.ProfileDeployment;
import org.jboss.vfs.VirtualFile;

/**
 * Modification checker delegating the actual checks to the {@ StructureModificationChecker}.
 *
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision$
 */
public class DelegateModificationChecker implements ProfileDeploymentModificationChecker
{

   /** The logger. */
   private static final Logger log = Logger.getLogger(ProfileDeploymentModificationChecker.class);

   /** The structure modification checker. */
   private StructureModificationChecker delegate;

   public StructureModificationChecker getModificationChecker()
   {
      return delegate;
   }

   public void setModificationChecker(StructureModificationChecker delegate)
   {
      this.delegate = delegate;
   }

   public boolean isDeploymentModified(ProfileDeployment deployment, VirtualFile deploymentRoot)
   {
      if(getModificationChecker() == null)
      {
         return false;
      }
      try
      {
         String deploymentName = deployment.getName();
         return delegate.hasStructureBeenModified(deploymentName, deploymentRoot);
      }
      catch(Exception e)
      {
         log.trace("failed to check structure", e);
         return false; // is this correct ?
      }
   }

   public void removed(ProfileDeployment deployment, VirtualFile deploymentRoot) {
      delegate.removeStructureRoot(deploymentRoot);
   }

}

