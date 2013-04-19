/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

import java.security.Principal;

import org.apache.catalina.Realm;
import org.apache.catalina.connector.Request;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.deploy.LoginConfig;

//$Id: ExtendedRealm.java 93046 2009-08-31 18:26:08Z remy.maucherat@jboss.com $

/**
 *  Realm interface that has the request/response messages
 *  available for authentication decisions
 *  @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 *  @since  May 24, 2006 
 *  @version $Revision: 93046 $
 */
public interface ExtendedRealm extends Realm
{
   public Principal authenticate(Request request, HttpServletResponse response, 
         LoginConfig loginConfig) throws Exception;
}