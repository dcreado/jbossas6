/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.web.tomcat.security;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.logging.Logger;
import org.jboss.security.plugins.HostThreadLocal; 

/** 
 This valve gets the remote host and sticks it in a thread local.  It is 
 to be used with RemoteHostTrustLoginModule in particular

 @author Andrew C. Oliver (acoliver@gmail.com)
 @version $Revision: 0 $
 */
public class RemoteHostValve extends ValveBase
{
	private static Logger log = Logger.getLogger(RemoteHostValve.class);
	private boolean trace = log.isTraceEnabled();

	public void invoke(Request request, Response response)
	throws IOException, ServletException
	{ 
		String remoteHost = request.getRemoteHost();
		if(trace) 
		{
			log.trace("RemoteHostValve set remoteHost to be "+remoteHost);
		} 
		
		HostThreadLocal.set(remoteHost);
		getNext().invoke(request, response);
	}
}