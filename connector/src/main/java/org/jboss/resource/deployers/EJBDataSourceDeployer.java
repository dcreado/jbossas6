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
package org.jboss.resource.deployers;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.spec.InterceptorMetaData;
import org.jboss.metadata.ejb.spec.InterceptorsMetaData;
import org.jboss.metadata.javaee.spec.DataSourcesMetaData;

/**
 * Deployer for processing @DataSourceDefinition/data-source for EJB3 deployments
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class EJBDataSourceDeployer extends AbstractDataSourceDeployer
{

   public EJBDataSourceDeployer()
   {
      this.setInput(JBossEnterpriseBeanMetaData.class);
      this.setComponentsOnly(true);
   }
   
   @Override
   protected DataSourcesMetaData getDataSources(DeploymentUnit unit)
   {
      JBossEnterpriseBeanMetaData enterpriseBean = unit.getAttachment(JBossEnterpriseBeanMetaData.class);
      
      if(!enterpriseBean.getJBossMetaData().isEJB31())
      {
         return null;
      }
      DataSourcesMetaData dataSources = new DataSourcesMetaData();
      // datasources on the EJB
      if (enterpriseBean.getDataSources() != null)
      {
         dataSources.addAll(enterpriseBean.getDataSources());
      }
      // datasources on the interceptors of the EJB
      InterceptorsMetaData interceptors = JBossMetaData.getInterceptors(enterpriseBean.getEjbName(), enterpriseBean.getJBossMetaData());
      if (interceptors != null)
      {
         for (InterceptorMetaData interceptor : interceptors)
         {
            if (interceptor == null || interceptor.getDataSources() == null)
            {
               continue;
            }
            dataSources.addAll(interceptor.getDataSources());
         }
      }
      
      return dataSources;
   }

}
