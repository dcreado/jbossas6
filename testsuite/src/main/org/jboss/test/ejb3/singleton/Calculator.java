/**
 * 
 */
package org.jboss.test.ejb3.singleton;

import javax.ejb.Singleton;




/**
 * Calculator
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
public class Calculator
{
   public int subtract(int a, int b)
   {
      return a - b;
   }

   public int add(int a, int b)
   {
      return a + b;
   }
}
