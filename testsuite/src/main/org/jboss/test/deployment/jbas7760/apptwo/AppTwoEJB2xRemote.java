/**
 * 
 */
package org.jboss.test.deployment.jbas7760.apptwo;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * AppOneEJB2xRemote
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public interface AppTwoEJB2xRemote extends EJBObject
{

   void doNothing() throws RemoteException;
}
