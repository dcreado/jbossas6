/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.weld.integration.deployer.scanning;

import java.net.URL;
import java.util.Collection;

import org.jboss.classloading.spi.visitor.ClassFilter;
import org.jboss.classloading.spi.visitor.ResourceContext;
import org.jboss.classloading.spi.visitor.ResourceFilter;
import org.jboss.classloading.spi.visitor.ResourceVisitor;
import org.jboss.scanning.plugins.helpers.VoidScanningHandle;
import org.jboss.scanning.spi.helpers.AbstractScanningPlugin;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.integration.deployer.env.WeldDiscoveryEnvironment;

/**
 * Weld scanning plugin.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class WeldScanningPlugin extends AbstractScanningPlugin<VoidScanningHandle, Object>
{
   private ResourceVisitor visitor;
   private Collection<VirtualFile> cpFiles;

   public WeldScanningPlugin(WeldDiscoveryEnvironment environment, Collection<VirtualFile> cpFiles)
   {
      this.visitor = new WBDiscoveryVisitor(environment);
      this.cpFiles = cpFiles;
   }

   @Override
   protected VoidScanningHandle doCreateHandle()
   {
      return VoidScanningHandle.INSTANCE;
   }

   public Class<Object> getHandleInterface()
   {
      return Object.class;
   }

   @Override
   public void cleanupHandle(Object handle)
   {
      visitor = null;
   }

   @Override
   public ResourceFilter getRecurseFilter()
   {
      return new ResourceFilter()
      {
         public boolean accepts(ResourceContext resource)
         {
            try
            {
               URL url = resource.getUrl();
               VirtualFile file = VFS.getChild(url.toURI());
               VirtualFile[] parents = file.getParentFiles();
               // we're going from the end, from API the first element is the leafmost.
               for (VirtualFile parent : parents)
               {
                  // is our resource part of weld's classpath
                  if (cpFiles.contains(parent))
                     return true;
               }
               return false;
            }
            catch (Exception e)
            {
               throw new RuntimeException(e);
            }
         }
      };
   }

   public ResourceFilter getFilter()
   {
      return visitor.getFilter();
   }

   public void visit(ResourceContext resource)
   {
      visitor.visit(resource);
   }

   private static class WBDiscoveryVisitor implements ResourceVisitor
   {
      private WeldDiscoveryEnvironment env;

      private WBDiscoveryVisitor(WeldDiscoveryEnvironment wbdi)
      {
         this.env = wbdi;
      }

      public ResourceFilter getFilter()
      {
         return ClassFilter.INSTANCE;
      }

      public void visit(ResourceContext resource)
      {
         env.addWeldClass(resource.getClassName());
      }
   }
}