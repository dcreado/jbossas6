/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.weld.integration.deployer.jndi;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEModuleInformer;
import org.jboss.switchboard.impl.resource.IndependentResource;
import org.jboss.switchboard.impl.resource.LinkRefResource;
import org.jboss.switchboard.javaee.environment.BeanManagerRefType;
import org.jboss.switchboard.mc.spi.MCBasedResourceProvider;
import org.jboss.switchboard.spi.Resource;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.util.JndiUtils;

public class BeanManagerResourceProvider implements MCBasedResourceProvider<BeanManagerRefType>
{

   JavaEEModuleInformer moduleInformer;


   public void setModuleInformer(JavaEEModuleInformer moduleInformer)
   {
      this.moduleInformer = moduleInformer;
   }

   public Resource provide(DeploymentUnit deploymentUnit, BeanManagerRefType type)
   {
      // TODO Auto-generated method stub
      if (deploymentUnit.getAttachment(DeployersUtils.WELD_FILES) != null)
      {
         return new LinkRefResource(JndiUtils.getGlobalBeanManagerPath(moduleInformer, deploymentUnit));
      }
      else
      {
         return new IndependentResource(null);
      }
   }

   public Class<BeanManagerRefType> getEnvironmentEntryType()
   {
      return BeanManagerRefType.class;
   }

}
