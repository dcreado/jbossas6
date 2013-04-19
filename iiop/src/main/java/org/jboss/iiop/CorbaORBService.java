/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.iiop;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.ejb.spi.HandleDelegate;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.apache.log4j.Logger;
import org.jboss.config.ServerConfigUtil;
import org.jboss.iiop.naming.ORBInitialContextFactory;
import org.jboss.metadata.IorSecurityConfigMetaData;
import org.jboss.proxy.ejb.handle.HandleDelegateImpl;
import org.jboss.security.SecurityDomain;
import org.jboss.system.Registry;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.PortableServer.IdAssignmentPolicy;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.IdUniquenessPolicyValue;
import org.omg.PortableServer.LifespanPolicy;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.ServantRetentionPolicyValue;

/**
 *  This is a JMX service that provides the default CORBA ORB
 *  for JBoss to use.
 *      
 *  @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 *  @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 *  @version $Revision: 108275 $
 */
public class CorbaORBService implements CorbaORBServiceMBean, ObjectFactory
{
   // Constants -----------------------------------------------------

   public static String ORB_NAME = "JBossCorbaORB";
   public static String POA_NAME = "JBossCorbaPOA";
   public static String IR_POA_NAME = "JBossCorbaInterfaceRepositoryPOA";
   public static String SSL_DOMAIN = "JBossCorbaSSLDomain";
   public static String IOR_SECURITY_CONFIG = "IORSecurityConfig";
   
   // Attributes ----------------------------------------------------

   private String orbClass = null;
   private String orbSingletonClass = null;
   private String orbSingletonDelegate = null;
   private String orbPropertiesFileName = "orb-properties-file-not-defined";
   private List<String> portableInterceptorInitializers = null;
   private int port = 0;
   private int sslPort = 0;
   private SecurityDomain sslDomain = null;
   private IorSecurityConfigMetaData defaultIORSecurityConfig = null;

   // Static --------------------------------------------------------

   private static ORB orb;
   private static POA poa;
   private static POA otsPoa;
   private static POA otsResourcePoa;
   private static POA irPoa;
   private static HandleDelegate hd;
   private static int oaSslPort;

   private static Logger log = Logger.getLogger(CorbaORBService.class);
   
   /** 
    * Addition of SSL components to IORs is disabled by default.
    * This must remain off for interoperation with IONA's ASP 6.0,
    * which does not accept IORs with SSL components.
    */
   private static boolean sslComponentsEnabledFlag = false;

   /** 
    * Sending an IIOP reply that carries both a CompleteEstablishContext 
    * (SAS accept) reply and an exception is enabled by default, because
    * the CSIv2 spect does not explicitly disallow an SAS accept in an
    * IIOP exception reply. This flag must be turned off for interoperation 
    * with IONA's ASP 6.0, which throws an ArrayIndexOutOfBoundsException 
    * when it receives an IIOP reply carrying both an application exception 
    * and a SAS reply CompleteEstablishContext.
    */
   private static boolean sendSasAcceptWithExceptionEnabledFlag = true;

   /**
    * True if the OTS context should be sent along with outgoing requests. 
    */
   private static boolean otsContextPropagationEnabledFlag = false;

   /**
    * Returns the actual SSL port. This method is intended to be called
    * by the CSIv2 IOR interceptor, which needs to know the SSL port.
    */
   public static int getTheActualSSLPort() 
   {
      return oaSslPort;
   }

   /**
    * Returns true if addition of SSL components to IORs is enabled.
    * This method is intended to be called by the CSIv2 IOR interceptor.
    */
   public static boolean getSSLComponentsEnabledFlag()
   {
      return sslComponentsEnabledFlag;
   }
   
   /**
    * Returns true if sending an SAS accept reply together with an IIOP
    * exception reply is enabled. This method is intended to be called by 
    * the CSIv2 server request interceptor.
    */
   public static boolean getSendSASAcceptWithExceptionEnabledFlag()
   {
      return sendSasAcceptWithExceptionEnabledFlag;
   }
   
