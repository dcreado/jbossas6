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
package org.jboss.weld.integration.deployer;

import java.util.Collection;
import java.util.List;

import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.deployers.structure.spi.DeploymentUnit;

/**
 * Weld deployers utils.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public final class DeployersUtils
{
   public static final String JAVAX_VALIDATION_VALIDATOR_FACTORY = "javax.validation.ValidatorFactory";

   private DeployersUtils()
   {
   }

   public static final String WELD_FILES = "WELD_FILES";
   public static final String WELD_CLASSPATH = "WELD_CLASSPATH";
   public static final String WELD_DEPLOYMENT_FLAG = "WELD_DEPLOYMENT_FLAG";

   public static final String WELD_BOOTSTRAP_BEAN = "_WeldBootstrapBean";

   /**
    * Get bootstrap bean name.
    *
    * @param unit the deployment unit
    * @return weld bootstrap bean name
    */
   public static String getBootstrapBeanName(DeploymentUnit unit)
   {
      if (unit == null)
         throw new IllegalArgumentException("Null deployment unit");

      DeploymentUnit top = unit.getTopLevel();
      return top.getName() + WELD_BOOTSTRAP_BEAN;
   }

   /**
    * Get the name of the bootstrap bean deployer attachment.
    *
    * @param unit the deployment unit
    * @return the deployer attachment name of the bootstrap bean
    */
   public static String getBootstrapBeanAttachmentName(DeploymentUnit unit)
   {
      return getBootstrapBeanName(unit) + "_" + BeanMetaData.class.getSimpleName();
   }

   /**
    * Is the bootstrap bean present.
    *
    * @param unit the deployment unit
    * @return true if there is bootstrap bean in attachments, false otherwise
    */
   public static boolean isBootstrapBeanPresent(DeploymentUnit unit)
   {
      String attachmentName = getBootstrapBeanAttachmentName(unit);
      DeploymentUnit top = unit.getTopLevel();
      return top.isAttachmentPresent(attachmentName);
   }

   /**
    * Get the name of the weld deployment bean.
    *
    * @param unit The deployment unit
    * @return the weld deployment bean name
    */
   public static String getDeploymentBeanName(DeploymentUnit unit)
   {
      if (unit == null)
         throw new IllegalArgumentException("Null deployment unit");

      return unit.getName() + "_JBossDeployment";
   }

   /**
    * Get the name of the weld deployment deployers attachment.
    *
    * @param unit The deployment unit
    * @return the deployer attachment name of the weld deployment
    */
   public static String getDeploymentAttachmentName(DeploymentUnit unit)
   {
      return getDeploymentBeanName(unit);
   }

   /**
    * Check deployment hierarchy for beans.xml files.
    * It checks the 'cached' flag.
    *
    * @param unit the deployment unit
    * @return true if beans.xml files exist, false otherwise
    */
   public static boolean checkForWeldFilesAcrossDeployment(DeploymentUnit unit)
   {
      if (unit == null)
         throw new IllegalArgumentException("Null deployment unit");

      DeploymentUnit top = unit.getTopLevel();

      Boolean flag = top.getAttachment(WELD_DEPLOYMENT_FLAG, Boolean.class);
      if (flag != null)
         return flag;

      flag = checkForWeldFiles(top, true);
      top.addAttachment(WELD_DEPLOYMENT_FLAG, flag, Boolean.class);

      return flag;
   }

   /**
    * Search deployment hierarchy for beans.xml files.
    *
    * @param unit the deployment unit
    * @param includeChildren whether children should be searched as well
    * @return true if beans.xml files exist, false otherwise
    */
   public static boolean checkForWeldFiles(DeploymentUnit unit, boolean includeChildren)
   {
      Collection files = unit.getAttachment(WELD_FILES, Collection.class);
      if (files != null && files.isEmpty() == false)
         return true;

      if (includeChildren)
      {
         List<DeploymentUnit> children = unit.getChildren();
         if (children != null && children.isEmpty() == false)
         {
            for (DeploymentUnit child : children)
            {
               boolean result = checkForWeldFiles(child, true);
               if (result)
                  return true;
            }
         }
      }
      return false;
   }
}