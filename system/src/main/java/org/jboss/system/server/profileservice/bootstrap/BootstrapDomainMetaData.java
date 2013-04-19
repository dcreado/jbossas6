/*
* JBoss, Home of Professional Open Source
* Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.system.server.profileservice.bootstrap;

import org.jboss.profileservice.domain.AbstractDomainMetaData;
import org.jboss.profileservice.domain.ManagementDomainMetaData;
import org.jboss.profileservice.domain.ServerMetaData;
import org.jboss.profileservice.domain.spi.DomainMetaData;
import org.jboss.profileservice.domain.spi.DomainMetaDataFragmentVisitor;

/**
 * @author Emanuel Muckenhuber
 */
public class BootstrapDomainMetaData extends AbstractDomainMetaData
{

   private DomainMetaData delegate;
   
   public BootstrapDomainMetaData(String domainName, String serverName, DomainMetaData delegate)
   {
      super();
      setDomain(new ManagementDomainMetaData(domainName));
      setServer(new ServerMetaData(serverName));
      this.delegate = delegate;
   }
   
   public void visit(DomainMetaDataFragmentVisitor visitor)
   {
      super.visit(visitor);
      this.delegate.visit(visitor);
   }
   
}

