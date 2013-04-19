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
package org.jboss.ant;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.jboss.util.propertyeditor.PropertyEditors;

/**
 * JMX.java. An ant plugin to call managed operations and set attributes
 * on mbeans in a jboss jmx mbean server.
 * To use this plugin with Ant, place the jbossjmx-ant.jar together with the
 * jboss jars jboss-j2ee.jar and jboss-common-client.jar, and the sun jnet.jar in the
 * ant/lib directory you wish to use.
 * If the JMX invoker is secured, set the username and password attributes in the task.
 *
 * Here is an example from an ant build file.
 *
 * <target name="jmx">
 *   <taskdef name="jmx"
 *	classname="org.jboss.ant.JMX"/>
 *   <jmx adapterName="jmx:HP.home.home:rmi">
 *
 *     <propertyEditor type="java.math.BigDecimal" editor="org.jboss.util.propertyeditor.BigDecimalEditor"/>
 *     <propertyEditor type="java.util.Date" editor="org.jboss.util.propertyeditor.DateEditor"/>
 *
 *
 *      <!-- define classes -->
 *     <invoke target="fgm.sysadmin:service=DefineClasses"
 *             operation="defineClasses">
 *       <parameter type="java.lang.String" arg="defineclasses.xml"/>
 *     </invoke>
 *   </jmx>
 *
 * Another example using security and setting the verbose flag:
 *
 * <project name="jmxtest" default="jmx" basedir=".">
 *     <description>
 *         test jmx task
 *     </description>
 * <target name="jmx">
 *   <taskdef name="jmx" classname="org.jboss.ant.JMX"/>
 *   <jmx verbose="true" username="sysadmin"  password="password">
 *   <invoke target="jboss.system:type=ServerInfo"
 *     operation="listThreadDump">
 *   </invoke>
 *   </jmx>
 * </target>
 *
 * Created: Tue Jun 11 20:17:44 2002
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:dsnyder_lion@users.sourceforge.net">David Snyder</a>
 * @version
 */
public class JMX extends Task
{
   private String serverURL = "service:jmx:rmi:///jndi/rmi://localhost:1090/jmxrmi";


   private String username;

   private String password;

   private List<Operation> ops = new ArrayList<Operation>();

   private List<PropertyEditorHolder> editors = new ArrayList<PropertyEditorHolder>();

   private boolean verbose;

   /**
    * Creates a new <code>JMX</code> instance.
    * Provides a default adapterName for the current server, so you only need to set it to
    * talk to a remote server.
    *
    * @exception Exception if an error occurs
    */
   public JMX() throws Exception
   {
   }

   public boolean isVerbose() {
      return verbose;
   }

   public void setVerbose(boolean verbose) {
      this.verbose = verbose;
   }

   /**
    * Use the <code>setServerURL</code> method to set the URL of the server
    * you wish to connect to.
    *
    * @param serverURL a <code>String</code> value
    */
   public void setServerURL(String serverURL)
   {
      if(verbose)
      {
         log("changing server url from " + this.serverURL + " to " + serverURL);
      }
      this.serverURL = serverURL;
   }

   /**
    * Use the <code>setAdapterName</code> method to set the name the
    * adapter mbean is bound under in jndi.
    *
    * @param adapterName a <code>String</code> value
    * @deprecated and now ignored
    */
   public void setAdapterName(String adapterName)
   {
      log("ignoring adapter name (" + adapterName+") as its no longer used.");
   }

   /**
    * Use the <code>setUsername</code> method to set the username for 
    * the JMX invoker (if it's secured).
    * 
    * @param username a <code>String</code> value
    */
   public void setUsername(String username)
   {
      if(verbose)
      {
         log("will perform operation as user " + username);
      }

      this.username = username;
   }

   /**
    * Use the <code>setPassword</code> method to set the password for 
    * the JMX invoker (if it's secured).
    * 
    * @param password a <code>String</code> value
    */
   public void setPassword(String password)
   {
      if(verbose)
      {
         log("password has been set");
      }

      this.password = password;
   }

