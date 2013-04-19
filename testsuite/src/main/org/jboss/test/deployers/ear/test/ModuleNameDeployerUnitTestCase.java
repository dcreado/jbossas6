/**
 * 
 */
package org.jboss.test.deployers.ear.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployment.ModuleNameDeployer;
import org.jboss.metadata.client.jboss.JBossClientMetaData;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.metadata.ear.spec.ModulesMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.javaee.jboss.NamedModule;
import org.jboss.metadata.web.jboss.JBossWebMetaData;

/**
 * Unit tests for {@link ModuleNameDeployer}.
 * 
 * @author Brian Stansberry
 * @version $Revision: 102186 $
 */
public class ModuleNameDeployerUnitTestCase extends TestCase
{
   private static final Set<Class<? extends NamedModule>> standardTypes = new HashSet<Class<? extends NamedModule>>();
   static
   {
      standardTypes.add(JBossWebMetaData.class);
      standardTypes.add(JBossMetaData.class);
      standardTypes.add(JBossClientMetaData.class);
//      standardTypes.add(getJCAMetaDataClass());
   }
   
   // At the time this test is being written, the JCA metadata class that
   // will implement NamedModule is unclear. This method lets me isolate
   // the effect of that decision on the test code in one place.
//   private static NamedModule getJCAMetaData()
//   {
//      return new RARDeploymentMetaData();
//   }
//   
//   private static Class<? extends NamedModule> getJCAMetaDataClass()
//   {
//      return getJCAMetaData().getClass();
//   }
   
   private static final String TEST = "test";
   private static final String SIMPLE = "simple";
   private static final String PATH = "pathTo/";
   private static final String SIMPLE_DU_EAR = SIMPLE + ".ear";
   private static final String SIMPLE_DU_WAR = SIMPLE + ".war";
   private static final String SIMPLE_DU_WAR_PATH = PATH + SIMPLE_DU_WAR;
   private static final String SIMPLE_DU_JAR = SIMPLE + ".jar";
   private static final String SIMPLE_DU_JAR_PATH = PATH + SIMPLE_DU_JAR;
   private static final String SIMPLE_DU_RAR = SIMPLE + ".rar";
   private static final String SIMPLE_DU_RAR_PATH = PATH + SIMPLE_DU_RAR;
   private static final String SIMPLE_DU_APPCLIENT = SIMPLE + "-client.jar";
   private static final String SIMPLE_DU_APPCLIENT_PATH = PATH + SIMPLE_DU_APPCLIENT;
   private static final String SIMPLE_TRIMMED_PATH = PATH + SIMPLE;
   private static final String SIMPLE_TRIMMED_CLIENT_PATH = PATH + SIMPLE + "-client";
   
   private ModuleNameDeployer testee;
   
   /**
    * @param name
    */
   public ModuleNameDeployerUnitTestCase(String name)
   {
      super(name);
   }

   /* (non-Javadoc)
    * @see junit.framework.TestCase#setUp()
    */
   protected void setUp() throws Exception
   {
      super.setUp();
      testee = new ModuleNameDeployer(standardTypes);
   }
   
   /** Ensure we handle DUs that we're not interested in */
   public void testNoNamedModules() throws DeploymentException
   {
      DUIH duih = new DUIH();
      DeploymentUnit unit = getDeploymentUnitProxy(duih);
      testee.deploy(unit);
      assertNull(duih.getNamedModuleMetaData(NamedModule.class));
   }
   
