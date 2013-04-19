package org.jboss.test.ejbdepends.ejb;

import java.rmi.RemoteException;

/**
 * A MySession.
 * 
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 * @version $Revision: 1.1 $
 */
public interface MySession extends javax.ejb.EJBObject
{
   void test() throws RemoteException;
}