   /**
    * Returns true if the OTS context should be sent along with outgoing 
    * requests. This method is intended to be called by the OTS client request 
    * interceptor installed in the JBoss server.
    */
   public static boolean getOTSContextPropagationEnabledFlag()
   {
      return otsContextPropagationEnabledFlag;
   }
   
   /**
    * Returns the CORBA transaction service main POA. Our OTS implementation
    * uses this POA for all OTS objects that are not Resource instances.
    */
   public static POA getOtsPoa()
   {
      return otsPoa;
   }
   
   /**
    * Returns the CORBA transaction service secondary POA. Our OTS 
    * implementation uses this POA for OTS Resources, so that they can have
    * their own default servant. (The name clash between Terminator::rollback 
    * and Resource::rollback disallows us to use a single default servant for
    * all OTS objects.)  
    */
   public static POA getOtsResourcePoa()
   {
      return otsResourcePoa;
   }

   //============================ Microcontainer lifecycle methods ==========================//

   public void start() throws Exception 
   {

      Properties props = new Properties();

      // Read orb properties file into props
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      InputStream is = cl.getResourceAsStream(orbPropertiesFileName);
      props.load(is);
      String oaiAddr = props.getProperty("OAIAddr");
      if (oaiAddr == null)
         oaiAddr = ServerConfigUtil.getSpecificBindAddress();
      if (oaiAddr != null)
         props.setProperty("OAIAddr", oaiAddr);
      log.debug("Using OAIAddr=" + oaiAddr);

      // Set ORB initialization properties
      Properties systemProps = System.getProperties();
      if (orbClass != null) {
         props.put("org.omg.CORBA.ORBClass", orbClass);
         systemProps.put("org.omg.CORBA.ORBClass", orbClass);
      }
      if (orbSingletonClass != null) {
         props.put("org.omg.CORBA.ORBSingletonClass", orbSingletonClass);
         systemProps.put("org.omg.CORBA.ORBSingletonClass", orbSingletonClass);
      }
      if (orbSingletonDelegate != null)
         systemProps.put(org.jboss.system.ORBSingleton.DELEGATE_CLASS_KEY,
                         orbSingletonDelegate);

      // JacORB-specific hack: add jacorb.config.log.verbosity to the system 
      // properties so that it is seen by JacORB at configuration time.
      // This allows us (by setting jacorb.config.log.verbosity=0) to get rid 
      // of the messages "jacorb.home unset! Will use '.'" and 
      // "File ./jacorb.properties for configuration jacorb not found", which
      // would otherwise be sent to the standard output.
      String str = props.getProperty("jacorb.config.log.verbosity");
      if (str != null)
         systemProps.put("jacorb.config.log.verbosity", str);

      System.setProperties(systemProps);
      
      // Make the default IOR security configuration available in the registry.
      if (this.defaultIORSecurityConfig == null)
    	  this.defaultIORSecurityConfig = new IorSecurityConfigMetaData();
      Registry.bind(IOR_SECURITY_CONFIG, this.defaultIORSecurityConfig);
      
      // Add portable interceptor initializers
      if (this.portableInterceptorInitializers != null)
      {
         for (String initializer : portableInterceptorInitializers)
         {
            if (initializer != null && !initializer.equals(""))
            {
               log.debug("Adding portable interceptor initializer: " + initializer);
               props.put("org.omg.PortableInterceptor.ORBInitializerClass." + initializer, "");
            }
         }
      }

      // Allows overriding of OAPort/OASSLPort 
      // from config file through MBean config
      if (port != 0)
         props.put("OAPort", Integer.toString(this.port));
      if (sslPort != 0)
         props.put("OASSLPort", Integer.toString(this.sslPort));

      // Keep the actual SSL port in a static field 
      // (the CSIv2 IOR interceptor needs this value)
      String oaSslPortString = props.getProperty("OASSLPort");
      if (oaSslPortString != null)
         oaSslPort = Integer.parseInt(oaSslPortString);

      // Allow SSL domain to be specified through bean config
      if (this.sslDomain != null) 
      {
         // Make SSL domain available to socket factories
         Registry.bind(SSL_DOMAIN, this.sslDomain);
      }

      // Initialize the ORB
      orb = ORB.init(new String[0], props);
      bind(ORB_NAME, "org.omg.CORBA.ORB");
      CorbaORB.setInstance(orb);
      ORBInitialContextFactory.setORB(orb);

      // Initialize the POA
      poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
      bind(POA_NAME, "org.omg.PortableServer.POA");

      // We will now create POAs for the CORBA transaction service. Those POAs
      // will remain in the holding state until the CORBA transaction service
      // MBean gets deployed.
      //
      // The early creation of OTS POAs serve the purpose of avoiding spurious
      // rollbacks that could happen between the ORB activation and the CORBA
      // transaction service activation. Such a rollback could happen if a 
      // resource sent us a reply_completion request in that time interval. 
      // The resource would get an OBJECT_NOT_EXIST exception and would presume
      // that the transaction had been rolled back. To avoid this problem, we
      // use the fact that CORBA ORBs do not throw OBJECT_NOT_EXIST while the
      // target POA is in the holding state. We also use the fact that JacORB 
      // release 2.2.2 (JBoss patch 3) does not throw OBJECT_NOT_EXIST if the 
      // target POA was not yet created and root POA is in the holding state. 
      // So we create the OTS POAs while the root POA is in the holding state. 
      // Then we activate the root POA, but keep the OTS POAs in the holding 
      // state. The OTS MBean will activate those POAs when it is started.

      Policy[] policies = { // Will create OTS POAs with these policies.
         poa.create_lifespan_policy(
                           LifespanPolicyValue.PERSISTENT),
         poa.create_id_assignment_policy(
                           IdAssignmentPolicyValue.USER_ID),
         poa.create_servant_retention_policy(
                           ServantRetentionPolicyValue.NON_RETAIN),
         poa.create_request_processing_policy(
                           RequestProcessingPolicyValue.USE_DEFAULT_SERVANT),
         poa.create_id_uniqueness_policy(
                           IdUniquenessPolicyValue.MULTIPLE_ID),
      };
      
      // Create a POA for OTS objects that are not Resource instances
      otsPoa = poa.create_POA("OTS", null, policies);
      
      // Create another POA for OTS Resources, so that they can have their own
      // default servant. (The name clash between Terminator::rollback and 
      // Resource::rollback disallows us to use a single default servant for 
      // all OTS objects.)
      otsResourcePoa = 
         poa.create_POA("OTSResources", otsPoa.the_POAManager(), policies);

      // OTS POAs already exist, we can activate the root POA.
      poa.the_POAManager().activate();

      // Make the ORB work
      new Thread(
         new Runnable() {
            public void run() {
               orb.run();
            }
         }, "ORB thread"
      ).start(); 

      // Create a POA for interface repositories
      try {
         LifespanPolicy lifespanPolicy = 
            poa.create_lifespan_policy(LifespanPolicyValue.PERSISTENT);
         IdAssignmentPolicy idAssignmentPolicy = 
            poa.create_id_assignment_policy(IdAssignmentPolicyValue.USER_ID);

         irPoa = poa.create_POA("IR", null,
                                new Policy[]{lifespanPolicy, 
                                             idAssignmentPolicy});
         bind(IR_POA_NAME, "org.omg.PortableServer.POA");
         
         // Activate the poa
         irPoa.the_POAManager().activate();
         
      } catch (Exception ex) {
         log.error("Error in IR POA initialization", ex);
      }
      
      // Keep a HandleDelegate
      hd = new HandleDelegateImpl();
   }

