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
package org.jboss.weld.integration.deployer.metadata;

import java.util.Collection;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloader.plugins.filter.CombiningClassFilter;
import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.vfs.VirtualFile;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyObject;

/**
 * Filter on Javassist' MethodHandler and ProxyObject.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ExcludeAppJavassistDeployer extends WeldAwareMetadataDeployer<ClassLoadingMetaData>
{
   private String PO = ClassLoaderUtils.classNameToPath(ProxyObject.class);
   private String MH = ClassLoaderUtils.classNameToPath(MethodHandler.class);
   private ClassFilter excludeFilter;

   public ExcludeAppJavassistDeployer()
   {
      super(ClassLoadingMetaData.class, true);
      setStage(DeploymentStages.PRE_DESCRIBE);
      excludeFilter = new ClassFilter()
      {
         public boolean matchesClassName(String className)
         {
            return (ProxyObject.class.getName().equals(className) || MethodHandler.class.getName().equals(className));
         }

         public boolean matchesResourcePath(String resourcePath)
         {
            return (PO.equals(resourcePath) || MH.equals(resourcePath));
         }

         public boolean matchesPackageName(String packageName)
         {
            return false;
         }
      };
   }

   protected void internalDeploy(VFSDeploymentUnit unit, ClassLoadingMetaData deployment, Collection<VirtualFile> wbXml) throws DeploymentException
   {
      String excludedPackages = deployment.getExcludedPackages();
      if (excludedPackages != null)
         return; // we have explicit set of package excludes

      ClassFilter excluded = deployment.getExcluded();
      if (excluded == null)
      {
         deployment.setExcluded(excludeFilter);
      }
      else
      {
         deployment.setExcluded(new CombiningClassFilter(false, new ClassFilter[]{excluded, excludeFilter}));
      }
   }
}