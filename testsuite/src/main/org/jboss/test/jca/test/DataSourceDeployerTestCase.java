package org.jboss.test.jca.test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

import junit.framework.Test;

import org.jboss.test.JBossTestCase;
import org.jboss.test.jca.support.JCATestSupport;

public class DataSourceDeployerTestCase extends JBossTestCase 
{
	private static final String EARXMLDS = "DSEARXML";
	private static final String DRIVEREARXML = "DriverEARXML";
	private static final String XADSEARXML = "XADSEARXML";
	
	private static final String WEBANNOTDS = "WebAnnotDS";
	private static final String WEBXMLDS = "DSWebXML";
	private static final String WEBOVRDS = "WebOverrideDS";
	
	
	public DataSourceDeployerTestCase(String name) 
	{
		super(name);		
	}

	public void testBasicDeployment() throws Exception
	{
		try
		{
			deploy("jca-mockdriver.jar");
			Properties props = getConnectionProperties(EARXMLDS);
	        assertTrue(props.containsKey("serverName") && props.containsKey("databaseName") && props.containsKey("portNumber"));
	        assertTrue(deploymentUsesDataSource(EARXMLDS));        
	        
	        props.clear();
	        
	        props = getConnectionProperties(DRIVEREARXML);
	        assertTrue(props.size() == 0);
	        assertFalse(deploymentUsesDataSource(DRIVEREARXML));
			
	        props.clear();
	        props = getConnectionProperties(XADSEARXML, true);
	        assertTrue("XADataSourceProperties should have been set", props.size() > 0);
	        
	        props.clear();
	        props = getConnectionProperties(WEBOVRDS, true);
	        assertTrue("XADataSourceProperties should have been set on overriden datasource", props.size() > 0);
	        
	        
	        
	                                        
			
		}
		catch(Exception e)
		{
			throw e;
		}
		finally
		{
			undeploy("jca-mockdriver.jar");
			
		}
	}

	
	public static Test suite() throws Exception 
	{
		//getDeploySetup(DataSourceDeployerTestCase.class, "jca-mockdriver.jar");
		Test t1 = getDeploySetup(DataSourceDeployerTestCase.class, "jca-dsdeployer-test.ear");
		return t1;
		
	}

	private Object getMCFAttribute(String name, String attribute, Class<?> type) throws Exception
	{
		return invoke(JCATestSupport.getMCFForDeployment(name), "getManagedConnectionFactoryAttribute", new Object[]{attribute}, new String[]{type.getName()});
	}
	
	private Properties getXAConnectionProperties(String name) throws Exception
	{
		return getConnectionProperties(name, true);
	}
	private Properties getConnectionProperties(String name) throws Exception
	{
		return getConnectionProperties(name, false);
	}

	private Properties getConnectionProperties(String name, boolean xa) throws Exception
	{
		String strResults = null;
		
		if(xa)
		{
			strResults = (String)getMCFAttribute(name, "XADataSourceProperties", String.class);
			
		}
		else
		{
			strResults = (String)getMCFAttribute(name, "ConnectionProperties", String.class);

		}
		
		log.debug("Received properties results from server fomr name " + name + "  " + strResults);
		
		Properties props = new Properties();
		System.out.println(strResults);
		
		if(strResults != null)
		{
			strResults = strResults.replaceAll("\\\\", "\\\\\\\\");
	        System.out.println(strResults);
	        InputStream is = new ByteArrayInputStream(strResults.getBytes());
	        props.load(is); 
			
		}

		return props;
	}
	
	private boolean deploymentUsesDataSource(String name) throws Exception
	{
		Boolean results = (Boolean)getMCFAttribute(name, "UseDataSource", String.class); 
		String url = (String)getMCFAttribute(name, "ConnectionURL", String.class);
		return (results != null && results && url == null) ? Boolean.TRUE : Boolean.FALSE;
	}

}