   public void stop() throws Exception
   {
      /*
      if( mJSR77 != null )
      {
          RMI_IIOPResource.destroy(
             getServer(),
             ORB_NAME
          );
          mJSR77 = null;
      }
      */
      try {
         // Unbind from JNDI
         unbind(ORB_NAME);
         unbind(POA_NAME);
         unbind(IR_POA_NAME);

         // Stop ORB
         orb.shutdown(false);
         
         // Unbind SSL domain
         Registry.unbind(SSL_DOMAIN);
      } catch (Exception e) {
         log.error("Exception while stopping ORB service", e);
      }
   }

   // CorbaORBServiceMBean implementation ---------------------------

   public ORB getORB()
   {
      return orb;
   }

   public HandleDelegate getHandleDelegate()
   {
      return hd;
   }
   
   public String getORBClass()
   {
      return orbClass;
   }

   public void setORBClass(String orbClass)
   {
      this.orbClass = orbClass;
   }

   public String getORBSingletonClass()
   {
      return orbSingletonClass;
   }

   public void setORBSingletonClass(String orbSingletonClass)
   {
      this.orbSingletonClass = orbSingletonClass;
   }

   public String getORBSingletonDelegate()
   {
      return orbSingletonDelegate;
   }

   public void setORBSingletonDelegate(String orbSingletonDelegate)
   {
      this.orbSingletonDelegate = orbSingletonDelegate;
   }

