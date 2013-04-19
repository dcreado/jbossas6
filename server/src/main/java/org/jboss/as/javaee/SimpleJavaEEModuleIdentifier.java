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
package org.jboss.as.javaee;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.client.jboss.JBossClientMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossEntityBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.javaee.jboss.NamedModule;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEModuleInformer;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: 102511 $
 */
public class SimpleJavaEEModuleIdentifier
{
   // TODO: this should really be hooked via an incallback visitor which dynamically identifies the deployment unit. So that this class has no tech dependencies.

   private static final String[] REQUIRED_ATTACHMENTS = { JBossClientMetaData.class.getName(), JBossMetaData.class.getName(), JBossWebMetaData.class.getName(), NamedModule.class.getName() };

   public JavaEEModuleInformer.ModuleType getModuleType(DeploymentUnit unit)
   {
      if(unit.isAttachmentPresent(JBossClientMetaData.class))
         return JavaEEModuleInformer.ModuleType.APP_CLIENT;
      if(unit.isAttachmentPresent(JBossMetaData.class) && isReallyAnEjbDeployment(unit))
         return JavaEEModuleInformer.ModuleType.EJB;
      if(unit.isAttachmentPresent(JBossWebMetaData.class))
         return JavaEEModuleInformer.ModuleType.WEB;
      return JavaEEModuleInformer.ModuleType.JAVA;
   }
   
   public String getModuleName(DeploymentUnit unit)
   {
      NamedModule moduleName = unit.getAttachment(NamedModule.class);
      
      return moduleName == null ? null : moduleName.getModuleName(); 
      
   }

   public String[] getRequiredAttachments()
   {
      return REQUIRED_ATTACHMENTS;
   }

   /*
    * Some hacks to counter problems.
    */
   private boolean isReallyAnEjbDeployment(DeploymentUnit unit)
   {
      JBossMetaData metaData = unit.getAttachment(JBossMetaData.class);
      // JBMETA-69
      if(metaData.getEnterpriseBeans() == null || metaData.getEnterpriseBeans().size() == 0)
         return false;
      // JBMETA-70
      // The chance of a persistence unit being defined with couple of EJB entity beans is
      // pretty slim.
      for(JBossEnterpriseBeanMetaData bean : metaData.getEnterpriseBeans())
      {
         if(!(bean instanceof JBossEntityBeanMetaData))
            return true;
      }
      return false;
   }
}
