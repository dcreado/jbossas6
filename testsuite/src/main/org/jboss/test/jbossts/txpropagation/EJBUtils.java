/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2008,
 * @author JBoss Inc.
 */
package org.jboss.test.jbossts.txpropagation;

import java.io.IOException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.jboss.test.jbossts.txpropagation.ejb2.TxPropagationEJB2Home;
import org.jboss.test.jbossts.txpropagation.ejb2.TxPropagationEJB2Rem;
import org.jboss.test.jbossts.txpropagation.ejb3.TxPropagationRem;

import com.arjuna.orbportability.ORB;

public class EJBUtils
{
   private static String EJB2_JNDI_CORBA_FAC = "com.sun.jndi.cosnaming.CNCtxFactory";
   // use the CORBA name service to do JNDI lookups
   // HOST is the host where the EJB has been registered
   // ORB is the implementation name of the ORB (default name is JBoss
   // - see conf/jacorb.properties property jacorb.implname)
   // the port 3528 is the IIOP port officially assigned to JBoss by IANA
//   private static String EJB2_JNDI_CORBA_URL = "corbaloc::HOST:PORT/ORB/Naming/root";
   private static String EJB2_JNDI_CORBA_URL = "corbaloc::HOST:PORT/NameService";
   private static String JBOSS_ORB = "JBoss";

   private static String JNDI_FAC = "org.jboss.naming.NamingContextFactory";  //"org.jnp.interfaces.NamingContextFactory"
   private static String JNDI_URL = "jnp://HOST:PORT";
   private static String JNDI_PKGS = "org.jboss.naming.client:org.jnp.interfaces";
   private static String JNDI_CORBA_PKGS = "org.jboss.iiop.naming:org.jboss.naming.client:org.jnp.interfaces";
//   private static String JNDI_PKGS_SERVER = "org.jboss.naming:org.jnp.interfaces";


   
   public static Object lookupEjb(String host, int jndiPort, int corbaPort, String name, @SuppressWarnings("unchecked") Class clazz, boolean useOTS) throws Exception
   {
      System.out.println("looking up " + name + " on host " + host);

      Context ctx = getNamingContext(host, jndiPort, corbaPort, useOTS);

      try
      {
         Object o = ctx.lookup(name);

         return PortableRemoteObject.narrow(o, clazz);
      }
      finally
      {
         ctx.close();
      }
   }

   public static Object lookupObject(String host, int jndiPort, String name) throws Exception
   {
      return lookupObject(host, jndiPort, 0, name, false);
   }
   
   public static Object lookupObject(String host, int jndiPort, int corbaPort, String name, boolean useOTS) throws Exception
   {
      System.out.println("looking up " + name + " on host " + host);

      Context ctx = getNamingContext(host, jndiPort, corbaPort, useOTS);
      return ctx.lookup(name);
   }

   
   private static boolean oi = false;

   /**
    * Initialize an ORB for sending IIOP requests to another JBoss server
    * TODO this should not be neccessary - figure out why the default orb
    * running in the server is used
    */
   private static void initOrb()
   {
       if (!oi)
       {
           oi = true;
           ORB orb = ORB.getInstance("ServerSide");
           orb.initORB(new String[] {}, null);
       }
   }

   
   public static Context getNamingContext(String host, int jndiPort, int corbaPort, boolean useOTS) throws NamingException, IOException
   {
//      if (useOTS && corbaPort == 3628)
//         initOrb();
      
      Properties properties = new Properties();
      String url  = useOTS ? EJB2_JNDI_CORBA_URL : JNDI_URL;
      String fac  = useOTS ? EJB2_JNDI_CORBA_FAC : JNDI_FAC;
      String pkgs = useOTS ? JNDI_CORBA_PKGS : JNDI_PKGS;

      url = url.replace("HOST", host).replace("ORB", JBOSS_ORB);
      url = url.replace("PORT", Integer.toString(useOTS ? corbaPort : jndiPort));
      System.out.println("jndi url: " + url);
      
      if (useOTS) {
         org.omg.CORBA.ORB norb = org.jboss.iiop.naming.ORBInitialContextFactory.getORB();
         if (norb != null)
             properties.put("java.naming.corba.orb", norb); 
         properties.put(Context.OBJECT_FACTORIES, "org.jboss.tm.iiop.client.IIOPClientUserTransactionObjectFactory");
      }
      
      properties.setProperty(Context.INITIAL_CONTEXT_FACTORY, fac);
      properties.setProperty(Context.URL_PKG_PREFIXES, pkgs);
      properties.setProperty(Context.PROVIDER_URL, url);

      return new InitialContext(properties);
   }

   public static TxPropagationEJB2Rem lookupTxPropagationBeanEJB2(String host, int jndiPort, int corbaPort, boolean useOTS) throws Exception
   {
      String jndiName = useOTS ? TxPropagationEJB2Rem.JNDI_NAME_IIOP : TxPropagationEJB2Rem.JNDI_NAME_JRMP;
      TxPropagationEJB2Home home = (TxPropagationEJB2Home) lookupEjb(host, jndiPort, corbaPort, jndiName, TxPropagationEJB2Home.class, useOTS);
      return home.create();
   }

   public static TxPropagationRem lookupTxPropagationBeanEJB3(String host, int jndiPort, int corbaPort, boolean useOTS) throws Exception
   {
      if (useOTS)
         throw new UnsupportedOperationException("ejb3 does not support IIOP protocol yet");

      return (TxPropagationRem) lookupObject(host, jndiPort, corbaPort, TxPropagationRem.JNDI_NAME, false);
   }

}
