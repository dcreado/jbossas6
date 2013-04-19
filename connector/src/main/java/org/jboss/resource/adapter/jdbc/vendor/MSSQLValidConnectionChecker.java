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
package org.jboss.resource.adapter.jdbc.vendor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import org.jboss.logging.Logger;
import org.jboss.resource.adapter.jdbc.ValidConnectionChecker;

/**
 * Implements sql connection validation check.
 * This should work on just about any version of the database,
 * but will use isValid() method with MSSQL JDBC driver 2.0 or later.
 * Prior to that version it just executes "SELECT 1" query.
 *
 * <p>Please note that the isValid() method requires java 6.
 *
 * <p>The code was inspired by MySQLValidConnectionChecker. See it's javadoc for
 * authors info. This code is released under the LGPL license.
 *
 * @author <a href="weston.price@jboss.com">Weston Price</a>              
 * @author Dmitry L (dmitryl att artezio dott com)
 */
public class MSSQLValidConnectionChecker implements ValidConnectionChecker, Serializable
{
   private static transient Logger log;

   private static final String SQLSERVER_CONNECTION_CLASS = "com.microsoft.sqlserver.jdbc.SQLServerConnection";

   private static final String VALIDATION_SQL_QUERY = "SELECT 1";
   
   private transient boolean driverHasIsValidMethod;

   // The validation method
   private transient Method isValid;

   // The timeout for isValid method
   private static Object[] timeoutParam = new Object[]{1};

   private static final long serialVersionUID = 4609709370615627138L;

   public MSSQLValidConnectionChecker()
   {
      initIsValid();
   }

   public SQLException isValidConnection(Connection c)
   {
      // if there is a isValid method then use it
      if (driverHasIsValidMethod)
      {
         try
         {
            Boolean valid = (Boolean) isValid.invoke(c, timeoutParam);
            if (!valid)
               return new SQLException("Connection is unvalid or " + timeoutParam[0] + " sec timeout has expired");
         }
         catch (Exception e)
         {
            if (e instanceof SQLException)
            {
               return (SQLException) e;
            }
            else
            {
               log.warn("Unexpected error in isValid() method", e);
               return new SQLException("isValid() method failed: " + e.toString());
            }
         }
      }
      else
      {
         // otherwise just use a sql statement
         Statement stmt = null;
         ResultSet rs = null;
         try
         {
            stmt = c.createStatement();
            rs = stmt.executeQuery(VALIDATION_SQL_QUERY);
         }
         catch (Exception e)
         {
            if (e instanceof SQLException)
            {
               return (SQLException) e;
            } 
            else
            {
               log.warn("Unexpected error in '" + VALIDATION_SQL_QUERY + "'", e);
               return new SQLException("'" + VALIDATION_SQL_QUERY + "' failed: " + e.toString());
            }
         }
         finally
         {
            //cleanup the Statement
            try
            {
               if (rs != null)
                  rs.close();
            }
            catch (SQLException ignore)
            {
               // ignore
            }

            try
            {
               if (stmt != null)
                  stmt.close();
            }
            catch (SQLException ignore)
            {
               // ignore
            }
         }
      }
      return null;
   }

   private void initIsValid()
   {
      log = Logger.getLogger(this.getClass());
      driverHasIsValidMethod = false;

      Class connectionClazz;
      try
      {
         connectionClazz = Thread.currentThread().getContextClassLoader().loadClass(SQLSERVER_CONNECTION_CLASS);
         isValid = connectionClazz.getMethod("isValid", new Class[]{int.class});
         if (isValid != null && !(Modifier.isAbstract(isValid.getModifiers())))
         {
            driverHasIsValidMethod = true;
         }
      }
      catch (ClassNotFoundException e)
      {
         log.error("Cannot resolve " + SQLSERVER_CONNECTION_CLASS +
                   " class. Wrong ValidConnectionChecker is configured?" +
                   " Will use '" + VALIDATION_SQL_QUERY + "' instead.", e);
      } 
      catch (SecurityException e)
      {
         log.error("Cannot access " + SQLSERVER_CONNECTION_CLASS + ".isValid() " +
                   "Will use '" + VALIDATION_SQL_QUERY + "' instead.", e);
      }
      catch (NoSuchMethodException e)
      {
         if (log.isDebugEnabled()) {
            log.debug("Cannot resolve isValid() method for " + SQLSERVER_CONNECTION_CLASS +
                      " class (JDBC driver 2.0+). Will use '" + VALIDATION_SQL_QUERY +
                      "' instead (JDBC driver 1.2 and earlier).");
         }
      }
   }

   private void writeObject(ObjectOutputStream out) throws IOException
   {
   }

   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
   {
      initIsValid();
   }
}
