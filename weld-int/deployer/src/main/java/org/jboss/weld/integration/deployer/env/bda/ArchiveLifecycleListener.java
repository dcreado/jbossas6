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

import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;


/**
 * Observes the lifecyle of a BeanDeploymentArchive.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @version $Revision: 1.1 
 * @see Archive#addLifecycleListener(BDALifecycleListener)
 */
interface ArchiveLifecycleListener
{
   /**
    * Notifies this listener that an Archive is visible to Weld classes in the form of
    * a BDA.
    * 
    * @param archive the archive whose corresponding BDA was created
    * @param bda     a Weld spi view that represents {@code archive}
    */
   public void archiveVisible(Archive archive, BeanDeploymentArchive bda);
   
   /**
    * Notifies that {@code archive} is being destroyed.
    * 
    * @param archive the archive
    */
   public void archiveDestroyed(Archive archive);
}
