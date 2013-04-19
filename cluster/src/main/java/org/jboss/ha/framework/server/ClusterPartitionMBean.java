/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009 Red Hat, Inc. and individual contributors
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

import java.util.List;

import org.jboss.ha.framework.interfaces.HAPartition;
import org.jboss.system.Service;

/** 
 * MBean interface for ClusterPartition.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>.
 * @author <a href="mailto:sacha.labourey@cogito-info.ch">Sacha Labourey</a>.
 * @version $Revision: 105748 $
 *
 * <p><b>Revisions:</b><br>
 */
public interface ClusterPartitionMBean extends Service
{
   /**
    * Name of the partition being built. All nodes/services belonging to 
    * a partition with the same name are clustered together.
    */
   String getPartitionName();

   /**
    * Uniquely identifies this node across the entire cluster. 
    * MUST be unique accros the whole cluster!
    */
   String getNodeName();
   
   /** The version of JGroups this is running on */
   String getJGroupsVersion();

   /**
    *  Number of milliseconds to wait until state has been transferred, or
    *  zero to wait forever. Increase this value for large states
    */
   long getStateTransferTimeout();

   /** Gets the max time (in ms) to wait for <em>synchronous</em> group method calls
    * ({@link HAPartition#callMethodOnCluster(String, String, Object[], Class[], boolean)}) 
    */ 
   long getMethodCallTimeout();
   
   /**
    * Returns whether this partition will synchronously notify any 
    * HAPartition.HAMembershipListener of membership changes using the 
    * calling thread from the underlying group communications layer
    * (e.g. JGroups).
    * 
    * @return <code>true</code> if registered listeners that don't implement
    *         <code>AsynchHAMembershipExtendedListener</code> or
    *         <code>AsynchHAMembershipListener</code> will be notified
    *         synchronously of membership changes; <code>false</code> if
    *         those listeners will be notified asynchronously.  Default
    *         is <code>false</code>.
    */
   public boolean getAllowSynchronousMembershipNotifications();
   
   /**
    * Gets the configuration name under which our cache is registered
    * with the cache manager.
    */
   String getCacheConfigName();
   
   /**
    * Gets the name of the JGroups channel protocol stack configuration
    * provided to the {@link #getChannelFactory() channel factory}. 
    */
   String getChannelStackName();

   /** Return the list of member nodes that built from the current view
    * @return A Vector Strings representing the host:port values of the nodes
    */
   List<String> getCurrentView();

   /**
    * Gets a listing of significant events since the instantiation of this 
    * service (e.g. view changes, member suspicions).
    * 
    * @return a String with one event per line
    */
   String showHistory();

   /**
    * Gets a listing of significant events since the instantiation of this 
    * service (e.g. view changes, member suspicions) in an XML format.
    * 
    * @return an XML string with each historical event wrapped in an
    *         "event" element, with all surrounded by an "events" element.
    * 
    * @return
    */
   String showHistoryAsXML ();
   
   String getName();
   int getState();
   String getStateString();
   
   
}
