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
package org.jboss.weld.integration.util;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.common.deployers.spi.AttachmentNames;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;



/**
 * @author Marius Bogoevici
 */
public abstract class EjbDiscoveryUtils
{
   public static Collection<String> getVisibleEJbNames(DeploymentUnit du)
   {
      // Ensure it's an EJB3 DU (by looking for the processed metadata)
      List<String> ejbNames = new ArrayList<String>();
      if (du.getAttachment(AttachmentNames.PROCESSED_METADATA, JBossMetaData.class) != null && du.getAttachment(JBossMetaData.class).isEJB3x())
      {
         JBossMetaData jBossMetaData = du.getAttachment(JBossMetaData.class);
         for (JBossEnterpriseBeanMetaData enterpriseBeanMetaData : jBossMetaData.getEnterpriseBeans())
         {
            ejbNames.add(enterpriseBeanMetaData.getEjbName());
         }
      }

      List<DeploymentUnit> children = du.getChildren();
      if (children != null && children.isEmpty() == false)
      {
         //scan children, but exclude wars
         for (DeploymentUnit childDu : children)
         {
            if (childDu.getAttachment(JBossWebMetaData.class) == null)
            {
               ejbNames.addAll(getVisibleEJbNames(childDu));
            }
         }
      }

      return ejbNames;
   }
}
