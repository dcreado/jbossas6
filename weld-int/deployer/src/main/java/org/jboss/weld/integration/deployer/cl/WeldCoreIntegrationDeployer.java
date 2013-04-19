package org.jboss.weld.integration.deployer.cl;


import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.deployer.ext.JBossWeldMetaData;

/**
 * Web Beans RI core integration deployer.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class WeldCoreIntegrationDeployer extends WeldUrlIntegrationDeployer<JBossWeldMetaData>
{
   public WeldCoreIntegrationDeployer()
   {
      super(JBossWeldMetaData.class);
      setTopLevelOnly(true); // only top level, as that's where Bootstrap bean is gonna be
   }

   protected boolean isIntegrationDeploymentInternal(VFSDeploymentUnit unit)
   {
      return DeployersUtils.checkForWeldFilesAcrossDeployment(unit);
   }

   @Override
   protected String getShortLibName()
   {
      return "weld-core-jsf-only.jar";
   }
}