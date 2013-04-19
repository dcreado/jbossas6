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
package org.jboss.kernel;

import org.jboss.dependency.plugins.tracker.NoopContextTracker;
import org.jboss.dependency.spi.tracker.ContextTracker;
import org.jboss.kernel.spi.metadata.KernelMetaDataRepository;
import org.jboss.logging.Logger;
import org.jboss.metadata.plugins.loader.memory.MemoryMetaDataLoader;
import org.jboss.metadata.spi.MutableMetaData;
import org.jboss.metadata.spi.repository.MutableMetaDataRepository;
import org.jboss.metadata.spi.retrieval.MetaDataRetrieval;
import org.jboss.metadata.spi.scope.ScopeKey;

/**
 * Kernel installer.
 * 
 * It installs default/dummy ContextTracker at ScopeKey.DEFAULT_SCOPE.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class KernelInstaller
{
   private Logger log = Logger.getLogger(KernelInstaller.class);
   private Kernel kernel;
   private ContextTracker tracker;

   public KernelInstaller(Kernel kernel)
   {
	   if (kernel == null)
	      throw new IllegalArgumentException("Null Kernel");
	   this.kernel = kernel;
   }

   public void create()
   {
      if (tracker == null)
         tracker = new NoopContextTracker();
   }

   public void start()
   {
      KernelMetaDataRepository kmdr = kernel.getMetaDataRepository();
      MutableMetaDataRepository repository = kmdr.getMetaDataRepository();
      MetaDataRetrieval retrieval = repository.getMetaDataRetrieval(ScopeKey.DEFAULT_SCOPE);
      if (retrieval == null)
      {
         retrieval = new MemoryMetaDataLoader(ScopeKey.DEFAULT_SCOPE);
         repository.addMetaDataRetrieval(retrieval);
      }
      if (retrieval instanceof MutableMetaData)
      {
         MutableMetaData mmd = (MutableMetaData)retrieval;
         mmd.addMetaData(tracker, ContextTracker.class);
      }
      else
      {
         log.info("Cannot add/modify default scoped metadata: " + retrieval);
      }
   }

   public void stop()
   {
      KernelMetaDataRepository kmdr = kernel.getMetaDataRepository();
      MutableMetaDataRepository repository = kmdr.getMetaDataRepository();
      MetaDataRetrieval retrieval = repository.getMetaDataRetrieval(ScopeKey.DEFAULT_SCOPE);
      if (retrieval != null && retrieval instanceof MutableMetaData)
      {
         MutableMetaData mmd = (MutableMetaData)retrieval;
         mmd.removeMetaData(ContextTracker.class);

         if (retrieval.isEmpty())
            repository.removeMetaDataRetrieval(retrieval.getScope());
      }
      else
      {
         log.debug("Ignoring remove/modify default scoped metadata: " + retrieval);
      }
   }

   /**
    * Set context tracker.
    * By default we use dummy/noop.
    *
    * @param tracker the context tracker
    */
   public void setTracker(ContextTracker tracker)
   {
      this.tracker = tracker;
   }
}
