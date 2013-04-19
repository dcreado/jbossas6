package org.jboss.weld.integration.deployer.jndi;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.integration.deployer.env.helpers.BootstrapBean;

/**
 * Returns the bean deployment archive that corresponds to a given deployment unit
 *
 * @author Marius Bogoevici
 */
public interface BeanDeploymentArchiveLocator
{
   BeanDeploymentArchive getBeanDeploymentArchive(BootstrapBean bootstrap, DeploymentUnit deploymentUnit);
}
