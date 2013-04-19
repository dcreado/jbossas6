/**
 * 
 */
package org.jboss.test.ejb3.nointerface.unit;

import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.Test;

import org.jboss.test.JBossTestCase;
import org.jboss.test.ejb3.nointerface.AccountManager;
import org.jboss.test.ejb3.nointerface.AccountManagerBean;
import org.jboss.test.ejb3.nointerface.Calculator;

/**
 * EJB31NoInterfaceViewTestCase
 * 
 * Tests EJB3.1 no-interface view support
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class EJB31NoInterfaceViewUnitTestCase extends JBossTestCase
{

   /**
    * 
    * @param name
    */
   public EJB31NoInterfaceViewUnitTestCase(String name)
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
      return getDeploySetup(EJB31NoInterfaceViewUnitTestCase.class, "ejb31nointerface.jar");
   }
   
   /**
    * Tests (indirect) access to a no-interface view bean ({@link Calculator})
    * 
    * @see Calculator and it's usage in {@link AccountManagerBean} 
    * @throws Exception
    */
   public void testNoInterfaceViewAccess() throws Exception
   {
      Context ctx = new InitialContext();
      AccountManager accountMgr = (AccountManager) ctx.lookup(AccountManagerBean.JNDI_NAME);
      
      long dummyAccountNumber = 123;
      // credit 50 dollars (Note that the current balance is hard coded in the bean to 100)
      // so after crediting, the current balance is going to be 150
      int currentBalance = accountMgr.credit(dummyAccountNumber, 50);
      
      assertEquals("Unexpected account balance after credit", 150, currentBalance);
      
      // now let's debit 10 dollars (Note that the current balance is again hard coded in the bean to 100).
      // So after debiting, the current balance is going to be 90
      currentBalance = accountMgr.debit(dummyAccountNumber, 10);
      
      assertEquals("Unexpected account balance after debit", 90, currentBalance);
   }
   

}
