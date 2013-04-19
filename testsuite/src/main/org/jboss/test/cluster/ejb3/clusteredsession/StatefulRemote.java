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
package org.jboss.test.cluster.ejb3.clusteredsession;

import java.rmi.dgc.VMID;

/**
 * Comment
 *
 * @author Ben Wang
 * @version $Revision: 108925 $
 */
public interface StatefulRemote
{
   int increment();
   String getHostAddress();

   int getPostActivate();

   int getPrePassivate();

   void setState(String state);

   String getState();

   void reset();

   void resetActivationCounter();

   void longRunning() throws Exception;

   void remove();

   public NodeAnswer getNodeState();

   public void setName(String name);

   public void setNameOnlyOnNode(String name, VMID node);

   public void setUpFailover(String failover);
}
