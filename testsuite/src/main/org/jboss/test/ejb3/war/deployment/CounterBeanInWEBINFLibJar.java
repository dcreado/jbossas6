/**
 * 
 */
package org.jboss.test.ejb3.war.deployment;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateful;



/**
 * CounterBeanInWEBINFLibJar
 * 
 * A Stateful bean configured deployed through a jar file in .war/WEB-INF/lib folder.
 * This bean uses a no-interface view bean ({@link CalculatorInWEBINFClasses}) to
 * perform its operations.
 * 
 * Will be used in {@link Ejb3WarDeploymentTestCase} for testing deployment of EJBs
 * through war files as defined in EJB3.1 Spec, Section 20.4
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Stateful
@Remote (Counter.class)
public class CounterBeanInWEBINFLibJar implements Counter
{

   private int count = 0;
   
   /**
    * Inject the no-interface view bean
    */
   @EJB
   private CalculatorInWEBINFClasses calculator;
   
   public int decrement()
   {
      this.count = this.calculator.subtract(this.count, 1);
      return this.count;
   }

   public int increment()
   {
      this.count = this.calculator.add(this.count, 1);
      return this.count;
   }

}