   /** Basic case where there's 1 MD object that specified its name */
   public void testConfiguredName() throws DeploymentException
   {
      DUIH duih = getSingleAttachmentDUIH();
      NamedModule md = getSingleAttachment(duih);
      md.setModuleName(TEST);
      DeploymentUnit unit = getDeploymentUnitProxy(duih);
      testee.deploy(unit);
      assertEquals(TEST, md.getModuleName());
      assertEquals(TEST, duih.getModuleName(NamedModule.class));
   }

   
   /** DU has 1 MD object that specified its name; others that didn't */
   public void testOneConfiguredNameMultipleAttachments() throws DeploymentException
   {
      DUIH duih = getMultiAttachmentDUIH();
      duih.getNamedModuleMetaData(JBossWebMetaData.class).setModuleName(TEST);
      DeploymentUnit unit = getDeploymentUnitProxy(duih);
      testee.deploy(unit);
      for (Class<? extends NamedModule> type : standardTypes)
      {
         assertEquals("Module name correct for " + type, TEST, duih.getModuleName(type));
      }
      assertEquals(TEST, duih.getModuleName(NamedModule.class));
   }
   
   /** Basic case where there's 1 MD object that did not specify its name */
   public void testNoConfiguredName() throws DeploymentException
   {
      DUIH duih = getSingleAttachmentDUIH();
      DeploymentUnit unit = getDeploymentUnitProxy(duih);
      testee.deploy(unit);
      assertEquals(SIMPLE, getSingleAttachment(duih).getModuleName());
      assertEquals(SIMPLE, duih.getModuleName(NamedModule.class));
   }
   
   /** DU has multiple attachments none of which specified its name */
   public void testNoConfiguredNameMultipleAttachments() throws DeploymentException
   {
      DUIH duih = getMultiAttachmentDUIH();
      DeploymentUnit unit = getDeploymentUnitProxy(duih);
      testee.deploy(unit);
      for (Class<? extends NamedModule> type : standardTypes)
      {
         assertEquals("Module name correct for " + type, SIMPLE, duih.getModuleName(type));
      }
      assertEquals(SIMPLE, duih.getModuleName(NamedModule.class));
   }
   
   /** Same as testConfiguredName but in an EAR */
   public void testConfiguredNameWithParent() throws DeploymentException
   {
      DUIH duih = getDUIHWithParent(false);
      NamedModule md = getSingleAttachment(duih);
      md.setModuleName(TEST);
      DeploymentUnit unit = getDeploymentUnitProxy(duih);
      testee.deploy(unit);
      assertEquals(TEST, md.getModuleName());
      assertEquals(TEST, duih.getModuleName(NamedModule.class));
      
      ModuleMetaData modmd = getModuleMetaData(duih);
      assertEquals(TEST, modmd.getUniqueName());
   }

   public void testOneConfiguredNameMultipleAttachmentsWithParent() throws DeploymentException
   {
      DUIH duih = getDUIHWithParent(true);
      duih.getNamedModuleMetaData(JBossWebMetaData.class).setModuleName(TEST);
      DeploymentUnit unit = getDeploymentUnitProxy(duih);
      testee.deploy(unit);
      for (Class<? extends NamedModule> type : standardTypes)
      {
         assertEquals("Module name correct for " + type, TEST, duih.getModuleName(type));
      }
      assertEquals(TEST, duih.getModuleName(NamedModule.class));
      
      ModuleMetaData modmd = getModuleMetaData(duih);
      assertEquals(TEST, modmd.getUniqueName());
   }
   
   public void testNoConfiguredNameWithParent() throws DeploymentException
   {
      DUIH duih = getDUIHWithParent(false);
      DeploymentUnit unit = getDeploymentUnitProxy(duih);
      testee.deploy(unit);
      assertEquals(SIMPLE_TRIMMED_PATH, getSingleAttachment(duih).getModuleName());
      assertEquals(SIMPLE_TRIMMED_PATH, duih.getModuleName(NamedModule.class));
      
      ModuleMetaData modmd = getModuleMetaData(duih);
      assertEquals(SIMPLE_TRIMMED_PATH, modmd.getUniqueName());
   }
   