   /**
    * Use the <code>addInvoke</code> method to add an <invoke> operation.
    * Include as attributes the target ObjectName and operation name.
    * Include as sub-elements parameters: see addParameter in the Invoke class.
    *
    * @param invoke an <code>Invoke</code> value
    */
   public void addInvoke(Invoke invoke)
   {
      if(verbose)
      {
         String operation = null;
         String name = null;
         if(invoke != null && invoke.target != null)
         {
            name = invoke.target.getCanonicalName();
         }
         if(invoke != null && invoke.operation != null)
         {
            operation = invoke.operation;
         }
         log("invoke added for "+ operation+" on " + name);
      }

      ops.add(invoke);
   }

   /**
    * Use the  <code>addSetAttribute</code> method to add a set-attribute
    * operation. Include as attributes the target ObjectName and the
    * the attribute name.  Include the value as a nested value tag
    * following the parameter syntax.
    *
    * @param setter a <code>Setter</code> value
    */
   public void addSetAttribute(Setter setter)
   {
      if(verbose)
      {
         String name = null;
         if(setter != null && setter.target != null)
         {
            name = setter.target.getCanonicalName();
         }
         log("set attribute " + setter.attribute + " on " + name);
      }
      ops.add(setter);
   }

   /**
    * Use the  <code>addGetAttribute</code> method to add a get-attribute
    * operation. Include as attributes the target ObjectName, the
    * the attribute name, and a property name to hold the result of the
    * get-attribute operation.
    *
    * @param getter a <code>Getter</code> value
    */
   public void addGetAttribute(Getter getter)
   {
      if(verbose)
      {
         String name = null;
         if(getter != null && getter.target != null)
         {
            name = getter.target.getCanonicalName();
         }

         log("get attribute " + getter.attribute + " on " + name);
      }

      ops.add(getter);
   }

   /**
    * Use the <code>addPropertyEditor</code> method to make a PropertyEditor
    * available for values.  Include attributes for the type and editor fully
    * qualified class name.
    *
    * @param peh a <code>PropertyEditorHolder</code> value
    */
   public void addPropertyEditor(PropertyEditorHolder peh)
   {
      if(verbose)
      {
         log("use property editor "+peh.getEditor());
      }
      editors.add(peh);
   }

   /**
    * The <code>execute</code> method is called by ant to execute the task.
    *
    * @exception BuildException if an error occurs
    */
   public void execute() throws BuildException
   {
      if(verbose)
      {
         log("started execute");
      }

      final ClassLoader origCL = Thread.currentThread().getContextClassLoader();
      try
      {
         Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
         try
         {
            for (int i = 0; i < editors.size(); i++)
            {
               if(verbose)
               {
                  log("execute editor " + editors.get(i).getEditor());
               }

               editors.get(i).execute();
            } // end of for ()

         }
         catch (Exception e)
         {
            e.printStackTrace();
            throw new BuildException("Could not register property editors: " + e);
         } // end of try-catch

         try
         {
            HashMap env = new HashMap();
            if (username != null && password != null)
            {
               if (verbose )
               {
                  log("will connect with username=" + username);
               }
               String[] creds = new String[2];
               creds[0] = username;
               creds[1] = password;
               env.put(JMXConnector.CREDENTIALS, creds);
            }
            
            JMXServiceURL url = new JMXServiceURL(serverURL);

            if (verbose )
            {
               log("will connect with JMXServiceURL = "+url);
            }

            JMXConnector jmxc = JMXConnectorFactory.connect(url, env);
            MBeanServerConnection server = jmxc.getMBeanServerConnection();
            if(verbose)
            {
               log("connected to server");
            }

            for (int i = 0; i < ops.size(); i++)
            {
               Operation op = ops.get(i);
               if(verbose)
               {
                  log("execute operation " + op.getLogInformation());
               }
               Object result = op.execute(server, this);
               if( verbose && result != null)
               {
                  log(result.toString());
               }
            } // end of for ()

         }
         catch (Exception e)
         {
            e.printStackTrace();
            throw new BuildException("problem: " + e);
         } // end of try-catch
      }
      finally
      {
         Thread.currentThread().setContextClassLoader(origCL);
      }
      if(verbose)
      {
         log("stopped execute");
      }
   }

