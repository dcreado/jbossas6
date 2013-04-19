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
package org.jboss.web.tomcat.service.deployers;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.core.StandardContext;
import org.apache.tomcat.util.modeler.Registry;
import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.reloaded.naming.CurrentComponent;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEComponentInformer;
import org.jboss.reloaded.naming.spi.JavaEEComponent;
import org.jboss.reloaded.naming.spi.JavaEEModule;
import org.jboss.security.SecurityUtil;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.web.WebApplication;
import org.jboss.web.deployers.AbstractWarDeployment;
import org.jboss.web.deployers.SharedTldMetaDataDeployer;
import org.jboss.web.tomcat.security.JaccContextValve;
import org.jboss.web.tomcat.security.RunAsListener;
import org.jboss.web.tomcat.security.SecurityAssociationValve;
import org.jboss.web.tomcat.security.SecurityContextEstablishmentValve;
import org.jboss.web.tomcat.service.CDIExceptionStore;
import org.jboss.web.tomcat.service.NamingListener;
import org.jboss.web.tomcat.service.TomcatInjectionContainer;
import org.jboss.web.tomcat.service.WebCtxLoader;
import org.omg.CORBA.ORB;

/**
 * A tomcat web application deployment.
 * 
 * @author Scott.Stark@jboss.org
 * @author Costin Manolache
 * @author adrian@jboss.org
 * @version $Revision: 111938 $
 */
public class TomcatDeployment extends AbstractWarDeployment
{
   private static final Logger log = Logger.getLogger(TomcatDeployment.class);

   private DeployerConfig config;

   private final String[] javaVMs = { " jboss.management.local:J2EEServer=Local,j2eeType=JVM,name=localhost" };

   private final String serverName = "jboss";

   private final HashMap<String, String> vhostToHostNames = new HashMap<String, String>();

   private ORB orb = null;

   private JavaEEComponentInformer componentInformer;

   private JavaEEComponent component;

   public ORB getORB()
   {
      return orb;
   }

   public void setORB(ORB orb)
   {
      this.orb = orb;
   }

   @Override
   public void init(Object containerConfig) throws Exception
   {
      this.config = (DeployerConfig)containerConfig;
      super.setJava2ClassLoadingCompliance(config.isJava2ClassLoadingCompliance());
      super.setUnpackWars(config.isUnpackWars());
      super.setLenientEjbLink(config.isLenientEjbLink());
      super.setDefaultSecurityDomain(config.getDefaultSecurityDomain());
   }

   @Override
   protected void performDeploy(WebApplication webApp, String warUrl) throws Exception
   {
      // Decode the URL as tomcat can't deal with paths with escape chars
      warUrl = URLDecoder.decode(warUrl, "UTF-8");
      webApp.setDomain(config.getCatalinaDomain());
      JBossWebMetaData metaData = webApp.getMetaData();
      // Get any jboss-web/virtual-hosts
      List<String> vhostNames = metaData.getVirtualHosts();
      // Map the virtual hosts onto the configured hosts
      String hostName = mapVirtualHosts(vhostNames);
      if (hostName == null)
      {
         Iterator hostNames = getDefaultHosts();
         if (hostNames.hasNext())
         {
            hostName = hostNames.next().toString();
         }
      }
      if (hostName == null)
      {
         throw new IllegalStateException("No default host defined");
      }
      performDeployInternal(webApp, hostName, warUrl);
   }

