/**
 * 
 */
package org.jboss.test.ejb3.nointerface;

/**
 * AccountManager
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public interface AccountManager
{
   /**
    * Credits the amount from the account corresponding to the
    * <code>accountNumber</code>
    * 
    * @param accountNumber Account number
    * @param amount Amount to be credited
    * @return
    */
   int credit(long accountNumber, int amount);
   
   /**
    * Debits the amount from the account corresponding to the
    * <code>accountNumber</code>
    * 
    * @param accountNumber Account number
    * @param amount Amount to be debited
    * @return
    */
   int debit(long accountNumber, int amount);
}