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

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.bootstrap.api.as.server.JBossASServer;

/**
 * A JMX client that uses an MBeanServerConnection to shutdown a remote JBoss
 * server.
 *
 * @version <tt>$Revision: 105008 $</tt>
 * @author  <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author  Scott.Stark@jboss.org
 */
public class Shutdown
{
   /////////////////////////////////////////////////////////////////////////
   //                         Command Line Support                        //
   /////////////////////////////////////////////////////////////////////////

   public static final String PROGRAM_NAME = System.getProperty("program.name", "shutdown");
   public static final String DEFAULT_BASEURL = "service:jmx:rmi:///jndi/rmi://";
   public static final String DEFAULT_RMIOBJECTNAME = "/jmxrmi";
   public static final String DEFAULT_JMXSERVICEURL = "service:jmx:rmi:///jndi/rmi://localhost:1090/jmxrmi";
   public static final String DEFAULT_HOSTNAME = "localhost";
   public static final String DEFAULT_PORT ="1090";

   protected static void displayUsage()
   {
      System.out.println("A JMX client to shutdown (exit or halt) a remote JBoss server.");
      System.out.println();
      System.out.println("usage: " + PROGRAM_NAME + " [options] <operation>");
      System.out.println();
      System.out.println("options:");
      System.out.println("    -h, --help                Show this help message (default)");
      System.out.println("    -D<name>[=<value>]        Set a system property");
      System.out.println("    --                        Stop processing options");
      System.out.println("    -s, --server=<url>        The JMX service URL of the remote server (e.g. "+ DEFAULT_JMXSERVICEURL +") ");
      System.out.println("    -o, --host=<HOSTNAME>     The name of the remote server (e.g. "+ DEFAULT_HOSTNAME +") ");
      System.out.println("    -r, --port=<PORTNUMBER>   The rmiRegistryPort of the remote server (e.g. "+ DEFAULT_PORT +") ");
      System.out.println("    -n, --serverName=<url>    Specify the JMX name of the ServerImpl");
      System.out.println("    -u, --user=<name>         Specify the username for authentication");
      System.out.println("    -p, --password=<name>     Specify the password for authentication");
      System.out.println("    -v, --verbose             Be noisy");
      System.out.println();
      System.out.println("operations:");
      System.out.println("    -S, --shutdown            Shutdown the server");
      System.out.println();
      System.out.println("for convenience, you can specify --host and --port but not with the --server=<url> which overrides host + port. ");

   }

   public static void main(final String[] args) throws Exception
   {
      if (args.length == 0)
      {
         displayUsage();
         System.exit(0);
      }

      String sopts = "-:hD:s:n:a:u:p:S::v::o:r:";
      LongOpt[] lopts =
      {
         new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
         new LongOpt("server", LongOpt.REQUIRED_ARGUMENT, null, 's'),
         new LongOpt("adapter", LongOpt.REQUIRED_ARGUMENT, null, 'a'),
         new LongOpt("serverName", LongOpt.REQUIRED_ARGUMENT, null, 'n'),
         new LongOpt("shutdown", LongOpt.NO_ARGUMENT, null, 'S'),
         new LongOpt("user", LongOpt.REQUIRED_ARGUMENT, null, 'u'),
         new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v'),
         new LongOpt("password", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
         new LongOpt("host", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
         new LongOpt("port", LongOpt.REQUIRED_ARGUMENT, null, 'r'),
      };

      Getopt getopt = new Getopt(PROGRAM_NAME, args, sopts, lopts);
      int code;
      String arg;

      String serverURL = null;
      String username = null;
      String password = null;
      ObjectName serverJMXName = new ObjectName("jboss.system:type=Server");
      String hostname=null;
      String port=null;
      boolean verbose = false;

      while ((code = getopt.getopt()) != -1)
      {
         switch (code)
         {
            case ':':
            case '?':
               // for now both of these should exit with error status
               System.exit(1);
               break;

            case 1:
               // this will catch non-option arguments
               // (which we don't currently care about)
               System.err.println(PROGRAM_NAME + ": unused non-option argument: " +
               getopt.getOptarg());
               break;
            case 'h':
               displayUsage();
               System.exit(0);
               break;
            case 'D':
            {
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
               break;
            }
            case 's':
               serverURL = getopt.getOptarg();
               break;
            case 'n':
               serverJMXName = new ObjectName(getopt.getOptarg());
               break;
            case 'S':
               // nothing...
               break;
            case 'a':
               String adapterName = getopt.getOptarg();
               System.out.println("adapter name is ignored " + adapterName);
               break;
            case 'u':
               username = getopt.getOptarg();
               break;
            case 'p':
               password = getopt.getOptarg();
               break;
            // host name
            case 'o':
               hostname = getopt.getOptarg();
               break;

            // host port
            case 'r':
               port = getopt.getOptarg();
               break;
            // be noisy
            case 'v':
               verbose = true;
               break;
         }
      }


      // If there was a username specified, but no password prompt for it
      if( username != null && password == null )
      {
         System.out.print("Enter password for "+username+": ");
         BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
         password = br.readLine();
      }

      if( serverURL == null)
      {
         serverURL = DEFAULT_BASEURL +
            (hostname != null ? hostname : DEFAULT_HOSTNAME) +
            ":" +
            (port != null ? port : DEFAULT_PORT) +
         DEFAULT_RMIOBJECTNAME;
      }

      if( verbose )
      {
         System.out.println("JMX server url=" + serverURL);
      }
      HashMap env = new HashMap();
      if (username != null && password != null)
      {
         if (verbose )
         {
            System.out.println("will connect with username=" + username);
         }
         String[] creds = new String[2];
         creds[0] = username;
         creds[1] = password;
         env.put(JMXConnector.CREDENTIALS, creds);
      }

      JMXServiceURL url = new JMXServiceURL(serverURL);
      JMXConnector jmxc = JMXConnectorFactory.connect(url, env);
      MBeanServerConnection adaptor = jmxc.getMBeanServerConnection();

      ServerProxyHandler handler = new ServerProxyHandler(adaptor, serverJMXName);
      Class<?>[] ifaces = {JBossASServer.class};
      ClassLoader tcl = Thread.currentThread().getContextClassLoader();
      JBossASServer server = (JBossASServer) Proxy.newProxyInstance(tcl, ifaces, handler);
      server.shutdown();

      System.out.println("Shutdown message has been posted to the server.");
      System.out.println("Server shutdown may take a while - check logfiles for completion");
      jmxc.close();
   }

   private static class ServerProxyHandler implements InvocationHandler
   {
      ObjectName serverName;
      MBeanServerConnection server;
      ServerProxyHandler(MBeanServerConnection server, ObjectName serverName)
      {
         this.server = server;
         this.serverName = serverName;
      }

      public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable
      {
         String methodName = method.getName();
         Class[] sigTypes = method.getParameterTypes();
         ArrayList sigStrings = new ArrayList();
         for(int s = 0; s < sigTypes.length; s ++)
            sigStrings.add(sigTypes[s].getName());
         String[] sig = new String[sigTypes.length];
         sigStrings.toArray(sig);
         Object value = null;
         try
         {
            value = server.invoke(serverName, methodName, args, sig);
         }
         catch(UndeclaredThrowableException e)
         {
            System.out.println("getUndeclaredThrowable: "+e.getUndeclaredThrowable());
            throw e.getUndeclaredThrowable();
         }
         return value;
      }
   }
}
