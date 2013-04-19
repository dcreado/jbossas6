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
package org.jboss.test.compatibility.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jboss.tools.ClassVersionInfo;
import org.jboss.tools.SerialVersionUID;


/**
 * Tests of serial version uid compatibility across jboss versions
 *
 * @author Scott.Stark@jboss.org
 * @version $Revision: 103546 $
 */
public class SerialVersionUIDUnitTestCase extends TestCase
{
   static Map currentClassInfoMap;

   public SerialVersionUIDUnitTestCase(String name)
   {
      super(name);
   }

   /** Validate the 4.2.3.GA serial version uids against the current build
    * @throws Exception
    */
   public void test423Compatibility()
         throws Exception
   {
      // The packages in jboss-4.2.3 with known serialization issues
      String[] badPackages = {
      "com.arjuna.ats.internal.jbossatx.jta.PropagationContextManager",
       // Ignore org.apache.* issues
      "org.apache",
      // Ignore hibernate issues
      "org.hibernate",
       // Ignore jgroups issues
      "org.jgroups",
       //JBAS-5872 - serialVersionUID mismatches from 4.2.3.GA to be resolved
       "org.jboss.aspects.versioned.StateManager",
       "org.jboss.cache.CacheException",
       "org.jboss.cache.Fqn",
       "org.jboss.cache.Modification",
       "org.jboss.cache.ReplicationException",
       "org.jboss.mx.util.InstanceOfQueryExp",
       "org.jboss.console",
       "org.jboss.ejb3.stateful.StatefulBeanContextReference",
       "org.jboss.iiop",
       "org.jboss.security.NestableGroup",
       "org.jboss.security.NestablePrincipal",
       "org.jboss.tm.iiop._TransactionServiceStub",
       "org.jboss.wsf",
       // other
       "javax.xml.registry.JAXRException",
       "javax.xml.registry.RegistryException",
       // lib/endorsed/stax-api.jar
       "javax.xml.namespace.QName",
	//JBAS-6572
	"com.sun.faces.ext.validator.RegexValidator",
	//JBAS-6599
        "org.omg",
	// JBAS-7265
	"javassist",
	"org.jacorb",
	"javax.faces",
	"org.jboss.resource.adapter.jdbc.vendor.MSSQLValidConnectionChecker",
        "javax.xml.ws.soap.SOAPFaultException",
	"javax.persistence.OptimisticLockException"
      };

      System.out.println("+++ test423Compatibility");
      // load the 4.2.3 serialVersionUID database
      String etc = System.getProperty("jbosstest.src.etc", "../src/etc");
      File serFile = new File(etc, "serialVersionUID/423.ser");
      FileInputStream fis = new FileInputStream(serFile);
      ObjectInputStream ois = new ObjectInputStream(fis);
      Map classInfoMap = (Map) ois.readObject();
      System.out.println("4.2.3 serial classes count: "+classInfoMap.size());

      Map currentClassInfoMap = calcClassInfoMap();
      int mismatchCount = compare(classInfoMap, currentClassInfoMap, "423", badPackages);
      currentClassInfoMap.clear();
      System.out.println("serialVersionUID mismatches = "+mismatchCount);
      assertTrue("There are no serialVersionUID mismatches("+mismatchCount+")",
         mismatchCount == 0);
   }