   public void testNoConfiguredNameMultipleAttachmentsWithParent() throws DeploymentException
   {
      DUIH duih = getDUIHWithParent(true);
      DeploymentUnit unit = getDeploymentUnitProxy(duih);
      testee.deploy(unit);
      for (Class<? extends NamedModule> type : standardTypes)
      {
         assertEquals("Module name correct for " + type, SIMPLE_TRIMMED_PATH, duih.getModuleName(type));
      }
      assertEquals(SIMPLE_TRIMMED_PATH, duih.getModuleName(NamedModule.class));
      
      ModuleMetaData modmd = getModuleMetaData(duih);
      assertEquals(SIMPLE_TRIMMED_PATH, modmd.getUniqueName());
   }
   
   public void testConfiguredNameConflict() throws DeploymentException
   {
      DUIH duih = getDUIHWithParent(false);
      NamedModule md = getSingleAttachment(duih);
      md.setModuleName(TEST);
      ModulesMetaData modules = getModulesMetaData(duih);
      ModuleMetaData conflict = new ModuleMetaData();
      conflict.setName(SIMPLE_DU_JAR_PATH);
      conflict.setUniqueName(TEST);
      modules.add(conflict);
      
      DeploymentUnit unit = getDeploymentUnitProxy(duih);
      testee.deploy(unit);
      assertEquals(SIMPLE_TRIMMED_PATH, md.getModuleName());
      assertEquals(SIMPLE_TRIMMED_PATH, duih.getModuleName(NamedModule.class));
      
      ModuleMetaData modmd = getModuleMetaData(duih);
      assertEquals(SIMPLE_TRIMMED_PATH, modmd.getUniqueName());
      
      assertEquals(TEST, conflict.getUniqueName());
   }
   
   public void testNoConfiguredNameConflict() throws DeploymentException
   {
      DUIH duih = getDUIHWithParent(false);
      ModulesMetaData modules = getModulesMetaData(duih);
      ModuleMetaData conflict = new ModuleMetaData();
      conflict.setName(SIMPLE_DU_JAR_PATH);
      conflict.setUniqueName(SIMPLE_TRIMMED_PATH);
      modules.add(conflict);
      
      DeploymentUnit unit = getDeploymentUnitProxy(duih);
      testee.deploy(unit);
      assertEquals(SIMPLE_DU_WAR_PATH, getSingleAttachment(duih).getModuleName());
      assertEquals(SIMPLE_DU_WAR_PATH, duih.getModuleName(NamedModule.class));
      
      ModuleMetaData modmd = getModuleMetaData(duih);
      assertEquals(SIMPLE_DU_WAR_PATH, modmd.getUniqueName());
      
      assertEquals(SIMPLE_TRIMMED_PATH, conflict.getUniqueName());
   }
   
   public void testConfiguredNamePathologicalConflict() throws DeploymentException
   {
      DUIH duih = getDUIHWithParent(false);
      NamedModule md = getSingleAttachment(duih);
      md.setModuleName(TEST);
      
      ModulesMetaData modules = getModulesMetaData(duih);
      ModuleMetaData conflict = new ModuleMetaData();
      conflict.setName(SIMPLE_DU_JAR_PATH);
      conflict.setUniqueName(TEST);
      modules.add(conflict);
      
      ModuleMetaData conflict2 = new ModuleMetaData();
      conflict2.setName(SIMPLE_DU_RAR_PATH);
      conflict2.setUniqueName(SIMPLE_TRIMMED_PATH);
      modules.add(conflict2);
      
      ModuleMetaData conflict3 = new ModuleMetaData();
      conflict3.setName("pathological.war");
      conflict3.setUniqueName(SIMPLE_DU_WAR_PATH);
      modules.add(conflict3);
      
      ModuleMetaData conflict4 = new ModuleMetaData();
      conflict4.setName("more-pathological.war");
      conflict4.setUniqueName(SIMPLE_TRIMMED_PATH + "-1");
      modules.add(conflict4);
      
      String expected = "test-1";
      
      DeploymentUnit unit = getDeploymentUnitProxy(duih);
      testee.deploy(unit);
      assertEquals(expected, md.getModuleName());
      assertEquals(expected, duih.getModuleName(NamedModule.class));
      
      ModuleMetaData modmd = getModuleMetaData(duih);
      assertEquals(expected, modmd.getUniqueName());
   }
   
