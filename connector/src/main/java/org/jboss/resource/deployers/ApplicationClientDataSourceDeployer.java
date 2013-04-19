package org.jboss.resource.deployers;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.client.spec.ApplicationClientMetaData;
import org.jboss.metadata.javaee.spec.DataSourcesMetaData;

/**
 * 
 * @author Weston M. Price
 *
 */
public class ApplicationClientDataSourceDeployer extends AbstractDataSourceDeployer 
{
	public ApplicationClientDataSourceDeployer()
	{
		this.setInput(ApplicationClientMetaData.class);
	}

	@Override
	protected DataSourcesMetaData getDataSources(DeploymentUnit unit) 
	{		
		//TODOD we need a meta data upgrade to complete
		return null;
	}

}
