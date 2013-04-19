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
package org.jboss;

import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.net.URLStreamHandlerFactory;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import org.jboss.bootstrap.api.as.config.JBossASServerConfig;
import org.jboss.bootstrap.api.as.server.JBossASServer;
import org.jboss.bootstrap.api.factory.ServerFactory;
import org.jboss.bootstrap.api.lifecycle.LifecycleState;
import org.jboss.bootstrap.spi.as.config.JBossASBasedConfigurationInitializer;


/**
 * Provides a command line interface to start the JBoss server.
 *
 * @author <a href="mailto:marc.fleury@jboss.org">Marc Fleury</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author <a href="mailto:adrian.brock@happeningtimes.com">Adrian Brock</a>
 * @author <a href="mailto:scott.stark@jboss.org">Scott Stark</a>
 * @author <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 111766 $
 */
public class Main
{

   /** A URL for obtaining microkernel patches */
   private URL bootURL;

   /** Extra jars from the /lib location that are added to the start of the boot
   classpath. This can be used to override jboss /lib boot classes.
   */
   private List<URL> bootLibraries = new LinkedList<URL>();

   /** Extra libraries to load the server with .*/
   private List<URL> extraLibraries = new LinkedList<URL>();

   /** Extra classpath URLS to load the server with .*/
   private List<URL> extraClasspath = new LinkedList<URL>();

   /**
    * The default list of boot libraries.  Does not include
    * the JAXP or JMX impl, users of this class should add the
    * proper libraries.
    * TODO: use vfs to list the root directory
    * http://www.jboss.org/index.html?module=bb&op=viewtopic&t=153175
    * 
    * Copied from legacy bootstrap ServerLoader
    * //TODO JBAS-6920
    */
   @Deprecated
   public static final String[] DEFAULT_BOOT_LIBRARY_LIST =
   {
         // Concurrent
         "concurrent.jar",
         // Logging
         "jboss-logging.jar",
         "jboss-logmanager.jar",
         // Common jars
         "jboss-common-core.jar",
         "xercesImpl.jar",
         "jbossxb.jar",
         // Bootstrap
         "jboss-bootstrap-spi.jar", "jboss-bootstrap-spi-as.jar", "jboss-bootstrap-spi-mc.jar",
         "jboss-bootstrap-impl-base.jar", "jboss-bootstrap-impl-as.jar",
         "jboss-bootstrap-impl-mc.jar","jboss-bootstrap-api-as.jar","jboss-bootstrap-api-mc.jar",
         "jboss-bootstrap-api.jar",
         // Microcontainer
         "javassist.jar", "jboss-reflect.jar", "jboss-mdr.jar", "jboss-dependency.jar", "jboss-kernel.jar",
         "jboss-metatype.jar", "jboss-managed.jar", "javax.inject.jar", "jboss-classpool.jar", "jboss-classpool-scoped.jar",
         "jboss-classpool-jbosscl.jar",
         
         // Fixme ClassLoading
         "jboss-vfs.jar", "jboss-classloading-spi.jar", "jboss-classloader.jar", "jboss-classloading.jar",
         "jboss-classloading-vfs.jar",
         // Fixme aop
         "jboss-aop.jar", "jboss-aop-mc-int.jar", "trove.jar",};

   /**
    * Server properties.  This object holds all of the required
    * information to get the server up and running. Use System
    * properties for defaults.
    */
   private Map<String, String> props = new HashMap<String, String>();

   /** 
    * The booted server instance.
    */
   private JBossASServer server;

   /**
    * The FQN of the default server implementation class to create
    */
   private static final String DEFAULT_AS_SERVER_IMPL_CLASS_NAME = "org.jboss.bootstrap.impl.as.server.JBossASServerImpl";

   /**
    * The name of the system property denoting where the boot.log will be placed
    */
   private static final String SYS_PROP_BOOT_LOG_DIR_NAME = "jboss.boot.server.log.dir";

   /**
    * Explicit constructor.
    */
   public Main()
   {
      super();

      // Set default properties
      // TODO for JBAS-7705 we do this again at the end of processCommandLine
      // so this can probably be eliminated
      final Properties sysProps = System.getProperties();
      for (final Object propName : sysProps.keySet())
      {
         final String propNameString = (String) propName;
         final String propValue = (String) sysProps.get(propNameString);
         props.put(propNameString, propValue);
      }
   }

