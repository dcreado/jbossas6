package org.jboss.resteasy.integration.deployers;

import javax.ws.rs.core.Application;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResteasyDeploymentScanningData
{
   private boolean scanAll;
   private boolean scanResources;
   private boolean scanProviders;
   private boolean createDispatcher;
   private Set<String> resources = new LinkedHashSet<String>();
   private Set<String> providers = new LinkedHashSet<String>();
   private Class<? extends Application> applicationClass;
   private boolean bootClasses;
   private boolean unwrappedExceptionsParameterSet;

   public Class<? extends Application> getApplicationClass()
   {
      return applicationClass;
   }

   public void setApplicationClass(Class<? extends Application> applicationClass)
   {
      this.applicationClass = applicationClass;
   }

   public boolean hasBootClasses()
   {
      return bootClasses;
   }

   public void setBootClasses(boolean bootClasses)
   {
      this.bootClasses = bootClasses;
   }

   public boolean shouldScan()
   {
      return scanAll || scanResources || scanProviders;
   }

   public boolean isScanAll()
   {
      return scanAll;
   }

   public void setScanAll(boolean scanAll)
   {
      if (scanAll)
      {
         scanResources = true;
         scanProviders = true;
      }
      this.scanAll = scanAll;
   }

   public boolean isScanResources()
   {
      return scanResources;
   }

   public void setScanResources(boolean scanResources)
   {
      this.scanResources = scanResources;
   }

   public boolean isScanProviders()
   {
      return scanProviders;
   }

   public void setScanProviders(boolean scanProviders)
   {
      this.scanProviders = scanProviders;
   }

   public Set<String> getResources()
   {
      return resources;
   }

   public void setResources(Set<String> resources)
   {
      this.resources = resources;
   }

   public Set<String> getProviders()
   {
      return providers;
   }

   public void setProviders(Set<String> providers)
   {
      this.providers = providers;
   }

   /**
    * A component layer wants a dispatcher created
    */
   public void createDispatcher()
   {
      this.createDispatcher = true;
   }

   public boolean shouldCreateDispatcher()
   {
      return createDispatcher || !resources.isEmpty() || !providers.isEmpty();
   }

   public boolean isUnwrappedExceptionsParameterSet()
   {
      return unwrappedExceptionsParameterSet;
   }

   public void setUnwrappedExceptionsParameterSet(boolean unwrappedExceptionsParameterSet)
   {
      this.unwrappedExceptionsParameterSet = unwrappedExceptionsParameterSet;
   }
}
