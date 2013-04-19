package org.jboss.test.jca.dsdeployer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.sql.DataSource;

@DataSourceDefinition(name="java:EJBAnnotDS", className="org.hsqldb.jdbcDriver", url="jdbc:hsqldb:mem:JCADataSourceDeployerDB")
@Stateless
@Local(TestEJBLocal.class)
public class TestEJB implements TestEJBLocal, TestEJBRemote 
{

//	@Resource(mappedName="java:EJBAnnotDS")
	private DataSource dataSource;
	
	@Override
	public void testDataSource() throws Exception 
	{
		Connection c = null;
		Statement s = null;
		ResultSet rs = null;
		
		try
		{
			//c = dataSource.getConnection();
			s = c.createStatement();
			rs = s.executeQuery("select * from iqserver");
						
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw e;
		}
		finally
		{
			try {
				if(rs != null)
				{
					rs.close();
				}
				if(s != null)
				{
					s.close();
				}
				if(c != null)
				{
					c.close();
				}
			} catch (SQLException ignore) 
			{
			}
		}
	}
}
