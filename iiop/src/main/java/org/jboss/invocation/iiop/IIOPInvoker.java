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
package org.jboss.invocation.iiop;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.jboss.iiop.CorbaORBService;
import org.jboss.logging.Logger;
import org.jboss.naming.Util;
import org.jboss.system.Registry;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.Policy;
import org.omg.CORBA.SetOverrideType;
import org.omg.CORBA.UNKNOWN;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.IdUniquenessPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantLocator;
import org.omg.PortableServer.ServantRetentionPolicyValue;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.ServantLocatorPackage.CookieHolder;

/**
 * IIOP invoker that routs IIOP requests to CORBA servants.
 * It implements the interface <code>ServantRegistries</code>, which
 * gives access to four <code>ServantRegistry</code> instances:
 * <ul>
 * <li>a <code>ServantRegistry</code> with a single transient POA
 *     shared among all its servants;</li>
 * <li>a <code>ServantRegistry</code> with a single persistent POA
 *     shared among all its servants;</li>
 * <li>a <code>ServantRegistry</code> with a transient POA per servant;</li>
 * <li>a <code>ServantRegistry</code> with persistent POA per servant.</li>
 * </ul>
 *
 * CORBA servants registered with any of these 
 * <code>ServantRegistry</code> instances will receive IIOP invocations.
 * These CORBA servants will typically be thin wrappers that merely forward
 * to the JBoss MBean server any invocations they receive.
 * 
 * @author  <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 108275 $
 */
public class IIOPInvoker implements IIOPInvokerMBean, ServantRegistries, ObjectFactory
{

   // Attributes -------------------------------------------------------------

   private static Logger log = Logger.getLogger(IIOPInvoker.class);
	
   /** A reference to the singleton IIOPInvoker. */
   private static IIOPInvoker theIIOPInvoker;

   /** The root POA. **/
   private POA rootPOA;

   /** A ServantRegistry with a transient POA shared by all servants. */
   private ServantRegistry registryWithSharedTransientPOA;

   /** The transient POA used by the ServantRegistry above. */
   private POA transientPOA;

   /** The transient servant map used by the ServantRegistry above. */
   private Map<String, Servant> transientServantMap;

   /** A ServantRegistry with a persistent POA shared by all servants. */
   private ServantRegistry registryWithSharedPersistentPOA;

   /** The persistent POA used by the ServantRegistry above. */
   private POA persistentPOA;

   /** The persistent servant map used by the ServantRegistry above. */
   private Map<String, Servant> persistentServantMap;

   /** A ServantRegistry with a transient POA per servant. */
   private ServantRegistry registryWithTransientPOAPerServant;

   /** The transient POA map used by the ServantRegistry above. */
   private Map<String, POA> transientPoaMap;

   /** POA policies used by the ServantRegistry above. */
   private Policy[] transientPoaPolicies;

   /** A ServantRegistry with a persistent POA per servant. */
   private ServantRegistry registryWithPersistentPOAPerServant;

   /** The persistent POA map used by the ServantRegistry above. */
   private Map<String, POA> persistentPoaMap;

   /** POA policies used by the ServantRegistry above. */
   private Policy[] persistentPoaPolicies;

   /** The name used as a key to the IIOP invoker in the system registry. */
   private String serviceName;
   
   /** The MBean server that will be used by the servants to route the invocations to the EJB container. */
   private MBeanServer server;

   //======================= Microcontainer lifecycle methods =========================//

   public void create() throws Exception 
   {
      theIIOPInvoker = this;
      transientServantMap = Collections.synchronizedMap(new HashMap<String, Servant>());
      persistentServantMap = Collections.synchronizedMap(new HashMap<String, Servant>());
      transientPoaMap = Collections.synchronizedMap(new HashMap<String, POA>());
      persistentPoaMap = Collections.synchronizedMap(new HashMap<String, POA>());     
   }

