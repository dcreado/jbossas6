/**
 * 
 */
package org.jboss.as.switchboard.resource.provider;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.switchboard.impl.resource.LinkRefResource;
import org.jboss.switchboard.javaee.environment.ORBRefType;
import org.jboss.switchboard.mc.spi.MCBasedResourceProvider;
import org.jboss.switchboard.spi.Resource;

/**
 * Provides {@link Resource} for java:comp/ORB
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class ORBRefResourceProvider implements MCBasedResourceProvider<ORBRefType>
{

   private String corbaJndiName;
   
   public ORBRefResourceProvider(String corbaJndiName)
   {
      if (corbaJndiName == null || corbaJndiName.trim().isEmpty())
      {
         throw new IllegalArgumentException("Corba JNDI name cannot be null or empty");
      }
      this.corbaJndiName = corbaJndiName;
   }
   
   public Class<ORBRefType> getEnvironmentEntryType()
   {
      return ORBRefType.class;
   }

   /**
    * Returns a {@link Resource resource} for java:comp/ORB
    * 
    */
   public Resource provide(DeploymentUnit context, ORBRefType type)
   {
      // As per JavaEE 6 spec, section EE.9.6, java:comp/ORB is optional.
      // So let's create a LinkRefResource with ignoreDependency = true, so
      // that the deployment doesn't fail in the absence of ORB
      return new LinkRefResource(this.corbaJndiName, null, true);
   }

}