   protected void performDeployInternal(WebApplication webApp, String hostName, String warUrlStr) throws Exception
   {
      JBossWebMetaData metaData = webApp.getMetaData();
      String ctxPath = metaData.getContextRoot();
      if (ctxPath.equals("/") || ctxPath.equals("/ROOT") || ctxPath.equals(""))
      {
         log.debug("deploy root context=" + ctxPath);
         ctxPath = "/";
         metaData.setContextRoot(ctxPath);
      }

      log.info("deploy, ctxPath=" + ctxPath);

      URL warUrl = new URL(warUrlStr);

      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      metaData.setContextLoader(loader);

      StandardContext context = (StandardContext)Class.forName(config.getContextClassName()).newInstance();

      DeploymentUnit depUnit = webApp.getDeploymentUnit();

      // Find all TLDs that have been processed by deployers, and place them in a map keyed by location
      Map<String, TldMetaData> tldMetaDataMap = new HashMap<String, TldMetaData>();
      Map<String, Object> attachements = depUnit.getAttachments();
      Iterator<String> attachementNames = attachements.keySet().iterator();
      while (attachementNames.hasNext()) {
         String name = attachementNames.next();
         Object attachement = depUnit.getAttachment(name);
         if (attachement != null && attachement instanceof TldMetaData
               && name.startsWith(TldMetaData.class.getName() + ":"))
         {
            tldMetaDataMap.put(name, (TldMetaData) attachement);
         }
      }
      List<TldMetaData> sharedTldMetaData = (List<TldMetaData>)
         depUnit.getAttachment(SharedTldMetaDataDeployer.SHARED_TLDS_ATTACHMENT_NAME);
      if (sharedTldMetaData != null)
      {
         for (TldMetaData tldMetaData : sharedTldMetaData)
         {
            tldMetaDataMap.put("shared:" + tldMetaData.toString(), tldMetaData);
         }
      }
      
      this.component = createJavaEEComponent();

      TomcatInjectionContainer injectionContainer = 
         new TomcatInjectionContainer(webApp, depUnit, context, 
               getPersistenceUnitDependencyResolver(), config.getDynamicClassloaders(), component, this.getInjectionManager());

      Loader webLoader = depUnit.getAttachment(Loader.class);
      if (webLoader == null)
         webLoader = getWebLoader(depUnit, metaData, loader, warUrl, injectionContainer);

      webApp.setName(warUrl.getPath());
      webApp.setClassLoader(loader);
      webApp.setURL(warUrl);

      String objectNameS = config.getCatalinaDomain() + ":j2eeType=WebModule,name=//" + ((hostName == null) ? "localhost" : hostName) + ctxPath
            + ",J2EEApplication=none,J2EEServer=none";

      ObjectName objectName = new ObjectName(objectNameS);

      if (Registry.getRegistry(null, null).getMBeanServer().isRegistered(objectName))
         throw new DeploymentException("Web mapping already exists for deployment URL " + warUrlStr);

      Registry.getRegistry(null, null).registerComponent(context, objectName, config.getContextClassName());

      context.setInstanceManager(injectionContainer);
      context.setPublicId(metaData.getPublicID());

      String docBase = depUnit.getAttachment("org.jboss.web.explicitDocBase", String.class);
      if (docBase == null)
         docBase = VFS.getChild(warUrl).getPhysicalFile().getAbsolutePath();

      context.setDocBase(docBase);

      // If there is an alt-dd set it
      if (metaData.getAlternativeDD() != null)
      {
         log.debug("Setting altDDName to: " + metaData.getAlternativeDD());
         context.setAltDDName(metaData.getAlternativeDD());
      }
      context.setJavaVMs(javaVMs);
      context.setServer(serverName);

      if (webLoader != null)
      {
         context.setLoader(webLoader);
      }
      else
      {
         context.setParentClassLoader(loader);
      }

      // Set the session cookies flag according to metadata
      switch (metaData.getSessionCookies())
      {
         case JBossWebMetaData.SESSION_COOKIES_ENABLED:
            context.setCookies(true);
            log.debug("Enabling session cookies");
            break;
         case JBossWebMetaData.SESSION_COOKIES_DISABLED:
            context.setCookies(false);
            log.debug("Disabling session cookies");
            break;
         default:
            log.debug("Using session cookies default setting");
      }

      String metaDataSecurityDomain = metaData.getSecurityDomain();
      if (metaDataSecurityDomain != null)
         metaDataSecurityDomain = metaDataSecurityDomain.trim();
      
      // Add a valve to establish security and naming context
      SecurityContextEstablishmentValve scevalve = new SecurityContextEstablishmentValve(metaDataSecurityDomain, SecurityUtil.unprefixSecurityDomain(config
            .getDefaultSecurityDomain()), SecurityActions.loadClass(config.getSecurityContextClassName()), getSecurityManagement(), component);
      context.addValve(scevalve);

      // Add a valve to estalish the JACC context before authorization valves
      Certificate[] certs = null;
      CodeSource cs = new CodeSource(warUrl, certs);
      JaccContextValve jaccValve = new JaccContextValve(metaData, cs);
      context.addValve(jaccValve);

      // Set listener
      context.setConfigClass("org.jboss.web.tomcat.service.deployers.JBossContextConfig");
      context.addLifecycleListener(new EncListener(loader, webLoader, injectionContainer, webApp));
      
      // Pass the metadata to the RunAsListener via a thread local
      RunAsListener.metaDataLocal.set(metaData);
      JBossContextConfig.metaDataLocal.set(metaData);
      JBossContextConfig.deployerConfig.set(config);
      JBossContextConfig.tldMetaDataMapLocal.set(tldMetaDataMap);
      NamingListener.idLocal.set(component);

      JBossContextConfig.kernelLocal.set(kernel);
      JBossContextConfig.deploymentUnitLocal.set(unit);
      try
      {
         // JBAS-8406: Temp hack, will move to NamingListener
         CurrentComponent.push(component);
         // Start it
         CDIExceptionStore.reset();
         context.start();
         // Build the ENC
         // JBAS-8278: abort deployment if there are CDI definitions errors
         if (CDIExceptionStore.currentExceptions().size()>0)
         {
            throw CDIExceptionStore.currentExceptions().get(0);
         }
      }
      catch (Throwable t)
      {
         context.destroy();
         DeploymentException.rethrowAsDeploymentException("URL " + warUrlStr + " deployment failed", t);
      }
      finally
      {
         RunAsListener.metaDataLocal.set(null);
         JBossContextConfig.metaDataLocal.set(null);
         JBossContextConfig.deployerConfig.set(null);
         JBossContextConfig.tldMetaDataMapLocal.set(null);
         NamingListener.idLocal.set(null);

         JBossContextConfig.kernelLocal.set(null);
         JBossContextConfig.deploymentUnitLocal.set(null);
         // JBAS-8406: Temp hack, will move to NamingListener
         CurrentComponent.pop();
         // JBAS-8278
         CDIExceptionStore.reset();
      }
      if (context.getState() != 1)
      {
         context.destroy();
         throw new DeploymentException("URL " + warUrlStr + " deployment failed");
      }

      /*
       * Add security association valve after the authorization valves so that the authenticated user may be associated
       * with the request thread/session.
       */
      if (!config.isStandalone())
      {
         SecurityAssociationValve securityAssociationValve = new SecurityAssociationValve(metaData, config.getSecurityManagerService());
         securityAssociationValve.setSubjectAttributeName(config.getSubjectAttributeName());
         context.addValve(securityAssociationValve);
      }

      /*
       * TODO: Retrieve the state, and throw an exception in case of a failure Integer state = (Integer)
       * server.getAttribute(objectName, "state"); if (state.intValue() != 1) { throw new DeploymentException("URL " +
       * warUrl + " deployment failed"); }
       */

      webApp.setAppData(objectName);

      /*
       * TODO: Create mbeans for the servlets ObjectName servletQuery = new ObjectName (config.getCatalinaDomain() +
       * ":j2eeType=Servlet,WebModule=" + objectName.getKeyProperty("name") + ",*"); Iterator iterator =
       * server.queryMBeans(servletQuery, null).iterator(); while (iterator.hasNext()) {
       * di.mbeans.add(((ObjectInstance)iterator.next()).getObjectName()); }
       */

      log.debug("Initialized: " + webApp + " " + objectName);
   }

