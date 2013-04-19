/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.test.cluster.ejb3.stateful.unit;

import junit.framework.Test;

import org.jboss.test.JBossTestCase;

/**
 * Test nested SFSB for repeated passivation
 *
 * @author  Ben.Wang@jboss.org
 * @version $Revision: 108925 $
 */
public class NestedBeanUnitTestCase extends JBossTestCase
{
   private NestedBeanTestRunner runner;
   
   public NestedBeanUnitTestCase (String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(NestedBeanUnitTestCase.class, "stateful-test.jar");
   }
   
   
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      runner = new NestedBeanTestRunner(getInitialContext(), getLog());
      runner.setUp();
      // Use a sleep time just a bit longer than twice the bean timeout
      runner.setSleepTime(2100L);
   }

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      
      if (runner != null)
         runner.tearDown();
   }

   public void testBasic()
   throws Exception
   {
      runner.testBasic();
   }
   
   public void testDependentLifecycle()
   throws Exception
   {
      runner.testDependentLifecycle();      
   }

   public void testStatefulPassivation()
   throws Exception
   {
      runner.testStatefulPassivation();
   }
}
