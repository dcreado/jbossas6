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
package org.jboss.system.server.profileservice.deployers;

import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.deployers.client.plugins.deployment.AbstractDeployment;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.vfs.spi.client.VFSDeploymentFactory;
import org.jboss.profileservice.deployment.DeploymentBuilder;
import org.jboss.profileservice.spi.ProfileDeployment;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.system.server.profileservice.repository.LegacyProfileDeploymentFactory.WorkaroundProfileDeployment;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;


/**
 * The VDF deployment builder.
 * 
 * TODO connect to persistence repository
 * 
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision: 101110 $
 */
public class VDFDeploymentBuilder implements DeploymentBuilder<Deployment>
{
 
   /** The instance. */
   private static final VDFDeploymentBuilder instance = new VDFDeploymentBuilder();
   
   /** The vfs deployment factory. */
   private static final VFSDeploymentFactory deploymentFactory = VFSDeploymentFactory.getInstance();
   
   private final StructureMetaDataBuilder deploymentProcessor = new StructureMetaDataBuilder();
   
   public static VDFDeploymentBuilder getInstance()
   {
      return instance;
   }

   private VDFDeploymentBuilder() { }
   
   public Deployment createDeployment(ProfileKey key, ProfileDeployment profileDeployment) throws Exception
   {
      Deployment d = null;
      if(profileDeployment.getRoot() == null)
      {
         d = new AbstractDeployment(profileDeployment.getName());
      }
      else
      {
         VirtualFile vf = profileDeployment.getRoot();
         // The ones created using the {@code AutoUnmounter}
         if(profileDeployment instanceof WorkaroundProfileDeployment) 
         {
            try
            {
               vf = VFS.getChild(new URI(profileDeployment.getName()));
            }
            catch (URISyntaxException e)
            {
               throw new RuntimeException(e);
            }
         }
         d = deploymentFactory.createVFSDeployment(profileDeployment.getName(), vf);
      }
      
      deploymentProcessor.processDeployment(profileDeployment, d);
      return d;
   }
   
}