   public void setORBPropertiesFileName(String orbPropertiesFileName)
   {
      this.orbPropertiesFileName = orbPropertiesFileName;
   }

   public String getORBPropertiesFileName()
   {
      return orbPropertiesFileName;
   }

   public List<String> getPortableInterceptorInitializers()
   {
      return portableInterceptorInitializers;
   }

   public void setPortableInterceptorInitializers(
                                       List<String> portableInterceptorInitializers)
   {
      this.portableInterceptorInitializers = portableInterceptorInitializers;
   }

   public IorSecurityConfigMetaData getDefaultIORSecurityConfig()
   {
      return this.defaultIORSecurityConfig;
   }
   
   public void setDefaultIORSecurityConfig(IorSecurityConfigMetaData config)
   {
	   this.defaultIORSecurityConfig = config;
   }
   
   public void setPort(int port)
   {
      this.port = port;
   }

   public int getPort()
   {
      return this.port;
   }

   public void setSSLPort(int sslPort)
   {
      this.sslPort = sslPort;
   }

   public int getSSLPort()
   {
      return this.sslPort;
   }

   public void setSecurityDomain(SecurityDomain sslDomain)
   {
      this.sslDomain = sslDomain;
   }

   public SecurityDomain getSecurityDomain()
   {
      return this.sslDomain;
   }

   public boolean getSSLComponentsEnabled()
   {
      return CorbaORBService.sslComponentsEnabledFlag;
   }

   public void setSSLComponentsEnabled(boolean sslComponentsEnabled)
   {
      CorbaORBService.sslComponentsEnabledFlag = sslComponentsEnabled;
   }

   public boolean getSendSASAcceptWithExceptionEnabled()
   {
      return CorbaORBService.sendSasAcceptWithExceptionEnabledFlag;
   }

   public void setSendSASAcceptWithExceptionEnabled(boolean value)
   {
      CorbaORBService.sendSasAcceptWithExceptionEnabledFlag = value;
   }
   
   public boolean getOTSContextPropagationEnabled()
   {
      return CorbaORBService.otsContextPropagationEnabledFlag;
   }

   public void setOTSContextPropagationEnabled(boolean value)
   {
      CorbaORBService.otsContextPropagationEnabledFlag = value;
   }

   public boolean getSunJDK14IsLocalBugFix()
   {
      return false;
   }

   public void setSunJDK14IsLocalBugFix(boolean sunJDK14IsLocalBugFix)
   {
      log.warn("Not setting SunJDK14IsLocalBugFix since JDK1.4 is no longer supported.");
   }

   // ObjectFactory implementation ----------------------------------

   public Object getObjectInstance(Object obj, Name name,
                                   Context nameCtx, Hashtable<?,?> environment)
      throws Exception
   {
      String s = name.toString();
      if (log.isTraceEnabled())
         log.trace("getObjectInstance: obj.getClass().getName=\"" +
                        obj.getClass().getName() +
                        "\n                   name=" + s);
      if (ORB_NAME.equals(s))
         return orb;
      if (POA_NAME.equals(s))
         return poa;
      if (IR_POA_NAME.equals(s))
         return irPoa;
      return null;
   }


   // Private -------------------------------------------------------

   private void bind(String name, String className)
      throws Exception
   {
      Reference ref = new Reference(className, getClass().getName(), null);
      new InitialContext().bind("java:/"+name, ref);
   }

   private void unbind(String name)
      throws Exception
   {
      new InitialContext().unbind("java:/"+name);
   }

}

