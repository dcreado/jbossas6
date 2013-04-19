package org.jboss.resource.deployers;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.ear.spec.Ear60MetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.javaee.spec.DataSourcesMetaData;

/**
 * Deployer for processing data-source for EAR deployments.
 * @author Weston M. Price
 *
 */
public class EARDataSourceDeployer extends AbstractDataSourceDeployer 
{

	public EARDataSourceDeployer()
	{
		this.setInput(EarMetaData.class);
		this.setTopLevelOnly(true);
	}
	@Override
	protected DataSourcesMetaData getDataSources(DeploymentUnit unit) 
	{
		EarMetaData emd = unit.getAttachment(EarMetaData.class);
		
		if(emd != null && emd.isEE6() && ((Ear60MetaData)emd).getEarEnvironmentRefsGroup() != null )
		{
			return ((Ear60MetaData)emd).getEarEnvironmentRefsGroup().getDataSources();
		}
		
		return null;
	
	}

}
