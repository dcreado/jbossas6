/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.naming.javaee;

import org.jboss.as.javaee.SimpleJavaEEModuleIdentifier;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.client.jboss.JBossClientMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEModuleInformer;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEModuleInformer.ModuleType;

/**
 * @see #getModuleType(DeploymentUnit) to see why we have this class instead
 * of just using {@link SimpleJavaEEModuleIdentifier}
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class JavaEEModuleIdentifier extends SimpleJavaEEModuleIdentifier
{

   /**
    * The original {@link SimpleJavaEEModuleIdentifier#getModuleType(DeploymentUnit)} has a hackish way of
    * identifying the Module type for a EJB deployment. That original implementation doesn't consider a unit
    * to be of type {@link ModuleType#EJB} if the deployment only contains (EJB2.x) entity beans.
    * <p>
    *    Unlike that original implementation, in this overridden implementation, we consider a unit to be
    *    of type {@link ModuleType#EJB} if it contains {@link JBossMetaData} attachment and we don't do any additional
    *    checks.
    * </p>
    */
   @Override
   public org.jboss.reloaded.naming.deployers.javaee.JavaEEModuleInformer.ModuleType getModuleType(DeploymentUnit unit) 
   {
      if(unit.isAttachmentPresent(JBossClientMetaData.class))
         return JavaEEModuleInformer.ModuleType.APP_CLIENT;
      if(unit.isAttachmentPresent(JBossMetaData.class))
         return JavaEEModuleInformer.ModuleType.EJB;
      if(unit.isAttachmentPresent(JBossWebMetaData.class))
         return JavaEEModuleInformer.ModuleType.WEB;
      return JavaEEModuleInformer.ModuleType.JAVA;
   }
}
