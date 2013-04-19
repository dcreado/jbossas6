/**
 * 
 */
package org.jboss.test.ejb3.singleton.unit;

import junit.framework.Test;

import org.jboss.test.JBossTestCase;
import org.jboss.test.ejb3.singleton.AccountManager;
import org.jboss.test.ejb3.singleton.AccountManagerBean;

/**
 * SingletonUnitTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonUnitTestCase extends JBossTestCase
{

   public SingletonUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(SingletonUnitTestCase.class, "ejb31singleton.jar");
   }

   public void testSingletonBeanAccess() throws Exception
   {
      AccountManager accountManager = (AccountManager) this.getInitialContext().lookup(AccountManagerBean.JNDI_NAME);

      int initialBalance = accountManager.balance();
      assertEquals("Unexpected initial balance", 0, initialBalance);

      // credit
      accountManager.credit(100);

      AccountManager anotherAccountManagerInstance = (AccountManager) this.getInitialContext().lookup(
            AccountManagerBean.JNDI_NAME);
      int balanceAfterCredit = anotherAccountManagerInstance.balance();
      assertEquals("Unexpected balance after credit", 100, balanceAfterCredit);

      // debit
      anotherAccountManagerInstance.debit(50);

      int balanceAfterDebit = accountManager.balance();
      assertEquals("Unexpected balance after debit", 50, balanceAfterDebit);

   }
}
