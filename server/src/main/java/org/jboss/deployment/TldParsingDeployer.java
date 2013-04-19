/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.deployment;

import org.jboss.deployers.spi.structure.MetaDataTypeFilter;
import org.jboss.deployers.vfs.spi.deployer.SchemaResolverDeployer;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.vfs.VirtualFile;

/**
 * An ObjectModelFactoryDeployer for translating tag library descriptors into
 * TldMetaData instances.
 * 
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision: 82920 $
 */
public class TldParsingDeployer extends SchemaResolverDeployer<TldMetaData>
{
   /**
    * Create a new TldParsingDeployer.
    */
   public TldParsingDeployer()
   {
      super(TldMetaData.class);
      setSuffix(".tld");
      setAllowMultipleFiles(true);
      setFilter(MetaDataTypeFilter.ALL);
   }
   
   protected void init(VFSDeploymentUnit unit, TldMetaData metaData, VirtualFile file) throws Exception
   {
      unit.addAttachment(TldMetaData.class.getName() + ":" + file.getPathNameRelativeTo(unit.getRoot()), metaData, getOutput());
   }

   protected TldMetaData parse(VirtualFile file) throws Exception {
      if (file == null)
         throw new IllegalArgumentException("Null file");

      // Implicit TLDs are reserved as the "implicit.tld" name
      if (file.getName().equals("implicit.tld")) {
         return new TldMetaData();
      } else {
         return super.parse(file);
      }
   }
}
