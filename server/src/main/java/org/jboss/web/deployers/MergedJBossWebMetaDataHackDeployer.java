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
package org.jboss.web.deployers;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb.deployers.MergedJBossMetaDataDeployer;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.javaee.spec.DataSourceMetaData;
import org.jboss.metadata.javaee.spec.DataSourcesMetaData;
import org.jboss.metadata.javaee.spec.Environment;
import org.jboss.metadata.web.jboss.JBossWebMetaData;

/**
 * As the name suggests this deployer isn't meant for any real use.
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
// As https://issues.jboss.org/browse/JBAS-8775 explains, the WarAnnotationMetaDataDeployer
// can be too aggressive while annotation scanning and subsequent metadata creation out of those
// annotations. This can lead to incorrect Environment metadata creation in JBossWebMetaData.
// Ultimately, the incorrect Environment leads to deployment failures. For more details refer
// to the forum post in that JIRA issue. 
// This deployer adds a hack to remove such incorrect metadata from the JBossWebMetaData's
// Environment.
public class MergedJBossWebMetaDataHackDeployer extends MergedJBossWebMetaDataDeployer
{

   public MergedJBossWebMetaDataHackDeployer()
   {
      // let the MergedJBossWebMetaDataDeployer add all the necessary
      // input/output. We just add MergedJBossMetaDataDeployer.EJB_MERGED_ATTACHMENT_NAME
      // as an input so that the ordering is right
      this.addInput(MergedJBossMetaDataDeployer.EJB_MERGED_ATTACHMENT_NAME);
   }
   
   /**
    * 
    */
   // We remove any DataSourceMetaData that was incorrectly created off an EJB's interceptor
   // and added to the JBossWebMetaData's Environment. Ideally, the EJB's interceptor class shouldn't really have 
   // been picked up by the WarAnnotationMetaDataDeployer
   @Override
   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      // let the MergedJBossWebMetaDataDeployer do all its usual work
      super.deploy(unit);
      // if we don't have any DataSource references in our ENC, then nothing to hack!
      JBossWebMetaData mergedJBossWebMetaData = unit.getAttachment(JBossWebMetaData.class);
      if (mergedJBossWebMetaData == null || !this.hasDataSources(mergedJBossWebMetaData))
      {
         return;
      }
      // we only hack if this web unit contains EJB deployments (i.e. Java EE6 shared ENC)
      if (!this.isSharedENC(unit))
      {
         return;
      }
      
      JBossMetaData jbossMetaData = unit.getAttachment(MergedJBossMetaDataDeployer.EJB_MERGED_ATTACHMENT_NAME, JBossMetaData.class);
      JBossEnterpriseBeansMetaData enterpriseBeans = jbossMetaData.getEnterpriseBeans();
      if (enterpriseBeans == null || enterpriseBeans.isEmpty())
      {
         // no beans, no hack!
         return;
      }
      
      // process each EJB
      for (JBossEnterpriseBeanMetaData enterpriseBean : enterpriseBeans)
      {
         this.removeCommonDataResourceReference(mergedJBossWebMetaData, enterpriseBean);
      }
      // attach the updated merged JBossWebMetaData to the unit
      unit.getTransientManagedObjects().addAttachment(JBossWebMetaData.class, mergedJBossWebMetaData);
      
   }
   
   /**
    * Returns true if the passed {@link DeploymentUnit} is a 3.0 web-app
    * and also contains EJBs within the unit.
    * <p/>
    * Java EE6 allows EJB deployment via .war files
    * 
    * @param unit
    * @return
    */
   private boolean isSharedENC(DeploymentUnit unit)
   {
      JBossWebMetaData jbossWeb = unit.getAttachment(JBossWebMetaData.class);
      // we are only interested in web-app 3.0 version which allows
      // deploying EJBs in .war
      if (jbossWeb == null || !jbossWeb.is30())
      {
         return false;
      }
      // now that we know it's a web-app 3.0, let's check if it contains EJBs
      return unit.isAttachmentPresent(JBossMetaData.class);
   }
   
   private void removeCommonDataResourceReference(JBossWebMetaData jbossWeb, Environment ejbJndiEnv)
   {
      Environment jbossWebEnv = jbossWeb.getJndiEnvironmentRefsGroup();
      if (jbossWebEnv == null || ejbJndiEnv == null)
      {
         return;
      }
      // datasource(s)
      DataSourcesMetaData jbossWebDataSources = jbossWebEnv.getDataSources();
      DataSourcesMetaData ejbCompDataSources = ejbJndiEnv.getDataSources();
      if (jbossWebDataSources != null && ejbCompDataSources != null)
      {
         for (DataSourceMetaData ejbCompDataSource : ejbCompDataSources)
         {
            if (ejbCompDataSource == null)
            {
               continue;
            }
            // remove from JBossWebMetaData environment, the datasource reference
            // which was picked up from an EJB
            boolean removed = jbossWebDataSources.remove(ejbCompDataSource);
            if (removed)
            {
               if (log.isTraceEnabled())
               {
                  log.trace("Removed data-source reference: " + ejbCompDataSource.getName()
                        + " from jbossweb metadata since the same reference is present in a EJB's jndi environment");
               }
            }
         }
      }
   }
   
   /**
    * Returns true if the passed {@link JBossWebMetaData} has atleast one {@link DataSourceMetaData}
    * in it's {@link Environment}. Else returns false.
    * 
    * @param jbossWebMetaData
    * @return
    */
   private boolean hasDataSources(JBossWebMetaData jbossWebMetaData)
   {
      Environment jbossWebEnv = jbossWebMetaData.getJndiEnvironmentRefsGroup();
      if (jbossWebEnv == null)
      {
         return false;
      }
      if(jbossWebEnv.getDataSources() == null || jbossWebEnv.getDataSources().isEmpty())
      {
         return false;
      }
      return true;
   }
   
}
