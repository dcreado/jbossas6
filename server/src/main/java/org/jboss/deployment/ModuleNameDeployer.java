/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010 Red Hat, Inc. and individual contributors
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
package org.jboss.deployment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.metadata.ear.spec.ModulesMetaData;
import org.jboss.metadata.javaee.jboss.NamedModule;

/**
 * Ensures that EE modules have names that comply with the requirements of
 * EE 6 Section EE.8.1.1.
 * 
 * @author Brian Stansberry
 * @version $Revision: 102186 $
 */
public class ModuleNameDeployer extends AbstractDeployer
{
   private final Set<Class<? extends NamedModule>> inputTypes;
   
   /**
    * Creates a new ModuleNameDeployer
    */
   public ModuleNameDeployer(Set<Class<? extends NamedModule>> inputTypes)
   {
      if (inputTypes == null)
      {
         throw new IllegalArgumentException("inputTypes cannot be null");
      }
      
      // We want to run after any scanning-based metadata is done
      setStage(DeploymentStages.PRE_REAL);
      addInput(JBossAppMetaData.class);
      addOutput(JBossAppMetaData.class);
      addOutput(NamedModule.class);
      for (Class<? extends NamedModule> type : inputTypes)
      {
         addInput(type);
         addOutput(type);
      }
      this.inputTypes = inputTypes;
   }

   /* (non-Javadoc)
    * @see org.jboss.deployers.spi.deployer.Deployer#deploy(org.jboss.deployers.structure.spi.DeploymentUnit)
    */
   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      // Find all the metadata types assoc w/ unit that specify a moduleName
      List<NamedModule> metadatas = null;
      for (Class<? extends NamedModule> type : inputTypes)
      {
         NamedModule nm = unit.getAttachment(type);
         if (nm != null)
         {
            if (metadatas == null)
            {
               metadatas = new ArrayList<NamedModule>();
            }
            metadatas.add(nm);
         }
      }
      
      if (metadatas == null)
      {
         return;
      }
      
      String moduleName = null;
      
      // See if any of the existing metadata specifies a module name
      for (NamedModule nm : metadatas)
      {
         moduleName = nm.getModuleName();
         if (moduleName != null)
         {
            break;
         }
      }
      
      // Enforce EAR rules from EE 6 EE.8.1.1
      JBossAppMetaData appMetaData = null;
      DeploymentUnit parent = unit.getParent();
      if (parent != null)
      {
         appMetaData = parent.getAttachment(JBossAppMetaData.class);
      }      
      if (appMetaData != null)
      {         
         moduleName = establishUniqueModuleName(unit, moduleName, appMetaData);
      }
      else if (moduleName == null)
      {
         // Not in an EAR and no name was configured. 
         // Create a default module name as per EE 6 EE.8.1.2:
         // "when a stand-alone module is deployed....  [t]he module name can be 
         // explicitly set in the module deployment descriptor. If not set, the 
         // name of the module is the base name of the module file with any 
         // extension (.war, .jar, .rar) removed and with any directory names removed."
         moduleName = trimExtension(unit.getSimpleName());
      }
      
      // Apply the name to all metadata
      for (NamedModule nm : metadatas)
      {
         nm.setModuleName(moduleName);
      }
      
      // Let other deployers get the module name w/o having to search
      // for all the possible metadata types
      unit.addAttachment(NamedModule.class, metadatas.get(0));
   }
   
   private String establishUniqueModuleName(DeploymentUnit unit, String configuredName, JBossAppMetaData appMetaData) throws DeploymentException
   {      
      String name = configuredName == null ? trimExtension(unit.getRelativePath()): configuredName;
      
      String modulePath = unit.getRelativePath();
      ModulesMetaData modules = appMetaData.getModules();
      if (modules == null)
      {
         throw new DeploymentException(unit + " has attached " + JBossAppMetaData.class.getSimpleName() + 
               " but it has no associated " + ModulesMetaData.class.getSimpleName());
      }
      ModuleMetaData ourModule = null;
      String uniqueName = null;
      // Synchronize on the modules to ensure concurrent deployments of the
      // modules pass serially through this logic
      synchronized(modules)
      {
         ourModule = modules.get(modulePath);
         if (ourModule == null)
         {
            String parentUnitName = unit.getParent().getName();
            throw new DeploymentException("No module with relative path " + 
                  modulePath + " found  in set of modules for " + parentUnitName + " "  + modules.keySet());
         }
         
          uniqueName = name;
          if (!isNameUnique(uniqueName, ourModule, modules))
          {
             // Try the relative path w/ extension removed
             uniqueName = trimExtension(unit.getRelativePath());
             if (uniqueName.equals(name) || !isNameUnique(uniqueName, ourModule, modules))
             {
                // Try leaving the extension
                uniqueName = unit.getRelativePath();
                if (!isNameUnique(uniqueName, ourModule, modules))
                {
                   // To get here, people would have to configure in xml a 
                   // module name that conflicts with the relative path of
                   // another module. Not likely, but...
                   // Append a digit until the name is unique
                   int i = 0;
                   do
                   {
                      i++;
                      uniqueName = name + "-" + i;
                   }
                   while (!isNameUnique(uniqueName, ourModule, modules));
                }
             }
          }
          
          ourModule.setUniqueName(uniqueName);
         
      }
      
      // Log a WARN if we had to change the module name
      if (configuredName != null && !configuredName.equals(uniqueName))
      {
         log.warn("Module name " + configuredName + " specified in deployment descriptor for " + unit + " was not unique within the application; using module name " + uniqueName + " instead");
      }
      else if (!name.equals(uniqueName))
      {
         log.warn("Module name " + name + " derived from the modules relative path in " + unit + " was not unique within the application; using module name " + uniqueName + " instead");
      }
      
      return uniqueName;
   }
   
   private static boolean isNameUnique(String uniqueName, ModuleMetaData ourModule, ModulesMetaData modules)
   {
      for (Iterator<ModuleMetaData> it = modules.iterator(); it.hasNext(); )
      {
         ModuleMetaData module = it.next();
         if (!ourModule.equals(module) && uniqueName.equals(module.getUniqueName()))
         {
            return false;
         }
      }
      return true;
   }

   private static String trimExtension(String path)
   {
      int lastDot = path.lastIndexOf('.');
      return (lastDot > -1) ? path.substring(0, lastDot) : path;
   }

}