   /**
    * The interface <code>Operation</code> provides a common interface
    * for the sub-tasks..
    *
    */
   public static interface Operation
   {
      Object execute(MBeanServerConnection server, Task parent) throws Exception;
      String getLogInformation();
   }

   /**
    * The class <code>Invoke</code> specifies the invocation of a
    * managed operation.
    *
    */
   public static class Invoke implements Operation
   {
      private ObjectName target;

      private String property;

      private String operation;

      private List<Param> params = new ArrayList<Param>();

      /**
       * The <code>setProperty</code> method sets the name of the property
       * that will contain the result of the operation.
       *
       * @param property a <code>String</code> value
       */
      public void setProperty(String property)
      {
         this.property = property;
      }

      /**
       * The <code>setTarget</code> method sets the ObjectName
       * of the target mbean.
       *
       * @param target an <code>ObjectName</code> value
       */
      public void setTarget(ObjectName target)
      {
         this.target = target;
      }

      /**
       * The <code>setOperation</code> method specifies the operation to
       * be performed.
       *
       * @param operation a <code>String</code> value
       */
      public void setOperation(String operation)
      {
         this.operation = operation;
      }

      /**
       * The <code>addParameter</code> method adds a parameter for
       * the operation. You must specify type and value.
       *
       * @param param a <code>Param</code> value
       */
      public void addParameter(Param param)
      {
         params.add(param);
      }

      public String getLogInformation()
      {
         return "invoking " + operation;
      }

      public Object execute(MBeanServerConnection server, Task parent) throws Exception
      {
         int paramCount = params.size();
         Object[] args = new Object[paramCount];
         String[] types = new String[paramCount];
         int pos = 0;
         for (int i = 0; i < params.size(); i++)
         {
            Param p = params.get(i);
            args[pos] = p.getValue();
            types[pos] = p.getType();
            pos++;
         } // end of for ()
         Object result = server.invoke(target, operation, args, types);
         if ((property != null) && (result != null))
         {
            parent.getProject().setProperty(property, result.toString());
         }
         return result;
      }
   }

   /**
    * The class <code>Setter</code> specifies setting an attribute
    * value on an mbean.
    *
    */
   public static class Setter implements Operation
   {
      private ObjectName target;

      private String attribute;

      private Param value;

      /**
       * The <code>setTarget</code> method sets the ObjectName
       * of the target mbean.
       *
       * @param target an <code>ObjectName</code> value
       */
      public void setTarget(ObjectName target)
      {
         this.target = target;
      }

      /**
       * The <code>setAttribute</code> method specifies the attribute to be set.
       *
       * @param attribute a <code>String</code> value
       */
      public void setAttribute(String attribute)
      {
         this.attribute = attribute;
      }

      /**
       * The <code>setValue</code> method specifies the value to be used.
       * The type is used to convert the value to the correct type.
       *
       * @param value a <code>Param</code> value
       */
      public void setValue(Param value)
      {
         this.value = value;
      }

      public String getLogInformation()
      {
         return "setting " + target.getCanonicalName() + ":" + attribute + " to " + value.getArg();
      }

      public Object execute(MBeanServerConnection server, Task parent) throws Exception
      {
         Attribute att = new Attribute(attribute, value.getValue());
         server.setAttribute(target, att);
         return null;
      }
   }

   /**
    * The class <code>Getter</code> specifies getting an attribute
    * value of an mbean.
    *
    */
   public static class Getter implements Operation
   {
      private ObjectName target;

      private String attribute;

      private String property;

      /**
       * The <code>setTarget</code> method sets the ObjectName
       * of the target mbean.
       *
       * @param target an <code>ObjectName</code> value
       */
      public void setTarget(ObjectName target)
      {
         this.target = target;
      }

