package org.jboss.resteasy.integration.deployers;

import org.jboss.resteasy.core.ThreadLocalResteasyProviderFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResteasyProviderFactoryInitializer
{
   private boolean useThreadLocalFactory = true;

   public boolean isUseThreadLocalFactory()
   {
      return useThreadLocalFactory;
   }

   public void setUseThreadLocalFactory(boolean useThreadLocalFactory)
   {
      this.useThreadLocalFactory = useThreadLocalFactory;
   }

   public void start()
   {
      ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
      if (useThreadLocalFactory)
      {
         ThreadLocalResteasyProviderFactory factory = new ThreadLocalResteasyProviderFactory(instance);
         ResteasyProviderFactory.setInstance(factory);
      }
   }

   public void stop()
   {
      ResteasyProviderFactory.setInstance(null);
   }
}