   public class EncListener implements LifecycleListener
   {
      protected ClassLoader loader;

      protected Loader webLoader;

      protected WebApplication webApp;

      protected JBossWebMetaData metaData;

      protected DeploymentUnit unit;

      protected TomcatInjectionContainer injectionContainer;

      public EncListener(ClassLoader loader, Loader webLoader, TomcatInjectionContainer injectionContainer, WebApplication webApp)
      {
         this.loader = loader;
         this.webLoader = webLoader;
         this.injectionContainer = injectionContainer;
         this.webApp = webApp;
         this.metaData = webApp.getMetaData();
         this.unit = webApp.getDeploymentUnit();
      }

      public void lifecycleEvent(LifecycleEvent event)
      {
         String eventType = event.getType();
         if (eventType.equals(StandardContext.AFTER_START_EVENT))
         {
            // make the context class loader known to the JBossWebMetaData, ws4ee needs it
            // to instanciate service endpoint pojos that live in this webapp
            metaData.setContextLoader(webLoader.getClassLoader());

            Thread currentThread = Thread.currentThread();
            ClassLoader currentLoader = currentThread.getContextClassLoader();
            try
            {
               // Create a java:comp/env environment unique for the web application
               log.debug("Creating ENC using ClassLoader: " + loader);
               ClassLoader parent = loader.getParent();
               while (parent != null)
               {
                  log.debug(".." + parent);
                  parent = parent.getParent();
               }
               // TODO: The enc should be an input?
               currentThread.setContextClassLoader(webLoader.getClassLoader());
               metaData.setENCLoader(webLoader.getClassLoader());
               Context javaCompCtx = component.getContext();
//               // Add ORB/UserTransaction
//               ORB orb = null;
//               try
//               {
//                  ObjectName ORB_NAME = new ObjectName("jboss:service=CorbaORB");
//                  orb = (ORB)server.getAttribute(ORB_NAME, "ORB");
//                  // Bind the orb
//                  if (orb != null)
//                  {
//                     NonSerializableFactory.rebind(javaCompCtx, "ORB", orb);
//                     log.debug("Bound java:comp/ORB");
//                  }
//               }
//               catch (Throwable t)
//               {
//                  log.debug("Unable to retrieve orb: " + t.toString());
//               }
//
//               // JTA links
//               javaCompCtx.bind("TransactionSynchronizationRegistry", new LinkRef("java:TransactionSynchronizationRegistry"));
//               log.debug("Linked java:comp/TransactionSynchronizationRegistry to JNDI name: java:TransactionSynchronizationRegistry");
//               javaCompCtx.bind("UserTransaction", new LinkRef("UserTransaction"));
//               log.debug("Linked java:comp/UserTransaction to JNDI name: UserTransaction");
               //envCtx = envCtx.createSubcontext("env");
               injectionContainer.populateEnc(webLoader.getClassLoader());

               // TODO: this should be bindings in the metadata
               currentThread.setContextClassLoader(webLoader.getClassLoader());
               String securityDomain = metaData.getSecurityDomain();
               log.debug("linkSecurityDomain");
               linkSecurityDomain(securityDomain, javaCompCtx);

            }
            catch (Throwable t)
            {
               log.error("ENC setup failed", t);
               throw new RuntimeException(t);
            }
            finally
            {
               currentThread.setContextClassLoader(currentLoader);

               log.debug("injectionContainer enabled and processing beginning");
               // we need to do this because the classloader is initialize by the web container and
               // the injection container needs the classloader so that it can build up Injectors and ENC populators
               injectionContainer.setClassLoader(webLoader.getClassLoader());
               injectionContainer.processMetadata();
            }
         }
         else if(eventType.equals(StandardContext.BEFORE_STOP_EVENT))
         {
            unbindSecurityDomainJndiBindings(webApp);
         }
      }

