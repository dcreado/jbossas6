package org.jboss.weld.integration.deployer.cl;


import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.deployer.ext.JBossWeldMetaData;

/**
 * Web Beans  core integration deployer.
 *
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class WeldMcExtensionsIntegrationDeployer extends WeldUrlIntegrationDeployer<JBossWeldMetaData>
{
   public WeldMcExtensionsIntegrationDeployer()
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
      return "jboss-weld-int.jar";
   }
}