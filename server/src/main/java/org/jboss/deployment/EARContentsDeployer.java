/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.deployment;

import java.io.IOException;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.ear.jboss.JBoss50AppMetaData;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.jboss.ServiceModuleMetaData;
import org.jboss.metadata.ear.spec.*;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * An implicit application.xml type of deployer. This deployer runs if
 * there is no META-INF/application.xml to identify 
 * {@link #scanEar(VFSDeploymentUnit, VirtualFile, JBossAppMetaData)}
 *
 * @author Bill Burke
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @author ales.justin@jboss.org
 * @version $Revision: 108661 $
 */
public class EARContentsDeployer extends AbstractDeployer
{
   /** Does the deployment have to end in .ear to process it */
   private boolean requiresEarSuffix = false;

   /**
    * Create the EARContentsDeployer and register as a DeploymentStage.PARSE
    * stage deployer with JBossAppMetaData output.
    *
    * We need to run after AppParsingDeployer.
    */
   public EARContentsDeployer()
   {
      setStage(DeploymentStages.PARSE);
      addInput(EarMetaData.class);
      addOutput(JBossAppMetaData.class);
      setTopLevelOnly(true);
   }

   public boolean isRequiresEarSuffix()
   {
      return requiresEarSuffix;
   }
   public void setRequiresEarSuffix(boolean requiresEarSuffix)
   {
      this.requiresEarSuffix = requiresEarSuffix;
   }

   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      /* If there is a META-INF/application.xml we don't process this. */
      if (unit.isAttachmentPresent(EarMetaData.class))
      {
         log.tracef("Ignoring ear with META-INF/application.xml: %1s", unit.getSimpleName());
         return;
      }

      // Ignore non-vfs deployments
      if (unit instanceof VFSDeploymentUnit == false)
      {
         log.tracef("Not a vfs deployment: %1s", unit.getName());
         return;
      }
      // See if the suffix matches the .ear requirement
      if (requiresEarSuffix && unit.getSimpleName().endsWith(".ear") == false)
      {
         log.tracef("Unit name does not end in .ear: %1s", unit.getSimpleName());
         return;
      }

      VFSDeploymentUnit vfsunit = VFSDeploymentUnit.class.cast(unit);
      deploy(vfsunit);
   }

   /**
    * Entry point for handling a VFSDeploymentUnit.
    *
    * @param unit the current deployment unit
    * @throws DeploymentException for any error
    */
   public void deploy(VFSDeploymentUnit unit) throws DeploymentException
   {
      VirtualFile root = unit.getRoot();
      String relativePath = unit.getRelativePath();
      VirtualFile ear = unit.getFile(relativePath);
      if (ear == null)
         throw new DeploymentException("No such ear file, relative path: '" + relativePath + "', root: " + root);

      deploy(unit, root, ear);
   }

   /**
    * Process the ear VFSDeploymentUnit.
    * 
    * @param unit - the parent VFSDeploymentUnit for the ear
    * @param root - the deployment VFS root
    * @param file - the deployment ear VF
    */
   protected void deploy(VFSDeploymentUnit unit, VirtualFile root, VirtualFile file)
   {
      try
      {
         JBossAppMetaData j2eeMetaData = new JBoss50AppMetaData();
         // TODO: need to scan for annotationss
         scanEar(unit, file, j2eeMetaData);

         unit.addAttachment(JBossAppMetaData.class, j2eeMetaData);
      }
      catch(Exception e)
      {
         throw new RuntimeException("Error determining ear contents: " + file.getName(), e);         
      }
   }

   /**
   For an ear without an application.xml, determine modules via:
   a. All ear modules with an extension of .war are considered web modules. The
    context root of the web module is the name of the file relative to the root
    of the application package, with the .war extension removed.
   b. All ear modules with extension of .rar are considered resource adapters.
   c. A directory named lib is considered to be the library directory, as
    described in Section�EE.8.2.1, �Bundled Libraries.�
   d. For all ear modules with a filename extension of .jar, but not in the lib
    directory, do the following:
   i. If the JAR file contains a META-INF/MANIFEST.MF file with a Main-Class
    attribute, or contains a META-INF/application-client.xml file, consider the
    jar file to be an application client module.
   ii. If the JAR file contains a META-INF/ejb-jar.xml file, or contains any
   class with an EJB component annotation (Stateless, etc.), consider the JAR
    file to be an EJB module.
   iii. All other JAR files are ignored unless referenced by a JAR file
    discovered above using one of the JAR file reference mechanisms such as the
    Class-Path header in a manifest file.
    */
   private void scanEar(VFSDeploymentUnit unit, VirtualFile root, JBossAppMetaData j2eeMetaData)
      throws IOException
   {
      List<VirtualFile> archives = root.getChildren();
      if (archives != null)
      {
         String earPath = root.getPathName();
         ModulesMetaData modules = j2eeMetaData.getModules();
         if (modules == null)
         {
            modules = new ModulesMetaData();
            j2eeMetaData.setModules(modules);
         }
         for (VirtualFile vfArchive : archives)
         {
            String filename = earRelativePath(earPath, vfArchive.getPathName());
            // Check if the module already exists, i.e. it is declared in jboss-app.xml 
            ModuleMetaData moduleMetaData = j2eeMetaData.getModule(filename);
            int type = typeFromSuffix(unit, filename, vfArchive);
            if (type >= 0 && moduleMetaData == null)
            {
               moduleMetaData = new ModuleMetaData();
               AbstractModule module = null;
               switch(type)
               {
                  case J2eeModuleMetaData.EJB:
                     module = new EjbModuleMetaData();
                     break;
                  case J2eeModuleMetaData.CLIENT:
                     module = new JavaModuleMetaData();
                     break;
                  case J2eeModuleMetaData.CONNECTOR:
                     module = new ConnectorModuleMetaData();
                     break;
                  case J2eeModuleMetaData.SERVICE:
                  case J2eeModuleMetaData.HAR:
                     module = new ServiceModuleMetaData();
                     break;
                  case J2eeModuleMetaData.WEB:
                     module = new WebModuleMetaData();
                     break;
               }
               module.setFileName(filename);
               moduleMetaData.setValue(module);
               modules.add(moduleMetaData);
            }
         }
      }
   }

   private int typeFromSuffix(VFSDeploymentUnit unit, String path, VirtualFile archive)
      throws IOException
   {
      int type = -1;
      if( path.endsWith(".war") )
         type = J2eeModuleMetaData.WEB;
      else if( path.endsWith(".rar") )
         type = J2eeModuleMetaData.CONNECTOR;
      else if( path.endsWith(".har") )
         type = J2eeModuleMetaData.HAR;
      else if( path.endsWith(".sar") )
         type = J2eeModuleMetaData.SERVICE;
      else if( path.endsWith(".jar") )
      {
         // Look for a META-INF/application-client.xml
         VirtualFile mfFile = unit.getMetaDataFile("MANIFEST.MF");
         VirtualFile clientXml = unit.getMetaDataFile("application-client.xml");
         VirtualFile ejbXml = unit.getMetaDataFile("ejb-jar.xml");
         VirtualFile jbossXml = unit.getMetaDataFile("jboss.xml");

         if( clientXml != null )
         {
            type = J2eeModuleMetaData.CLIENT;
         }
         else if( mfFile != null )
         {
            Manifest mf = VFSUtils.readManifest(mfFile);
            Attributes attrs = mf.getMainAttributes();
            if( attrs.containsKey(Attributes.Name.MAIN_CLASS) )
            {
               type = J2eeModuleMetaData.CLIENT;
            }
            else
            {
               // TODO: scan for annotations. Assume EJB for now
               type = J2eeModuleMetaData.EJB;
            }
         }
         else if( ejbXml != null || jbossXml != null )
         {
            type = J2eeModuleMetaData.EJB;
         }
         else
         {
            // TODO: scan for annotations. Assume EJB for now
            type = J2eeModuleMetaData.EJB;
         }
      }
      
      return type;
   }

   private String earRelativePath(String earPath, String pathName)
   {
      StringBuilder tmp = new StringBuilder(pathName);
      tmp.delete(0, earPath.length() +  1);
      return tmp.toString();
   }

}