  /** Validate the 5.0.0.GA serial version uids against the current build
    * @throws Exception
    */
   public void test500Compatibility()
         throws Exception
   {
      // The packages in jboss-5.0.0.GA with known serialization issues
      String[] badPackages = {
      "org.apache",
      "org.jboss.console",
      // those were changed to match 423 ids
      "org.jboss.crypto.JBossSXProvider",
      "org.jboss.metadata.rar.spec.JCA15MetaData",
      "org.jboss.resource.adapter.jdbc.CachedPreparedStatement",
      "org.jboss.resource.metadata.ConnectorMetaData",
      "org.jboss.resource.metadata.DescriptionMetaDataContainer",
      "org.jboss.security.SimplePrincipal",
      "org.jboss.security.SubjectSecurityProxyFactory",
      "org.jboss.services.binding.DuplicateServiceException",
      // ignore webservices framework classes
      "org.jboss.wsf",
      // lib/endorsed/stax-api.jar
      "javax.xml.namespace.QName",
	//JBAS-6572
	"com.sun.faces.ext.validator.RegexValidator",
	//JBAS-6599
        "org.omg",
	"org.jboss.metadata.rar",
	"org.jgroups",
	"org.jboss.web.tomcat.metadata.ContextMetaData",
	//JBMAN-91
	"org.jboss.managed.plugins.ManagedObjectImpl",
	// JBAS-7265
	"javassist",
	"org.jacorb",
	"javax.faces",
	"org.jboss.resource.adapter.jdbc.vendor.MSSQLValidConnectionChecker",
        "javax.xml.ws.soap.SOAPFaultException",
	"org.jboss.metadata.ear.jboss.JBossAppMetaData",
	"org.jboss.metadata.jpa.spec.PersistenceUnitMetaData",
        "org.jboss.classloading.spi.dependency.Module",
        // those were changed to match 423 ids
	"javax.xml.registry.JAXRException",
	"javax.xml.registry.RegistryException",
        "org.jboss.classloading.spi.dependency.Module",
	"org.hibernate",
	"javax.persistence.OptimisticLockException"
      };

      System.out.println("+++ test500Compatibility");
      // load the 5.0.0 serialVersionUID database
      String etc = System.getProperty("jbosstest.src.etc", "../src/etc");
      File serFile = new File(etc, "serialVersionUID/500.ser");
      FileInputStream fis = new FileInputStream(serFile);
      ObjectInputStream ois = new ObjectInputStream(fis);
      Map classInfoMap = (Map) ois.readObject();
      System.out.println("5.0.0.GA serial classes count: "+classInfoMap.size());

      Map currentClassInfoMap = calcClassInfoMap();
      int mismatchCount = compare(classInfoMap, currentClassInfoMap, "500", badPackages);
      currentClassInfoMap.clear();
      System.out.println("serialVersionUID mismatches = "+mismatchCount);
      assertTrue("There are no serialVersionUID mismatches("+mismatchCount+")",
         mismatchCount == 0);
   }


  /** Validate the 5.0.1.GA serial version uids against the current build
    * @throws Exception
    */
   public void test501Compatibility()
         throws Exception
   {
      // The packages in jboss-5.0.1.GA with known serialization issues
      String[] badPackages = {
      "javassist",
      "org.apache",
      "org.jboss.classloading.spi.dependency.Module",
      "org.jboss.console",
      // those were changed to match 423 ids
      "org.jboss.crypto.JBossSXProvider",
      "org.jboss.metadata.rar.spec.JCA15MetaData",
      "org.jboss.resource.adapter.jdbc.CachedPreparedStatement",
      "org.jboss.resource.metadata.ConnectorMetaData",
      "org.jboss.resource.metadata.DescriptionMetaDataContainer",
      "org.jboss.security.SimplePrincipal",
      "org.jboss.security.SubjectSecurityProxyFactory",
      "org.jboss.services.binding.DuplicateServiceException",
      // unclear if vfs classes are serialized to client
      // ignore webservices framework classes
      "org.jboss.wsf",
      // lib/endorsed/stax-api.jar
      "javax.xml.namespace.QName",
	"javassist",
	//JBAS-6572
	"com.sun.faces.ext.validator.RegexValidator",
	//JBAS-6599
        "org.omg",
	"org.jboss.metadata.rar",
	"org.jgroups",
	"org.jboss.web.tomcat.metadata.ContextMetaData",
	//JBMAN-91
	"org.jboss.managed.plugins.ManagedObjectImpl",
	// JBAS-7265
	"org.jacorb",
	"javax.faces",
	"org.jboss.resource.adapter.jdbc.vendor.MSSQLValidConnectionChecker",
        "javax.xml.ws.soap.SOAPFaultException",
	"org.jboss.metadata.ear.jboss.JBossAppMetaData",
	"org.jboss.metadata.jpa.spec.PersistenceUnitMetaData",
        // those were changed to match 423 ids
	"javax.xml.registry.JAXRException",
	"javax.xml.registry.RegistryException",
	"org.hibernate",
	"javax.persistence.OptimisticLockException"
      };

      System.out.println("+++ test501Compatibility");
      // load the 5.0.1.GA serialVersionUID database
      String etc = System.getProperty("jbosstest.src.etc", "../src/etc");
      File serFile = new File(etc, "serialVersionUID/501.ser");
      FileInputStream fis = new FileInputStream(serFile);
      ObjectInputStream ois = new ObjectInputStream(fis);
      Map classInfoMap = (Map) ois.readObject();
      System.out.println("5.0.1.GA serial classes count: "+classInfoMap.size());

      Map currentClassInfoMap = calcClassInfoMap();
      int mismatchCount = compare(classInfoMap, currentClassInfoMap, "501", badPackages);
      currentClassInfoMap.clear();
      System.out.println("serialVersionUID mismatches = "+mismatchCount);
      assertTrue("There are no serialVersionUID mismatches("+mismatchCount+")",
         mismatchCount == 0);
   }