   public void start() throws Exception 
   {
      // Get a reference for the root POA
      try {
         rootPOA = (POA)new InitialContext().lookup("java:/"
                                              + CorbaORBService.POA_NAME);
      } 
      catch (NamingException e) {
         throw new RuntimeException("Cannot lookup java:/"
                                    + CorbaORBService.POA_NAME + ": " + e);
      }

      // Policies for per-servant transient POAs
      transientPoaPolicies = new Policy[] {
         rootPOA.create_lifespan_policy(
                           LifespanPolicyValue.TRANSIENT),
         rootPOA.create_id_assignment_policy(
                           IdAssignmentPolicyValue.USER_ID),
         rootPOA.create_servant_retention_policy(
                           ServantRetentionPolicyValue.NON_RETAIN),
         rootPOA.create_request_processing_policy(
                           RequestProcessingPolicyValue.USE_DEFAULT_SERVANT),
         rootPOA.create_id_uniqueness_policy(
                           IdUniquenessPolicyValue.MULTIPLE_ID),
      };

      // Policies for per-servant persistent POAs
      persistentPoaPolicies = new Policy[] {
         rootPOA.create_lifespan_policy(
                           LifespanPolicyValue.PERSISTENT),
         rootPOA.create_id_assignment_policy(
                           IdAssignmentPolicyValue.USER_ID),
         rootPOA.create_servant_retention_policy(
                           ServantRetentionPolicyValue.NON_RETAIN),
         rootPOA.create_request_processing_policy(
                           RequestProcessingPolicyValue.USE_DEFAULT_SERVANT),
         rootPOA.create_id_uniqueness_policy(
                           IdUniquenessPolicyValue.MULTIPLE_ID),
      };

      // Policies for this IIOPInvoker's shared transient POA
      Policy[] policies = new Policy[] {
            rootPOA.create_lifespan_policy(
                        LifespanPolicyValue.TRANSIENT),
            rootPOA.create_id_assignment_policy(
                        IdAssignmentPolicyValue.USER_ID),
            rootPOA.create_servant_retention_policy(
                        ServantRetentionPolicyValue.NON_RETAIN),
            rootPOA.create_request_processing_policy(
                        RequestProcessingPolicyValue.USE_SERVANT_MANAGER),
            rootPOA.create_id_uniqueness_policy(
                        IdUniquenessPolicyValue.MULTIPLE_ID)
      };

      // Create this IIOPInvoker's shared transient POA 
      // and set its servant locator
      transientPOA = rootPOA.create_POA("TPOA", null, policies);
      transientPOA.set_servant_manager(new TransientServantLocator());

      // Change just one policy for this IIOPInvoker's shared persistent POA
      policies[0] = rootPOA.create_lifespan_policy(
            LifespanPolicyValue.PERSISTENT);
 
      // Create this IIOPInvoker's shared persisten POA 
      // and set its servant locator
      persistentPOA = rootPOA.create_POA("PPOA", null, policies);
      persistentPOA.set_servant_manager(new PersistentServantLocator());

      // Create this IIOPInvoker's ServantRegistry implementations
      registryWithSharedTransientPOA =
         new ServantRegistryWithSharedTransientPOA();
      registryWithSharedPersistentPOA =
         new ServantRegistryWithSharedPersistentPOA();
      registryWithTransientPOAPerServant =
         new ServantRegistryWithTransientPOAPerServant();
      registryWithPersistentPOAPerServant =
         new ServantRegistryWithPersistentPOAPerServant();

      // Export this invoker
      Registry.bind(new ObjectName(this.serviceName), this);
      
      // Activate my shared POAs
      transientPOA.the_POAManager().activate();
      persistentPOA.the_POAManager().activate();
      
      Context context = new InitialContext();
      
      // Bind the invoker in the JNDI invoker naming space
      Util.rebind(
            // The context
            context,
            // It should look like so "invokers/<name>/iiop" 
            "invokers/" + InetAddress.getLocalHost().getHostName() + "/iiop", 
            // A reference to this invoker
            new Reference(getClass().getName(), 
                          getClass().getName(), 
                          null));

      log.debug("Bound IIOP invoker for JMX node");
   }

   public void stop() throws Exception
   {
      // Destroy my shared POAs
      try {
         transientPOA.the_POAManager().deactivate(
                                            false, /* etherealize_objects */
                                            true   /* wait_for_completion */ );
         persistentPOA.the_POAManager().deactivate(
                                            false, /* etherealize_objects */
                                            true   /* wait_for_completion */ );
         transientPOA.destroy(false, /* etherealize_objects */
                              false  /* wait_for_completion */ );
         persistentPOA.destroy(false, /* etherealize_objects */
                               false  /* wait_for_completion */ );
      }
      catch (AdapterInactive adapterInactive) {
          log.error("Cannot deactivate home POA", adapterInactive);
      }
   }

