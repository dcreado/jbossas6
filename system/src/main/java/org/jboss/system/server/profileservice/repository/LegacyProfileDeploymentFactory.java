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
package org.jboss.system.server.profileservice.repository;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.profileservice.deployment.AbstractProfileDeployment;
import org.jboss.profileservice.deployment.ProfileDeploymentFactory;
import org.jboss.profileservice.spi.ProfileDeployment;
import org.jboss.vfs.VirtualFile;

/**
 * Basic ProfileDeploymentFactory.
 * 
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision: 109729 $
 */
public class LegacyProfileDeploymentFactory extends AutoUnmounter
{

   /** The instance. */
   private static final LegacyProfileDeploymentFactory INSTANCE = new LegacyProfileDeploymentFactory();

   /** The delegate. */
   private ProfileDeploymentFactory delegate = ProfileDeploymentFactory.getInstance();
   
   public static String createDeploymentName(VirtualFile original) throws URISyntaxException
   {
      return original.asFileURI().toString();
   }
   
   public static String createDeploymentName(URI uri)
   {
      // Always use the name without the "/"
      String name = uri.toString();
      if(name.endsWith("/"))
         return name.substring(0, name.length() -1);
      return name;
   }
   
   public static LegacyProfileDeploymentFactory getInstance()
   {
      return INSTANCE;
   }

   protected LegacyProfileDeploymentFactory()
   {
      //
   }
   
   public ProfileDeployment createProfileDeployment(String name)
   {
      return delegate.createDeployment(name);
   }
   
   @Deprecated
   public ProfileDeployment createProfileDeployment(VirtualFile vf) throws URISyntaxException 
   {
      String name = createDeploymentName(vf);
      return new WorkaroundProfileDeployment(name, vf);
   }
   
   public ProfileDeployment createProfileDeployment(String profileName, String name, VirtualFile original) throws IOException
   {
      VirtualFile copy = backup(profileName, name, original);
      return new WorkaroundProfileDeployment(name, copy);
   }

   /**
    * Cleanup the deployment
    */
   public void cleanup(String deploymentName) throws IOException
   {
      super.cleanup(deploymentName);
   }
 
   public class WorkaroundProfileDeployment extends AbstractProfileDeployment
   {

      protected WorkaroundProfileDeployment(String name, VirtualFile root)
      {
         super(name, ProfileDeploymentFactory.noopDeploymentMetaData);
         setRoot(root);
      }
      
   }
   
   
}

