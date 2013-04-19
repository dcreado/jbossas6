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
package org.jboss.console.twiddle;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.NamingException;

import org.jboss.console.twiddle.command.Command;
import org.jboss.console.twiddle.command.CommandContext;
import org.jboss.console.twiddle.command.CommandException;
import org.jboss.console.twiddle.command.NoSuchCommandException;
import org.jboss.logging.Logger;
import org.jboss.util.Strings;

/**
 * A command to invoke an operation on an MBean (or MBeans).
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author Scott.Stark@jboss.org
 * @author <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 * @version $Revision: 109975 $
 */
public class Twiddle
{
   public static final String PROGRAM_NAME = System.getProperty("program.name", "twiddle");
   public static final String CMD_PROPERTIES = "/org/jboss/console/twiddle/commands.properties";
   public static final String DEFAULT_BASEURL = "service:jmx:rmi:///jndi/rmi://";
   public static final String DEFAULT_RMIOBJECTNAME = "/jmxrmi";
   public static final String DEFAULT_JMXSERVICEURL = "service:jmx:rmi:///jndi/rmi://localhost:1090/jmxrmi";
   public static final String DEFAULT_HOSTNAME = "localhost";
   public static final String DEFAULT_PORT ="1090";

   private static final Logger log = Logger.getLogger(Twiddle.class);
   // Command Line Support
   private static Twiddle twiddle = new Twiddle(new PrintWriter(System.out, true),
      new PrintWriter(System.err, true));
   private static String commandName;
   private static String[] commandArgs;
   private static boolean commandHelp;
   private static URL cmdProps;

   private List commandProtoList = new ArrayList();
   private Map commandProtoMap = new HashMap();
   private PrintWriter out;
   private PrintWriter err;
   private String serverURL = buildJMXServiceUrl();
   private String hostname;
   private String port;
   private boolean quiet;
   private MBeanServerConnection server;
   JMXConnector jmxc;
   private String username;
   private String password;
   private boolean verbose;

   public Twiddle(final PrintWriter out, final PrintWriter err)
   {
      this.out = out;
      this.err = err;
   }

   // build JMXServiceURL, should look like  "service:jmx:rmi:///jndi/rmi://localhost:1090/jmxrmi";
   private String buildJMXServiceUrl()
   {
	   // JBAS-8540
      return
         DEFAULT_BASEURL +
         fixHostnameForURL(hostname != null ? hostname : DEFAULT_HOSTNAME) +
         ":" +
         (port != null ? port : DEFAULT_PORT) +
         DEFAULT_RMIOBJECTNAME;
   }
   