      protected void unbindSecurityDomainJndiBindings(WebApplication webApplication)
      {
         Thread currentThread = Thread.currentThread();
         ClassLoader currentLoader = currentThread.getContextClassLoader();
         try
         {
            currentThread.setContextClassLoader(webApplication.getMetaData().getENCLoader());
            final Context envCtx = component.getContext();
            unlinkSecurityDomain(webApplication.getMetaData().getSecurityDomain(), envCtx);
         }
         catch (final NamingException e)
         {
            log.error(e.getMessage(), e);
         }
         finally
         {
            currentThread.setContextClassLoader(currentLoader);
         }
      }
   }

   public Loader getWebLoader(DeploymentUnit unit, JBossWebMetaData metaData, ClassLoader loader, URL rl, TomcatInjectionContainer injectionContainer) throws MalformedURLException
   {
      Loader webLoader = null;

      /*
       * If we are using the jboss class loader we need to augment its path to include the WEB-INF/{lib,classes} dirs or
       * else scoped class loading does not see the war level overrides. The call to setWarURL adds these paths to the
       * deployment UCL.
       */
      @SuppressWarnings("unchecked")
      List<URL> classpath = unit.getAttachment("org.jboss.web.expandedWarClasspath", List.class);
      if (classpath == null && unit instanceof VFSDeploymentUnit)
      {
         VFSDeploymentUnit vfsUnit = (VFSDeploymentUnit)unit;
         try
         {
            VirtualFile classes = vfsUnit.getFile("WEB-INF/classes");
            // Tomcat can't handle the vfs urls yet
            URL vfsURL = classes.toURL();
            String vfsurl = vfsURL.toString();
            if (vfsurl.startsWith("vfs"))
               vfsURL = new URL(vfsurl.substring(3));
            classpath = new ArrayList<URL>();
            classpath.add(vfsURL);
         }
         catch (Exception ignored)
         {
         }
      }

      WebCtxLoader jbossLoader = new WebCtxLoader(loader, injectionContainer);
      if (classpath != null)
         jbossLoader.setClasspath(classpath);

      webLoader = jbossLoader;
      return webLoader;
   }

