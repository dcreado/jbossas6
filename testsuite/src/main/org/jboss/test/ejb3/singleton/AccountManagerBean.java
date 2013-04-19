/**
 * 
 */
package org.jboss.test.ejb3.singleton;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Singleton;

import org.jboss.ejb3.annotation.RemoteBinding;



/**
 * AccountManagerBean
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
@Remote (AccountManager.class)
@RemoteBinding (jndiBinding = AccountManagerBean.JNDI_NAME)
public class AccountManagerBean implements AccountManager
{

   /**
    * JNDI name
    */
   public static final String JNDI_NAME = "singleton-account-manager";
   
   /**
    * Inject the no-interface view of the Calculator
    */
   @EJB
   private Calculator simpleCalculator;
   
   private int balance;

   /**
    * @see org.jboss.ejb3.nointerface.integration.test.common.AccountManager#credit(int)
    */
   public void credit(int amount)
   {
      this.balance = this.simpleCalculator.add(this.balance, amount);

   }

   /**
    * @see org.jboss.ejb3.nointerface.integration.test.common.AccountManager#debit(int)
    */
   public void debit(int amount)
   {
     this.balance = this.simpleCalculator.subtract(this.balance, amount);
   }
   
   public int balance()
   {
      return this.balance;
   }

}
