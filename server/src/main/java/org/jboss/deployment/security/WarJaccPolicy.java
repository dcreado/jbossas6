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
package org.jboss.deployment.security;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyContextException;

import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.web.WebPermissionMapping;

//$Id: WarJaccPolicy.java 82920 2009-01-15 17:29:45Z pgier $

/**
 *  Top level Jacc Policy For WARs
 *  @author Anil.Saldhana@redhat.com
 *  @since  Feb 18, 2008 
 *  @version $Revision: 82920 $
 */
public class WarJaccPolicy extends JaccPolicy<JBossWebMetaData>
{ 
   public WarJaccPolicy(String id)
   {
      super(id); 
   }
      
   public WarJaccPolicy(String id, JBossWebMetaData metaData, Boolean standaloneDeployment)
   {
      super(id, metaData, standaloneDeployment); 
   }

   @Override
   protected void createPermissions(JBossWebMetaData metaData, 
         PolicyConfiguration policyConfiguration) 
   throws PolicyContextException
   {
      WebPermissionMapping.createPermissions(metaData, policyConfiguration); 
   } 
}
