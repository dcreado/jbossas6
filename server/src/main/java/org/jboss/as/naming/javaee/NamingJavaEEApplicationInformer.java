package org.jboss.as.naming.javaee;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.spec.Ear6xMetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEApplicationInformer;
import org.jboss.system.metadata.ServiceDeployment;

import static org.jboss.as.naming.javaee.NamingJavaEEUtil.stripSuffix;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class NamingJavaEEApplicationInformer implements JavaEEApplicationInformer
{
   private static final String REQUIRED_ATTACHMENTS[] = { JBossAppMetaData.class.getName(), ServiceDeployment.class.getName() };

   public String getApplicationName(DeploymentUnit deploymentUnit) throws IllegalArgumentException
   {
      if(!isJavaEEApplication(deploymentUnit))
         return null;

      // JBAS-8528
      String explicitAppName = this.getExplicitApplicationName(deploymentUnit);
      if (explicitAppName != null)
      {
         return explicitAppName;
      }
      
      String name = deploymentUnit.getSimpleName();
      
      // prevent StringIndexOutOfBoundsException and only cut when there is a typical extension
      return stripSuffix(name);
   }

   public boolean isEnterpriseApplicationArchive(DeploymentUnit deploymentUnit)
   {
      return deploymentUnit.isAttachmentPresent(JBossAppMetaData.class) || isTopLevelServiceArchive(deploymentUnit);
   }

   public boolean isJavaEEApplication(DeploymentUnit deploymentUnit)
   {
      // JavaEE 6.0 FR 5.2.2
      // practically everything deployed standalone is considered a JavaEEApplication in terms of naming
      return deploymentUnit.isTopLevel();
   }

   /**
    * In a deployment consisting of x.sar/ejbs.jar, the x.sar substitutes an EAR.
    */
   protected boolean isTopLevelServiceArchive(DeploymentUnit deploymentUnit)
   {
      // Added additional check for file extension to prevent a .jar with jboss-service.xml 
      // from being considered as an enterprise archive (https://issues.jboss.org/browse/JBAS-8770)
      return deploymentUnit.isTopLevel() && deploymentUnit.isAttachmentPresent(ServiceDeployment.class)
            && deploymentUnit.getSimpleName().endsWith(".sar");  
   }

   public String[] getRequiredAttachments()
   {
      return REQUIRED_ATTACHMENTS;
   }
   
   /**
    * Returns the application-name specified in the application.xml of a deployment.
    * If no application-name is specified in the application.xml, then this method returns null.
    * 
    * @param deploymentUnit The deployment unit 
    * @return
    */
   private String getExplicitApplicationName(DeploymentUnit deploymentUnit)
   {
      EarMetaData earMetaData = deploymentUnit.getAttachment(EarMetaData.class);
      if (earMetaData != null && earMetaData.isEE6() && earMetaData instanceof Ear6xMetaData)
      {
         Ear6xMetaData ear6x = (Ear6xMetaData) earMetaData;
         String explicitAppName = ear6x.getApplicationName();
         if (explicitAppName != null && !explicitAppName.trim().isEmpty())
         {
            return explicitAppName;
         }
      }
      return null;
   }
}