   //======================= Bean properties getters/setters =========================//

   /**
    * <p>
    * Gets the name under which this {@code IIOPInvoker} will be registered.
    * </p>
    * 
    * @return a {@code String} representing the service name.
    */
   public String getServiceName() 
   {
      return this.serviceName;
   }

   /**
    * <p>
    * Sets the name under which this {@code IIOPInvoker} is to be registered.
    * </p>
    * 
    * @param serviceName a {@code String} representing the service name.
    */
   public void setServiceName(String serviceName) 
   {
      this.serviceName = serviceName;
   }
   
   /**
    * <p>
    * Gets a reference to the {@code MBeanServer}.
    * </p>
    * 
    * @return a reference to the {@code MBeanServer}.
    */
   public MBeanServer getServer() 
   {
      return server;
   }
   
   /**
    * <p>
    * Sets the {@code MBeanServer} that will be used by the servants to dispatch the invocations to
    * the EJB container.
    * </p>
    * 
    * @param server a reference to the {@code MBeanServer}.
    */
   public void setServer(MBeanServer server) 
   {
      this.server = server;
   }
   
   // Auxiliary static methods -----------------------------------------------

   private static Policy[] concatPolicies(Policy[] policies1, 
                                          Policy[] policies2)
   {
      Policy[] policies = new Policy[policies1.length + policies2.length];
      int j = 0;
      for (int i = 0; i < policies1.length; i++, j++) {
         policies[j] = policies1[i];
      }
      for (int i = 0; i < policies2.length; i++, j++) {
         policies[j] = policies2[i];
      }
      return policies;
   }


   // Implementation of the interface ServantRegistries -----------------------

   public ServantRegistry getServantRegistry(ServantRegistryKind kind)
   {
      if (kind == ServantRegistryKind.SHARED_TRANSIENT_POA) {
         return registryWithSharedTransientPOA;
      }
      else if (kind == ServantRegistryKind.SHARED_PERSISTENT_POA) {
         return registryWithSharedPersistentPOA;
      }
      else if (kind == ServantRegistryKind.TRANSIENT_POA_PER_SERVANT) {
         return registryWithTransientPOAPerServant;
      }
      else if (kind == ServantRegistryKind.PERSISTENT_POA_PER_SERVANT) {
         return registryWithPersistentPOAPerServant;
      }
      else {
         return null;
      }
   }

   // Implementation of the interface ObjectFactory ---------------------------

   public Object getObjectInstance(Object obj, Name name,
                                   Context nameCtx, Hashtable<?,?> environment)
         throws Exception
   {
      String s = name.toString();
      if (log.isTraceEnabled())
         log.trace("getObjectInstance: obj.getClass().getName=\"" +
                        obj.getClass().getName() +
                        "\n                   name=" + s);
      if (s.equals("iiop"))
         return theIIOPInvoker;
      else
         return null;
   }

   // Static nested classes that implement the interface ReferenceFactory -----

   static class PoaAndPoliciesReferenceFactory
      implements ReferenceFactory
   {
      private POA poa;
      private String servantName;
      private Policy[] policies;
      private byte[] servantId;
      
      PoaAndPoliciesReferenceFactory(POA poa, 
                                     String servantName, Policy[] policies)
      {
         this.poa = poa;
         this.servantName = servantName;
         this.policies = policies;
         servantId = ReferenceData.create(servantName);
      }
      
      PoaAndPoliciesReferenceFactory(POA poa, Policy[] policies)
      {
         this(poa, null, policies);
      }
      
      public org.omg.CORBA.Object createReference(String interfId)
            throws Exception 
      {
         org.omg.CORBA.Object corbaRef = 
            poa.create_reference_with_id(servantId, interfId);
         return corbaRef._set_policy_override(policies, 
                                              SetOverrideType.ADD_OVERRIDE);
      }

      public org.omg.CORBA.Object createReferenceWithId(Object id, 
                                                        String interfId)
            throws Exception
      {
         byte[] referenceData = 
            (servantName == null) ? ReferenceData.create(id) 
                                  : ReferenceData.create(servantName, id);
         org.omg.CORBA.Object corbaRef =
            poa.create_reference_with_id(referenceData, interfId);
         return corbaRef._set_policy_override(policies, 
                                              SetOverrideType.ADD_OVERRIDE);
      }

      public POA getPOA()
      {
         return poa;
      }

   }