   /**
    * Called as part of the undeploy() method template to ask the subclass for perform the web container specific
    * undeployment steps.
    */
   @Override
   protected void performUndeploy(WebApplication warInfo, String warUrl) throws Exception
   {
      if (warInfo == null)
      {
         log.debug("performUndeploy, no WebApplication found for URL " + warUrl);
         return;
      }

      log.info("undeploy, ctxPath=" + warInfo.getMetaData().getContextRoot());

      JBossWebMetaData metaData = warInfo.getMetaData();
      // Get any jboss-web/virtual-hosts
      List<String> vhostNames = metaData.getVirtualHosts();
      // Map the virtual hosts onto the configured hosts
      String hostName = mapVirtualHosts(vhostNames);
      if (hostName == null)
      {
         Iterator hostNames = getDefaultHosts();
         if (hostNames.hasNext())
         {
            hostName = hostNames.next().toString();
         }
      }
      performUndeployInternal(warInfo, hostName, warUrl);
   }

   protected void performUndeployInternal(WebApplication warInfo, String hostName, String warUrlStr) throws Exception
   {
      JBossWebMetaData metaData = warInfo.getMetaData();
      String ctxPath = metaData.getContextRoot();

      // TODO: Need to remove the dependency on MBeanServer
      MBeanServer server = MBeanServerLocator.locateJBoss();
      // If the server is gone, all apps were stopped already
      if (server == null)
         return;

      ObjectName objectName = new ObjectName(config.getCatalinaDomain() + ":j2eeType=WebModule,name=//" + ((hostName == null) ? "localhost" : hostName) + ctxPath
            + ",J2EEApplication=none,J2EEServer=none");

      if (server.isRegistered(objectName))
      {
         try
         {
            // JBAS-8406: Temp hack, will move to NamingListener
            CurrentComponent.push(component);
            // Contexts should be stopped by the host already
            server.invoke(objectName, "destroy", new Object[] {}, new String[] {});
         }
         finally
         {
            CurrentComponent.pop();
         }
      }
   }

