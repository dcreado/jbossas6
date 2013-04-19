/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc. and individual contributors
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

package org.jboss.web;

import org.jboss.system.ServiceMBean;


/**
 * Standard MBean interface for RMICodebaseConfigurer
 *
 * @author Brian Stansberry
 * 
 * @version $Revision: 100443 $
 * 
 * @deprecated will be removed before JBoss AS 6.0.0.CR1
 */
public interface RMICodebaseConfigurerMBean extends ServiceMBean
{
   /** 
    * Gets the host portion of the codebase URL
    *
    * @return the address, or <code>null</code> if not set
    */
   String getCodebaseHost();
   
   /** 
    * Sets the host portion of the codebase URL
    *
    * @param host the host
    */
   void setCodebaseHost(String host);
   
   /**
    * Gets the port portion of the codebase URL
    *
    * @return the port, or -1 if not configured
    */
   int getCodebasePort();
   
   /**
    * Sets the port portion of the codebase URL
    *
    * @param port the port
    */
   void setCodebasePort(int port);
}
