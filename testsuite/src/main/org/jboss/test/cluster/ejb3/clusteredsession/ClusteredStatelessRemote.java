/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.test.cluster.ejb3.clusteredsession;

/**
 * comment
 *
 * @author Ben Wang
 */
public interface ClusteredStatelessRemote
{
   NodeAnswer getNodeState();
}
