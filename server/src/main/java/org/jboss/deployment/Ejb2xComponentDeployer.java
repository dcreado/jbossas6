/**
 * 
 */
package org.jboss.deployment;

import java.util.ArrayList;
import java.util.List;

import org.jboss.deployers.spi.deployer.helpers.AbstractComponentDeployer;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeploymentVisitor;
import org.jboss.deployers.spi.deployer.helpers.DeploymentVisitor;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;

/**
 * Creates and deploys EJB2x beans as components
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class Ejb2xComponentDeployer extends AbstractComponentDeployer<JBossMetaData, JBossEnterpriseBeanMetaData>
{
   private static final Logger log = Logger.getLogger(Ejb2xComponentDeployer.class);

   private static final JBossDeploymentVisitor deploymentVisitor = new JBossDeploymentVisitor();

   private static final String attachmentName = JBossMetaData.class.getName();
   
   public Ejb2xComponentDeployer()
   {
      setInput(JBossMetaData.class);
      setOutput(deploymentVisitor.getComponentType());

      setDeploymentVisitor(deploymentVisitor);
   }

   @Override
   protected <U> void deploy(DeploymentUnit unit, DeploymentVisitor<U> visitor) throws DeploymentException
   {
      U deployment = unit.getAttachment(attachmentName, visitor.getVisitorType());
      try
      {
         visitor.deploy(unit, deployment);
      }
      catch (Throwable t)
      {
         throw DeploymentException.rethrowAsDeploymentException("Error deploying: " + unit.getName(), t);
      }
   }

   @Override
   protected <U> void undeploy(DeploymentUnit unit, DeploymentVisitor<U> visitor)
   {
      if (visitor == null)
         return;

      U deployment = unit.getAttachment(attachmentName, visitor.getVisitorType());
      visitor.undeploy(unit, deployment);
   }

   private static class JBossDeploymentVisitor
         extends
            AbstractDeploymentVisitor<JBossEnterpriseBeanMetaData, JBossMetaData>
   {
      @Override
      public Class<JBossEnterpriseBeanMetaData> getComponentType()
      {
         return JBossEnterpriseBeanMetaData.class;
      }

      @Override
      protected List<? extends JBossEnterpriseBeanMetaData> getComponents(JBossMetaData deployment)
      {
         // Process only 2.x beans
         if (deployment == null || !deployment.isEJB2x())
            return null;
         JBossEnterpriseBeansMetaData enterpriseBeans = deployment.getEnterpriseBeans();
         if (enterpriseBeans == null)
         {
            return null;
         }
         return new ArrayList<JBossEnterpriseBeanMetaData>(enterpriseBeans);
      }

      @Override
      protected String getComponentName(JBossEnterpriseBeanMetaData attachment)
      {
         return JBossEnterpriseBeanMetaData.class.getName() + "." + attachment.getEjbName();
      }

      public Class<JBossMetaData> getVisitorType()
      {
         return JBossMetaData.class;
      }
   }

}
