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
package org.jboss.jmx.adaptor.snmp.agent;

import java.net.InetAddress;

import javax.management.MBeanServer;

import org.jboss.logging.Logger;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.smi.OctetString;

/**
 * Implement RequestHandler with simple tracing of incoming requests.
 * 
 * Derived classes are expected to implement actual behaviour.
 * 
 * @author <a href="mailto:krishnaraj@ieee.org">Krishnaraj S</a>
 * @author <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 * @version $Revision: 112094 $
 */
public abstract class RequestHandlerSupport implements RequestHandler
{
   // Protected Data ------------------------------------------------
   
   /** Logger object */
   protected Logger log;
   
   /** the MBeanServer */
   protected MBeanServer server;
   
   /** the file name to get mapping info from */
   protected String resourceName;
   
   /** the agent clock */
   protected Clock clock;
   
   // Constructors --------------------------------------------------
   
   /**
    * Default CTOR
    */
   public RequestHandlerSupport()
   {
      // empty
   }
   
   // RequestHandler Implementation ---------------------------------
   
   /**
    * Initialize
    */
   public void initialize(String resourceName, MBeanServer server, Logger log, Clock uptime)
      throws Exception
   {
      this.resourceName = resourceName;
      this.server = server;
      this.log = log;
      this.clock = uptime;
   }
}
