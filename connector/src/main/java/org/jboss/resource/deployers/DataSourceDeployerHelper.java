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
import java.util.List;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.DataSourceMetaData;
import org.jboss.metadata.javaee.spec.PropertyMetaData;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEComponentInformer;
import org.jboss.resource.metadata.mcf.ConnectionPoolMetaData;
import org.jboss.resource.metadata.mcf.DataSourceConnectionPropertyMetaData;
import org.jboss.resource.metadata.mcf.DataSourceDeploymentMetaData;
import org.jboss.resource.metadata.mcf.LocalDataSourceDeploymentMetaData;
import org.jboss.resource.metadata.mcf.NoTxDataSourceDeploymentMetaData;
import org.jboss.resource.metadata.mcf.NonXADataSourceDeploymentMetaData;

/**
 * Simple helper class for generating DataSourceDeploymentMetaData from a DataSourceDefinition as provided by EE5.17.
 * @author Weston M. Price
 *
 */
public class DataSourceDeployerHelper {

	private static final Logger logger = Logger.getLogger(DataSourceDeployerHelper.class);

	private static final List<String> scopes = new ArrayList<String>();
	
	static
	{
		scopes.add("app");
		scopes.add("global");
		scopes.add("module");
		scopes.add("comp");
		
	}
	
   public static String normalizeJndiName(DataSourceMetaData dsmd, DeploymentUnit unit, JavaEEComponentInformer informer)
   {
      // If it starts with some namespace (like java:comp, java:module, java:app or java:global)
      // then return as is
      String refName = dsmd.getName();
      if (refName.startsWith("java:comp/") || refName.startsWith("java:module/") || refName.startsWith("java:app/")
            || refName.startsWith("java:global/"))
      {
         return normalizeJndiName(refName, unit, informer);
      }
      else
      {
         // the reference name *doesn't* start with any namespace. So prefix a "env" before the
         // name and return
         return normalizeJndiName("env/" + refName, unit, informer);
      }

   }
	
	public static String normalizeJndiName(String dsJndiName, DeploymentUnit unit, JavaEEComponentInformer informer)
    {
        String jndiName = normalizeJndiName(dsJndiName);
        StringBuffer internalJndiName = new StringBuffer();
        String dsName = jndiName;
        
        if(jndiName.indexOf("/") != -1 && scopes.contains(jndiName.substring(0, jndiName.indexOf("/"))))
        {                       
            dsName = jndiName.substring(jndiName.indexOf("/") + 1, jndiName.length());
        }
        internalJndiName.append("internal/" + informer.getApplicationName(unit) + "/" + informer.getModuleName(unit) + "/");
        
        if(informer.isJavaEEComponent(unit))
        {
            internalJndiName.append(informer.getComponentName(unit) + "/");
        }
                    
        return internalJndiName.append(dsName).toString();
        
    }
	
		
	public static DataSourceDeploymentMetaData createDeployment(final DataSourceMetaData dsmd)
	{
		NonXADataSourceDeploymentMetaData deploymentMetaData = null;
		
		if(dsmd.isTransactional())
		{
			deploymentMetaData= new LocalDataSourceDeploymentMetaData();
		}
		else
		{
			deploymentMetaData = new NoTxDataSourceDeploymentMetaData();
		}
		
		List<DataSourceConnectionPropertyMetaData> connectionProps = deploymentMetaData.getDataSourceConnectionProperties();

		boolean useUrl = true;

		String serverName = dsmd.getServerName();
		String dataBaseName = dsmd.getDatabaseName();
		int portNumber = dsmd.getPortNumber();
		String url = dsmd.getUrl();
		
		DataSourceConnectionPropertyMetaData connectionProp = null;
		
		if(serverName != null && !serverName.isEmpty())
		{
			useUrl = false;
			connectionProp = new DataSourceConnectionPropertyMetaData();
			connectionProp.setName("serverName");
			connectionProp.setValue(serverName);
			connectionProps.add(connectionProp);
		}
		
		if(dataBaseName != null && !dataBaseName.isEmpty())
		{
			useUrl = false;
			connectionProp = new DataSourceConnectionPropertyMetaData();
			connectionProp.setName("databaseName");
			connectionProp.setValue(dataBaseName);					
			connectionProps.add(connectionProp);
		}
		
		if(portNumber > -1)
		{
			useUrl = false;
			connectionProp = new DataSourceConnectionPropertyMetaData();
			connectionProp.setName("portNumber");
			connectionProp.setValue(String.valueOf(portNumber));					
			connectionProps.add(connectionProp);			
		}
		
		connectionProp = new DataSourceConnectionPropertyMetaData();
		connectionProp.setName("loginTimeout");
		connectionProp.setValue(String.valueOf(dsmd.getLoginTimeout()));
		connectionProps.add(connectionProp);
						
		if(useUrl)
		{
			deploymentMetaData.setConnectionUrl(url);
		}
		else
		{
			//Hack for EE5.17 we are going to use the new DataSource rar to manage data sources that do not specify a Url, or specify properties
			//within the deployment ie serverName, dataBaseName etc. This is largely to reduce the number of changes required to support
			//this particular section in the spec.
			deploymentMetaData.setUseDataSource(true);
			logger.debug("Creating data source using standard JDBC4 based properties.");
		}

		deploymentMetaData.setDriverClass(dsmd.getClassName());
		deploymentMetaData.setUserName(dsmd.getUser());
		deploymentMetaData.setPassWord(dsmd.getPassword());
		
		if(dsmd.getIsolationLevel() != null)
		{
			deploymentMetaData.setTransactionIsolation(dsmd.getIsolationLevel().toString());				
		}
		
		if(dsmd.getMaxStatements() > -1)
		{
			deploymentMetaData.setPreparedStatementCacheSize(dsmd.getMaxStatements());				
		}
		
		ConnectionPoolMetaData cpmd = (ConnectionPoolMetaData)deploymentMetaData;
		
		if(dsmd.getMinPoolSize() != -1)
		{
			cpmd.setMinSize(dsmd.getMinPoolSize());				
		}
		if(dsmd.getMaxPoolSize() != -1)
		{
			cpmd.setMaxSize(dsmd.getMaxPoolSize());				
		}

		cpmd.setIdleTimeoutMinutes(dsmd.getMaxIdleTime() / 60);			

		
		if(dsmd.getProperties() != null && dsmd.getProperties().keySet().size() > 0)
		{
			for(String key: dsmd.getProperties().keySet())
			{
				PropertyMetaData pmd = dsmd.getProperties().get(key);
				connectionProp = new DataSourceConnectionPropertyMetaData();
				connectionProp.setName(pmd.getName());
				connectionProp.setValue(pmd.getValue());
				connectionProps.add(connectionProp);
			}
			
		}
		
		
		return deploymentMetaData;
	}
	
	private static String normalizeJndiName(String jndiName)
	{

		if(jndiName != null)
		{
			if(jndiName.contains("java:"))
			{
				return jndiName.replace("java:", "").trim();
			}
		}
		
		return jndiName;
		
	}
	
}
