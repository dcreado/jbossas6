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

import org.jboss.as.javaee.SimpleJavaEEModuleIdentifier;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEModuleInformer;

import java.lang.reflect.Array;

import static org.jboss.as.naming.javaee.NamingJavaEEUtil.stripSuffix;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class NamingJavaEEModuleInformer extends NamingJavaEEApplicationInformer implements JavaEEModuleInformer
{
   // TODO: for now we delegate to the former JPA SPI informer. This informer should be merged with that one into one integration component
   private SimpleJavaEEModuleIdentifier identifier = new JavaEEModuleIdentifier();

   private String[] requiredAttachments;

   protected static <T> T[] concat(T[] array1, T[] array2)
   {
      if(array1 == null)
         return array2;
      if(array2 == null)
         return array1;
      T[] result = (T[]) Array.newInstance(array1.getClass().getComponentType(), array1.length + array2.length);
      System.arraycopy(array1, 0, result, 0, array1.length);
      System.arraycopy(array2, 0, result, array1.length, array2.length);
      return result;
   }

   public String getApplicationName(DeploymentUnit deploymentUnit)
   {
      return super.getApplicationName(deploymentUnit.getTopLevel());
   }

   public String getModuleName(DeploymentUnit deploymentUnit)
   {
      // FIXME this is a hack to give the CURRENT users of this class, 
      // ComponentNamingDeployer and ModuleNamingDeployer, the info they actually
      // want, rather than what they ask for. :) Tear this out once they
      // use the NamedModule DU attachment directly. 
      String moduleName = identifier.getModuleName(deploymentUnit);
      if (moduleName != null)
         return moduleName;
      /*
       * JavaEE 6 FR 8.1.1:
       * The name can be explicitly set in the deployment descriptor for the module. If not set, the name
       * of the module is the pathname of the module in the ear file with any filename extension (.jar, .war, .rar)
       * removed, but with any directory names included.
       */
      // TODO: get the module name from the deployment descriptor
      // FIXME: AS can't handle unique module names yet because we deploy a.jar and a.war at the same time, to counter don't strip the extension.
      String path = deploymentUnit.getRelativePath();
      if(path == null || path.length() == 0)
         path = deploymentUnit.getSimpleName();
      return stripSuffix(path);
   }

   public ModuleType getModuleType(DeploymentUnit deploymentUnit)
   {
      return identifier.getModuleType(deploymentUnit);
   }

   @Override
   public String[] getRequiredAttachments()
   {
      return requiredAttachments;
   }

   public void setJavaEEModuleIdentifier(SimpleJavaEEModuleIdentifier identifier)
   {
      this.identifier = identifier;
      this.requiredAttachments = concat(super.getRequiredAttachments(), identifier.getRequiredAttachments());
   }
   
}
