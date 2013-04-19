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
package org.jboss.test.mdbdefaultraname.bean;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.logging.Logger;
import org.jboss.metadata.BeanMetaData;
import org.jboss.ejb.Container;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 82920 $
 */
public class TestStatusBean implements SessionBean
{
   private static final Logger log = Logger.getLogger(TestStatusBean.class.getName());
   
   public TestStatusBean() {}
   
   public Map<String, String> getResourceAdapterNames()
   {
      MBeanServer server = org.jboss.mx.util.MBeanServerLocator.locate();
      ObjectName ejbModuleName;
      try
      {
         ejbModuleName = new ObjectName("jboss.j2ee:service=EjbModule,module=\"mdbdefaultraname.jar\"");
      }
      catch(Exception e)
      {
         throw new IllegalStateException("Failed to create object name", e);
      }
      
      Collection<Container> containers;
      try
      {
         containers = (Collection<Container>) server.getAttribute(ejbModuleName, "Containers");
      }
      catch(Exception e)
      {
         throw new IllegalStateException("Failed to get attribute Containers", e);
      }
      
      if(containers == null)
         throw new IllegalStateException("the ejb module contains no containers");
      if(containers.size() != 3)
         throw new IllegalStateException("Expected one containers but got " + containers.size());
    
      Map<String, String> map = new HashMap<String, String>();
      for(Container container : containers)
      {
         BeanMetaData metadata = container.getBeanMetaData();
         if(!metadata.isMessageDriven())
            continue;

         String invokerBinding = null;
         Iterator<String> bindings = metadata.getInvokerBindings();
         if(!bindings.hasNext())
            throw new IllegalStateException("Expected one invoker binding.");
         while(bindings.hasNext())
         {
            if(invokerBinding != null)
               throw new IllegalStateException("Expected only one invoker binding.");
            invokerBinding = bindings.next();
         }

         MyJMSContainerInvoker invoker = (MyJMSContainerInvoker)container.lookupProxyFactory(invokerBinding);
         map.put(metadata.getEjbName(), invoker.getResourceAdapterName());
      }
      return map;
   }

   // Container callbacks -------------------------------------------
   
   public void setSessionContext(SessionContext ctx) {}
   public void ejbCreate() throws CreateException {}
   public void ejbRemove() {}
   public void ejbActivate() {}
   public void ejbPassivate() {}
}
