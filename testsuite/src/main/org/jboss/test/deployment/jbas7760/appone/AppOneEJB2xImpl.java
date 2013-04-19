/**
 * 
 */
package org.jboss.test.deployment.jbas7760.appone;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * FirstEJB2xImpl
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class AppOneEJB2xImpl implements SessionBean, AppOneEJB2xRemote
{
   
   public void ejbCreate() throws CreateException, RemoteException
   {
      
   }


   public void ejbActivate() throws EJBException, RemoteException
   {
      // TODO Auto-generated method stub
      
   }

   public void ejbPassivate() throws EJBException, RemoteException
   {
      // TODO Auto-generated method stub
      
   }

   public void ejbRemove() throws EJBException, RemoteException
   {
      // TODO Auto-generated method stub
      
   }

   public void setSessionContext(SessionContext ctx) throws EJBException, RemoteException
   {
      // TODO Auto-generated method stub
      
   }


   public void doNothing() throws RemoteException
   {
      // do nothing
      
   }


   public EJBHome getEJBHome() throws RemoteException
   {
      // TODO Auto-generated method stub
      return null;
   }


   public Handle getHandle() throws RemoteException
   {
      // TODO Auto-generated method stub
      return null;
   }


   public Object getPrimaryKey() throws RemoteException
   {
      // TODO Auto-generated method stub
      return null;
   }


   public boolean isIdentical(EJBObject ejbo) throws RemoteException
   {
      // TODO Auto-generated method stub
      return false;
   }


   public void remove() throws RemoteException, RemoveException
   {
      // TODO Auto-generated method stub
      
   }

}
