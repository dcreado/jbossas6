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
package org.jboss.weld.integration.deployer.env.bda;

import java.util.Collection;

import org.jboss.weld.ejb.spi.EjbDescriptor;

/**
 * An archive factory.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @version $Revision: 108369 $
 * @see Archive
 */
class ArchiveFactory
{
   /**
    * Creates an Archive, and performs some initialization operations required for
    * consistency and cleanup of the ArchiveInfo.
    * <br>
    * This factory method can be called only when {@code ejbs} become
    * available. Prior to this stage, use {@link ArchiveInfo} to keep data during deployment.
    * 
    * @param archiveInfo contains all information necessary for the initialization of the
    *                    archive
    */
   public static Archive createArchive(ArchiveInfo archiveInfo, Collection<EjbDescriptor<?>> ejbs)
   {
      Archive archive = new Archive(archiveInfo, ejbs);
      if (archiveInfo.hasClasspathAdapter())
      {
         // initialize adapter
         ArchiveToClasspath classpathAdapter = archiveInfo.getClasspathAdapter();
         classpathAdapter.addArchive(archive);
      }
      // dispose archiveInfo
      archiveInfo.cleanUp();
      return archive;
   }
}
