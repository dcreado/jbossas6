package org.jboss.test.ejbdepends.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * A MySessionBean.
 * 
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 * @version $Revision: 1.1 $
 */
public class MySessionBean implements SessionBean
{

   public void test() throws RemoteException
   {
      
   }
   
   public void ejbCreate() throws RemoteException
   {
   }
   
   @Override
   public void ejbActivate() throws EJBException, RemoteException
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void ejbPassivate() throws EJBException, RemoteException
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void ejbRemove() throws EJBException, RemoteException
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void setSessionContext(SessionContext arg0) throws EJBException, RemoteException
   {
      // TODO Auto-generated method stub
      
   }

}