   static class PoaReferenceFactory
      implements ReferenceFactory
   {
      private POA poa;
      private String servantName;
      private byte[] servantId;
      
      
      PoaReferenceFactory(POA poa, String servantName)
      {
         this.poa = poa;
         this.servantName = servantName;
         servantId = ReferenceData.create(servantName);
      }
      
      PoaReferenceFactory(POA poa)
      {
         this(poa, null);
      }
      
      public org.omg.CORBA.Object createReference(String interfId)
            throws Exception
      {
         return poa.create_reference_with_id(servantId, interfId);
      }

      public org.omg.CORBA.Object createReferenceWithId(Object id, 
                                                        String interfId)
            throws Exception
      {
         byte[] referenceData = 
            (servantName == null) ? ReferenceData.create(id) 
                                  : ReferenceData.create(servantName, id);
         return poa.create_reference_with_id(referenceData, interfId);
      }

      public POA getPOA()
      {
         return poa;
      }

   }

   // Inner classes that implement the interface ServantRegistry --------------

   /** ServantRegistry with a shared transient POA */
   class ServantRegistryWithSharedTransientPOA
         implements ServantRegistry 
   {
      public ReferenceFactory bind(String name, 
                                   Servant servant, 
                                   Policy[] policies)
      {
         if (servant instanceof ServantWithMBeanServer) {
            ((ServantWithMBeanServer)servant).setMBeanServer(getServer());
         }
         transientServantMap.put(name, servant);
         return new PoaAndPoliciesReferenceFactory(transientPOA, 
                                                   name, policies);
      }
      
      public ReferenceFactory bind(String name, Servant servant)
      {
         if (servant instanceof ServantWithMBeanServer) {
            ((ServantWithMBeanServer)servant).setMBeanServer(getServer());
         }
         transientServantMap.put(name, servant);
         return new PoaReferenceFactory(transientPOA, name);
      }

      public void unbind(String name)
      {
         transientServantMap.remove(name);
      }
      
   }

   /** ServantRegistry with a shared persistent POA */
   class ServantRegistryWithSharedPersistentPOA
         implements ServantRegistry 
   {
      public ReferenceFactory bind(String name, 
                                   Servant servant, 
                                   Policy[] policies)
      {
         if (servant instanceof ServantWithMBeanServer) {
            ((ServantWithMBeanServer)servant).setMBeanServer(getServer());
         }
         persistentServantMap.put(name, servant);
         return new PoaAndPoliciesReferenceFactory(persistentPOA, 
                                                   name, policies);
      }
      
      public ReferenceFactory bind(String name, Servant servant)
      {
         if (servant instanceof ServantWithMBeanServer) {
            ((ServantWithMBeanServer)servant).setMBeanServer(getServer());
         }
         persistentServantMap.put(name, servant);
         return new PoaReferenceFactory(persistentPOA, name);
      }

      public void unbind(String name)
      {
         persistentServantMap.remove(name);
      }
      
   }