   /**
    * Access the booted server.
    * @return the Server instance.
    */
   public JBossASServer getServer()
   {
      return server;
   }

   /**
    * Boot up JBoss.
    *
    * @param args   The command line arguments.
    *
    * @throws Exception    Failed to boot.
    */
   public void boot(final String[] args) throws Exception
   {
      // TODO -- remove this once we have Javassist Reflect performance fixed
      String tif = System.getProperty("org.jboss.reflect.spi.TypeInfoFactory");
      if (tif == null)
         System.setProperty("org.jboss.reflect.spi.TypeInfoFactory", "org.jboss.reflect.plugins.introspection.IntrospectionTypeInfoFactory");

      // TODO: remove this when JBAS-6744 is fixed
      String useUnorderedSequence = System.getProperty("xb.builder.useUnorderedSequence");
      if (useUnorderedSequence == null)
         System.setProperty("xb.builder.useUnorderedSequence", "true");

      // TODO -- remove once we know which parsing deployers need this
      String nim = System.getProperty("org.jboss.deployers.spi.deployer.matchers.NameIgnoreMechanism");
      if (nim == null)
         System.setProperty("org.jboss.deployers.spi.deployer.matchers.NameIgnoreMechanism", "org.jboss.deployers.spi.deployer.helpers.DummyNameIgnoreMechanism");

      // Workaround for http://community.jboss.org/message/532042#532042 // TODO -- remove this after XB update/resolution
      System.setProperty("xb.builder.repeatableParticleHandlers", "false");

      // First process the command line to pickup custom props/settings
      processCommandLine(args);

      // Set up the host name before the logmanager config, so it can be referenced there
      String hostName = System.getProperty("jboss.host.name");
      String qualifiedHostName = System.getProperty("jboss.qualified.host.name");
      if (qualifiedHostName == null)
      {
         // if host name is specified, don't pick a qualified host name that isn't related to it
         qualifiedHostName = hostName;
         if (qualifiedHostName == null)
         {
            // POSIX-like OSes including Mac should have this set
            qualifiedHostName = System.getenv("HOSTNAME");
         }
         if (qualifiedHostName == null)
         {
            // Certain versions of Windows
            qualifiedHostName = System.getenv("COMPUTERNAME");
         }
         if (qualifiedHostName == null)
         {
            try
            {
               qualifiedHostName = InetAddress.getLocalHost().getHostName();
            }
            catch (UnknownHostException e)
            {
               qualifiedHostName = null;
            }
         }
         if (qualifiedHostName != null && qualifiedHostName.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$|:"))
         {
            // IP address is not acceptable
            qualifiedHostName = null;
         }
         if (qualifiedHostName == null)
         {
            // Give up
            qualifiedHostName = "unknown-host.unknown-domain";
         }
         qualifiedHostName = qualifiedHostName.trim().toLowerCase();
         System.setProperty("jboss.qualified.host.name", qualifiedHostName);
      }
      if (hostName == null)
      {
         // Use the host part of the qualified host name
         final int idx = qualifiedHostName.indexOf('.');
         hostName = idx == -1 ? qualifiedHostName : qualifiedHostName.substring(0, idx);
         System.setProperty("jboss.host.name", hostName);
      }

      // Set up the node name
      String nodeName = System.getProperty("jboss.node.name");
      if (nodeName == null)
      {
         nodeName = hostName;
         System.setProperty("jboss.node.name", nodeName);
      }

      // Initialize the JDK logmanager
      String logManagerClassName = System.getProperty("java.util.logging.manager");
      if (logManagerClassName == null)
      {
         System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
         String pluginClass = System.getProperty("org.jboss.logging.Logger.pluginClass");
         if (pluginClass == null)
         {
            System.setProperty("org.jboss.logging.Logger.pluginClass", "org.jboss.logging.logmanager.LoggerPluginImpl");
            // disable log4j configuration, in case any early modules trigger it
            System.setProperty("log4j.defaultInitOverride", "true");
         }
      }
      
      // Set the RmiClassLoaderSpi to JBoss specific one
      // see https://jira.jboss.org/jira/browse/JBAS-7588?focusedCommentId=12509300#action_12509300
      String rmiClassLoaderSpi = System.getProperty("java.rmi.server.RMIClassLoaderSpi");
      // if already set, then don't override it
      if (rmiClassLoaderSpi == null)
      {
         String jbossRMIClassLoader = "org.jboss.system.JBossRMIClassLoader";
         // Is the class available?
         try
         {
            Thread.currentThread().getContextClassLoader().loadClass(jbossRMIClassLoader);
            // class was available, so set the property
            System.setProperty("java.rmi.server.RMIClassLoaderSpi", jbossRMIClassLoader);
         }
         catch (Throwable ignore)
         {
            // class isn't available, or there was some problem
            // loading that class, so don't set the system property 
         }
         
      }

      // Get JBOSS_HOME appropriately  
      final String homeUrl = props.get(JBossASServerConfig.PROP_KEY_JBOSSAS_HOME_URL);
      final String homeDir = props.get(JBossASServerConfig.PROP_KEY_JBOSSAS_HOME_DIR);
      URL jbossHome = null;
      // We've been given home URL
      if (homeUrl != null)
      {
         jbossHome = new URL(homeUrl);
      }
      // We've been given home dir
      else if (homeDir != null)
      {
         final File homeDirFile = new File(homeDir);
         if (!homeDirFile.exists())
         {
            throw new IllegalArgumentException("Specified " + JBossASServerConfig.PROP_KEY_JBOSSAS_HOME_DIR
                  + " does not point to a valid location: " + homeDirFile.toString());
         }
         jbossHome = homeDirFile.toURI().toURL();
      }
      // Nothing specified, so autoset relative to our location
      else
      {
         String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getFile();
         /* The 1.4 JDK munges the code source file with URL encoding so run
          * this path through the decoder so that is JBoss starts in a path with
          * spaces we don't come crashing down.
         */
         path = URLDecoder.decode(path, "UTF-8");
         File runJar = new File(path);
         File homeFile = runJar.getParentFile().getParentFile();
         URL homeUrlFromDir = homeFile.toURI().toURL();
         jbossHome = homeUrlFromDir;
      }

      // Get Library URL
      String libUrlFromProp = props.get(JBossASServerConfig.PROP_KEY_JBOSSAS_BOOT_LIBRARY_URL);
      URL libUrl = null;
      if (libUrlFromProp != null)
      {
         libUrl = new URL(libUrlFromProp);
      }
      else
      {
         libUrl = new URL(jbossHome, JBossASBasedConfigurationInitializer.VALUE_LIBRARY_URL_SUFFIX_DEFAULT);
      }

      /*
       * Set boot log directory
       */
      final String sysPropBootLogDir = SYS_PROP_BOOT_LOG_DIR_NAME;
      final String sysPropLogDir = JBossASServerConfig.PROP_KEY_JBOSSAS_SERVER_LOG_DIR;
      String serverName = props.get(JBossASServerConfig.PROP_KEY_JBOSSAS_SERVER_NAME);
      if (serverName == null || serverName.length() == 0)
      {
         serverName = JBossASBasedConfigurationInitializer.VALUE_SERVER_NAME_DEFAULT;
      }
      final String manualBootLogDir = props.get(sysPropBootLogDir);
      final String manualLogDir = props.get(sysPropLogDir);
      // If nothing's been explicitly specified
      if (manualBootLogDir == null && manualLogDir == null)
      {
         // We default it
         final URL serverLog = new URL(jbossHome, "server/" + serverName + "/log/");
         final File serverLogFile = new File(serverLog.toURI());
         final String serverLogString = serverLogFile.getAbsolutePath();
         SecurityActions.setSystemProperty(sysPropBootLogDir, serverLogString);
      }
      // If we've got a manual log dir, use it
      else if (manualLogDir != null)
      {
         SecurityActions.setSystemProperty(sysPropBootLogDir, manualLogDir);
      }

      // Get TCCL
      final ClassLoader tccl = SecurityActions.getThreadContextClassLoader();

      // Define a Set URLs to have visible to the CL loading the Server
      final Set<URL> urls = new HashSet<URL>();

      /* If there is a patch dir specified make it the first element of the
      loader bootstrap classpath. If its a file url pointing to a dir, then
      add the dir and its contents.
      */
      if (bootURL != null)
      {
         if (bootURL.getProtocol().equals("file"))
         {
            File dir = new File(bootURL.getFile());
            if (dir.exists())
            {
               // Add the local file patch directory
               urls.add(dir.toURL());

               // Add the contents of the directory too
               File[] jars = dir.listFiles(new JarFilter());

               for (int j = 0; jars != null && j < jars.length; j++)
               {
                  urls.add(jars[j].getCanonicalFile().toURL());
               }
            }
         }
         else
         {
            urls.add(bootURL);
         }
      }

      // Add any extra libraries
      for (int i = 0; i < bootLibraries.size(); i++)
      {
         urls.add(bootLibraries.get(i));
      }

      //      // Add the jars from the endorsed dir
      //      loader.addEndorsedJars();

      // jmx UnifiedLoaderRepository needs a concurrent class...
      //      urls.add(concurrentLib);

      // Add any extra libraries after the boot libs
      for (int i = 0; i < extraLibraries.size(); i++)
      {
         urls.add(extraLibraries.get(i));
      }

      // Add any extra classapth URLs
      for (int i = 0; i < extraClasspath.size(); i++)
      {
         urls.add(extraClasspath.get(i));
      }

      // Add all boot libs required from $JBOSS_HOME/lib
      final File bootLibDir = new File(libUrl.toURI());
      if (!bootLibDir.exists())
      {
         throw new IllegalArgumentException("Boot lib directory not found: " + bootLibDir.toString());
      }
      if (!bootLibDir.isDirectory())
      {
         throw new IllegalArgumentException("Boot lib directory is not a directory: " + bootLibDir.toString());
      }
      for (String filename : DEFAULT_BOOT_LIBRARY_LIST)
      {
         final File bootLibFile = new File(bootLibDir, filename);
         if (!bootLibFile.exists())
         {
            System.out.println("WARNING: Could not find expected boot lib " + bootLibFile);
         }
         final URL bootLibUrl = bootLibFile.toURI().toURL();
         urls.add(bootLibUrl);
      }

      // Make a ClassLoader to be used in loading the server
      final URL[] urlArray = urls.toArray(new URL[]{});
      final ClassLoader loadingCl = new URLClassLoader(urlArray, tccl);

      // Load the server
      server = JBossASServer.class.cast(ServerFactory.createServer(DEFAULT_AS_SERVER_IMPL_CLASS_NAME, loadingCl));

      // Get out the default configuration
      // This cast to object first is to workaround the JDK Bug: http://bugs.sun.com/view_bug.do?bug_id=6548436
      final Object jdk6Bug6548436Hack = (Object) server.getConfiguration();
      JBossASServerConfig config = (JBossASServerConfig) jdk6Bug6548436Hack;

      // Set JBOSS_HOME and properties
      config.properties(props).jbossHome(jbossHome);

      // Make a shutdown hook
      SecurityActions.addShutdownHook(new ShutdownHook(server, loadingCl));

      try
      {
         // Set the CL
         SecurityActions.setThreadContextClassLoader(loadingCl);

         // Initialize the server
         server.initialize();

         // Start 'er up mate!
         server.start();
      }
      finally
      {
         // Reset the CL 
         SecurityActions.setThreadContextClassLoader(tccl);
      }
   }

   /**
    * Shutdown the booted Server instance.
    *
    */
   public void shutdown() throws Throwable
   {
      server.shutdown();
   }

   private URL makeURL(String urlspec) throws MalformedURLException
   {
      urlspec = urlspec.trim();

      URL url;

      try
      {
         url = new URL(urlspec);
         if (url.getProtocol().equals("file"))
         {
            // make sure the file is absolute & canonical file url
            File file = new File(url.getFile()).getCanonicalFile();
            url = file.toURL();
         }
      }
      catch (Exception e)
      {
         // make sure we have a absolute & canonical file url
         try
         {
            File file = new File(urlspec).getCanonicalFile();
            url = file.toURL();
         }
         catch (Exception n)
         {
            throw new MalformedURLException(n.toString());
         }
      }

      return url;
   }

   /** Process the command line... */
   private void processCommandLine(final String[] args) throws Exception
   {
      // set this from a system property or default to jboss
      String programName = System.getProperty("program.name", "jboss");
      String sopts = "-:hD:d:p:n:c:Vj::B:L:C:P:b:g:u:m:H:N:";
      LongOpt[] lopts =
      {
            new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
            new LongOpt("bootdir", LongOpt.REQUIRED_ARGUMENT, null, 'd'),
            new LongOpt("patchdir", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
            new LongOpt("netboot", LongOpt.REQUIRED_ARGUMENT, null, 'n'),
            new LongOpt("configuration", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
            new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'V'),
            new LongOpt("jaxp", LongOpt.REQUIRED_ARGUMENT, null, 'j'),
            new LongOpt("bootlib", LongOpt.REQUIRED_ARGUMENT, null, 'B'),
            new LongOpt("library", LongOpt.REQUIRED_ARGUMENT, null, 'L'),
            new LongOpt("classpath", LongOpt.REQUIRED_ARGUMENT, null, 'C'),
            new LongOpt("properties", LongOpt.REQUIRED_ARGUMENT, null, 'P'),
            new LongOpt("host", LongOpt.REQUIRED_ARGUMENT, null, 'b'),
            new LongOpt("partition", LongOpt.REQUIRED_ARGUMENT, null, 'g'),
            new LongOpt("udp", LongOpt.REQUIRED_ARGUMENT, null, 'u'),
            new LongOpt("mcast_port", LongOpt.REQUIRED_ARGUMENT, null, 'm'),
            new LongOpt("hostname", LongOpt.REQUIRED_ARGUMENT, null, 'H'),
            new LongOpt("nodename", LongOpt.REQUIRED_ARGUMENT, null, 'N'),
      };

      Getopt getopt = new Getopt(programName, args, sopts, lopts);
      int code;
      String arg;
      while ((code = getopt.getopt()) != -1)
      {
         switch (code)
         {
            case ':' :
            case '?' :
               // for now both of these should exit with error status
               System.exit(1);
               break; // for completeness

            case 1 :
               // this will catch non-option arguments
               // (which we don't currently care about)
               System.err.println(programName + ": unused non-option argument: " + getopt.getOptarg());
               break; // for completeness

            case 'h' :
               // show command line help
               System.out.println("usage: " + programName + " [options]");
               System.out.println();
               System.out.println("options:");
               System.out.println("    -h, --help                    Show this help message");
               System.out.println("    -V, --version                 Show version information");
               System.out.println("    --                            Stop processing options");
               System.out.println("    -D<name>[=<value>]            Set a system property");
               System.out
                     .println("    -d, --bootdir=<dir>           Set the boot patch directory; Must be absolute or url");
               System.out.println("    -p, --patchdir=<dir>          Set the patch directory; Must be absolute or url");
               System.out.println("    -n, --netboot=<url>           Boot from net with the given url as base");
               System.out.println("    -c, --configuration=<name>    Set the server configuration name");
               System.out.println("    -B, --bootlib=<filename>      Add an extra library to the front bootclasspath");
               System.out.println("    -L, --library=<filename>      Add an extra library to the loaders classpath");
               System.out.println("    -C, --classpath=<url>         Add an extra url to the loaders classpath");
               System.out.println("    -P, --properties=<url>        Load system properties from the given url");
               System.out.println("    -b, --host=<host or ip>       Bind address for all JBoss services");
               System.out.println("    -g, --partition=<name>        HA Partition name (default=DefaultDomain)");
               System.out.println("    -m, --mcast_port=<ip>         UDP multicast port; only used by JGroups");
               System.out.println("    -u, --udp=<ip>                UDP multicast address");
               System.out.println("    -H, --hostname=<name>         Set the host name");
               System.out.println("    -N, --nodename=<name>         Set the node name to use");
               System.out.println();
               System.exit(0);
               break; // for completeness

            case 'D' : {
               // set a system property
               arg = getopt.getOptarg();
               String name, value;
               int i = arg.indexOf("=");
               if (i == -1)
               {
                  name = arg;
                  value = "true";
               }
               else
               {
                  name = arg.substring(0, i);
                  value = arg.substring(i + 1, arg.length());
               }
               System.setProperty(name, value);
               // Ensure setting the old bind.address property also sets the new
               // jgroups.bind_addr property, otherwise jgroups may ignore it
               if ("bind.address".equals(name))
               {
                  System.setProperty("jgroups.bind_addr", value);
               }
               break;
            }

            case 'd' :
               // set the boot patch URL
               bootURL = makeURL(getopt.getOptarg());
               break;

            case 'p' : {
               // set the patch URL
               URL patchURL = makeURL(getopt.getOptarg());
               //TODO
               //               props.put(ServerConfig.PATCH_URL, patchURL.toString());

               break;
            }

            case 'n' :
               // set the net boot url
               arg = getopt.getOptarg();

               // make sure there is a trailing '/'
               if (!arg.endsWith("/"))
                  arg += "/";

               props.put(JBossASServerConfig.PROP_KEY_JBOSSAS_HOME_URL, new URL(arg).toString());
               break;

            case 'c' :
               // set the server name
               arg = getopt.getOptarg();
               props.put(JBossASServerConfig.PROP_KEY_JBOSSAS_SERVER_NAME, arg);
               break;

            case 'V' : {
               // Package information for org.jboss
               Package jbossPackage = Package.getPackage("org.jboss");

               // show version information
               System.out.println("JBoss " + jbossPackage.getImplementationVersion());
               System.out.println();
               System.out.println("Distributable under LGPL license.");
               System.out.println("See terms of license at gnu.org.");
               System.out.println();
               System.exit(0);
               break; // for completness
            }

            case 'j' :
               // Show an error and exit
               System.err.println(programName + ": option '-j, --jaxp' no longer supported");
               System.exit(1);
               break; // for completness

            case 'B' :
               arg = getopt.getOptarg();
               bootLibraries.add(new File(arg).toURI().toURL());
               break;

            case 'L' :
               arg = getopt.getOptarg();
               extraLibraries.add(new File(arg).toURI().toURL());
               break;

            case 'C' : {
               URL url = makeURL(getopt.getOptarg());
               extraClasspath.add(url);
               break;
            }

            case 'P' : {
               // Set system properties from url/file
               URL url = makeURL(getopt.getOptarg());
               Properties props = System.getProperties();
               props.load(url.openConnection().getInputStream());
               break;
            }
            case 'b' :
               arg = getopt.getOptarg();
               final String bindAddressPropName = JBossASServerConfig.PROP_KEY_JBOSSAS_BIND_ADDRESS;
               props.put(bindAddressPropName, arg);
               System.setProperty(bindAddressPropName, arg);
               // used by JGroups; only set if not set via -D so users
               // can use a different interface for cluster communication
               // There are 2 versions of this property, deprecated bind.address
               // and the new version, jgroups.bind_addr
               String bindAddress = System.getProperty("bind.address");
               if (bindAddress == null)
               {
                  System.setProperty("bind.address", arg);
               }
               bindAddress = System.getProperty("jgroups.bind_addr");
               if (bindAddress == null)
               {
                  System.setProperty("jgroups.bind_addr", arg);
               }

               // Set the java.rmi.server.hostname if not set
               String rmiHost = System.getProperty("java.rmi.server.hostname");
               if (rmiHost == null)
               {
                  System.setProperty("java.rmi.server.hostname", arg);
               }
               break;

            case 'g' :
               arg = getopt.getOptarg();
               final String partitionNamePropName = JBossASServerConfig.PROP_KEY_JBOSSAS_PARTITION_NAME;
               props.put(partitionNamePropName, arg);
               System.setProperty(partitionNamePropName, arg);
               break;

            case 'u' :
               arg = getopt.getOptarg();
               final String udpGroupPropName = JBossASServerConfig.PROP_KEY_JBOSSAS_PARTITION_UDP_GROUP;
               props.put(udpGroupPropName, arg);
               System.setProperty(udpGroupPropName, arg);
               // the new jgroups property name
               System.setProperty("jgroups.udp.mcast_addr", arg);
               break;

            case 'm' :
               arg = getopt.getOptarg();
               final String udpPortPropName = JBossASServerConfig.PROP_KEY_JBOSSAS_PARTITION_UDP_PORT;
               props.put(udpPortPropName, arg);
               System.setProperty(udpPortPropName, arg);
               break;

            case 'H' : {
               arg = getopt.getOptarg();
               System.setProperty("jboss.qualified.host.name", arg.trim().toLowerCase());
               break;
            }

            case 'N' : {
               arg = getopt.getOptarg();
               System.setProperty("jboss.node.name", arg.trim());
               break;
            }

            default :
               // this should not happen,
               // if it does throw an error so we know about it
               throw new Error("unhandled option code: " + code);
         }
      }      

      // JBAS-7705 -- -P or other switches above may have gotten 'props'
      // and system properties out of sync -- fix that
      final Properties sysProps = System.getProperties();
      for (final Object propName : sysProps.keySet())
      {
         final String propNameString = (String) propName;
         final String propValue = (String) sysProps.get(propNameString);
         props.put(propNameString, propValue);
      }
      
      // Ensure we have a bind address
      final String propKeyJBossasBindAddress = JBossASServerConfig.PROP_KEY_JBOSSAS_BIND_ADDRESS;
      if (props.get(propKeyJBossasBindAddress) == null)
      {
         final String defaultBindAddress = "127.0.0.1";
         props.put(propKeyJBossasBindAddress, defaultBindAddress);
         System.setProperty(propKeyJBossasBindAddress, defaultBindAddress);
      }

      // Make sure some address properties are set and/or don't specify
      // a "any local address" value that's useless for their intended usage
      String defaultAddress = System.getProperty(JBossASServerConfig.PROP_KEY_JBOSSAS_BIND_ADDRESS);
      ServerConfigUtil.fixRemoteAddressProperty("java.rmi.server.hostname", defaultAddress);
      ServerConfigUtil.fixRemoteAddressProperty("jgroups.bind_addr", defaultAddress);
      // Don't set deprecated bind.address -- just fix it if it's already set
      ServerConfigUtil.fixRemoteAddressProperty("bind.address", null);

      // Enable jboss.vfs.forceCopy by default, if unspecified
      if (System.getProperty("jboss.vfs.forceCopy") == null)
         System.setProperty("jboss.vfs.forceCopy", "true");
   }

   /**
    * This is where the magic begins.
    *
    * <P>Starts up inside of a "jboss" thread group to allow better
    *    identification of JBoss threads.
    *
    * @param args    The command line arguments.
    * @throws Exception for any error
    */
   public static void main(final String[] args) throws Exception
   {
      Runnable worker = new Runnable()
      {
         public void run()
         {
            try
            {
               Main main = new Main();
               main.boot(args);
            }
            catch (Exception e)
            {
               System.err.println("Failed to boot JBoss:");
               e.printStackTrace();
            }
         }

      };

      ThreadGroup threads = new ThreadGroup("jboss");
      new Thread(threads, worker, "main").start();
   }

   /**
    * This method is here so that if JBoss is running under
    * Alexandria (An NT Service Installer), Alexandria can shutdown
    * the system down correctly.
    * 
    * @param argv the arguments
    */
   public static void systemExit(String argv[])
   {
      System.exit(0);
   }

   static class JarFilter implements FilenameFilter
   {
      public boolean accept(File dir, String name)
      {
         return name.endsWith(".jar");
      }
   }

   /**
    * ServerConfigUtil
    * 
    * Utilities for accessing server configuration
    * 
    * @author <a href="mailto:adrian@jboss.org">Adrian Brock</a>
    * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a> Maintained only
    * @version $Revision: 111766 $
    * @deprecated Moved from jboss-bootstrap Legacy as a transition class; 
    *   will be removed once we move entirely to a new Main
    */
   @Deprecated
   private static class ServerConfigUtil
   {
      /**
       * Fix the remote address.
       * 
       * If we pass the address to the client we don't want to
       * tell it to connect to 0.0.0.0, use our host name instead
       * @param address the passed address
       * @return the fixed address
       */
      public static String fixRemoteAddress(String address)
      {
         try
         {
            if (address == null || JBossASBasedConfigurationInitializer.VALUE_BIND_ADDRESS_ANY.equals(address))
               return InetAddress.getLocalHost().getHostName();
         }
         catch (UnknownHostException ignored)
         {
         }
         return address;
      }
      
      /**
       * Checks if the given system property is set; if so ensures it's value has
       * been fixed by {@link #fixRemoteAddress(String)}; otherwise if 
       * a default value has been provided, passes the defaultValue through
       * {@link #fixRemoteAddress(String)} and sets the system property. 
       * 
       * @param systemPropertyName the name of the system property
       * @param defaultValue the defaultValue to use if the property isn't
       *                     already set
       */
      public static void fixRemoteAddressProperty(String systemPropertyName, 
                                                  String defaultValue)
      {
         String old = System.getProperty(systemPropertyName);
         if (old == null)
         {
            if (defaultValue != null)
            {
               String fixed = fixRemoteAddress(defaultValue);
               System.setProperty(systemPropertyName, fixed);
            }
         }
         else
         {
            String fixed = fixRemoteAddress(old);
            System.setProperty(systemPropertyName, fixed);
         }
      }

   }

   /**
    * ShutdownHook
    *
    * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
    * @version $Revision: 111766 $
    */
   private static class ShutdownHook extends Thread
   {

      /**
       * Underlying server instance
       */
      private JBossASServer server;

      /**
       * ClassLoader used to start/init the server
       */
      private ClassLoader serverCl;

      /**
       * Constructor
       * 
       * @param server
       * @param serverCl The ClassLoader to be optionally closed upon shutdown
       * @throws IllegalArgumentException If any argument is not supplied (null)
       */
      ShutdownHook(final JBossASServer server, final ClassLoader serverCl) throws IllegalArgumentException
      {
         // Precondition checks
         if (server == null)
         {
            throw new IllegalArgumentException("server must be specified");
         }
         if (serverCl == null)
         {
            throw new IllegalArgumentException("server ClassLoader must be specified");
         }

         // Set properties
         this.server = server;
         this.serverCl = serverCl;
      }

      /**
       * Shuts down the server if running
       */
      @Override
      public void run()
      {
         // If we have a server
         if (server != null)
         {
            // Log out
            System.out.println("Posting Shutdown Request to the server...");

            
            // start in new thread to give positive
            // feedback to requesting client of success.
            final Thread shutdownThread = new Thread()
            {
               public void run()
               {
                  try
                  {
                     // Flag if we've got a clean shutdown (ie. server had completely started)
                     boolean cleanShutdown = false;
                     
                     /*
                      * This bit is a hack.
                      * 
                      * It's in place because AS does not presently respond to Thread
                      * interruption.  If it did, then the start lifecycle would
                      * attempt to cleanly finish in expedient fashion such that the shutdown
                      * lifecycle could take over.
                      * 
                      * Until the server is able to have start() get interrupted, we'd be 
                      * blocking on a complete startup before shutdown could continue.  
                      * We don't want to wait for full startup, so this hack only triggers
                      * shutdown if fully started.  If still starting, we prematurely
                      * halt the VM.
                      * 
                      * JBBOOT-75
                      * JBAS-6974
                      */
                     // Shutdown if started only
                     if (server.getState().equals(LifecycleState.STARTED))
                     {
                        cleanShutdown = true;
                        server.shutdown();
                     }
                     
                     /*
                      * Close the Loading CL, if URLCL and JDK7+
                      * JBBOOT-23
                      */
                     if (serverCl != null && serverCl instanceof Closeable)
                     {
                        try
                        {
                           ((Closeable) serverCl).close();
                        }
                        catch (IOException ioe)
                        {
                           // Swallow
                        }
                     }
                     
                     /*
                      * Part of the halt hack as explained above
                      * 
                      * JBBOOT-75
                      * JBAS-6974
                      */
                     // If the server's not fully started, we're not going to block on it
                     // before calling shutdown, so just kill the VM
                     if (!cleanShutdown)
                     {
                        System.out.println("Server startup has not completed, so halting the process");
                        Runtime.getRuntime().halt(-1);
                     }
                     
                  }
                  // In case of any Exception thrown up the chain, let us know
                  catch (final Exception e)
                  {
                     throw new RuntimeException("Exception encountered in shutting down the server", e);
                  }
               }
            };

            // Run
            shutdownThread.start();

            // Block until done
            try
            {
               shutdownThread.join();
            }
            catch (final InterruptedException ie)
            {
               // Clear the flag
               Thread.interrupted();
            }
         }
      }
   }

}
