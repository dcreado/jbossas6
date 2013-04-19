/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
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
package org.jboss.as.naming.javaee;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEComponentInformer;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class NamingJavaEEComponentInformer extends NamingJavaEEModuleInformer implements JavaEEComponentInformer
{
   private static final String[] REQUIRED_ATTACHMENTS = { JBossEnterpriseBeanMetaData.class.getName(), JBossServletMetaData.class.getName() };

   public String getComponentName(DeploymentUnit unit)
   {
      // FIXME: it's real ugly to analyze the deployment unit at this stage. Better to let the ComponentNamingDeployer be explicitly driven by meta data.
      JBossEnterpriseBeanMetaData ejb = unit.getAttachment(JBossEnterpriseBeanMetaData.class);
      JBossServletMetaData servlet = unit.getAttachment(JBossServletMetaData.class);
      assert ejb != null || servlet != null : "borked deployment unit " + unit;
      if(ejb != null)
         return ejb.getEjbName();
      if(servlet != null)
         return servlet.getServletName();
      throw new IllegalStateException("Deployment unit " + unit + " has no known component meta data");
   }

   @Override
   public String[] getRequiredAttachments()
   {
      return concat(super.getRequiredAttachments(), REQUIRED_ATTACHMENTS);
   }

   public boolean isJavaEEComponent(DeploymentUnit unit)
   {
      return unit.isAttachmentPresent(JBossEnterpriseBeanMetaData.class) || unit.isAttachmentPresent(JBossServletMetaData.class);
   }
   
   @Override
   public boolean belongsToWebModule(DeploymentUnit deploymentUnit)
   {
      return deploymentUnit.isAttachmentPresent(JBossWebMetaData.class);
   }
}
