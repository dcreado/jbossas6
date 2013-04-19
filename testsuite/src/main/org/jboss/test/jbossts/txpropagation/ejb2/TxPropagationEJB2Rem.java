/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2009
 * @author JBoss Inc.
 */
package org.jboss.test.jbossts.txpropagation.ejb2;

import java.rmi.RemoteException;

import org.jboss.test.jbossts.recovery.ASFailureSpec;

public interface TxPropagationEJB2Rem extends javax.ejb.EJBObject
{
   String JNDI_NAME_JRMP = "TxPropagationEJB2Bean";
   String JNDI_NAME_IIOP = "TxPropagationEJB2BeanIIOP";

   /**
    * Dedicated for the first node of the propagation tests.
    * 
    * @param fSpecsNode0
    * @param fSpecsNode1
    * @param testEntityPK0
    * @param testEntityPK1
    * @param host1
    * @param useOTS
    * @return
    */
   public String testXA(ASFailureSpec[] fSpecsNode, ASFailureSpec[] fSpecsRemoteNode, String testEntityPK, String testEntityPKRemoteNode, String remoteHost, boolean expectFailureOnRemoteNode, boolean useOTS, int remoteJndiPort, int remoteCorbaPort) throws RemoteException;
   
   /**
    * Dedicated for the second node of the propagation tests.
    * 
    * @param fSpecsNode
    * @param testEntityPK
    * @return
    */
   public String testXA(ASFailureSpec[] fSpecsNode, String testEntityPK) throws RemoteException;
}