   // would ordinarily use ServerConfigUtil.fixHostnameForURL() but not on client classpath
   private String fixHostnameForURL(String host) {
       if (host == null)
    	   return host ;

       // if the hostname is an IPv6 literal, enclose it in brackets
       if (host.indexOf(':') != -1)
    	   return "[" + host + "]" ;
       else 
    	   return host ;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(final String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(final String password) {
      this.password = password;
   }

   public void setHostname(String hostname)
   {
      this.hostname = hostname;
      setServerURL(buildJMXServiceUrl());    // use new hostname
   }

   public void setPort(String port)
   {
      this.port = port;
      setServerURL(buildJMXServiceUrl());  // use new port setting
   }

   public void setVerbose( boolean verbose )
   {
      this.verbose = verbose;
   }

   public void setServerURL(final String url)
   {
      if (verbose)
      {
         log.info("replacing JMX service url (" + this.serverURL + ") with " + url);
      }
      this.serverURL = url;
   }

   public void setQuiet(final boolean flag)
   {
      this.quiet = flag;
   }

   public void addCommandPrototype(final Command proto)
   {
      String name = proto.getName();

      log.debug("Adding command '" + name + "'; proto: " + proto);

      commandProtoList.add(proto);
      commandProtoMap.put(name, proto);
   }

   private CommandContext createCommandContext()
   {
      return new CommandContext()
      {
         public boolean isQuiet()
         {
            return quiet;
         }

         public PrintWriter getWriter()
         {
            return out;
         }

         public PrintWriter getErrorWriter()
         {
            return err;
         }

         public void closeServer()
         {
            if (jmxc != null)
            {
               server = null;
               try {
                  jmxc.close();
               } catch (IOException e) {
                  throw new org.jboss.util.NestedRuntimeException(e);
               }
            }

         }

         public MBeanServerConnection getServer()
         {
            try
            {
               connect();
            }
            catch (Exception e)
            {
               throw new org.jboss.util.NestedRuntimeException(e);
            }

            return server;
         }
      };
   }

   public Command createCommand(final String name)
      throws NoSuchCommandException, Exception
   {
      //
      // jason: need to change this to accept unique substrings on command names
      //

      Command proto = (Command) commandProtoMap.get(name);
      if (proto == null)
      {
         throw new NoSuchCommandException(name);
      }

      Command command = (Command) proto.clone();
      command.setCommandContext(createCommandContext());

      return command;
   }

   private int getMaxCommandNameLength()
   {
      int max = 0;

      Iterator iter = commandProtoList.iterator();
      while (iter.hasNext())
      {
         Command command = (Command) iter.next();
         String name = command.getName();
         if (name.length() > max)
         {
            max = name.length();
         }
      }

      return max;
   }

   public void displayCommandList()
   {
      if( commandProtoList.size() == 0 )
      {
         try
         {
            loadCommands();
         }
         catch(Exception e)
         {
            System.err.println("Failed to load commnads from: "+cmdProps);
            e.printStackTrace();
         }
      }
      Iterator iter = commandProtoList.iterator();

      out.println(PROGRAM_NAME + " commands: ");

      int maxNameLength = getMaxCommandNameLength();
      log.debug("max command name length: " + maxNameLength);

      while (iter.hasNext())
      {
         Command proto = (Command) iter.next();
         String name = proto.getName();
         String desc = proto.getDescription();

         out.print("    ");
         out.print(name);

         // an even pad, so things line up correctly
         out.print(Strings.pad(" ", maxNameLength - name.length()));
         out.print("    ");

         out.println(desc);
      }

      out.flush();
   }

   private JMXConnector createMBeanServerConnection()
      throws NamingException, IOException
   {
      HashMap env = new HashMap();
      if (username != null && password != null)
      {
         if (this.verbose )
         {
            out.println("will connect with username=" + username);
         }
         String[] creds = new String[2];
         creds[0] = username;
         creds[1] = password;
         env.put(JMXConnector.CREDENTIALS, creds);
      }
      JMXServiceURL url = new JMXServiceURL(this.serverURL);
      JMXConnector jmxc = JMXConnectorFactory.connect(url, env);
      return jmxc;
   }

   private void connect()
      throws NamingException, IOException
   {
      if (server == null)
      {
         jmxc = createMBeanServerConnection();
         server = jmxc.getMBeanServerConnection();

      }
   }

   public static void main(final String[] args)
   {
      Command command = null;

      try
      {
         // initialize java.protocol.handler.pkgs
         initProtocolHandlers();
         
         // Prosess global options
         processArguments(args);
         loadCommands();

         // Now execute the command
         if (commandName == null)
         {
            // Display program help
            displayHelp();
         }
         else
         {
            command = twiddle.createCommand(commandName);

            if (commandHelp)
            {
               System.out.println("Help for command: '" + command.getName() + "'");
               System.out.println();
               
               command.displayHelp();
            }
            else
            {
               // Execute the command
               command.execute(commandArgs);
            }
         }

         System.exit(0);
      }
      catch (CommandException e)
      {
         log.error("Command failure", e);
         System.err.println();
         
         if (e instanceof NoSuchCommandException)
         {
            twiddle.displayCommandList();
         }
         else
         {

            if (command != null)
            {
               System.err.println("Help for command: '" + command.getName() + "'");
               System.err.println();
               
               command.displayHelp();
            }
         }
         System.exit(1);
      }
      catch (Exception e)
      {
         log.error("Exec failed", e);
         System.exit(1);
      }
   }

   private static void initProtocolHandlers()
   {
      // Include the default JBoss protocol handler package
      String handlerPkgs = System.getProperty("java.protocol.handler.pkgs");
      if (handlerPkgs != null)
      {
         handlerPkgs += "|org.jboss.net.protocol";
      }
      else
      {
         handlerPkgs = "org.jboss.net.protocol";
      }
      System.setProperty("java.protocol.handler.pkgs", handlerPkgs);      
   }
   
   private static void loadCommands() throws Exception
   {
      // load command protos from property definitions
      if( cmdProps == null )
         cmdProps = Twiddle.class.getResource(CMD_PROPERTIES);
      if (cmdProps == null)
         throw new IllegalStateException("Failed to find: " + CMD_PROPERTIES);
      InputStream input = cmdProps.openStream();
      log.debug("command proto type properties: " + cmdProps);
      Properties props = new Properties();
      props.load(input);
      input.close();

      Iterator iter = props.keySet().iterator();
      while (iter.hasNext())
      {
         String name = (String) iter.next();
         String typeName = props.getProperty(name);
         Class type = Class.forName(typeName);

         twiddle.addCommandPrototype((Command) type.newInstance());
      }      
   }

   private static void displayHelp()
   {
      java.io.PrintStream out = System.out;

      out.println("A JMX client to 'twiddle' with a remote JBoss server.");
      out.println();
      out.println("usage: " + PROGRAM_NAME + " [options] <command> [command_arguments]");
      out.println();
      out.println("options:");
      out.println("    -h, --help                Show this help message");
      out.println("        --help-commands       Show a list of commands");
      out.println("    -H<command>               Show command specific help");
      out.println("    -c=command.properties     Specify the command.properties file to use");
      out.println("    -D<name>[=<value>]        Set a system property");
      out.println("    --                        Stop processing options");
      out.println("    -s, --server=<url>        The JMX service URL of the remote server (e.g. "+ DEFAULT_JMXSERVICEURL +") ");
      out.println("    -o, --host=<HOSTNAME>     The name of the remote server (e.g. "+ DEFAULT_HOSTNAME +") ");
      out.println("    -r, --port=<PORTNUMBER>   The rmiRegistryPort of the remote server (e.g. "+ DEFAULT_PORT +") ");
      out.println("    -u, --user=<name>         Specify the username for authentication");
      out.println("    -p, --password=<name>     Specify the password for authentication"); 
      out.println("    -q, --quiet               Be somewhat more quiet");
      out.println("    -v, --verbose             Be noisy");
      out.println();
      out.println("for convenience, you can specify --host and --port but not with the --server=<url> which overrides host + port. ");
      out.flush();
   }

   private static void processArguments(final String[] args) throws Exception
   {
      for(int a = 0; a < args.length; a ++)
      {
         if (!logPassword(args, a))
            log.debug("args["+a+"]="+args[a]);
      }
      String sopts = "-:hH:u:p:c:D:s:a:q::v::o:r:";
      LongOpt[] lopts =
         {
            new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
            new LongOpt("help-commands", LongOpt.NO_ARGUMENT, null, 0x1000),
            new LongOpt("server", LongOpt.REQUIRED_ARGUMENT, null, 's'),
            new LongOpt("adapter", LongOpt.REQUIRED_ARGUMENT, null, 'a'),
            new LongOpt("quiet", LongOpt.NO_ARGUMENT, null, 'q'),
            new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v'),
            new LongOpt("user", LongOpt.REQUIRED_ARGUMENT, null, 'u'),
            new LongOpt("host", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
            new LongOpt("port", LongOpt.REQUIRED_ARGUMENT, null, 'r'),
            new LongOpt("password", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
         };

      Getopt getopt = new Getopt(PROGRAM_NAME, args, sopts, lopts);
      int code;

      PROCESS_ARGUMENTS:

        while ((code = getopt.getopt()) != -1)
        {
           switch (code)
           {
              case ':':
              case '?':
                 // for now both of these should exit with error status
                 System.exit(1);
                 break; // for completeness

                 // non-option arguments
              case 1:
                 {
                    // create the command
                    commandName = getopt.getOptarg();
                    log.debug("Command name: " + commandName);

                    // pass the remaining arguments (if any) to the command for processing
                    int i = getopt.getOptind();

                    if (args.length > i)
                    {
                       commandArgs = new String[args.length - i];
                       System.arraycopy(args, i, commandArgs, 0, args.length - i);
                    }
                    else
                    {
                       commandArgs = new String[0];
                    }
                    // Log the command options
                    log.debug("Command arguments: " + Strings.join(commandArgs, ","));

                    // We are done, execute the command
                    break PROCESS_ARGUMENTS;
                 }

                 // show command line help
              case 'h':
                 displayHelp();
                 System.exit(0);
                 break; // for completeness

                 // Show command help
              case 'H':
                 commandName = getopt.getOptarg();
                 commandHelp = true;
                 break PROCESS_ARGUMENTS;

                 // help-commands
              case 0x1000:
                 twiddle.displayCommandList();
                 System.exit(0);
                 break; // for completeness

              case 'c':
                 // Try value as a URL
                 String props = getopt.getOptarg();
                 try
                 {
                    cmdProps = new URL(props);
                 }
                 catch (MalformedURLException e)
                 {
                    log.debug("Failed to use cmd props as url", e);
                    File path = new File(props);
                    if( path.exists() == false )
                    {
                       String msg = "Failed to locate command props: " + props
                          + " as URL or file";
                       throw new IllegalArgumentException(msg);
                    }
                    cmdProps = path.toURL();
                 }
                 break;
                 // set a system property
              case 'D':
                 {
                    String arg = getopt.getOptarg();
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

                 // host name
              case 'o':
                 twiddle.setHostname(getopt.getOptarg());
                 break;

              // host port
              case 'r':
                 twiddle.setPort(getopt.getOptarg());
                 break;

                 // Set the JNDI server URL
              case 's':
                 twiddle.setServerURL(getopt.getOptarg());
                 break;

                 // adapter JNDI name is not supported anymore
              case 'a':
                 String arg = getopt.getOptarg();
                 log.info("adapter name is ignored " + arg);
                 break;
              case 'u':
                 twiddle.setUsername(getopt.getOptarg());
                 break;
              case 'p':
                 twiddle.setPassword(getopt.getOptarg());
                 break;

              // be noisy
              case 'v':
                 twiddle.setVerbose(true);
                 break;

              // Enable quiet operations
              case 'q':
                 twiddle.setQuiet(true);
                 break;
           }
        }
   }
   
   private static boolean logPassword(final String args[], int a)
   {
      // check current argument
      if (args[a].startsWith("-p") && args[a].length() > 2)
      {
         log.debug("args["+a+"]=-pxxxx");
         return true;
      }
      else if (args[a].indexOf('=') != -1)
      {
         String[] split = args[a].split("=");
         if ("--password".indexOf(split[0]) != -1)
         {
            log.debug("args["+a+"]="+split[0]+"=xxxx");
            return true;
         }
      }
      // check previous argument
      try {
         if (args[a-1].equals("-p") || (args[a-1].indexOf('=') == -1 && "--password".indexOf(args[a-1]) != -1))
         {
            log.debug("args["+a+"]=xxxx");
            return true;
         }
      }
      catch (IndexOutOfBoundsException ioobe)
      {
      }
      return false;
   }

}
