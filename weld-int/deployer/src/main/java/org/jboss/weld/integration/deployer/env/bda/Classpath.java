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
import java.util.Iterator;

import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;

/**
 * An archive classpath keeps track of the archives that are visible to an archive.
 * It also works on BDA level, i.e., it can tell which BDAs are visible to a specific BDA.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @version $Revision: 107075 $
 */
public interface Classpath extends Iterable<Archive>
{
   /**
    * Returns a name that identifies this classpath.
    * 
    * @return the name of this classpath
    */
   public String getName();

   /**
    * Adds an archive to this classpath. When an archive is added to a classpath, the
    * corresponding BeanDeploymentArchive, when available, is automatically part of this
    * classpath as well.
    * 
    * @param archive an archive
    */
   public void addArchive(Archive archive);

   /**
    * Returns an iterator over the set of archives contained in this classpath. 
    * 
    * @return an iterator for iterating over all archives that are part of this classpath
    */
   Iterator<Archive> iterator();
   
   /**
    * Returns the parent classpath of this classpath.
    * 
    * @return the parent classpath of this classpath 
    */
   Classpath getClasspath();

   /**
    * Returns the collection of all BeanDeploymentArchive instances reachable from 
    * {@code bda}.
    * 
    * @param bda the BeanDeploymentArchive that is performing this query
    * @return    a collection of all BDAs visible to {@code bda}
    */
   public Collection<BeanDeploymentArchive> getBDAs(BeanDeploymentArchive bda);
}
