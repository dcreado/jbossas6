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
package org.jboss.console.twiddle.command;

import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import javax.management.ObjectName;
import javax.management.MBeanServerConnection;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.MBeanAttributeInfo;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import org.jboss.util.Strings;

/**
 * Get the values of one or more MBean attributes.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author Scott.Stark@jboss.org
 * @version $Revision: 105008 $
 */
public class GetCommand
   extends MBeanServerCommand
{
   private ObjectName objectName;

   private List attributeNames = new ArrayList(5);

   private boolean prefix = true;

   public GetCommand()
   {
      super("get", "Get the values of one or more MBean attributes");
   }

   public void displayHelp()
   {
      PrintWriter out = context.getWriter();

      out.println(desc);
      out.println();
      out.println("usage: " + name + " [options] <name> [<attr>+]");
      out.println("  If no attribute names are given all readable attributes are retrieved");
      out.println("options:");
      out.println("    --noprefix    Do not display attribute name prefixes");
      out.println("    --            Stop processing options");

      out.flush();
   }

   private boolean processArguments(final String[] args)
      throws CommandException
   {
      log.debug("processing arguments: " + Strings.join(args, ","));

      if (args.length == 0)
      {
         throw new CommandException("Command requires arguments");
      }

      String sopts = "-:";
      LongOpt[] lopts =
         {
            new LongOpt("noprefix", LongOpt.NO_ARGUMENT, null, 0x1000),
         };

      Getopt getopt = new Getopt(null, args, sopts, lopts);
      getopt.setOpterr(false);

      int code;
      int argidx = 0;

      while ((code = getopt.getopt()) != -1)
      {
         switch (code)
         {
            case ':':
               throw new CommandException
                  ("Option requires an argument: " + args[getopt.getOptind() - 1]);

            case '?':
               throw new CommandException
                  ("Invalid (or ambiguous) option: " + args[getopt.getOptind() - 1]);

            case 0x1000:
               prefix = false;
               break;
                  
               // non-option arguments
            case 1:
               {
                  String arg = getopt.getOptarg();

                  switch (argidx++)
                  {
                     case 0:
                        objectName = createObjectName(arg);
                        log.debug("mbean name: " + objectName);
                        break;

                     default:
                        log.debug("adding attribute name: " + arg);
                        attributeNames.add(arg);
                        break;
                  }
                  break;
               }
         }
      }

      return true;
   }

   public void execute(String[] args) throws Exception
   {
      processArguments(args);

      if (objectName == null)
         throw new CommandException("Missing object name");

      log.debug("attribute names: " + attributeNames);

      MBeanServerConnection server = getMBeanServer();
      if (attributeNames.size() == 0)
      {
         // Display all readable attributes
         attributeNames.clear();
         MBeanInfo info = server.getMBeanInfo(objectName);
         MBeanAttributeInfo[] attrInfos = info.getAttributes();
         for (int a = 0; a < attrInfos.length; a++)
         {
            MBeanAttributeInfo attrInfo = attrInfos[a];
            if (attrInfo.isReadable())
               attributeNames.add(attrInfo.getName());
         }
      }

      String[] names = new String[attributeNames.size()];
      attributeNames.toArray(names);
      log.debug("as string[]: " + Strings.join(names, ","));

      AttributeList attrList = server.getAttributes(objectName, names);
      log.debug("attribute list: " + attrList);

      if (attrList.size() == 0)
      {
         throw new CommandException("No matching attributes");
      }
      else if (attrList.size() != names.length)
      {
         log.warn("Not all specified attributes were found");
      }

      PrintWriter out = context.getWriter();

      Iterator iter = attrList.iterator();
      while (iter.hasNext())
      {
         Attribute attr = (Attribute) iter.next();
         if (prefix)
         {
            out.print(attr.getName());
            out.print("=");
         }
         out.println(attr.getValue());
      }
      closeServer();
   }
}
