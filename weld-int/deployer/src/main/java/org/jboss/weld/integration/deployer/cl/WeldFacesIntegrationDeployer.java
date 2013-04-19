package org.jboss.weld.integration.deployer.cl;


import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.weld.integration.deployer.DeployersUtils;

/**
 * Weld integration deployer.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 * @author Pete Muir
 */
public class WeldFacesIntegrationDeployer extends WeldUrlIntegrationDeployer<JBossWebMetaData>
{
   public WeldFacesIntegrationDeployer()
   {                                                          
      super(JBossWebMetaData.class);
      // We do this at top level to ensure that any deployment (ear or war)
      // that supports WB gets this integration (even if that particular war doesn't have beans.xml)
      setDisableOptional(true);
   }

   protected boolean isIntegrationDeploymentInternal(VFSDeploymentUnit unit)
   {
      return DeployersUtils.checkForWeldFiles(unit, false);
   }

   @Override
   protected String getShortLibName()
   {
      return "faces";
   }
}