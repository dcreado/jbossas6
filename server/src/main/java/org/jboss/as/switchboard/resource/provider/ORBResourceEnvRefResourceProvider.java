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
package org.jboss.as.switchboard.resource.provider;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.switchboard.impl.resource.LinkRefResource;
import org.jboss.switchboard.javaee.jboss.environment.JBossResourceEnvRefType;
import org.jboss.switchboard.mc.spi.MCBasedResourceProvider;
import org.jboss.switchboard.spi.Resource;
import org.omg.CORBA.ORB;

/**
 * Provides {@link Resource} for resource-env-ref corresponding to type {@link ORB}
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class ORBResourceEnvRefResourceProvider implements MCBasedResourceProvider<JBossResourceEnvRefType>
{

   private String corbaJndiName;

   public ORBResourceEnvRefResourceProvider(String corbaJndiName)
   {
      if (corbaJndiName == null || corbaJndiName.trim().isEmpty())
      {
         throw new IllegalArgumentException("Corba JNDI name cannot be null or empty");
      }
      this.corbaJndiName = corbaJndiName;
   }

   @Override
   public Class<JBossResourceEnvRefType> getEnvironmentEntryType()
   {
      return JBossResourceEnvRefType.class;
   }

   /**
    * Returns a {@link Resource resource} for {@link ORB} resource-env-ref
    * 
    */
   @Override
   public Resource provide(DeploymentUnit context, JBossResourceEnvRefType type)
   {
      return new LinkRefResource(this.corbaJndiName);
   }

}
