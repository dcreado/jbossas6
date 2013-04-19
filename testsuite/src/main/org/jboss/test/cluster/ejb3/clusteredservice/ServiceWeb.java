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

import javax.jws.WebMethod; 
import javax.jws.WebService; 
import javax.jws.soap.SOAPBinding; 
import javax.naming.InitialContext; 
import javax.naming.NamingException; 
import javax.naming.Context; 
import javax.xml.rpc.ServiceException; 
import javax.xml.rpc.server.ServiceLifecycle; 
import javax.rmi.PortableRemoteObject; 
import java.util.Properties; 

/**
 * @version <tt>$Revision: 108925 $</tt>
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@WebService 
@SOAPBinding(style = SOAPBinding.Style.RPC) 
public class ServiceWeb implements ServiceLifecycle
{ 
   ServiceRemote service; 

   public ServiceWeb() {} 

   @WebMethod(operationName = "RemoteMethod") 
   public void remoteMethod() { 
      System.out.println("ServiceWeb.remoteMethod"); 
      try
      { 
         service.remoteMethod(); 
      } catch (Exception e)
      {
         e.printStackTrace(); 
      } 
   } 

   private InitialContext getContext() throws NamingException{ 
      Properties p = new Properties(); 
      p.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory"); 
      p.put(Context.URL_PKG_PREFIXES, "jboss.naming:org.jnp.interfaces"); 
      p.put("jnp.partitionName", "HASingletonPartition"); 
      return new InitialContext(p); 
   } 

   public void init(Object object) throws ServiceException { 
      try
      { 
         service = (ServiceRemote) PortableRemoteObject.narrow( 
               getContext().lookup("ServiceBean/remote"), ServiceRemote.class); 
      } catch (NamingException e)
      { 
         e.printStackTrace(); 
         throw new ServiceException("Could not find Service in JNDI service", e); 
      } 
   } 

   public void destroy() { 
   } 

}