   /** Validate the 5.1.0.GA serial version uids against the current build
    * @throws Exception
    */
   public void test510Compatibility()
         throws Exception
   {
      // The packages in jboss-5.1.0.GA with known serialization issues
      String[] badPackages = {
        // lib/endorsed/stax-api.jar
       "javax.xml.namespace.QName",
	//JBAS-6599
        "org.omg",
        //JBMAN-91
        "org.jboss.managed.plugins.ManagedObjectImpl",
	// JBAS-7265
	"javassist",
        "org.apache",
	"org.jacorb",
	"org.jgroups",
	"javax.faces",
	"org.jboss.resource.adapter.jdbc.vendor.MSSQLValidConnectionChecker",
        "javax.xml.ws.soap.SOAPFaultException",
	"org.jboss.classloading.spi.dependency.Module",
        "org.jboss.metadata.ear.jboss.JBossAppMetaData",
	"org.jboss.metadata.jpa.spec.PersistenceUnitMetaData",
        // ignore webservices framework classes as done in other test versions
        "org.jboss.wsf",
        // those were changed to match 423 ids
	"javax.xml.registry.JAXRException",
	"javax.xml.registry.RegistryException",
	"org.hibernate",
	"javax.persistence.OptimisticLockException"
      };

     System.out.println("+++ test510Compatibility");
      // load the 5.1.0.GA serialVersionUID database
      String etc = System.getProperty("jbosstest.src.etc", "../src/etc");
      File serFile = new File(etc, "serialVersionUID/510.ser");
      FileInputStream fis = new FileInputStream(serFile);
      ObjectInputStream ois = new ObjectInputStream(fis);
      Map classInfoMap = (Map) ois.readObject();
      System.out.println("5.1.0.GA serial classes count: "+classInfoMap.size());

      Map currentClassInfoMap = calcClassInfoMap();
      int mismatchCount = compare(classInfoMap, currentClassInfoMap, "510", badPackages);
      currentClassInfoMap.clear();
      System.out.println("serialVersionUID mismatches = "+mismatchCount);
      assertTrue("There are no serialVersionUID mismatches("+mismatchCount+")",
         mismatchCount == 0);
   }



