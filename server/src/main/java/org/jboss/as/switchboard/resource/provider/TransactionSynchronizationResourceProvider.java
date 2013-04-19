/**
 * 
 */
package org.jboss.as.switchboard.resource.provider;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.switchboard.impl.resource.LinkRefResource;
import org.jboss.switchboard.javaee.environment.TransactionSynchronizationRegistryRefType;
import org.jboss.switchboard.mc.spi.MCBasedResourceProvider;
import org.jboss.switchboard.spi.Resource;

/**
 * TransactionSynchronizationRefResourceProvider
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class TransactionSynchronizationResourceProvider implements MCBasedResourceProvider<TransactionSynchronizationRegistryRefType>
{

   public Class<TransactionSynchronizationRegistryRefType> getEnvironmentEntryType()
   {
      return TransactionSynchronizationRegistryRefType.class;
   }

   public Resource provide(DeploymentUnit context, TransactionSynchronizationRegistryRefType type)
   {
      return new LinkRefResource("java:TransactionSynchronizationRegistry");
   }

}
