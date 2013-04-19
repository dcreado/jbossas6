package org.jboss.test.security.test.authorization.secured;

import java.net.*;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.test.JBossTestCase;
import org.jboss.test.JBossTestSetup;

/**
 * Test verifies that there is no jmx-console security baypass in secured profiles.
 * Reused test from JBPAPP-3952, JBPAPP-4160.
 *
 * @author bshim@redhat.com
 * @author rsvoboda@redhat.com
 */
public class HttpRequestJmxAuthenticationUnitTestCase extends JBossTestCase {
	
	private URL u;
	private HttpURLConnection con;
	private static final String GET = "GET";
	private static final String POST = "POST";
	private static final String HEAD = "HEAD";
	private static final String OPTIONS = "OPTIONS";
	private static final String PUT = "PUT";
	private static final String DELETE = "DELETE";
	private static final String TRACE = "TRACE"; 
	
	public HttpRequestJmxAuthenticationUnitTestCase(String name){
		super(name);
	}
	
	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite();
		suite.addTest(new TestSuite(HttpRequestJmxAuthenticationUnitTestCase.class));
		// Create an initializer for the test suite
		TestSetup wrapper = new JBossTestSetup(suite)
	      		{
         		@Override
        	 	protected void setUp() throws Exception
	         	{
            			super.setUp();
         		}

        	 	@Override
	         	protected void tearDown() throws Exception
         		{
        	    		super.tearDown();
        		}
      		};
      		return wrapper;
	}

	public void testGet() throws Exception {
		con.setRequestMethod(GET);
		con.connect();			
		assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, con.getResponseCode());
	}
	
	public void testPost() throws Exception {
		con.setRequestMethod(POST);
		con.connect();
		assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, con.getResponseCode());
	}
	
	public void testHead() throws Exception {
		con.setRequestMethod(HEAD);
		con.connect();			
		assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, con.getResponseCode());
	}
	
	public void testOptions() throws Exception {
		con.setRequestMethod(OPTIONS);
		con.connect();
		assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, con.getResponseCode());
	}
	
	public void testPut() throws Exception {
		con.setRequestMethod(PUT);
		con.connect();			
		assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, con.getResponseCode());
	}
	
	public void testTrace()  throws Exception {
		con.setRequestMethod(TRACE);
		con.connect();
                assertEquals(HttpURLConnection.HTTP_BAD_METHOD, con.getResponseCode());
	}
	
	public void testDelete()  throws Exception {
		con.setRequestMethod(DELETE);
		con.connect();
		assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, con.getResponseCode());
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		u = new URL("http://" + getServerHost() + ":8080/jmx-console");
		con = (HttpURLConnection) u.openConnection();
		try {
			con.setDoInput(true);
			con.setRequestProperty("Cookie","MODIFY ME IF NEEDED");
		} finally {
			con.disconnect();
		}
	}
	
	protected void tearDown(){
		if (con != null)
			con.disconnect();
	}
}
