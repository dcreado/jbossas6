/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.test.cluster.ejb3.stateful.nested.base.std;

import javax.ejb.EJB;
import javax.ejb.Remove;

import org.jboss.test.cluster.ejb3.stateful.nested.base.DeepNestedStateful;
import org.jboss.test.cluster.ejb3.stateful.nested.base.PassivationActivationWatcherBean;

/**
 * Base class for a nested SFSB. Declares no class annotations,
 * giving subclasses configuration freedom.
 *
 * @author Ben Wang
 * @author Brian Stansberry
 * 
 * @version $Revision: 108925 $
 */
public abstract class NestedStatefulBean extends PassivationActivationWatcherBean 
   implements NestedStateful
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;
   
   private int counter = 0;
   
   @EJB(name="testDeepNestedStateful")
   private DeepNestedStateful deepNestedStateful;
   
   public void reset()
   {
      super.reset();
      counter = 0;
      deepNestedStateful.reset();
   }

   public int increment()
   {
      counter++;
      log.debug("INCREMENT - counter: " + counter);
      return counter;
   }
   
   @Remove
   public void remove() 
   {
      log.debug("Being removed");
   }
   

   public DeepNestedStateful getDeepNestedStateful()
   {
      return deepNestedStateful;
   }
}