   /** ServantRegistry with a transient POA per servant */
   class ServantRegistryWithTransientPOAPerServant
         implements ServantRegistry
   {

      public ReferenceFactory bind(String name, 
                                   Servant servant, 
                                   Policy[] policies)
            throws Exception
      {
         if (servant instanceof ServantWithMBeanServer) {
            ((ServantWithMBeanServer)servant).setMBeanServer(getServer());
         }
         Policy[] poaPolicies = concatPolicies(transientPoaPolicies, policies);
         POA poa = rootPOA.create_POA(name, null, poaPolicies);
         transientPoaMap.put(name, poa);
         poa.set_servant(servant);
         poa.the_POAManager().activate();
         return new PoaReferenceFactory(poa); // no servantName: in this case
                                              // name is the POA name
      }

      public ReferenceFactory bind(String name, Servant servant)
            throws Exception
      {
         if (servant instanceof ServantWithMBeanServer) {
            ((ServantWithMBeanServer)servant).setMBeanServer(getServer());
         }
         POA poa = rootPOA.create_POA(name, null, transientPoaPolicies);
         transientPoaMap.put(name, poa);
         poa.set_servant(servant);
         poa.the_POAManager().activate();
         return new PoaReferenceFactory(poa); // no servantName: in this case
                                              // name is the POA name
      }

      public void unbind(String name)
            throws Exception
      {
         POA poa = (POA) transientPoaMap.remove(name);
         if (poa != null) {
            poa.the_POAManager().deactivate(false, /* etherealize_objects */
                                            true   /* wait_for_completion */ );
            poa.destroy(false, /* etherealize_objects */
                        false  /* wait_for_completion */ );
         }
      }
      
   }

   /** ServantRegistry with a persistent POA per servant */
   class ServantRegistryWithPersistentPOAPerServant
         implements ServantRegistry
   {

      public ReferenceFactory bind(String name, 
                                   Servant servant, 
                                   Policy[] policies)
            throws Exception
      {
         if (servant instanceof ServantWithMBeanServer) {
            ((ServantWithMBeanServer)servant).setMBeanServer(getServer());
         }
         Policy[] poaPolicies = 
            concatPolicies(persistentPoaPolicies, policies);
         POA poa = rootPOA.create_POA(name, null, poaPolicies);
         persistentPoaMap.put(name, poa);
         poa.set_servant(servant);
         poa.the_POAManager().activate();
         return new PoaReferenceFactory(poa); // no servantName: in this case
                                              // name is the POA name
      }

      public ReferenceFactory bind(String name, Servant servant)
            throws Exception
      {
         if (servant instanceof ServantWithMBeanServer) {
            ((ServantWithMBeanServer)servant).setMBeanServer(getServer());
         }
         POA poa = rootPOA.create_POA(name, null, persistentPoaPolicies);
         persistentPoaMap.put(name, poa);
         poa.set_servant(servant);
         poa.the_POAManager().activate();
         return new PoaReferenceFactory(poa); // no servantName: in this case
                                              // name is the POA name
      }

      public void unbind(String name)
            throws Exception
      {
         POA poa = (POA) persistentPoaMap.remove(name);
         if (poa != null) {
            poa.the_POAManager().deactivate(false, /* etherealize_objects */
                                            true   /* wait_for_completion */ );
            poa.destroy(false, /* etherealize_objects */
                        false  /* wait_for_completion */ );
         }
      }

   }

   // Inner classes that implement the interface ServantLocator ---------------

   /** ServantLocator for the shared transient POA */
   class TransientServantLocator       
         extends LocalObject 
         implements ServantLocator 
   {

      public Servant preinvoke(byte[] oid, 
                               POA adapter, 
                               String operation, 
                               CookieHolder the_cookie) 
      {
         try {
            the_cookie.value = null;
            Object id = ReferenceData.extractServantId(oid);
            return (Servant)transientServantMap.get(id);
         }
         catch (Exception e) {
            log.trace("Unexpected exception in preinvoke:", e);
            throw new UNKNOWN(e.toString());
         }
      }
      
      public void postinvoke(byte[] oid, 
                             POA adapter, 
                             String operation, 
                             Object the_cookie, 
                             Servant the_servant) 
      {
      }

   }

   /** ServantLocator for the shared persistent POA */
   class PersistentServantLocator
         extends LocalObject 
         implements ServantLocator 
   {

      public Servant preinvoke(byte[] oid, 
                               POA adapter, 
                               String operation, 
                               CookieHolder the_cookie) 
      {
         try {
            the_cookie.value = null;
            Object id = ReferenceData.extractServantId(oid);
            return (Servant)persistentServantMap.get(id);
         }
         catch (Exception e) {
            log.trace("Unexpected exception in preinvoke:", e);
            throw new UNKNOWN(e.toString());
         }
      }
      
      public void postinvoke(byte[] oid, 
                             POA adapter, 
                             String operation, 
                             Object the_cookie, 
                             Servant the_servant) 
      {
      }

   }

}
