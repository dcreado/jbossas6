package org.jboss.test.jca.dsdeployer;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

public class MockDataSource implements DataSource, XADataSource {

	private String serverName;
	private String databaseName;
	private int portNumber;
	
	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return null;
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return 0;
	}

	@Override
	public void setLogWriter(PrintWriter arg0) throws SQLException {

	}

	@Override
	public void setLoginTimeout(int arg0) throws SQLException {

	}

	@Override
	public boolean isWrapperFor(Class<?> arg0) throws SQLException {
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> arg0) throws SQLException {
		return null;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return new MockConnection();
	}

	@Override
	public Connection getConnection(String arg0, String arg1)
			throws SQLException {
		return new MockConnection();
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public int getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	@Override
	public XAConnection getXAConnection() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public XAConnection getXAConnection(String arg0, String arg1)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	
	
	
	
}
