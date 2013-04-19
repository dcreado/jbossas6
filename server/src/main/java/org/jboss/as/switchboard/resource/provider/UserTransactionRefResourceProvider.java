/**
 * 
 */
package org.jboss.as.switchboard.resource.provider;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.switchboard.impl.resource.LinkRefResource;
import org.jboss.switchboard.javaee.environment.UserTransactionRefType;
import org.jboss.switchboard.mc.spi.MCBasedResourceProvider;
import org.jboss.switchboard.spi.Resource;

/**
 * {@link MCBasedResourceProvider} for java:comp/UserTransaction
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class UserTransactionRefResourceProvider implements MCBasedResourceProvider<UserTransactionRefType>
{

   public Class<UserTransactionRefType> getEnvironmentEntryType()
   {
      return UserTransactionRefType.class;
   }

   public Resource provide(DeploymentUnit context, UserTransactionRefType userTransactionRef)
   {
      return new LinkRefResource("UserTransaction");
   }

}
