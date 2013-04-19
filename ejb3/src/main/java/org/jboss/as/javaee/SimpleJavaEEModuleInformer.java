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
import org.jboss.jpa.javaee.JavaEEModuleInformer;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: 108013 $
 */
public class SimpleJavaEEModuleInformer implements JavaEEModuleInformer
{
   // TODO: wire via SPI or deprecated this construct in favor of a generic JavaEE module informer
   private SimpleJavaEEModuleIdentifier identifier = new SimpleJavaEEModuleIdentifier();
   
   public String getApplicationName(DeploymentUnit unit)
   {
      DeploymentUnit topLevel = unit.getTopLevel();
      if(topLevel.isAttachmentPresent(JBossAppMetaData.class))
         return topLevel.getSimpleName();
      return null;
   }

   public String getModulePath(DeploymentUnit unit)
   {
      // first check if this a JavaEE Application (i.e. if it's a .ear).
      // If yes, then return the relative path (i.e. module name) of the unit 
      // relative to the .ear 
      DeploymentUnit topLevel = unit.getTopLevel();
      if(topLevel.isAttachmentPresent(JBossAppMetaData.class))
      {
         return unit.getRelativePath();
      }
      // if it's not a JavaEE application (i.e. not a .ear), then
      // return the simple name of the unit
      return unit.getSimpleName(); 
   }

   public ModuleType getModuleType(DeploymentUnit unit)
   {
      org.jboss.reloaded.naming.deployers.javaee.JavaEEModuleInformer.ModuleType type = identifier.getModuleType(unit);
      switch(type)
      {
         case APP_CLIENT:
            return ModuleType.APP_CLIENT;
         case EJB:
            return ModuleType.EJB;
         case WEB:
            return ModuleType.WEB;
         default:
            return ModuleType.JAVA;
      }
   }
}
