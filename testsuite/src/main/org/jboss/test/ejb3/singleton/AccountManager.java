/**
 * 
 */
package org.jboss.test.ejb3.singleton;

/**
 * AccountManager
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public interface AccountManager
{

   /**
    * Credits the amount to the account 
    * @param amount Amount to be credited
    * @return
    */
   void credit(int amount);
   
   /**
    * Debits the amount from the account 
    * @param amount Amount to be debited
    * @return
    */
   void debit(int amount);
   
   int balance();
}
