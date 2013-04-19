/**
 * 
 */
package org.jboss.test.deployment.jbas7760.appone;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * AppOneEJB2xRemote
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public interface AppOneEJB2xRemote extends EJBObject
{

   void doNothing() throws RemoteException;
}
