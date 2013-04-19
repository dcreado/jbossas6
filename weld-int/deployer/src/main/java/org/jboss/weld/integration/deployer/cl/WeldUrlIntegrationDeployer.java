package org.jboss.weld.integration.deployer.cl;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

import org.jboss.deployers.vfs.plugins.classloader.PathUrlIntegrationDeployer;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.util.StringPropertyReplacer;
import org.jboss.weld.integration.deployer.DeployersUtils;

/**
 * Web Beans RI integration deployer.
 *
 * @param <T> exact input type
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class WeldUrlIntegrationDeployer<T> extends PathUrlIntegrationDeployer<T>
{
   protected WeldUrlIntegrationDeployer(Class<T> input)
   {
      super(input);
      setInputs(DeployersUtils.WELD_FILES);
      setIntegrationURLs(getURLs());
   }

   protected boolean isIntegrationDeployment(VFSDeploymentUnit unit)
   {
      return isIntegrationDeploymentInternal(unit);
   }

   /**
    * Check if this is integration deployment.
    *
    * @param unit the current deployment unit
    * @return true if integration, false otherwise
    */
   protected abstract boolean isIntegrationDeploymentInternal(VFSDeploymentUnit unit);

   /**
    * Get the short name.
    *
    * @return the short name
    */
   protected abstract String getShortLibName();

   /**
    * Get the Weld core integration urls.
    *
    * @return the weld jbossas integration urls
    */
   protected Set<URL> getURLs()
   {
      try
      {
         String libOpt = getServerHome() + getOptionalLib();
         libOpt = StringPropertyReplacer.replaceProperties(libOpt);
         return Collections.singleton(new URL(libOpt + getShortLibName()));
      }
      catch (MalformedURLException e)
      {
         throw new IllegalArgumentException("Unexpected error: " + e);
      }
   }

   /**
    * Get server home.
    *
    * @return the jboss server home location
    */
   protected String getServerHome()
   {
      return "${jboss.server.home.url}";
   }

   /**
    * Get the optinal lib path.
    *
    * @return the integration path
    */
   protected String getOptionalLib()
   {
      return "deployers/weld.deployer/lib-int/";
   }
}