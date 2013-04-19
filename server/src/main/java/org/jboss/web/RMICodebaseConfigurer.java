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

import org.jboss.system.ServiceMBeanSupport;

/**
 * Sets system property java.rmi.server.codebase to a URL derived from injected
 * values. Hack temporary fix for https://jira.jboss.org/jira/browse/JBAS-7274 
 * intended to be deployed early in the AS boot, but after ServiceBindingManager
 * (i.e. in conf/jboss-service.xml).
 *
 * @author Brian Stansberry
 * 
 * @version $Revision: 100443 $
 * 
 * @deprecated will be removed before JBoss AS 6.0.0.CR1
 */
public class RMICodebaseConfigurer extends ServiceMBeanSupport implements RMICodebaseConfigurerMBean
{
   private String host;
   private int port = -1;
   
   public String getCodebaseHost()
   {
      return host;
   }

   public int getCodebasePort()
   {
      return port;
   }

   public void setCodebaseHost(String host)
   {
      this.host = host;
   }

   public void setCodebasePort(int port)
   {
      this.port = port;
   }

   /**
    * Sets property java.rmi.server.codebase if not already set
    */
   @Override
   protected void startService() throws Exception
   {
      // Set the rmi codebase if it is not already set
      String codebase = System.getProperty("java.rmi.server.codebase");
      if (codebase == null)
      {
         if (host != null)
         {
            codebase = "http://" + getHostPortion() + getPortPortion() + "/";
            System.setProperty("java.rmi.server.codebase", codebase);
         }
         else
         {
            getLog().warn("Property codebaseHost has not been set; cannot set java.rmi.server.codebase");
         }
      }
   }

   private String getHostPortion()
   {
      if (host.indexOf(':') > -1 && host.indexOf('[') != 0)
      {
         return "[" + host + "]";
      }
      return host;
   }
   
   private String getPortPortion()
   {
      if (port > 0)
      {
         return ":" + port;
      }
      return "";
   }

   
}
