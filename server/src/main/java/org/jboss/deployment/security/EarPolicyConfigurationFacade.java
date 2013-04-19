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

import org.jboss.metadata.ear.jboss.JBossAppMetaData;

//$Id: EarPolicyConfigurationFacade.java 82920 2009-01-15 17:29:45Z pgier $

/**
 *  A facade for constructing Permissions into the PolicyConfiguration
 *  @author Anil.Saldhana@redhat.com
 *  @since  Feb 18, 2008 
 *  @version $Revision: 82920 $
 */
public class EarPolicyConfigurationFacade<T extends JBossAppMetaData>
extends PolicyConfigurationFacade<JBossAppMetaData>
{ 
   public EarPolicyConfigurationFacade(String id, T md)
   {
      super(id, md); 
   }

   @Override
   protected void createPermissions(JBossAppMetaData metaData, 
         PolicyConfiguration policyConfiguration) throws PolicyContextException
   {
      return; //No need for permissions 
   } 
}