      /**
       * The <code>setAttribute</code> method specifies the attribute to be
       * retrieved.
       *
       * @param attribute a <code>String</code> value
       */
      public void setAttribute(String attribute)
      {
         this.attribute = attribute;
      }

      /**
       * The <code>setProperty</code> method specifies the name of the property
       * to be set with the attribute value.
       *
       * @param property a <code>String</code> value
       */
      public void setProperty(String property)
      {
         this.property = property;
      }

      public String getLogInformation()
      {
         return "getting " + target.getCanonicalName() + ":" + attribute;
      }

      public Object execute(MBeanServerConnection server, Task parent) throws Exception
      {
         Object result = server.getAttribute(target, attribute);
         if ((property != null) && (result != null))
         {
            parent.getProject().setProperty(property, result.toString());
         }
         return result;
      }
   }

   /**
    * The class <code>Param</code> is used to represent a object by
    * means of a string representation of its value and its type.
    *
    */
   public static class Param
   {
      private String arg;

      private String type;

      /**
       * The <code>setArg</code> method sets the string representation
       * of the parameters value.
       *
       * @param arg a <code>String</code> value
       */
      public void setArg(String arg)
      {
         this.arg = arg;
      }

      public String getArg()
      {
         return arg;
      }

      /**
       * The <code>setType</code> method sets the fully qualified class
       * name of the type represented by the param object.
       *
       * @param type a <code>String</code> value
       */
      public void setType(String type)
      {
         this.type = type;
      }

      public String getType()
      {
         return type;
      }

      /**
       * The <code>getValue</code> method uses PropertyEditors to convert
       * the string representation of the value to an object, which it returns.
       * The PropertyEditor to use is determined by the type specified.
       *
       * @return an <code>Object</code> value
       * @exception Exception if an error occurs
       */
      public Object getValue() throws Exception
      {
         PropertyEditor editor = PropertyEditors.getEditor(type);
         editor.setAsText(arg);
         return editor.getValue();
      }
   }

   /**
    * The class <code>PropertyEditorHolder</code> allows you to add a
    * PropertyEditor to the default set.
    *
    */
   public static class PropertyEditorHolder
   {
      private String type;

      private String editor;

      /**
       * The <code>setType</code> method specifies the return type from the
       * property editor.
       *
       * @param type a <code>String</code> value
       */
      public void setType(final String type)
      {
         this.type = type;
      }

      public String getType()
      {
         return type;
      }

      private Class<?> getTypeClass() throws ClassNotFoundException
      {
         //with a little luck, one of these will work with Ant's classloaders
         try
         {
            return Class.forName(type);
         }
         catch (ClassNotFoundException e)
         {
         } // end of try-catch
         try
         {
            return getClass().getClassLoader().loadClass(type);
         }
         catch (ClassNotFoundException e)
         {
         } // end of try-catch
         return Thread.currentThread().getContextClassLoader().loadClass(type);
      }

      /**
       * The <code>setEditor</code> method specifies the fully qualified
       * class name of the PropertyEditor for the type specified in the type field.
       *
       * @param editor a <code>String</code> value
       */
      public void setEditor(final String editor)
      {
         this.editor = editor;
      }

      public String getEditor()
      {
         return editor;
      }

      private Class<?> getEditorClass() throws ClassNotFoundException
      {
         //with a little luck, one of these will work with Ant's classloaders
         try
         {
            return Class.forName(editor);
         }
         catch (ClassNotFoundException e)
         {
         } // end of try-catch
         try
         {
            return getClass().getClassLoader().loadClass(editor);
         }
         catch (ClassNotFoundException e)
         {
         } // end of try-catch
         return Thread.currentThread().getContextClassLoader().loadClass(editor);
      }

      public void execute() throws ClassNotFoundException
      {
         PropertyEditorManager.registerEditor(getTypeClass(), getEditorClass());
      }
   }

}// JMX
