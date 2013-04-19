package org.jboss.weld.integration.deployer.jndi;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.integration.deployer.env.helpers.BootstrapBean;
import org.jboss.weld.integration.util.IdFactory;

/**
 * {@link BeanDeploymentArchiveLocator} implementation for non-flat (hierarchical) archives
 *
 * @author Marius Bogoevici
 * @author Ales Justin
 */
public class JBossBeanDeploymentArchiveLocator implements BeanDeploymentArchiveLocator
{
   public BeanDeploymentArchive getBeanDeploymentArchive(BootstrapBean bootstrap, DeploymentUnit deploymentUnit)
   {
      for (BeanDeploymentArchive beanDeploymentArchive: bootstrap.getDeployment().getBeanDeploymentArchives())
      {
         String id = IdFactory.getIdFromClassLoader(deploymentUnit.getClassLoader());
         if (beanDeploymentArchive.getId().equals(id))
         {
            return beanDeploymentArchive;
         }
      }
      return null;
   }
}
