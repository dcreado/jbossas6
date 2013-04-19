/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.test.cluster.ejb3.clusteredsession;

import java.io.Serializable;
import java.rmi.dgc.VMID;

/** A data class used to track the identity of the cluster node.
 *
 * @author Ben Wang
 */
public class NodeAnswer implements Serializable
{
   private static final long serialVersionUID = -2206023019867587062L;
   
   public VMID nodeId = null;
   public Object answer = null;

   public NodeAnswer (VMID node, Object answer)
   {
      this.nodeId = node;
      this.answer = answer;
   }

   public VMID getNodeId ()
   {
      return this.nodeId;
   }

   public Object getAnswer()
   {
      return this.answer;
   }

   @Override
   public boolean equals(Object obj)
   {
      boolean same = (this == obj);
      
      if (!same && obj instanceof NodeAnswer)
      {
         NodeAnswer other = (NodeAnswer) obj;
         same = this.nodeId.equals(other.nodeId)
                && this.answer.equals(other.answer);
      }
      return same;
   }

   @Override
   public int hashCode()
   {
      int result = 17;
      result = result * 29 + nodeId.hashCode();
      result = result * 29 + answer.hashCode();
      return result;
   }

   public String toString ()
   {
      return "{ " + this.nodeId + " ; " + this.answer + " }";
   }
}
