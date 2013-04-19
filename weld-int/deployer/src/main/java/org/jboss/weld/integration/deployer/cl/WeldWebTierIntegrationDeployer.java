package org.jboss.weld.integration.deployer.cl;


import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.weld.integration.deployer.DeployersUtils;

/**
 * Web Beans RI integration deployer.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class WeldWebTierIntegrationDeployer extends WeldUrlIntegrationDeployer<JBossWebMetaData>
{
   public WeldWebTierIntegrationDeployer()
   {
      super(JBossWebMetaData.class); // we only look at wars
      setDisableOptional(true); // it needs to be web deployment, or why would you use JSF?
   }

   protected boolean isIntegrationDeploymentInternal(VFSDeploymentUnit unit)
   {
      // assume that it is an integration deployment
      return DeployersUtils.checkForWeldFiles(unit, false);
   }

   protected String getShortLibName()
   {
      return "weld-int-webtier.jar";
   }
}