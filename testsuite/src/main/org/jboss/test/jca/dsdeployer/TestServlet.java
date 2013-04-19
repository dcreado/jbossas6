package org.jboss.test.jca.dsdeployer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

@DataSourceDefinitions({
		@DataSourceDefinition(name="WebAnnotDS", className="org.hsqldb.jdbcDriver", url="jdbc:hsqldb:mem:JCADataSourceDeployerDB", user="sa"),
		@DataSourceDefinition(name="WebOverrideDS", className="org.hsqldb.jdbcDriver", url="jdbc:hsqldb:mem:JCADataSourceDeployerDB", user="sa", transactional=false)		
}
)

@WebServlet(value="/test", name="TestServlet")
public class TestServlet extends HttpServlet
{
	
	@Resource(mappedName="java:WebAnnotDS")
	private DataSource webAnnotDS;
	
	@Resource(mappedName="java:DSWebXML")
	private DataSource webXMLDS;
	
	@Resource(mappedName="java:DSEARXML")
	private DataSource earXMLDS;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		Connection c = null;
		Statement s = null;
		ResultSet rs = null;
		
		try
		{
			s = c.createStatement();
		}
		catch(Exception e)
		{
			throw new ServletException(e.getMessage(), e);
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
