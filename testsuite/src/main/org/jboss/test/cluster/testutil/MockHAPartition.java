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
package org.jboss.test.cluster.testutil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Future;

import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.ha.framework.interfaces.DistributedReplicantManager;
import org.jboss.ha.framework.interfaces.DistributedState;
import org.jboss.ha.framework.interfaces.GroupCommunicationService;
import org.jboss.ha.framework.interfaces.GroupMembershipListener;
import org.jboss.ha.framework.interfaces.HAPartition;
import org.jboss.ha.framework.interfaces.ResponseFilter;
import org.jboss.ha.framework.interfaces.SerializableStateTransferResult;
import org.jboss.ha.framework.interfaces.StateTransferProvider;
import org.jboss.ha.framework.interfaces.StreamStateTransferResult;

/**
 * Mock implementation of HAPartition intended to support unit testing
 * of DistributedReplicantManagerImpl without the need for an underlying
 * JChannel.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @author <a href="mailto:galder.zamarreno@jboss.com">Galder Zamarreno</a>
 * @version $Id: MockHAPartition.java 105748 2010-06-05 13:49:33Z bstansberry@jboss.com $
 */
public class MockHAPartition implements HAPartition
{   
   public static final String PARTITION_NAME = "MockPartition";
   
   private DistributedReplicantManager drm;
   private Vector<ClusterNode> currentNodes;
   private ClusterNode localAddress;
   @SuppressWarnings("unchecked")
   private ArrayList remoteReplicants;
   
   public MockHAPartition(ClusterNode localAddress)
   {
      this.localAddress = localAddress;
   }
   
   public MockHAPartition()
   {
      
   }
   
   // ------------------------------------------------------------  HAPartition
   
   public String getNodeName()
   {
      return localAddress.getName();
   }

   public String getPartitionName()
   {
      return PARTITION_NAME;
   }

   public DistributedReplicantManager getDistributedReplicantManager()
   {
      return drm;
   }

   public DistributedState getDistributedStateService()
   {

      throw new UnsupportedOperationException("not implemented");
   }

   public void registerRPCHandler(String serviceName, Object handler)
   {
      if (handler instanceof DistributedReplicantManager)
         drm = (DistributedReplicantManager) handler;
      else
         throw new UnsupportedOperationException("not implemented");
   }
   
   public void registerRPCHandler(String serviceName, Object handler, ClassLoader classloader)
   {
      throw new UnsupportedOperationException("not implemented");
   }

   public void unregisterRPCHandler(String serviceName, Object subscriber)
   {
      if (subscriber == drm)
         drm = null;
      else    
         throw new UnsupportedOperationException("not implemented");
   }

   public ArrayList callMethodOnCluster(String serviceName, String methodName, Object[] args, Class<?>[] types,
         boolean excludeSelf) throws InterruptedException
   {
      return callMethodOnCluster(serviceName, methodName, args, types, Object.class, true, null, 0, false);
   }

   public <T> ArrayList<T> callMethodOnCluster(String serviceName, String methodName, Object[] args, Class<?>[] types,
         Class<T> returnType, boolean excludeSelf, ResponseFilter filter, long methodTimeout, boolean unordered)
         throws InterruptedException
   {
      if (excludeSelf)
      {
         if ("_add".equals(methodName)) 
         {
            // no-op -- there is no cluster
            return null;
         }
         else if ("lookupLocalReplicants".equals(methodName) && args.length == 0)
         {
            return remoteReplicants;
         }
      }
      // TODO Implement lookupLocalReplicants for DRM SERVICE_NAME

      throw new UnsupportedOperationException("not implemented");
   }
   
   public ArrayList callMethodOnCluster(String serviceName, String methodName, Object[] args, Class<?>[] types, 
         boolean excludeSelf, ResponseFilter filter) throws InterruptedException
   {
      throw new UnsupportedOperationException("not implemented");
   }

   public void callMethodOnCluster(String serviceName, String methodName, Object[] args, boolean excludeSelf)
         throws Exception
   {
      throw new UnsupportedOperationException("not implemented");
   }

   public void callAsynchMethodOnCluster(String serviceName, String methodName, Object[] args, Class<?>[] types,
         boolean excludeSelf) throws InterruptedException
   {
      throw new UnsupportedOperationException("not implemented");
   }

   public void callAsynchMethodOnCluster(String serviceName, String methodName, Object[] args, boolean excludeSelf)
         throws Exception
   {
      throw new UnsupportedOperationException("not implemented");
   }   

   public void callAsynchMethodOnCluster(String objName, String methodName, Object[] args, Class<?>[] types,
         boolean excludeSelf, boolean unordered) throws InterruptedException
   {
      throw new UnsupportedOperationException("not implemented");
   }

   public ArrayList callMethodOnCoordinatorNode(String serviceName, String methodName, Object[] args, Class<?>[] types,
         boolean excludeSelf) throws Exception
   {
      throw new UnsupportedOperationException("not implemented");
   }

   public <T> T callMethodOnCoordinatorNode(String serviceName, String methodName, Object[] args, Class<?>[] types,
         Class<T> returnType, boolean excludeSelf, long methodTimeout, boolean unordered) throws Exception
   {
      throw new UnsupportedOperationException("not implemented");
   }

