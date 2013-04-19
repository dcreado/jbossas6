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
package org.jboss.test.deployers.spaces.test;

import java.io.File;
import java.net.URI;

import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.vfs.spi.client.VFSDeploymentFactory;
import org.jboss.test.deployers.OldAbstractDeploymentTest;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * A test that deploys everything in an EAR.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 101688 $
 */
public class SpacesUnitTestCase extends OldAbstractDeploymentTest
{
   public void testEARDeployment() throws Exception
   {
      // Fixup the uri and get a root context with spaces
      String deployDir = System.getProperty("jbosstest.deploy.dir");
      File file = new File(deployDir);
      file = new File(file, "dir with spaces");
      URI contextName = file.toURI();
      VirtualFile contextFile = VFS.getChild(contextName);
      
      // Create the deployment
      VirtualFile vf = contextFile.getChild("spaces.ear");
      assertNotNull(vf);
      Deployment deployment = VFSDeploymentFactory.getInstance().createVFSDeployment(vf);

      // Make sure we can deploy/undeploy it
      invoke(getDeployerName(), "deploy", new Object[]{ deployment }, new String[] { Deployment.class.getName() });
      invoke(getDeployerName(), "checkIncompleteDeployments", null, null);
      invoke(getDeployerName(), "undeploy", new Object[]{ deployment }, new String[] { Deployment.class.getName() });
   }
   
   public SpacesUnitTestCase(String test)
   {
      super(test);
   }
}