   /** Validate the JavaEE 5.0 RI serial version uids against the current build
    * @throws Exception
    */
   public void testJavaEE50Compatibility()
         throws Exception
   {
      // The packages excluded with known serialization issues
      String[] badPackages = {
        "org.apache",
        "com.sun.faces.config.JSFVersionTracker",
        "com.sun.faces.lifecycle.ELResolverInitPhaseListener",
        "com.sun.faces.taglib.jsf_core.ConverterTag",
        "com.sun.faces.taglib.jsf_core.LoadBundleTag",
        "com.sun.faces.taglib.jsf_core.PhaseListenerTag",
        "com.sun.faces.taglib.jsf_core.ValidatorTag",
        "javax.servlet.GenericServlet",
        "javax.servlet.ServletException",
        "javax.xml.registry.JAXRException",
        "javax.xml.registry.RegistryException",
        "javax.persistence.OptimisticLockException",
        "javax.ejb.ConcurrentAccessException",
        "javax.xml.ws.soap.SOAPFaultException",
        "org.omg",
	// JBAS-7265
	"javax.faces"
      };

      System.out.println("+++ testJavaEE50Compatibility");
      System.getProperties().remove("org.jboss.j2ee.LegacySerialization");
      String etc = System.getProperty("jbosstest.src.etc", "../src/etc");
      File serFile = new File(etc, "serialVersionUID/javaee500.ser");
      FileInputStream fis = new FileInputStream(serFile);
      ObjectInputStream ois = new ObjectInputStream(fis);
      Map classInfoMap = (Map) ois.readObject();
      System.out.println("JavaEE RI serial classes count: "+classInfoMap.size());

      Map currentClassInfoMap = calcClassInfoMap();
      int mismatchCount = compare(classInfoMap, currentClassInfoMap, "JavaEE5.0", badPackages);
      currentClassInfoMap.clear();
      System.out.println("serialVersionUID mismatches = "+mismatchCount);
      assertTrue("There are no serialVersionUID mismatches("+mismatchCount+")",
         mismatchCount == 0);
   }

   private int compare(Map classInfoMap, Map currentClassInfoMap,
      String versionName, String[] badPackages)
   {
      int mismatchCount = 0;
      Iterator iter = currentClassInfoMap.values().iterator();
      while( iter.hasNext() )
      {
         ClassVersionInfo cvi = (ClassVersionInfo) iter.next();
         String name = cvi.getName();
         ClassVersionInfo cviLegacy = (ClassVersionInfo) classInfoMap.get(name);
         if( cviLegacy != null && cvi.getSerialVersion() != cviLegacy.getSerialVersion() )
         {
            String msg = "serialVersionUID error for "+name
               +", " + versionName + " " + cviLegacy.getSerialVersion()
               +", current: "+cvi.getSerialVersion();
            // Don't count classes from badPackages
            boolean isInBadPkg = false;
            for(int n = 0; n < badPackages.length; n ++)
            {
               String pkg = badPackages[n];
               if( name.startsWith(pkg) )
               {
                  isInBadPkg = true;
                  break;
               }
            }
            if( isInBadPkg == false )
            {
               mismatchCount ++;
               System.err.println("FAIL: " + msg);
            }
            else
            {
               System.out.println("EXCLUDED: " + msg);               
            }
         }
      }
      return mismatchCount;
   }

   static Map calcClassInfoMap()
      throws IOException
   {
      String jbossDist = System.getProperty("jbosstest.dist");
      File jbossHome = new File(jbossDist);
      jbossHome = jbossHome.getCanonicalFile();
      System.out.println("Calculating serialVersionUIDs for jbossHome: "+jbossHome);
      Map classInfoMap = SerialVersionUID.generateJBossSerialVersionUIDReport(jbossHome);
      return classInfoMap;
   }
   
   public static Test suite() throws Exception
   {
      // JBAS-3600, the execution order of tests in this test case is important
      // so it must be defined explicitly when running under some JVMs
      TestSuite suite = new TestSuite();
      suite.addTest(new SerialVersionUIDUnitTestCase("test423Compatibility"));
      suite.addTest(new SerialVersionUIDUnitTestCase("test500Compatibility"));
      suite.addTest(new SerialVersionUIDUnitTestCase("test501Compatibility"));
      suite.addTest(new SerialVersionUIDUnitTestCase("test510Compatibility"));
      //suite.addTest(new SerialVersionUIDUnitTestCase("testJavaEE50Compatibility"));

      return suite;
   }

   public static void main(String[] args)
   {
      junit.textui.TestRunner.run(SerialVersionUIDUnitTestCase.class);
   }
}
