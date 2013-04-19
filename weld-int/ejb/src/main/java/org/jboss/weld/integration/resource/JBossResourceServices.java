package org.jboss.weld.integration.resource;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.injection.spi.helpers.AbstractResourceServices;

public class JBossResourceServices extends AbstractResourceServices implements ResourceInjectionServices
{

   private static Logger log = Logger.getLogger(JBossResourceServices.class);

   private static final String USER_TRANSACTION_LOCATION = "java:comp/UserTransaction";
   private static final String USER_TRANSACTION_CLASS_NAME = "javax.transaction.UserTransaction";
   private static final String HANDLE_DELEGATE_CLASS_NAME = "javax.ejb.spi.HandleDelegate";
   private static final String TIMER_SERVICE_CLASS_NAME = "javax.ejb.TimerService";
   private static final String ORB_CLASS_NAME = "org.omg.CORBA.ORB";


   protected static String getEJBResourceName(InjectionPoint injectionPoint, String proposedName)
   {
      if (injectionPoint.getType() instanceof Class<?>)
      {
         Class<?> type = (Class<?>) injectionPoint.getType();
         if (USER_TRANSACTION_CLASS_NAME.equals(type.getName()))
         {
            return USER_TRANSACTION_LOCATION;
         }
         else if (HANDLE_DELEGATE_CLASS_NAME.equals(type.getName()))
         {
            log.warn("Injection of @Resource HandleDelegate not supported in managed beans. Injection Point: " + injectionPoint);
            return proposedName;
         }
         else if (ORB_CLASS_NAME.equals(type.getName()))
         {
            log.warn("Injection of @Resource ORB not supported in managed beans. Injection Point: " + injectionPoint);
            return proposedName;
         }
         else if (TIMER_SERVICE_CLASS_NAME.equals(type.getName()))
         {
            log.warn("Injection of @Resource TimerService not supported in managed beans. Injection Point: " + injectionPoint);
            return proposedName;
         }
      }
      return proposedName;
   }

   private final Context context;

   public JBossResourceServices() throws NamingException
   {
      this.context = new InitialContext();
   }

   @Override
   protected Context getContext()
   {
      return context;
   }

   @Override
   protected String getResourceName(InjectionPoint injectionPoint)
   {
      return getEJBResourceName(injectionPoint, super.getResourceName(injectionPoint));
   }




}