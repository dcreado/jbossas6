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
package org.jboss.ha.framework.server;

import java.util.Map;
import java.util.Set;

import org.jboss.ha.core.channelfactory.ChannelInfo;
import org.jboss.ha.core.channelfactory.ProtocolStackConfigInfo;
import org.jboss.ha.framework.server.managed.OpenChannelsMapper;
import org.jboss.ha.framework.server.managed.ProtocolStackConfigurationsMapper;
import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.jboss.metatype.api.annotations.MetaMapping;

/**
 * Implementation of the JGroups <code>ChannelFactory</code> that supports a 
 * number of JBoss AS-specific behaviors:
 * <p>
 * <ul>
 * <li>Passing a config event to newly created channels containing 
 * "additional_data" that will be associated with the JGroups 
 * <code>IpAddress</code> for the peer. Used to provide logical addresses
 * to cluster peers that remain consistent across channel and server restarts.</li>
 * <li>Never returns instances of {@link org.jgroups.mux.MuxChannel} from
 * the <code>createMultiplexerChannel</code> methods.  Instead always returns
 * a channel with a shared transport protocol.</li>
 * <li>Configures the channel's thread pools and thread factories to ensure
 * that application thread context classloaders don't leak to the channel
 * threads.</li>
 * <li>Exposes a ProfileService ManagementView interface.</li>
 * </ul>
 * </p>
 * 
 * @author <a href="mailto://brian.stansberry@jboss.com">Brian Stansberry</a>
 * @author <a href="mailto:galder.zamarreno@jboss.com">Galder Zamarreno</a>
 * 
 * @version $Revision: 105748 $
 */
@ManagementObject(name="JChannelFactory", 
      componentType=@ManagementComponent(type="MCBean", subtype="JGroupsChannelFactory"),
      properties=ManagementProperties.EXPLICIT,
      isRuntime=true)
public class JChannelFactory
      extends org.jboss.ha.core.channelfactory.JChannelFactory
{
   
   // -------------------------------------------------------------  Properties

   /**
    * Gets the domain portion of the JMX ObjectName to use when registering channels and protocols
    * 
    * @return the domain. Will not return <code>null</code> after {@link #create()}
    *         has been invoked.
    */
   @ManagementProperty(use={ViewUse.CONFIGURATION}, description="The domain portion of the JMX ObjectName to use when registering channels and protocols")
   public String getDomain() 
   {
       return super.getDomain();
   }

   @ManagementProperty(use={ViewUse.CONFIGURATION}, description="Whether to expose channels we create via JMX")
   public boolean isExposeChannels() 
   {
       return super.isExposeChannels();
   }

   @ManagementProperty(use={ViewUse.CONFIGURATION}, description="Whether to expose protocols via JMX as well if we expose channels")
   public boolean isExposeProtocols() 
   {
       return super.isExposeProtocols();
   }

   /**
    * Get any logical name assigned to this server; if not null this value
    * will be the value of the 
    * {@link #setAssignLogicalAddresses(boolean) logical address} assigned
    * to the channels this factory creates.
    * 
    * @return the logical name for this server, or <code>null</code>.
    */
   @ManagementProperty(use={ViewUse.CONFIGURATION}, description="The cluster-unique logical name of this node")
   public String getNodeName()
   {
      return super.getNodeName();
   }
   
   /**
    * Gets whether this factory should create a "logical address" (or use
    * one set via {@link #setNodeName(String)} and assign it to
    * any newly created <code>Channel</code> as JGroups "additional_data".
    * 
    * @see #setAssignLogicalAddresses(boolean)
    */
   @ManagementProperty(use={ViewUse.CONFIGURATION}, description="Whether this factory should assign a logical address for this node to all channels")
   public boolean getAssignLogicalAddresses()
   {
      return super.getAssignLogicalAddresses();
   }
   
   /**
    * Gets whether this factory should update the standard JGroups
    * thread factories to ensure application classloaders do not leak to 
    * newly created channel threads.
    * 
    * @return <code>true</code> if the factories should be updated.
    *         Default is <code>true</code>.
    */
   @ManagementProperty(use={ViewUse.CONFIGURATION}, description="Whether this factory should update the standard JGroups thread factories to ensure classloader leaks do not occur")
   public boolean getManageNewThreadClassLoader()
   {
      return super.getManageNewThreadClassLoader();
   }

   /**
    * Gets whether this factory should update the standard JGroups
    * thread pools to ensure application classloaders have not leaked to 
    * threads returned to the pool.
    * 
    * @return <code>true</code> if the pools should be updated.
    *         Default is <code>false</code>.
    */
   @ManagementProperty(use={ViewUse.CONFIGURATION}, description="Whether this factory should update the standard JGroups thread pools to ensure classloader leaks do not occur")
   public boolean getManageReleasedThreadClassLoader()
   {
      return super.getManageReleasedThreadClassLoader();
   }

   /**
    * Gets whether {@link #createMultiplexerChannel(String, String)} should 
    * create a synthetic singleton name attribute for a channel's transport
    * protocol if one isn't configured.  If this is <code>false</code> and
    * no <code>singleton_name</code> is configured, 
    * {@link #createMultiplexerChannel(String, String)} will throw an
    * <code>IllegalStateException</code>. 
    * 
    * @return <code>true</code> if synthetic singleton names should be created.
    *         Default is <code>true</code>.
    */
   @ManagementProperty(use={ViewUse.CONFIGURATION}, 
         description="Whether this factory should create a synthetic singleton name attribute for a channel's transport protocol if one isn't configured")
   public boolean getAddMissingSingletonName()
   {
      return super.getAddMissingSingletonName();
   }
   
   // -------------------------------------------------------------  Public
   

   // --------------------------------------------------------  Management View
   
   /**
    * Gets information on channels created by this factory that are currently
    * open.
    */
   @ManagementProperty(use={ViewUse.STATISTIC}, 
         description="Information on channels created by this factory that are currently open",
         readOnly=true)
   @MetaMapping(value=OpenChannelsMapper.class)
   public Set<ChannelInfo> getOpenChannels()
   {
      return super.getOpenChannels();
   }
   
   @ManagementProperty(use={ViewUse.CONFIGURATION, ViewUse.RUNTIME}, 
         description="Protocol stack configurations available for use")
   @MetaMapping(value=ProtocolStackConfigurationsMapper.class)
   public Map<String, ProtocolStackConfigInfo> getProtocolStackConfigurations()
   {
      return super.getProtocolStackConfigurations();
   }

   // ---------------------------------------------------  JChannelFactoryMBean
   
   // -------------------------------------------------------------  Lifecycle
   
   // --------------------------------------------------------------- Protected

   // ----------------------------------------------------------------- Private
   
}
