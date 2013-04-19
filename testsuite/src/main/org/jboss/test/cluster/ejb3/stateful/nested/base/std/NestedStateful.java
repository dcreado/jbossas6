/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.test.cluster.ejb3.stateful.nested.base.std;

import java.rmi.dgc.VMID;

import org.jboss.test.cluster.ejb3.stateful.nested.base.MidLevel;


/**
 * Comment
 *
 * @author Ben Wang
 * @version $Revision: 108925 $
 */
public interface NestedStateful extends MidLevel
{
   VMID getVMID();
   int increment();
}
