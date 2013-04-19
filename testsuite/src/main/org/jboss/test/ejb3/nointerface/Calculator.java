/**
 * 
 */
package org.jboss.test.ejb3.nointerface;

import javax.ejb.Stateless;

/**
 * Calculator
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Stateless
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
