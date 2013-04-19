package org.jboss.test.ejbdepends.ejb;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/**
 * A MySessionHome.
 * 
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 * @version $Revision: 1.1 $
 */
public interface MySessionHome extends EJBHome
{
   MySession create() throws CreateException, RemoteException;
}
