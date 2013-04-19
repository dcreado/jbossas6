/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.resource.deployers;

import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEComponentInformer;
import org.jboss.switchboard.impl.resource.LinkRefResource;
import org.jboss.switchboard.javaee.jboss.environment.JBossResourceRefType;
import org.jboss.switchboard.mc.spi.MCBasedResourceProvider;
import org.jboss.switchboard.spi.Resource;

/**
 * {@link MCBasedResourceProvider Resource provider} for res-ref of type javax.sql.DataSource
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class DataSourceResRefResourceProvider implements MCBasedResourceProvider<JBossResourceRefType>
{

   
   private static Logger logger = Logger.getLogger(DataSourceResRefResourceProvider.class);
   
   /**
    * JavaEE component informer
    */
   private JavaEEComponentInformer informer;

   @Inject
   public void setJavaEEComponentInformer(JavaEEComponentInformer informer)
   {
      this.informer = informer;
   }
   
   @Override
   public Class<JBossResourceRefType> getEnvironmentEntryType()
   {
      return JBossResourceRefType.class;
   }

   @Override
   public Resource provide(DeploymentUnit unit, JBossResourceRefType resRef)
   {
      // let's check if there's any explicit jndi/mapped/lookup name
      String lookupName = resRef.getLookupName();
      if (lookupName != null && !lookupName.trim().isEmpty())
      {
         return new LinkRefResource(lookupName, null, resRef.isIgnoreDependency());
      }

      // now check mapped name
      String mappedName = resRef.getMappedName();
      if (mappedName != null && !mappedName.trim().isEmpty())
      {
         return new LinkRefResource(mappedName, null, resRef.isIgnoreDependency());
      }
      
      // now check (JBoss specific) jndi name!
      String jndiName = resRef.getJNDIName();
      if (jndiName != null && !jndiName.trim().isEmpty())
      {
         return new LinkRefResource(jndiName, null, resRef.isIgnoreDependency());
      }
      String internalJndiNameWithoutNamespace = DataSourceDeployerHelper.normalizeJndiName(resRef.getName(), unit, informer);
      String targetJndiName = "java:/" + internalJndiNameWithoutNamespace;
      // the binder which binds to the internal JBoss specific jndi name
      String binderName = "jboss.jca:name=" + internalJndiNameWithoutNamespace + ",service=DataSourceBinding";

      logger.debug("No jndi-name/mapped-name/lookup specified for res-ref: " + resRef.getName() + " of type datasource. Will return a Resource which depends on datasource binder: " + binderName);

      // create and return the resource
      return new DataSourceResource(targetJndiName, binderName);   
   }

}
