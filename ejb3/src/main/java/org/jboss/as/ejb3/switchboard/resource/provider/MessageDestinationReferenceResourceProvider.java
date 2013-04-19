/**
 * 
 */
package org.jboss.as.ejb3.switchboard.resource.provider;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.resolvers.MessageDestinationReferenceResolver;
import org.jboss.logging.Logger;
import org.jboss.switchboard.impl.resource.LinkRefResource;
import org.jboss.switchboard.javaee.jboss.environment.JBossMessageDestinationRefType;
import org.jboss.switchboard.mc.spi.MCBasedResourceProvider;
import org.jboss.switchboard.spi.Resource;

/**
 * A {@link MCBasedResourceProvider} for processing message-destination-ref
 * references
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class MessageDestinationReferenceResourceProvider implements MCBasedResourceProvider<JBossMessageDestinationRefType>
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(MessageDestinationReferenceResourceProvider.class);
   
   /**
    * A {@link MessageDestinationReferenceResolver} will be used to resolve a jndi-name out of 
    * the message-destination link, used in message-destination-ref 
    */
   private MessageDestinationReferenceResolver messageDestinationResolver;
   
   /**
    * {@inheritDoc}
    */
   public Class<JBossMessageDestinationRefType> getEnvironmentEntryType()
   {
      return JBossMessageDestinationRefType.class;
   }

   /**
    * Processes a message-destination-ref and returns a {@link Resource} corresponding to that
    * reference.
    * If a {@link Resource} cannot be created out of the message-destination-ref, then this
    * method throws a {@link RuntimeException}
    */
   public Resource provide(DeploymentUnit unit, JBossMessageDestinationRefType messageDestRef)
   {
      // first check lookup name
      String lookupName = messageDestRef.getLookupName();
      if (lookupName != null && !lookupName.trim().isEmpty())
      {
         return new LinkRefResource(lookupName);
      }

      // now check mapped name
      String mappedName = messageDestRef.getMappedName();
      if (mappedName != null && !mappedName.trim().isEmpty())
      {
         return new LinkRefResource(mappedName);
      }
      
      // now check (JBoss specific) jndi name!
      String jndiName = messageDestRef.getJNDIName();
      if (jndiName != null && !jndiName.trim().isEmpty())
      {
         return new LinkRefResource(jndiName);
      }
      
      // Now check if a message-destination link is specified.
      // The "link" itself is a logical name to the destination, so we'll
      // use a resolver to resolve a jndi name out of it.
      String messageDestLink = messageDestRef.getMessageDestinationLink();
      if (messageDestLink != null && !messageDestLink.trim().isEmpty())
      {
         if (this.messageDestinationResolver == null)
         {
            logger.warn("Cannot resolve message-destination link: " + messageDestLink
                  + " for message-destination-ref: " + messageDestRef.getName() + " due to absence of a "
                  + MessageDestinationReferenceResolver.class.getName());
         }
         else
         {
            // the DU which depends on this message-destination-ref 
            DeploymentUnit dependentDU = unit;
            // the MessageDestinationReferenceResolver works on non-component deployment units.
            // So if we are currently processing component DUs (like we do for EJBs), then pass the
            // component DUs parent during resolution.
            if (unit.isComponent())
            {
               dependentDU = unit.getParent();
            }
            String resolvedJNDIName = this.messageDestinationResolver.resolveMessageDestinationJndiName(dependentDU, messageDestLink);
            logger.debug("Resolved jndi-name: " + resolvedJNDIName + " for message-destination link: "
                  + messageDestLink + " in message-destination-ref: " + messageDestRef.getName());
            if (resolvedJNDIName != null && !resolvedJNDIName.trim().isEmpty())
            {
               return new LinkRefResource(resolvedJNDIName);
            }
            
         }
      }
      throw new RuntimeException("Cannot provide a resource for message-destination-ref: " + messageDestRef.getName() + " in unit: " + unit);
   }

   /**
    * Sets the {@link MessageDestinationReferenceResolver}, which will be used to resolve the jndi-name
    * out of a message-destination link, used in the message-destination-ref
    * @param resolver
    */
   public void setMessageDestinationLinkResolver(MessageDestinationReferenceResolver resolver)
   {
      this.messageDestinationResolver = resolver;
   }
}