   public Object callMethodOnNode(String serviceName, String methodName,
           Object[] args, Class[] types, long methodTimeout, ClusterNode targetNode) throws Exception
   {
      throw new UnsupportedOperationException("not implemented");
   }

   public <T> T callMethodOnNode(String serviceName, String methodName, Object[] args, Class<?>[] types,
         Class<T> returnType, long methodTimeout, ClusterNode targetNode, boolean unordered) throws Exception
   {
      throw new UnsupportedOperationException("not implemented");
   }

   public Object callMethodOnNode(String serviceName, String methodName, Object[] args, Class<?>[] types,
         ClusterNode targetNode) throws Exception
   {
      throw new UnsupportedOperationException("not implemented");
   }


   public void callAsyncMethodOnNode(String serviceName, String methodName,
           Object[] args, Class<?>[] types, long methodTimeout, ClusterNode targetNode) throws Throwable
   {
      throw new UnsupportedOperationException("not implemented");
   }

   public void callAsyncMethodOnCoordinatorNode(String serviceName, String methodName, Object[] args, Class<?>[] types,
         boolean excludeSelf, boolean unordered) throws Exception
   {
      throw new UnsupportedOperationException("not implemented");
   }

   public void callAsyncMethodOnCoordinatorNode(String serviceName, String methodName, Object[] args, Class<?>[] types,
         boolean excludeSelf) throws Exception
   {
      throw new UnsupportedOperationException("not implemented");
   }

   public void callAsyncMethodOnNode(String serviceName, String methodName, Object[] args, Class<?>[] types,
         ClusterNode targetNode, boolean unordered) throws Exception
   {
      throw new UnsupportedOperationException("not implemented");
   }

   public void callAsyncMethodOnNode(String serviceName, String methodName, Object[] args, Class<?>[] types,
         ClusterNode targetNode) throws Exception
   {
      throw new UnsupportedOperationException("not implemented");
   }

   public long getMethodCallTimeout()
   {
      return 60000;
   }

   public void subscribeToStateTransferEvents(String serviceName, HAPartitionStateTransfer subscriber)
   {
      // no-op. at this point the test fixture directly passes state
      // to the target DRM
   }

   public void unsubscribeFromStateTransferEvents(String serviceName, HAPartitionStateTransfer subscriber)
   {
      // no-op. at this point the test fixture directly passes state
      // to the target DRM
   }

   public void registerMembershipListener(HAMembershipListener listener)
   {
      // no-op. at this point the test fixture directly passes membership
      // changes to the target DRM
   }

   public void unregisterMembershipListener(HAMembershipListener listener)
   {
      // no-op. at this point the test fixture directly passes membership
      // changes to the target DRM
   }
   
   public boolean getAllowSynchronousMembershipNotifications()
   {
      return false;
   }
   
   public void setAllowSynchronousMembershipNotifications(boolean allowSync)
   {
      // no-op      
   }

   public long getCurrentViewId()
   {

      throw new UnsupportedOperationException("not implemented");
   }

   public Vector<String> getCurrentView()
   {
      Vector<String> result = new Vector<String>();
      for (int i = 0; i < currentNodes.size(); i++)
         result.add(currentNodes.elementAt(i).getName());
         
      return result;
   }

   public ClusterNode[] getClusterNodes()
   {
      ClusterNode[] result = new ClusterNode[currentNodes.size()];
      return (ClusterNode[]) currentNodes.toArray(result);
   }

   public ClusterNode getClusterNode()
   {
      return localAddress;
   }
   
   // ---------------------------------------------------------  Public Methods
   
   public void setCurrentViewClusterNodes(Vector<ClusterNode> nodes)
   {
      this.currentNodes = nodes;
   }
   
   public void setRemoteReplicants(ArrayList<?> remoteReplicants)
   {
      this.remoteReplicants = remoteReplicants;
   }
   
   public void setLocalAddress(ClusterNode localAddress)
   {
      this.localAddress = localAddress;
   }

   public String getGroupName()
   {
      return getPartitionName();
   }

   public boolean isConsistentWith(GroupCommunicationService other)
   {
      return this == other;
   }

   public void registerGroupMembershipListener(GroupMembershipListener listener)
   {
      // no-op. at this point the test fixture directly passes membership
      // changes to the target DRM
   }

   public void unregisterGroupMembershipListener(GroupMembershipListener listener)
   {
      // no-op. at this point the test fixture directly passes membership
      // changes to the target DRM
   }

   public Future<SerializableStateTransferResult> getServiceState(String serviceName)
   {
      // TODO Auto-generated method stub
      return null;
   }

   public Future<SerializableStateTransferResult> getServiceState(String serviceName, ClassLoader classloader)
   {
      // TODO Auto-generated method stub
      return null;
   }

   public Future<StreamStateTransferResult> getServiceStateAsStream(String serviceName)
   {
      // TODO Auto-generated method stub
      return null;
   }

   public void registerStateTransferProvider(String serviceName, StateTransferProvider provider)
   {
      // no-op. at this point the test fixture directly passes state
      // to the target DRM
   }

   public void unregisterStateTransferProvider(String serviceName)
   {
      // no-op. at this point the test fixture directly passes state
      // to the target DRM
   }

}
