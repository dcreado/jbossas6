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
package org.jboss.system.server.jmx;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.jboss.bootstrap.api.as.config.JBossASBasedServerConfig;
import org.jboss.bootstrap.api.config.InvalidConfigurationException;
import org.jboss.bootstrap.api.lifecycle.LifecycleEventException;
import org.jboss.bootstrap.api.lifecycle.LifecycleEventHandler;
import org.jboss.bootstrap.api.lifecycle.LifecycleState;
import org.jboss.bootstrap.spi.Bootstrap;
import org.jboss.bootstrap.spi.as.server.MCJBossASBasedServerProvider;
import org.jboss.bootstrap.spi.config.ConfigurationInitializer;
import org.jboss.bootstrap.spi.config.ConfigurationValidator;
import org.jboss.bootstrap.spi.server.ServerInitializer;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloading.spi.RealClassLoader;
import org.jboss.classloading.spi.metadata.ExportAll;
import org.jboss.classloading.spi.vfs.policy.VFSClassLoaderPolicy;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.spi.deployment.KernelDeployment;
import org.jboss.logging.Logger;
import org.jboss.mx.loading.RepositoryClassLoader;
import org.jboss.mx.server.ServerConstants;
import org.jboss.mx.util.JMXExceptionDecoder;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.mx.util.ObjectNameFactory;
import org.jboss.system.ServiceController;
import org.jboss.system.ServiceControllerMBean;
import org.jboss.system.server.ServerConfigImpl;
import org.jboss.system.server.ServerConfigImplMBean;
import org.jboss.system.server.ServerImplMBean;
import org.jboss.system.server.ServerInfoMBean;
import org.jboss.util.JBossObject;
import org.jboss.util.file.FileSuffixFilter;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * A pojo that creates a legacy jmx kernel ala the jboss-4.x server bootstrap.
 * This is used to support the SARDeployer and mbean integration.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision: 101688 $
 */
