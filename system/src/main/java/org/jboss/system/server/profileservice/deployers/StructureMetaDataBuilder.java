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

import java.util.ArrayList;
import java.util.List;

import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.client.spi.DeploymentFactory;
import org.jboss.deployers.spi.structure.ClassPathEntry;
import org.jboss.deployers.spi.structure.ContextInfo;
import org.jboss.deployers.vfs.spi.client.VFSDeploymentFactory;
import org.jboss.profileservice.deployment.DeploymentBuilder.DeploymentAttachmentsProcessor;
import org.jboss.profileservice.spi.ProfileDeployment;
import org.jboss.profileservice.spi.virtual.assembly.VirtualDeploymentAssemblyContext;
import org.jboss.profileservice.virtual.assembly.BasicVirtualAssemblyContext;

/**
 * The structure meta data builder.
 * 
 * TODO differentiate between more meta data locations.
 * 
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision: 101110 $
 */
public class StructureMetaDataBuilder implements DeploymentAttachmentsProcessor<Deployment>
{

   /** The deployment factory. */
   private static final DeploymentFactory deploymentFactory = VFSDeploymentFactory.getInstance();
   
   public void processDeployment(ProfileDeployment deployment, Deployment target)
   {
      // Get the assembly context
      VirtualDeploymentAssemblyContext ctx = deployment.getTransientAttachments().getAttachment(
            VirtualDeploymentAssemblyContext.class.getName(), VirtualDeploymentAssemblyContext.class);
      
      // Create the structure meta data
      if(ctx != null)
      {
         createStructureMetaData(target, "", ctx);
      }
   }
   
   protected ContextInfo createStructureMetaData(Deployment deployment, String path, VirtualDeploymentAssemblyContext ctx)
   {
      if(ctx instanceof BasicVirtualAssemblyContext)
      {
         return createStructureMetaData(deployment, path, (BasicVirtualAssemblyContext) ctx);
      }
      return null;
   }
   
   protected ContextInfo createStructureMetaData(Deployment deployment, String path, BasicVirtualAssemblyContext ctx)
   {
      List<String> metaDataLocations = ctx.getMetaDataLocations();
      List<ClassPathEntry> classPathEntries = getClassPathEntries(ctx);
      
      ContextInfo info = deploymentFactory.addContext(deployment, path, metaDataLocations, classPathEntries);
      if(ctx.getChildren() != null && ctx.getChildren().isEmpty() == false)
      {
         for(VirtualDeploymentAssemblyContext child : ctx.getChildren())
         {
            String childPath = child.getRoot().getName();
            createStructureMetaData(deployment, childPath, child);
         }
      }
      return info;
   }
   
   protected List<ClassPathEntry> getClassPathEntries(BasicVirtualAssemblyContext ctx)
   {
      List<ClassPathEntry> entries = new ArrayList<ClassPathEntry>();
      if(ctx.getClassPathLocations() != null && ctx.getClassPathLocations().isEmpty() == false)
      {
         for(String s : ctx.getClassPathLocations())
         {
            entries.add(DeploymentFactory.createClassPathEntry(s));
         }
      }
      // Add the root
      entries.add(DeploymentFactory.createClassPathEntry(""));
      return entries;
   }
   
}

