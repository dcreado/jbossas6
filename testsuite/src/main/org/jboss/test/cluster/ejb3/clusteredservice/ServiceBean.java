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
package org.jboss.test.cluster.ejb3.clusteredservice;

import javax.ejb.Local;
import javax.ejb.Remote;

import org.jboss.ejb3.annotation.Clustered;
import org.jboss.ejb3.annotation.Depends;
import org.jboss.ejb3.annotation.Management;
import org.jboss.ejb3.annotation.Service;
import org.jboss.logging.Logger;

/**
 * @version <tt>$Revision: 108925 $</tt>
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Service(objectName = "default:service=service")
@Clustered(partition = "DefaultPartition") 
@Remote(ServiceRemote.class)
@Local(ServiceLocal.class)
@Management(ServiceManagement.class)
@Depends("jboss.ha:service=HASingletonDeployer,type=BarrierControllerMBean")
public class ServiceBean implements ServiceLocal, ServiceRemote, ServiceManagement
{
   private static final Logger log = Logger.getLogger(ServiceBean.class);
   
   private int remoteAccessed = 0;
   
   public synchronized void localMethod()
   {
   }

   public synchronized void remoteMethod()
   {
      log.info("ACESSSED " + ++remoteAccessed);
   }

   public void create() throws Exception
   {
      log.info("CREATE");
   }

   public void start() throws Exception
   {
      log.info("START");
   }

   public void stop()
   {
      log.info("STOP");
   }

   public void destroy()
   {
      log.info("DESTROY");
   }

}
