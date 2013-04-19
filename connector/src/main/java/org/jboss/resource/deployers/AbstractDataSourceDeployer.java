/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.resource.deployers;

import java.util.ArrayList;
import java.util.Collection;

import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.DataSourceMetaData;
import org.jboss.metadata.javaee.spec.DataSourcesMetaData;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEComponentInformer;
import org.jboss.resource.metadata.mcf.ConnectionPoolMetaData;
import org.jboss.resource.metadata.mcf.DataSourceDeploymentMetaData;
import org.jboss.resource.metadata.mcf.ManagedConnectionFactoryDeploymentGroup;

/**
 * The DataSourceDeployer is designed to satisfy the EE6 specification
 * requirement 5.17 where data-sources can be defined in the application.xml,
 * ejb-jar.xml, web.xml and application-client.xml descriptors. The
 * DataSourceDeployer simply reads the appropriate instances of meta data and
 * translates each to the appropriate ManagedConnectionFactoryDeployment type which is in turn
 * passed to the ManagedConnectionFactoryDeployer for real deployment.
 * 
 * @author Weston M. Price
 * 
 * 
 */
public abstract class AbstractDataSourceDeployer extends AbstractDeployer {

	private static final Logger logger = Logger.getLogger(AbstractDataSourceDeployer.class);
	
	private JavaEEComponentInformer informer;
	
	public AbstractDataSourceDeployer() {
		setStage(DeploymentStages.REAL);		
		this.setOutput(ManagedConnectionFactoryDeploymentGroup.class);
	}
		

	@Inject
	public void setJavaEEComponentInformer(JavaEEComponentInformer informer)
	{
		this.informer = informer;
	}
	
	@Override
	public void deploy(DeploymentUnit unit) throws DeploymentException 
	{
		DataSourcesMetaData dsmd = this.getDataSources(unit);
		if (dsmd == null)
		{
		   return;
		}
		Collection<DataSourceDeploymentMetaData> dataSourceDeployments = getDataSourceDeployments(dsmd, unit);
		this.attachManagedConnectionFactories(unit, dataSourceDeployments);  
	}
	
	protected abstract DataSourcesMetaData getDataSources(DeploymentUnit unit);

	private Collection<DataSourceDeploymentMetaData> getDataSourceDeployments(DataSourcesMetaData dataSources, DeploymentUnit unit) 
	{
		Collection<DataSourceDeploymentMetaData> datasourceDeployments = new ArrayList<DataSourceDeploymentMetaData>();
				
		for(String key: dataSources.keySet())
		{
			DataSourceMetaData dsmd = dataSources.get(key);
			DataSourceDeploymentMetaData deploymentMetaData = DataSourceDeployerHelper.createDeployment(dsmd);
			deploymentMetaData.setJndiName(DataSourceDeployerHelper.normalizeJndiName(dsmd, unit, informer));
			datasourceDeployments.add(deploymentMetaData);						
			
		}
						
		return datasourceDeployments;
	}
	
	private void attachManagedConnectionFactories(DeploymentUnit unit, Collection<DataSourceDeploymentMetaData> datasourceDeployments)
	{
	   DeploymentUnit nonComponentDU =  unit.isComponent() ? unit.getParent() : unit;
	   ManagedConnectionFactoryDeploymentGroup managedConnectionFactories = nonComponentDU.getAttachment(ManagedConnectionFactoryDeploymentGroup.class);
       if (managedConnectionFactories == null)
       {
          managedConnectionFactories = new ManagedConnectionFactoryDeploymentGroup();
       }
       
       for (DataSourceDeploymentMetaData dataSourceDeployment : datasourceDeployments)
       {
          managedConnectionFactories.addManagedConnectionFactoryDeployment(dataSourceDeployment);
       }
       nonComponentDU.addAttachment(ManagedConnectionFactoryDeploymentGroup.class, managedConnectionFactories);
	}

	
}
