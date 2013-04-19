/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc, and individual contributors
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
package org.jboss.web.tomcat.service.ondemand;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.mapper.Mapper;
import org.apache.tomcat.util.http.mapper.OnDemandContextMappingListener;

/**
 * Default implementation of OnDemandContextIntegrator.
 * 
 * @author Brian Stansberry
 * @version $Revision: 1.1 $
 */
public class DefaultOnDemandContextIntegrator implements OnDemandContextIntegrator
{
   private static final Logger log = Logger.getLogger(DefaultOnDemandContextIntegrator.class);
   
   private final Set<ContextDemandAdapter> adapters = Collections.synchronizedSet(new HashSet<ContextDemandAdapter>());

   private final  Map<String, Mapper> mappers = Collections.synchronizedMap(new HashMap<String, Mapper>());
   
   // -----------------------------------------------------------------  Public
   
   /**
    * Makes this object aware of the given {@link Mapper}, allowing on-demand
    * contexts to be registered with it.
    * 
    * @param serviceName name of the JBoss Web <code>Service</code> with
    *                    which the service is associated. Cannot be <code>null</code>
    * @param mapper the mapper. Cannot be <code>null</code>
    */
   public void registerMapper(String serviceName, Mapper mapper)
   {
      if (serviceName == null)
      {
         throw new IllegalArgumentException("Argument serviceName is null");
      }
      if (mapper == null)
      {
         throw new IllegalArgumentException("Argument mapper is null");
      }
      mappers.put(serviceName, mapper);
      ContextDemandAdapter adapter = new ContextDemandAdapter(serviceName);
      mapper.registerOnDemandContextMappingListener(adapter);
      adapters.add(adapter);
   }
   
   // ------------------------------------------------  OnDemandContextListener
   
   public void registerContextDemandListener(ContextDemandListener listener)
   {
      for (ContextDemandAdapter adapter : adapters)
      {
         adapter.registerContextDemandListener(listener);
      }
   }
   
   public void removeContextDemandListener(ContextDemandListener listener)
   {
      for (ContextDemandAdapter adapter : adapters)
      {
         adapter.removeContextDemandListener(listener);
      }
   }
   
   public void registerOnDemandContext(String serviceName, String hostName, String contextName)
   {
      Mapper mapper = mappers.get(serviceName);
      if (mapper != null)
      {
         // ensure the path starts w/ slash or Mapper won't match it correctly
         if (contextName.length() > 0 && ('/' != contextName.charAt(0)))
         {
            contextName = "/" + contextName;
         }
         mapper.addOnDemandContext(hostName, contextName);
      }
      else
      {
         log.warn("Cannot register on-demand context for unknown engine " + serviceName);
      }
   }
   
   public void removeOnDemandContext(String serviceName, String hostName, String contextName)
   {
      Mapper mapper = mappers.get(serviceName);
      if (mapper != null)
      {
         mapper.removeContext(hostName, contextName);
      }
      else
      {
         log.warn("Cannot remove on-demand context for unknown engine " + serviceName);
      }
   }
   
   private class ContextDemandAdapter implements OnDemandContextMappingListener
   {
      private final String engineName;
      private final Set<ContextDemandListener> contextDemandListeners = 
          Collections.synchronizedSet(new HashSet<ContextDemandListener>());

      private ContextDemandAdapter(String engineName)
      {
         this.engineName = engineName;
      }

      public void onDemandContextMapped(String hostName, String contextName)
      {
         for (ContextDemandListener listener : contextDemandListeners)
         {
            listener.contextDemanded(this.engineName, hostName, contextName);
         }         
      }
      
      private void registerContextDemandListener(ContextDemandListener listener)
      {
         if (listener != null )
         {
            contextDemandListeners.add(listener);
         }
      }
      
      private void removeContextDemandListener(ContextDemandListener listener)
      {
         if (listener != null )
         {
            contextDemandListeners.remove(listener);
         }
      }
   }
}