package org.jboss.weld.integration.deployer.jndi;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.integration.deployer.env.helpers.BootstrapBean;

/**
 * {@link BeanDeploymentArchiveLocator} implementation for flat archives
 *
 * @author Marius Bogoevici
 */
public class FlatBeanDeploymentArchiveLocator implements BeanDeploymentArchiveLocator
{
   public BeanDeploymentArchive getBeanDeploymentArchive(BootstrapBean bootstrap, DeploymentUnit deploymentUnit)
   {
      for (BeanDeploymentArchive beanDeploymentArchive: bootstrap.getDeployment().getBeanDeploymentArchives())
      {
         if (beanDeploymentArchive.getId().equals("flat"))
         {
            return beanDeploymentArchive;
         }
      }
      return null;
   }
}
