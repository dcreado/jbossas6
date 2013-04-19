package org.jboss.test.jca.support;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

public class JCATestSupport 
{
	private static MBeanServerConnection server;
	
	private JCATestSupport(MBeanServerConnection server)
	{
		this.server = server;
	}
	
	public static JCATestSupport getInstance(MBeanServerConnection server)
	{
		return new JCATestSupport(server);
	}
	public static ObjectName getMCFForDeployment(final String name) throws Exception
	{
		return new ObjectName("jboss.jca:name=" + name + ",service=ManagedConnectionFactory");
	}
	
	public static ObjectName getPoolForDeployment(final String name) throws Exception
	{
		return new ObjectName("jboss.jca:name=" + name + ",service=ManagedConnectionPool");
		
	}
	
}
