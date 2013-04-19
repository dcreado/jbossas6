/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.test.ejb3.spec.global.jndi;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

/**
 * AbstractJNDILookup
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public abstract class AbstractJNDILookup
{
   private static Logger logger = Logger.getLogger(AbstractJNDILookup.class);

   //@Resource (mappedName = "java:app/AppName")
   // FIXME: It should actually come from the above @Resource, once it 
   // starts working
   protected String appName;

   //@Resource (mappedName = "java:module/ModuleName")
   // FIXME: It should actually come from the above @Resource, once it 
   // starts working
   protected String moduleName;

   protected void lookupPortableJNDINames(Class<?> beanClass, boolean skipEJB2xViews, boolean isEJB31) throws NamingException
   {
      String beanName = beanClass.getSimpleName();
      Context ctx = new InitialContext();

      // java:global
      String remoteBusinessInterfaceJNDIName = this.getJavaGlobalJNDIName(beanName, EchoRemote.class.getName());
      String localBusinessInterfaceJNDIName = this.getJavaGlobalJNDIName(beanName, EchoLocal.class.getName());
      

      logger.info("Looking up " + remoteBusinessInterfaceJNDIName);
      EchoRemote remoteEcho = (EchoRemote) ctx.lookup(remoteBusinessInterfaceJNDIName);
      logger.info("Looking up " + localBusinessInterfaceJNDIName);
      EchoLocal localEcho = (EchoLocal) ctx.lookup(localBusinessInterfaceJNDIName);
      
      if (isEJB31)
      {
         String noInterfaceViewJNDIName = this.getJavaGlobalJNDIName(beanName, beanClass.getName());
         logger.info("Looking up " + noInterfaceViewJNDIName);
         Object noInterfaceView = ctx.lookup(noInterfaceViewJNDIName);
         if (!beanClass.isInstance(noInterfaceView))
         {
            throw new RuntimeException("Unexpected object " + noInterfaceView + " in JNDI for jndi name "
                  + noInterfaceViewJNDIName);
         }
      }
      if (!skipEJB2xViews)
      {
         String homeJNDIName = this.getJavaGlobalJNDIName(beanName, SimpleHome.class.getName());
         String localHomeJNDIName = this.getJavaGlobalJNDIName(beanName, SimpleLocalHome.class.getName());

         logger.info("Looking up " + homeJNDIName);
         SimpleHome home = (SimpleHome) ctx.lookup(homeJNDIName);
         logger.info("Looking up " + localHomeJNDIName);
         SimpleLocalHome localHome = (SimpleLocalHome) ctx.lookup(localHomeJNDIName);
      }

      logger.info("Successfully looked up various views in java:global namespace for SLSB: " + SimpleSLSB.class);

      // java:app
      remoteBusinessInterfaceJNDIName = this.getJavaAppJNDIName(beanName, EchoRemote.class.getName());
      localBusinessInterfaceJNDIName = this.getJavaAppJNDIName(beanName, EchoLocal.class.getName());
      

      logger.info("Looking up " + remoteBusinessInterfaceJNDIName);
      remoteEcho = (EchoRemote) ctx.lookup(remoteBusinessInterfaceJNDIName);
      logger.info("Looking up " + localBusinessInterfaceJNDIName);
      localEcho = (EchoLocal) ctx.lookup(localBusinessInterfaceJNDIName);
      
      if (isEJB31)
      {
         String noInterfaceViewJNDIName = this.getJavaAppJNDIName(beanName, beanClass.getName());
         logger.info("Looking up " + noInterfaceViewJNDIName);
         Object noInterfaceView = ctx.lookup(noInterfaceViewJNDIName);
         if (!beanClass.isInstance(noInterfaceView))
         {
            throw new RuntimeException("Unexpected object " + noInterfaceView + " in JNDI for jndi name "
                  + noInterfaceViewJNDIName);
         }
      }

      if (!skipEJB2xViews)
      {
         String homeJNDIName = this.getJavaAppJNDIName(beanName, SimpleHome.class.getName());
         String localHomeJNDIName = this.getJavaAppJNDIName(beanName, SimpleLocalHome.class.getName());

         logger.info("Looking up " + homeJNDIName);
         SimpleHome home = (SimpleHome) ctx.lookup(homeJNDIName);
         logger.info("Looking up " + localHomeJNDIName);
         SimpleLocalHome localHome = (SimpleLocalHome) ctx.lookup(localHomeJNDIName);
      }

      // java:module
      remoteBusinessInterfaceJNDIName = this.getJavaModuleJNDIName(beanName, EchoRemote.class.getName());
      localBusinessInterfaceJNDIName = this.getJavaModuleJNDIName(beanName, EchoLocal.class.getName());
      
      logger.info("Looking up " + remoteBusinessInterfaceJNDIName);
      remoteEcho = (EchoRemote) ctx.lookup(remoteBusinessInterfaceJNDIName);
      logger.info("Looking up " + localBusinessInterfaceJNDIName);
      localEcho = (EchoLocal) ctx.lookup(localBusinessInterfaceJNDIName);
      
      if (isEJB31)
      {
         String noInterfaceViewJNDIName = this.getJavaModuleJNDIName(beanName, beanClass.getName());
         logger.info("Looking up " + noInterfaceViewJNDIName);
         Object noInterfaceView = ctx.lookup(noInterfaceViewJNDIName);
         if (!beanClass.isInstance(noInterfaceView))
         {
            throw new RuntimeException("Unexpected object " + noInterfaceView + " in JNDI for jndi name "
                  + noInterfaceViewJNDIName);
         }
      }
      if (!skipEJB2xViews)
      {
         String homeJNDIName = this.getJavaModuleJNDIName(beanName, SimpleHome.class.getName());
         String localHomeJNDIName = this.getJavaModuleJNDIName(beanName, SimpleLocalHome.class.getName());

         logger.info("Looking up " + homeJNDIName);
         SimpleHome home = (SimpleHome) ctx.lookup(homeJNDIName);
         logger.info("Looking up " + localHomeJNDIName);
         SimpleLocalHome localHome = (SimpleLocalHome) ctx.lookup(localHomeJNDIName);
      }
   }

   private String getJavaGlobalJNDIName(String beanName, String beanInterface)
   {
      String globalJNDIName = "java:global/";
      if (appName != null)
      {
         globalJNDIName = globalJNDIName + appName + "/";
      }
      globalJNDIName = globalJNDIName + moduleName + "/" + beanName;
      if (beanInterface != null)
      {
         globalJNDIName += "!" + beanInterface;
      }

      return globalJNDIName;
   }

   private String getJavaAppJNDIName(String beanName, String beanInterface)
   {
      String jndiName = "java:app/" + moduleName + "/" + beanName;

      if (beanInterface != null)
      {
         jndiName += "!" + beanInterface;
      }

      return jndiName;
   }

   private String getJavaModuleJNDIName(String beanName, String beanInterface)
   {
      String jndiName = "java:module/" + beanName;

      if (beanInterface != null)
      {
         jndiName += "!" + beanInterface;
      }
      return jndiName;
   }
}
