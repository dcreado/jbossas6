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

package org.jboss.weld.integration.util;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEModuleInformer;

/**
 * @author Marius Bogoevici
 */
public class JndiUtils
{
   /**
    * The global path for bean managers is java:/global/cdi/<applicationName>/<moduleName>/BeanManager
    */
   public static final String BEAN_MANAGER_GLOBAL_SUBCONTEXT = "cdi";

   public static String getJndiSubcontexPathForBeanManager(JavaEEModuleInformer moduleInformer, DeploymentUnit unit)
   {
      String applicationName = moduleInformer.getApplicationName(unit);
      String path = (applicationName == null) ? "" : (applicationName + "/");
      path += moduleInformer.getModuleName(unit);
      return path;
   }

   public static String getGlobalBeanManagerPath(JavaEEModuleInformer moduleInformer, DeploymentUnit unit)
   {
      DeploymentUnit deploymentUnit = unit.isComponent() ? unit.getParent() : unit;
      String subcontexPathForBeanManager = getJndiSubcontexPathForBeanManager(moduleInformer, deploymentUnit);
      return "java:global/" + BEAN_MANAGER_GLOBAL_SUBCONTEXT + "/" + subcontexPathForBeanManager + "/BeanManager";
   }
}
