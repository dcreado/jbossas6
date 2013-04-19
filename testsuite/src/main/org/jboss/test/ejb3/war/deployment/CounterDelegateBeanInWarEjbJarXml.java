/**
 * 
 */
package org.jboss.test.ejb3.war.deployment;

import javax.ejb.EJB;



/**
 * CounterDelegateBeanInWarEjbJarXml
 *
 * A Stateful bean configured through a ejb-jar.xml in .war/WEB-INF folder. This bean
 * just delegates the calls to a bean ({@link CounterBeanInWEBINFLibJar}) which is deployed
 * in the .war/WEB-INF/lib/<somejar>.jar
 * 
 * Will be used in {@link Ejb3WarDeploymentTestCase} for testing deployment of EJBs
 * through war files as defined in EJB3.1 Spec, Section 20.4
 * 
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class CounterDelegateBeanInWarEjbJarXml implements Counter
{


   /**
    * 
    */
   @EJB(beanName = "CounterBeanInWEBINFLibJar")
   private Counter counterBean;

   
   public int decrement()
   {
      return this.counterBean.decrement();
   }

   public int increment()
   {
      return this.counterBean.increment();
   }

}
