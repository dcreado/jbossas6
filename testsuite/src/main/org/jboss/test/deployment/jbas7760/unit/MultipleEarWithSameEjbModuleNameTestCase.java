/**
 * 
 */
package org.jboss.test.deployment.jbas7760.unit;

import javax.rmi.PortableRemoteObject;

import junit.framework.Test;

import org.jboss.test.JBossTestCase;
import org.jboss.test.deployment.jbas7760.appone.AppOneEJB2xHome;
import org.jboss.test.deployment.jbas7760.appone.AppOneEJB2xRemote;
import org.jboss.test.deployment.jbas7760.apptwo.AppTwoEJB2xHome;
import org.jboss.test.deployment.jbas7760.apptwo.AppTwoEJB2xRemote;

/**
 * Test case for:
 * 
 * https://jira.jboss.org/jira/browse/JBAS-7760
 * https://jira.jboss.org/jira/browse/JBAS-7789
 * 
 * <p>
 * Tests that multiple EAR files containing a EJB2.x jar with the same name
 * deploy fine without throwing any InstanceAlreadyExists exception while 
 * registering the deployment as a MBean. Also tests that the jmx names for
 * such deployments is deterministic.
 * </p>
 * 
 * 
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class MultipleEarWithSameEjbModuleNameTestCase extends JBossTestCase
{

   private static final String EAR_ONE_NAME = "jbas-7760-earone.ear";
   
   private static final String EAR_TWO_NAME = "jbas-7760-eartwo.ear";
   
   public MultipleEarWithSameEjbModuleNameTestCase(String name)
   {
      super(name);
   }
   
   /**
    * Test that the 2 ears containing a EJB2.x jar with the same name deploys
    * fine and the <depends> element in a .war of the .ear can use a 
    * deterministic jmx name to depend on the EJB2.x deployment
    * @throws Exception
    */
   public void testDeploymentOfSameEjbJarNameInMultipleEar() throws Exception
   {
      AppOneEJB2xHome appOneHome = (AppOneEJB2xHome) this.getInitialContext().lookup("jbas-7760-appone-ejb");
      AppOneEJB2xRemote appOneRemote = (AppOneEJB2xRemote) PortableRemoteObject.narrow(appOneHome.create(), AppOneEJB2xRemote.class);
      
      // just test a simple invocation
      appOneRemote.doNothing();
      
      // do the same with the other app
      AppTwoEJB2xHome appTwoHome = (AppTwoEJB2xHome) this.getInitialContext().lookup("jbas-7760-apptwo-ejb");
      AppTwoEJB2xRemote appTwoRemote = (AppTwoEJB2xRemote) PortableRemoteObject.narrow(appTwoHome.create(), AppTwoEJB2xRemote.class);
      
      // just test a simple invocation
      appTwoRemote.doNothing();
      
      
      
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(MultipleEarWithSameEjbModuleNameTestCase.class, EAR_ONE_NAME + "," + EAR_TWO_NAME);
   }
}
