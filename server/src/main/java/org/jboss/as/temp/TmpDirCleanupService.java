/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.temp;

import java.io.File;

import org.jboss.bootstrap.api.config.ServerConfig;
import org.jboss.bootstrap.api.lifecycle.LifecycleEventException;
import org.jboss.bootstrap.api.lifecycle.LifecycleEventHandler;
import org.jboss.bootstrap.api.lifecycle.LifecycleState;
import org.jboss.bootstrap.api.server.Server;

/**
 * Cleanup tmp dir at shutdown.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TmpDirCleanupService implements LifecycleEventHandler
{
   private Server server;
   private File tmp;

   public TmpDirCleanupService(Server server)
   {
      if (server == null)
         throw new IllegalArgumentException("Null server");
      this.server = server;
   }

   public void start()
   {
      server.registerEventHandler(this, LifecycleState.STOPPED, LifecycleState.IDLE);

      ServerConfig config = server.getConfiguration();
      String property = config.getProperty("jboss.server.temp.dir");
      if (property != null)
         tmp = new File(property);
   }

   public void handleEvent(LifecycleState state) throws LifecycleEventException
   {
      if (state == LifecycleState.STOPPED)
      {
         if (tmp != null && tmp.exists())
         {
            File[] files = tmp.listFiles();
            if (files != null)
            {
               for (File f : files)
                  delete(f);
            }
         }
      }
      else if (state == LifecycleState.IDLE) // should be after stopped, as we're past starting when start() is invoked
      {
         // should be ok, as collections used are concurrent
         server.unregisterEventHandler(this, LifecycleState.STOPPED);
         server.unregisterEventHandler(this, LifecycleState.IDLE);
         server = null;
      }
   }

   private static boolean delete(final File dir)
   {
      boolean success = true;

      File files[] = dir.listFiles();
      if (files != null)
      {
         for (File f : files)
         {
            if (f.isDirectory() == true)
            {
               // delete the directory and all of its contents.
               if (delete(f) == false)
               {
                  success = false;
               }
            }
            // delete each file in the directory
            else if (f.delete() == false)
            {
               success = false;
            }
         }
      }

      // finally delete the directory
      if (dir.delete() == false)
      {
         success = false;
      }

      return success;
   }
}
