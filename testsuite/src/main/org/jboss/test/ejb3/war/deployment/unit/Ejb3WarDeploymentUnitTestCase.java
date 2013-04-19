/**
 * 
 */
package org.jboss.test.ejb3.war.deployment.unit;

import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.Test;

import org.jboss.test.JBossTestCase;
import org.jboss.test.ejb3.war.deployment.Counter;

/**
 * Ejb3WarDeploymentTestCase
 * 
 * TestCase for testing the deployment of EJBs through .war files as defined
 * in EJB3.1 Spec, Section 20.4.
 * 
 *  @see https://jira.jboss.org/jira/browse/JBAS-7639
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class Ejb3WarDeploymentUnitTestCase extends JBossTestCase
{

   public Ejb3WarDeploymentUnitTestCase(String name)
   {
      super(name);
   }

   /**
    * 
    * @return
    * @throws Exception
    */
   public static Test suite() throws Exception
   {
      return getDeploySetup(Ejb3WarDeploymentUnitTestCase.class, "ejb3war.war");
   }

   /**
    * Tests that the beans deployed through a .war, in various ways (WEB-INF/ejb-jar.xml,
    * WEB-INF/lib/<somejar>.jar, WEB-INF/classes) works correctly.
    *  
    * @throws Exception
    */
   public void testEjbDeploymentInWar() throws Exception
   {
      Context ctx = new InitialContext();
      Counter counter = (Counter) ctx.lookup("CounterDelegateBean/remote");

      int count = counter.increment();
      assertEquals("Unexpected count after increment", 1, count);

      // increment one more time
      count = counter.increment();
      assertEquals("Unexpected count after second increment", 2, count);

      // now decrement
      count = counter.decrement();
      assertEquals("Unexpected count after decrement", 1, count);

      // decrement one more time
      count = counter.decrement();
      assertEquals("Unexpected count after second decrement", 0, count);

   }

}
