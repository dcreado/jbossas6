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
package org.jboss.test.deployers.support.deployer;

import java.net.URL;
import java.util.Collections;
import java.util.Set;

import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.deployer.cl.WeldUrlIntegrationDeployer;
import org.jboss.weld.integration.deployer.ext.JBossWeldMetaData;

/**
 * Mock war classloader deployer.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class MockUrlIntegrationDeployer extends WeldUrlIntegrationDeployer<JBossWeldMetaData>
{
   public MockUrlIntegrationDeployer()
   {
      super(JBossWeldMetaData.class);
   }

   protected String getShortLibName()
   {
      return "<ignore>";
   }

   protected boolean isIntegrationDeploymentInternal(VFSDeploymentUnit unit)
   {
      //  This is a copy from PathUrlIntegrationDeployer

      String[] files = getFiles();
      if (files == null || files.length == 0)
         return false;

      for(String path : files)
      {
         VirtualFile vf = unit.getFile(path);
         if (vf != null)
            return true;
      }

      return false;
   }

   @Override
   protected Set<URL> getURLs()
   {
      try
      {
         URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
         return Collections.singleton(url);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }
}