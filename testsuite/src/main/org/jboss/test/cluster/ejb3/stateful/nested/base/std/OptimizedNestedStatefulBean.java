/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.test.cluster.ejb3.stateful.nested.base.std;

import org.jboss.ejb3.cache.Optimized;

/**
 * NestedStatefulBean subclass that implements Optimized in such a way
 * that getPrePassivate() and getPostActivate() calls are treated as
 * non-modifications.
 *
 * @author Ben Wang
 * @author Brian Stansberry
 * 
 * @version $Revision: 108925 $
 */
public abstract class OptimizedNestedStatefulBean 
   extends NestedStatefulBean
   implements Optimized
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;
   
   private boolean modified = true;
   
   
   public boolean isModified()
   {
      boolean answer = modified;
      modified = true;
      return answer;
   }

   @Override
   public int getPostActivate()
   {
      // by default all calls except these passivate/activate checks
      // are modifications, so set modified to false
      modified = false;
      return super.getPostActivate();
   }

   @Override
   public int getPrePassivate()
   {
      // by default all calls except these passivate/activate checks
      // are modifications, so set modified to false
      modified = false;
      return super.getPrePassivate();
   }
}