   public void testNoConfiguredNamePathologicalConflict() throws DeploymentException
   {
      DUIH duih = getDUIHWithParent(false);
      
      ModulesMetaData modules = getModulesMetaData(duih);
      ModuleMetaData conflict = new ModuleMetaData();
      conflict.setName(SIMPLE_DU_JAR_PATH);
      conflict.setUniqueName(TEST);
      modules.add(conflict);
      
      ModuleMetaData conflict2 = new ModuleMetaData();
      conflict2.setName(SIMPLE_DU_RAR_PATH);
      conflict2.setUniqueName(SIMPLE_TRIMMED_PATH);
      modules.add(conflict2);
      
      ModuleMetaData conflict3 = new ModuleMetaData();
      conflict3.setName("pathological.war");
      conflict3.setUniqueName(SIMPLE_DU_WAR_PATH);
      modules.add(conflict3);
      
      ModuleMetaData conflict4 = new ModuleMetaData();
      conflict4.setName("more-pathological.war");
      conflict4.setUniqueName(SIMPLE_TRIMMED_PATH + "-1");
      modules.add(conflict4);
      
      String expected = SIMPLE_TRIMMED_PATH + "-2";
      
      DeploymentUnit unit = getDeploymentUnitProxy(duih);
      testee.deploy(unit);
      assertEquals(expected, getSingleAttachment(duih).getModuleName());
      assertEquals(expected, duih.getModuleName(NamedModule.class));
      
      ModuleMetaData modmd = getModuleMetaData(duih);
      assertEquals(expected, modmd.getUniqueName());
   }
   
   /**
    * Tests situation where several DUs associated with same ear are
    * concurrently passing through deployer.
    * 
    * @throws InterruptedException
    */
   public void testConcurrentDeployments() throws InterruptedException
   {
      // Set up an ear with a war, jar, ear
      
      DUIH parentDUIH = getParentDUIH(true);
      DeploymentUnit parent = getDeploymentUnitProxy(parentDUIH);
      JBossAppMetaData appmd = (JBossAppMetaData) parentDUIH.attachments.get(JBossAppMetaData.class);
      ModulesMetaData modsmd = appmd.getModules();
      
      DUIH war = getSingleAttachmentDUIH(); // this does most setup for war
      war.parent = parent;
      war.relativePath = SIMPLE_DU_WAR_PATH;
      ModuleMetaData warmod = appmd.getModule(SIMPLE_DU_WAR_PATH);
      
      DUIH jar = new DUIH();
      jar.attachments.put(JBossMetaData.class, new JBossMetaData());
      jar.parent = parent;
      jar.relativePath = SIMPLE_DU_JAR_PATH;
      jar.simpleName = SIMPLE_DU_JAR;
      ModuleMetaData jarmod = new ModuleMetaData();
      jarmod.setName(SIMPLE_DU_JAR_PATH);
      modsmd.add(jarmod);
      
      DUIH appClient = new DUIH();
      appClient.attachments.put(JBossClientMetaData.class, new JBossClientMetaData());
      appClient.parent = parent;
      appClient.relativePath = SIMPLE_DU_APPCLIENT_PATH;
      appClient.simpleName = SIMPLE_DU_APPCLIENT;
      ModuleMetaData appclientmod = new ModuleMetaData();
      appclientmod.setName(SIMPLE_DU_APPCLIENT_PATH);
      modsmd.add(appclientmod);
      
      // Make assertions simple by not letting anything have SIMPLE_TRIMMED_PATH
      ModuleMetaData conflict = new ModuleMetaData();
      conflict.setName("conflict.war");
      conflict.setUniqueName(SIMPLE_TRIMMED_PATH);
      modsmd.add(conflict);
      
      ExecutorService executor = Executors.newFixedThreadPool(3);
      
      CountDownLatch startLatch = new CountDownLatch(4);
      CountDownLatch finishLatch = new CountDownLatch(3);
      
      DeploymentTask warTask = new DeploymentTask(startLatch, finishLatch, war);
      executor.execute(warTask);
      
      DeploymentTask jarTask = new DeploymentTask(startLatch, finishLatch, jar);
      executor.execute(jarTask);
      
      DeploymentTask appclientTask = new DeploymentTask(startLatch, finishLatch, appClient);
      executor.execute(appclientTask);
      
      startLatch.countDown();
      
      assertTrue(finishLatch.await(5, TimeUnit.SECONDS));
      
      assertNull(warTask.exception);
      assertNull(jarTask.exception);
      assertNull(appclientTask.exception);
      
      assertEquals(SIMPLE_DU_WAR_PATH, getSingleAttachment(war).getModuleName());
      assertEquals(SIMPLE_DU_WAR_PATH, warmod.getUniqueName());
      
      assertEquals(SIMPLE_DU_JAR_PATH, ((NamedModule) jar.attachments.get(JBossMetaData.class)).getModuleName());
      assertEquals(SIMPLE_DU_JAR_PATH, jarmod.getUniqueName());
      
      assertEquals(SIMPLE_TRIMMED_CLIENT_PATH, ((NamedModule) appClient.attachments.get(JBossClientMetaData.class)).getModuleName());
      assertEquals(SIMPLE_TRIMMED_CLIENT_PATH, appclientmod.getUniqueName());
   }
   
