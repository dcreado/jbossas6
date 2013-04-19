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
package org.jboss.system.server.profileservice.persistence.deployer;

import java.io.IOException;
import java.util.List;

import org.jboss.deployers.spi.structure.ContextInfo;
import org.jboss.deployers.spi.structure.MetaDataEntry;
import org.jboss.deployers.spi.structure.StructureMetaData;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.util.SuffixMatchFilter;

/**
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision$
 */
public class PersistenceModificationChecker
{

   /** The logger. */
   private static final Logger log = Logger.getLogger(PersistenceModificationChecker.class);

   /** The filter. */
   private static VirtualFileFilter filter = new SuffixMatchFilter(".xml");

   public static boolean hasBeenModified(VFSDeploymentUnit unit, long lastModified) throws Exception
   {
      VirtualFile root = unit.getRoot();
      if (root.isFile())
      {
         if(root.getLastModified() > lastModified)
            return true;
      }

      StructureMetaData structureMetaData = unit.getAttachment(StructureMetaData.class);
      if(structureMetaData == null)
         return false;

      ContextInfo info = structureMetaData.getContext(unit.getSimpleName());
      if(info == null && unit.isTopLevel())
         info = structureMetaData.getContext("");

      if(info == null)
         return false;

      return hasBeenModifed(root, info, lastModified);
   }

   protected static boolean hasBeenModifed(VirtualFile root, ContextInfo contextInfo, long lastModified) throws IOException
   {
      List<MetaDataEntry> metadataPaths = contextInfo.getMetaDataPath();
      if (metadataPaths != null && metadataPaths.isEmpty() == false)
      {
         for (MetaDataEntry metaDataPath : metadataPaths)
         {
            VirtualFile mdpVF = root.getChild(metaDataPath.getPath());
            if (mdpVF.exists())
            {
               List<VirtualFile> children = mdpVF.getChildren(filter);
               if (children != null && children.isEmpty() == false)
               {
                  for (VirtualFile child : children)
                  {
                     if (child.getLastModified() > lastModified)
                     {
                        if (log.isTraceEnabled())
                           log.trace("Metadata location modified: " + child);
                        return true;
                     }
                  }
               }
            }
         }
      }
      return false;
   }
}