public class JMXKernel<K extends MCJBossASBasedServerProvider<K, T>, T extends JBossASBasedServerConfig<T>> extends JBossObject
   implements JMXKernelMBean<K,T>, NotificationEmitter
{
   private final static ObjectName DEFAULT_LOADER_NAME =
      ObjectNameFactory.create(ServerConstants.DEFAULT_LOADER_NAME);

   private static final Logger log = Logger.getLogger(JMXKernel.class);

   /** The JMX MBeanServer which will serve as our communication bus. */
   private MBeanServer mbeanServer;
   private K serverImpl;
   private ServiceController controller;
   private T serverConfig;
   private ServerConfigImplMBean<T> serverConfigMBean;
   private ServerInfoMBean serverInfo;
   /** The kernel */
   private Kernel kernel;
   /** The serverImpl cast as an emitter */
   private NotificationEmitter notificationEmitter;
   
   /** The bootstrap UCL class loader ObjectName */
   private ObjectName bootstrapUCLName;
   private boolean started;
   /** Whether to use the old classloader */
   private boolean oldClassLoader;

   public JMXKernel()
   {
      super(log);
   }

   public K getServerImpl()
   {
      return serverImpl;
   }
   public void setServerImpl(K serverImpl)
   {
      this.serverImpl = serverImpl;
      this.notificationEmitter = (NotificationEmitter) serverImpl;
   }

   public ServiceControllerMBean getServiceController()
   {
      return this.controller;
   }
   public MBeanServer getMbeanServer()
   {
      return mbeanServer;
   }

   public ServerInfoMBean getServerInfo()
   {
      return serverInfo;
   }
   public void setServerInfo(ServerInfoMBean serverInfo)
   {
      this.serverInfo = serverInfo;
   }

   /**
    * Set the kernel.
    * 
    * @param kernel the kernel.
    */
   public void setKernel(Kernel kernel)
   {
      this.kernel = kernel;
   }
   
   public boolean isOldClassLoader()
   {
      return oldClassLoader;
   }

   public void setOldClassLoader(boolean oldClassLoader)
   {
      this.oldClassLoader = oldClassLoader;
   }
   
   public void start() throws Exception
   {
      ClassLoader tcl = Thread.currentThread().getContextClassLoader();
      // Create the MBeanServer
      String builder = System.getProperty(ServerConstants.MBEAN_SERVER_BUILDER_CLASS_PROPERTY,
                                          ServerConstants.DEFAULT_MBEAN_SERVER_BUILDER_CLASS);
      System.setProperty(ServerConstants.MBEAN_SERVER_BUILDER_CLASS_PROPERTY, builder);

      serverConfig = serverImpl.getConfiguration();
      serverConfigMBean = new ServerConfigImpl(serverConfig);
      // Check if we'll use the platform MBeanServer or instantiate our own
      if (serverConfig.isUsePlatformMBeanServer() == true)
      {
         // jdk1.5+
         ClassLoader cl = Thread.currentThread().getContextClassLoader();
         Class<?> clazz = cl.loadClass("java.lang.management.ManagementFactory");
         Class<?>[] sig = null;
         Method method = clazz.getMethod("getPlatformMBeanServer", sig);
         Object[] args = null;
         mbeanServer = (MBeanServer) method.invoke(null, args);
         // Tell the MBeanServerLocator to point to this mbeanServer
         MBeanServerLocator.setJBoss(mbeanServer);
         /* If the LazyMBeanServer was used, we need to reset to the jboss
         MBeanServer to use our implementation for the jboss services.
         */
         mbeanServer = LazyMBeanServer.resetToJBossServer(mbeanServer);
      }
      else
      {
         // Create our own MBeanServer
         mbeanServer = MBeanServerFactory.createMBeanServer("jboss");
      }
      log.debug("Created MBeanServer: " + mbeanServer);      

      // Register mbeanServer components
      mbeanServer.registerMBean(this, ServerImplMBean.OBJECT_NAME);
      mbeanServer.registerMBean(serverConfigMBean, ServerConfigImplMBean.OBJECT_NAME);
      
      // Initialize spine boot libraries
      ClassLoader cl;
      if (oldClassLoader)
         cl = initBootLibrariesOld();
      else
         cl = initBootLibraries();
      
      // Set ServiceClassLoader as classloader for the construction of
      // the basic system
      try
      {
         Thread.currentThread().setContextClassLoader(cl);
   
         // General Purpose Architecture information -- 
         mbeanServer.registerMBean(serverInfo, new ObjectName("jboss.system:type=ServerInfo"));

         // Service Controller
         controller = new ServiceController();
         controller.setKernel(kernel);
         controller.setMBeanServer(mbeanServer);
         mbeanServer.registerMBean(controller, new ObjectName("jboss.system:service=ServiceController"));
   
         log.info("Legacy JMX core initialized");
         started = true;
      }
      finally
      {
         Thread.currentThread().setContextClassLoader(tcl);
      }
   }

   /**
    * Stop the mbeans
    *
    * @throws IllegalStateException - if not started.
    */
   public void stop() throws IllegalStateException
   {
      if (log.isTraceEnabled())
         log.trace("stop caller:", new Throwable("Here"));

      if (!started)
         throw new IllegalStateException("Server not started");

      log.debug("Shutting down all services");
      shutdownServices();

      // Make sure all mbeans are unregistered
      removeMBeans();
   }

   ///////////////////////////////////////////////////////////////////////////
   //                          NotificationEmitter                          //
   ///////////////////////////////////////////////////////////////////////////
   
   public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
   {
      notificationEmitter.addNotificationListener(listener, filter, handback);
   }
   
   public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException
   {
      notificationEmitter.removeNotificationListener(listener);
   }
   
   public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
      throws ListenerNotFoundException
   {
      notificationEmitter.removeNotificationListener(listener, filter, handback);
   }
      
   public MBeanNotificationInfo[] getNotificationInfo()
   {
      return notificationEmitter.getNotificationInfo();
   }

   // ServerImplMBean delegation
   public void init(Properties props) throws Exception
   {
      serverImpl.initialize();
   }

   // AS isn't always going to be a standalone process
//   public void exit()
//   {
//      serverImpl.exit();
//   }
//
//   public void exit(int exitcode)
//   {
//      serverImpl.exit(exitcode);
//   }

   public T getConfig()
   {
      return serverConfig;
   }
   /**
    * Get the optional server configuration metadata
    * @return a possibly empty map of configuration metadata.
    */
   public Map<String, Object> getMetaData()
   {
      return Collections.emptyMap();
   }
   public String getBuildDate()
   {
      return serverImpl.getBuildDate();
   }

   public String getBuildID()
   {
      return serverImpl.getBuildID();
   }

   public String getBuildJVM()
   {
      return serverImpl.getBuildJVM();
   }

   public String getBuildNumber()
   {
      return serverImpl.getBuildNumber();
   }

   public String getBuildOS()
   {
      return serverImpl.getBuildOS();
   }

   public Date getStartDate()
   {
      return serverImpl.getStartDate();
   }

   public String getVersion()
   {
      return serverImpl.getVersion();
   }

   public String getVersionName()
   {
      return serverImpl.getVersionName();
   }

   public String getVersionNumber()
   {
      return serverImpl.getVersionNumber();
   }

   // AS isn't always going to be a standalone process
//   public void halt()
//   {
//      serverImpl.halt();
//   }
//
//   public void halt(int exitcode)
//   {
//      serverImpl.halt(exitcode);
//   }

   public boolean isInShutdown()
   {
      return serverImpl.getState().equals(LifecycleState.STOPPING);
   }

   public boolean isStarted()
   {
      return serverImpl.getState().equals(LifecycleState.STARTED);
   }

   public void runFinalization()
   {
      
   }

   public void runGarbageCollector()
   {
      
   }

   public void traceInstructions(Boolean flag)
   {
      
   }

   public void traceMethodCalls(Boolean flag)
   {
      
   }
   
   public void shutdown()
   {
      // JBAS-6916
      // We'll shut down the server via the Main shutdown hook...
//      if (log.isTraceEnabled())
//         log.trace("Shutdown caller:", new Throwable("Here"));
//      try{
//         serverImpl.shutdown();
//      }
//      catch(Throwable t)
//      {
//         throw new RuntimeException("Error in shutdown of server: " + serverImpl,t);
//      }
      
      // JBAS-6916:
      //TODO This must be removed, VM exit is not a feature of server shutdown.
      // Now exit the Runtime, because AS has non-daemon Threads still
      // chugging away. 
      new Thread()
      {
         public void run()
         {
            log.info("Server exit called, exiting the JVM now!");
            Runtime.getRuntime().exit(1);
         }
      }.start();
   }

   /**
    * The <code>shutdownServices</code> method calls the one and only
    * ServiceController to shut down all the mbeans registered with it.
    */
   protected void shutdownServices()
   {
      try
      {
         // get the deployed objects from ServiceController
         controller.shutdown();
      }
      catch (Exception e)
      {
         Throwable t = JMXExceptionDecoder.decode(e);
         log.error("Failed to shutdown services", t);
      }
   }

   /**
    * The <code>removeMBeans</code> method uses the mbean mbeanServer to unregister
    * all the mbeans registered here.
    */
   protected void removeMBeans()
   {
      try
      {
         mbeanServer.unregisterMBean(ServerConfigImplMBean.OBJECT_NAME);
      }
      catch (Exception e)
      {
         Throwable t = JMXExceptionDecoder.decode(e);
         log.error("Failed to unregister mbeans", t);
      }
      try
      {
         mbeanServer.unregisterMBean(ServerImplMBean.OBJECT_NAME);
      }
      catch (Exception e)
      {
         Throwable t = JMXExceptionDecoder.decode(e);
         log.error("Failed to unregister mbeans", t);
      }
      try
      {
         MBeanServer registeredServer = mbeanServer;
         if (serverConfig.isUsePlatformMBeanServer() == true)
            registeredServer = LazyMBeanServer.getRegisteredMBeanServer(mbeanServer);
         MBeanServerFactory.releaseMBeanServer(registeredServer);
      }
      catch (Exception e)
      {
         Throwable t = JMXExceptionDecoder.decode(e);
         log.error("Failed to release mbean mbeanServer", t);
      }
   }


   /**
    * Initialize the boot libraries using the old classloader
    * 
    * @return the classloader
    * @throws Exception for any error
    */
   private List<URL> getBootURLs() throws Exception
   {
      // Build the list of URL for the spine to boot
      List<URL> list = new ArrayList<URL>();

      // Add the patch URL.  If the url protocol is file, then
      // add the contents of the directory it points to
      URL patchURL = null; //serverConfig.getPatchURL(); //TODO Supply a patchURL?
      if (patchURL != null)
      {
         if (patchURL.getProtocol().equals("file"))
         {
            File dir = new File(patchURL.getFile());
            if (dir.exists())
            {
               // Add the local file patch directory
               list.add(dir.toURI().toURL());

               // Add the contents of the directory too
               File[] jars = dir.listFiles(new FileSuffixFilter(new String[] { ".jar", ".zip" }, true));

               for (int j = 0; jars != null && j < jars.length; j++)
               {
                  list.add(jars[j].getCanonicalFile().toURI().toURL());
               }
            }
         }
         else
         {
            list.add(patchURL);
         }
      }

      // Add the mbeanServer configuration directory to be able to load serverConfig files as resources
      list.add(serverConfig.getServerConfLocation());
      log.debug("Boot url list: " + list);
      
      return list;
   }

   /**
    * Initialize the boot libraries using the old classloader
    * 
    * @return the classloader
    * @throws Exception for any error
    */
   private ClassLoader initBootLibrariesOld() throws Exception
   {
      List<URL> list = getBootURLs();
      
      // Create loaders for each URL
      RepositoryClassLoader loader = null;
      for (URL url : list)
      {
         log.debug("Creating loader for URL: " + url);

         // This is a boot URL, so key it on itself.
         Object[] args = {url, Boolean.TRUE};
         String[] sig = {"java.net.URL", "boolean"};
         loader = (RepositoryClassLoader) mbeanServer.invoke(DEFAULT_LOADER_NAME, "newClassLoader", args, sig);
      }
      bootstrapUCLName = loader.getObjectName();
      mbeanServer.registerMBean(loader, bootstrapUCLName);
      return loader;
   }

   /**
    * Initialize the boot libraries using the new classloader
    * 
    * @return the classloader
    * @throws Exception for any error
    */
   private ClassLoader initBootLibraries() throws Exception
   {
      ClassLoaderSystem system = ClassLoaderSystem.getInstance();
      mbeanServer.registerMBean(system, new ObjectName("jboss.classloader:service=ClassLoaderSystem"));
      
      List<URL> list = getBootURLs();

      VirtualFile[] files = new VirtualFile[list.size()];
      for (int i = 0; i < list.size(); ++i)
      {
         URL url = list.get(i);
         files[i] = VFS.getChild(url);
      }
      
      VFSClassLoaderPolicy policy = new VFSClassLoaderPolicy(files);
      policy.setExportAll(ExportAll.NON_EMPTY);
      policy.setImportAll(true);
      ClassLoader classLoader = system.registerClassLoaderPolicy(policy);
      if (classLoader instanceof RealClassLoader)
         bootstrapUCLName = ((RealClassLoader) classLoader).getObjectName();
      return classLoader;
   }
   
   /*
    * Below this line are delegate methods only to
    * fulfill the contract of JBossASServer
    */

   public Map<String, KernelDeployment> getDeployments()
   {
      return serverImpl.getDeployments();
   }

   public Kernel getKernel()
   {
      return serverImpl.getKernel();
   }

   public void addBootstrap(final Bootstrap<K, T> arg0) throws IllegalArgumentException
   {
      serverImpl.addBootstrap(arg0);
   }

   public ConfigurationInitializer<T> getConfigInitializer()
   {
      return serverImpl.getConfigInitializer();
   }

   public T getConfiguration()
   {
      return serverImpl.getConfiguration();
   }

   public ServerInitializer<K, T> getServerInitializer()
   {
      return serverImpl.getServerInitializer();
   }

   public LifecycleState getState()
   {
      return serverImpl.getState();
   }

   public ConfigurationValidator<T> getValidator()
   {
      return serverImpl.getValidator();
   }

   public void initialize() throws IllegalStateException, InvalidConfigurationException, LifecycleEventException
   {
      serverImpl.initialize();
   }

   public void registerEventHandler(final LifecycleEventHandler arg0, final EnumSet<LifecycleState> arg1)
         throws IllegalArgumentException
   {
      serverImpl.registerEventHandler(arg0, arg1);
   }

   public void registerEventHandler(final LifecycleEventHandler arg0, final LifecycleState... arg1)
         throws IllegalArgumentException
   {
      serverImpl.registerEventHandler(arg0, arg1);

   }

   public void registerEventHandler(final LifecycleState arg0, final LifecycleEventHandler arg1)
         throws IllegalArgumentException
   {
      serverImpl.registerEventHandler(arg0, arg1);
   }

   public void registerEventHandlers(final LifecycleState arg0, final LifecycleEventHandler... arg1)
         throws IllegalArgumentException
   {
      serverImpl.registerEventHandlers(arg0, arg1);
   }

   public void removeBootstrap(final Bootstrap<K, T> arg0)
         throws IllegalArgumentException
   {
      serverImpl.removeBootstrap(arg0);

   }

   public void setConfigInitializer(final ConfigurationInitializer<T> arg0)
         throws IllegalStateException
   {
      serverImpl.setConfigInitializer(arg0);

   }

   public void setConfiguration(final T arg0)
   {
      serverImpl.setConfiguration(arg0);
   }

   public void setServerInitializer(final ServerInitializer<K, T> arg0)
         throws IllegalStateException
   {
      serverImpl.setServerInitializer(arg0);

   }

   public void setValidator(final ConfigurationValidator<T> arg0) throws IllegalStateException
   {
      serverImpl.setValidator(arg0);

   }

   public boolean unregisterEventHandler(final LifecycleEventHandler arg0, final LifecycleState arg1)
         throws IllegalArgumentException
   {
      return serverImpl.unregisterEventHandler(arg0, arg1);
   }
}

