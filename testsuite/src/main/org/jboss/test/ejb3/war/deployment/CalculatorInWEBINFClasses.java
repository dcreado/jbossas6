/**
 * 
 */
package org.jboss.test.ejb3.war.deployment;

import javax.ejb.Stateless;



/**
 * CalculatorInWEBINFClasses
 *
 * A no-interface view bean which will be placed in the .war/WEB-INF/classes folder.
 * Will be used in {@link Ejb3WarDeploymentTestCase} for testing deployment of EJBs
 * through war files as defined in EJB3.1 Spec, Section 20.4
 *   
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Stateless
public class CalculatorInWEBINFClasses
{

   public int add (int a, int b)
   {
      return a + b;
   }
   
   public int subtract (int a, int b)
   {
      return a - b;
   }
}

