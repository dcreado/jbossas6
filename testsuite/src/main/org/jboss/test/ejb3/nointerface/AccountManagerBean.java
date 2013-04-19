/**
 * 
 */
package org.jboss.test.ejb3.nointerface;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.ejb3.annotation.RemoteBinding;



/**
 * AccountManagerBean
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Stateful
@Remote (AccountManager.class)
@RemoteBinding (jndiBinding = AccountManagerBean.JNDI_NAME)
public class AccountManagerBean implements AccountManager
{

   /**
    * JNDI name
    */
   public static final String JNDI_NAME = "AccountManagerRemoteView";
   
   /**
    * Inject the no-interface view of the Calculator
    */
   @EJB
   private Calculator simpleCalculator;

   /**
    * @see org.jboss.ejb3.nointerface.integration.test.common.AccountManager#credit(int)
    */
   public int credit(long accountNumber, int amount)
   {
      // get current account balance of this account number, from DB.
      // But for this example let's just hardcode it
      int currentBalance = 100;

      Calculator calculator = null;
      // lookup the no-interface view of the Calculator
      // We could have used the injected Calculator too, but
      // in this method we wanted to demonstrate how to lookup an no-interface view
      try
      {
         Context context = new InitialContext();
         calculator = (Calculator) context.lookup(Calculator.class.getSimpleName() + "/no-interface");
      }
      catch (NamingException ne)
      {
         throw new RuntimeException("Could not lookup no-interface view of calculator: ", ne);
      }
      return calculator.add(currentBalance, amount);

   }

   /**
    * @see org.jboss.ejb3.nointerface.integration.test.common.AccountManager#debit(int)
    */
   public int debit(long accountNumber, int amount)
   {
      // get current account balance of this account number, from DB.
      // But for this example let's just hardcode it
      int currentBalance = 100;
      
      // let's use the injected calculator
      return this.simpleCalculator.subtract(currentBalance, amount);
   }

}
