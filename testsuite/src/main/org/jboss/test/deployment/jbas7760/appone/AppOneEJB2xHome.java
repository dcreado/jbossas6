/**
 * 
 */
package org.jboss.test.deployment.jbas7760.appone;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/**
 * AppOneEJB2xHome
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public interface AppOneEJB2xHome extends EJBHome
{
   AppOneEJB2xRemote create() throws CreateException, RemoteException;
}
