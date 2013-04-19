/**
 * 
 */
package org.jboss.test.deployment.jbas7760.apptwo;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/**
 * AppOneEJB2xHome
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public interface AppTwoEJB2xHome extends EJBHome
{
   AppTwoEJB2xRemote create() throws CreateException, RemoteException;
}