   /**
    * Resolve the input virtual host names to the names of the configured Hosts
    * 
    * @param vhostNames Iterator<String> for the jboss-web/virtual-host elements
    * @return Iterator<String> of the unique Host names
    * @throws Exception
    */
   protected synchronized String mapVirtualHosts(List<String> vhostNames) throws Exception
   {
      if (vhostToHostNames.size() == 0)
      {
         // Query the configured Host mbeans
         String hostQuery = config.getCatalinaDomain() + ":type=Host,*";
         ObjectName query = new ObjectName(hostQuery);
         Set hosts = server.queryNames(query, null);
         Iterator iter = hosts.iterator();
         while (iter.hasNext())
         {
            ObjectName host = (ObjectName)iter.next();
            String name = host.getKeyProperty("host");
            if (name != null)
            {
               vhostToHostNames.put(name, name);
               String[] aliases = (String[])server.invoke(host, "findAliases", null, null);
               int count = aliases != null ? aliases.length : 0;
               for (int n = 0; n < count; n++)
               {
                  vhostToHostNames.put(aliases[n], name);
               }
            }
         }
      }

      // Map the virtual host names to the hosts
      HashSet<String> hosts = new HashSet<String>();
      String webappHost = null;
      if (vhostNames != null)
      {
         for (String vhost : vhostNames)
         {
            String host = (String)vhostToHostNames.get(vhost);
            if (host == null)
            {
               log.warn("Failed to map vhost: " + vhost);
               // This will cause a new alias to be added
               hosts.add(vhost);
            }
            else
            {
               if (webappHost == null)
               {
                  webappHost = host;
               }
               else if (!webappHost.equals(host))
               {
                  throw new IllegalStateException("Cannot add webapp to two different hosts: " 
                        + webappHost + " and " + host);
               }
            }
         }
         // Add to default host if none exists
         if (webappHost == null)
         {
            Iterator defaultHosts = getDefaultHosts();
            if (defaultHosts.hasNext())
            {
               webappHost = defaultHosts.next().toString();
            }
         }
         // Add missing aliases to matching host
         if (webappHost != null)
         {
            String hostQuery = config.getCatalinaDomain() + ":type=Host,host=" + webappHost + ",*";
            ObjectName query = new ObjectName(hostQuery);
            Set hostONs = server.queryNames(query, null);
            Iterator iter = hostONs.iterator();
            if (iter.hasNext())
            {
               ObjectName host = (ObjectName)iter.next();
               Object[] name = new Object[1];
               String[] args = { "java.lang.String" };
               for (String vhost : hosts)
               {
                  log.warn("Adding alias to vhost: " + vhost);
                  name[0] = vhost;
                  server.invoke(host, "addAlias", name, args);
               }
            }
         }
      }
      return webappHost;
   }

   /**
    * Find the default hosts for all existing engines
    */
   protected synchronized Iterator getDefaultHosts() throws Exception
   {
      // Map the virtual host names to the hosts
      HashSet defaultHosts = new HashSet();
      // Query the configured Engine mbeans
      String engineQuery = config.getCatalinaDomain() + ":type=Engine,*";
      ObjectName query = new ObjectName(engineQuery);
      Set engines = server.queryNames(query, null);
      Iterator iter = engines.iterator();
      while (iter.hasNext())
      {
         ObjectName engine = (ObjectName)iter.next();

         String defaultHost = (String)server.getAttribute(engine, "defaultHost");
         if (defaultHost != null)
         {
            defaultHosts.add(defaultHost);
         }
      }
      return defaultHosts.iterator();
   }
   
   /**
    * Traverse the parent chain of the context to reach the Catalina Engine
    * @param context Context of the web application
    * @return
    */
   private Engine getCatalinaEngine(org.apache.catalina.Context context)
   {
      Container parentContainer = context.getParent();
      while(parentContainer != null && !(parentContainer instanceof Engine))
         parentContainer = parentContainer.getParent();
      return (Engine) parentContainer;
   }

   private JavaEEComponent createJavaEEComponent()
   {
      final String beanName = getJavaEEModuleName();
      
      // TODO: must come in via MC injection
      final JavaEEModule module = getBean(beanName, JavaEEModule.class);

      // Web uses a Module context
      return new JavaEEComponent() {

         public Context getContext()
         {
            return module.getContext();
         }

         public JavaEEModule getModule()
         {
            return module;
         }

         public String getName()
         {
            return module.getName();
         }
      };
   }

   private <T> T getBean(String beanName, Class<T> type)
   {
      ControllerContext context = kernel.getController().getInstalledContext(beanName);
      if(context == null)
         throw new IllegalArgumentException("Can't find bean " + beanName + " in " + kernel);
      Object target = context.getTarget();
      if(target == null)
         throw new IllegalArgumentException("Bean " + beanName + " has no target instance in " + kernel);
      return type.cast(target);
   }

   // TODO: weirdness, if I make this package private I get IllegalAccessError
   public String getJavaEEModuleName()
   {
      assert componentInformer != null : "componentInformer is null";
      // assert ControllerState.INSTALLED

      final String applicationName = componentInformer.getApplicationName(unit);
      final String moduleName = componentInformer.getModuleName(unit);

      // TODO: remove hard coded "jboss.naming" constant
      final String beanName = "jboss.naming:" + (applicationName != null ? "application=" + applicationName + "," : "") + "module=" + moduleName;

      return beanName;
   }

   @Inject
   public void setJavaEEComponentInformer(JavaEEComponentInformer componentInformer)
   {
      this.componentInformer = componentInformer;
   }
}