   private static JBossAppMetaData getJBossAppMetaData(boolean addModule)
   {
      JBossAppMetaData appmd = new JBossAppMetaData();
      appmd.setModules(new ModulesMetaData());
      if (addModule)
      {
         ModuleMetaData modmd = new ModuleMetaData();
         modmd.setName(SIMPLE_DU_WAR_PATH);
         appmd.getModules().add(modmd);         
      }
      return appmd;
   }
   
   private static DeploymentUnit getDeploymentUnitProxy(DUIH duih)
   {
      return (DeploymentUnit) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{DeploymentUnit.class}, duih);
   }
   
   private static DUIH getMultiAttachmentDUIH()
   {
      DUIH duih = new DUIH();
      JBossWebMetaData webmd = new JBossWebMetaData();
      duih.attachments.put(JBossWebMetaData.class, webmd);
      JBossMetaData ejbmd = new JBossMetaData();
      duih.attachments.put(JBossMetaData.class, ejbmd);
      JBossClientMetaData clientmd = new JBossClientMetaData();
      duih.attachments.put(JBossClientMetaData.class, clientmd);
//      NamedModule jcamd = getJCAMetaData();
//      duih.attachments.put(jcamd.getClass(), jcamd);
      
      return duih;
   }
   
   private static DUIH getSingleAttachmentDUIH()
   {
      DUIH duih = new DUIH();
      JBossWebMetaData webmd = new JBossWebMetaData();
      duih.attachments.put(JBossWebMetaData.class, webmd);
      return duih;
   }
   
   private NamedModule getSingleAttachment(DUIH duih)
   {
      return duih.getNamedModuleMetaData(JBossWebMetaData.class);
   }
   
   private static DUIH getDUIHWithParent(boolean multiAttachment)
   {
      DUIH parent = getParentDUIH(true);
      DUIH child = multiAttachment ? getMultiAttachmentDUIH() : getSingleAttachmentDUIH();
      child.parent = getDeploymentUnitProxy(parent);
      child.relativePath = SIMPLE_DU_WAR_PATH;
      return child;
   }
   
   private static DUIH getParentDUIH(boolean addModule)
   {
      DUIH parent = new DUIH();
      parent.simpleName = SIMPLE_DU_EAR;
      JBossAppMetaData appmd = getJBossAppMetaData(addModule);
      parent.attachments.put(JBossAppMetaData.class, appmd);
      return parent;
   }
   
   private static ModulesMetaData getModulesMetaData(DUIH child)
   {
      DUIH parent = getParentDUIH(child);
      JBossAppMetaData appmd = (JBossAppMetaData) parent.attachments.get(JBossAppMetaData.class);
      return appmd.getModules();
   }
   
   private static ModuleMetaData getModuleMetaData(DUIH child)
   {
      DUIH parent = getParentDUIH(child);
      JBossAppMetaData appmd = (JBossAppMetaData) parent.attachments.get(JBossAppMetaData.class);
      return appmd.getModule(child.relativePath);
   }
   
   private static DUIH getParentDUIH(DUIH child)
   {
      return (DUIH) Proxy.getInvocationHandler(child.parent);
   }
   
   /** 
    * Simple InvocationHandler for mock DeploymentUnit proxies that only handles the 
    * few methods ModuleNameDeployer is expected to invoke. Bit poor as it's 
    * tightly coupled to expected implementation, but if ModuleNameDeployer adds some 
    * new call for some reason, just add handling for it here.
    */
   private static class DUIH implements InvocationHandler
   {
      private final Map<Object, Object> attachments = new HashMap<Object, Object>();
      private DeploymentUnit parent;
      private String simpleName = SIMPLE_DU_WAR;
      private String relativePath = simpleName;

      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
      {
         Object result = null;
         String methodName = method.getName();
         if ("addAttachment".equals(method.getName()) && args != null && args.length == 2)
         {
            attachments.put(args[0], args[1]);
         }
         else if ("getAttachment".equals(method.getName()) && args != null && args.length == 1)
         {
            result = attachments.get(args[0]);
         }
         else if ("getParent".equals(method.getName()) && (args == null || args.length == 0))
         {
            result = parent;
         }
         else if ("getSimpleName".equals(method.getName()) && (args == null || args.length == 0))
         {
            return simpleName;
         }
         else if ("getName".equals(method.getName()) && (args == null || args.length == 0))
         {
            return simpleName;
         }
         else if ("getRelativePath".equals(method.getName()) && (args == null || args.length == 0))
         {
            return relativePath;
         }
         else if ("toString".equals(method.getName()) && (args == null || args.length == 0))
         {
            return "MockDeploymentUnit[" + relativePath + "]";
         }
         else
         {
            throw new IllegalArgumentException("ModuleNameDeployer invoked unknown method " + 
                  methodName + " or number of args " + args + 
                  " -- please add them to this test's mock invocation handler");
         }
         return result;
      }
      
      private NamedModule getNamedModuleMetaData(Class<? extends NamedModule> key)
      {
         return (NamedModule) attachments.get(key);
      }
      
      private String getModuleName(Class<? extends NamedModule> key)
      {
         NamedModule nm = getNamedModuleMetaData(key);
         return nm == null ? null : nm.getModuleName();
      }
   }
   
   private class DeploymentTask implements Runnable
   {
      private final CountDownLatch startLatch;
      private final CountDownLatch finishLatch;
      private final DeploymentUnit unit;
      private Exception exception;
      
      private DeploymentTask(CountDownLatch startLatch, CountDownLatch finishLatch, DUIH duih)
      {
         this.startLatch = startLatch;
         this.finishLatch = finishLatch;
         this.unit = getDeploymentUnitProxy(duih);
      }

      public void run()
      {
         try
         {
            startLatch.countDown();
            startLatch.await(3, TimeUnit.SECONDS);
            testee.deploy(unit);
         }
         catch (Exception e)
         {
            this.exception = e;
         }
         finally
         {
            finishLatch.countDown();
         }
         
      }
      
      
   }

}
