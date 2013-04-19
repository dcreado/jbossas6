/*
* JBoss, Home of Professional Open Source
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
package org.jboss.ejb.deployers;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.ejb.jboss.JBossMessageDrivenBeanMetaData;

/**
 * CreateDestinationFactory.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 102506 $
 */
public interface CreateDestinationFactory
{
   /**
    * Return the attachment to create the destination
    * 
    * @param unit the unit
    * @param mdb the mdb
    * @return the attachment
    * @throws DeploymentException for any error
    */
    boolean create(DeploymentUnit unit, JBossMessageDrivenBeanMetaData mdb) throws DeploymentException;

    /**
     * Return the Output class used by this factory.
     * The DestinationFactory may choose to deploy other objects instead of calling the deployment directly.
     * @return
     */
    Class<?> getOutput();
}