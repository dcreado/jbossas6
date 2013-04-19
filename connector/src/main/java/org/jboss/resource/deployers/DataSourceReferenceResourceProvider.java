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
import org.jboss.reloaded.naming.deployers.javaee.JavaEEComponentInformer;
import org.jboss.switchboard.javaee.environment.DataSourceType;
import org.jboss.switchboard.mc.spi.MCBasedResourceProvider;
import org.jboss.switchboard.spi.Resource;

/**
 * {@link MCBasedResourceProvider Resource provider} for processing &lt;data-source&gt;/@DataSourceDefinition
 * references in a Java EE component
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class DataSourceReferenceResourceProvider implements MCBasedResourceProvider<DataSourceType>
{

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
   public Class<DataSourceType> getEnvironmentEntryType()
   {
      return DataSourceType.class;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Resource provide(DeploymentUnit unit, DataSourceType dataSource)
   {
      // get the jndi name of the datasource. 
      String dsJndiName = dataSource.getName();
      // convert it to internal JBoss specific jndi name
      // Note that the normalize method return a jndi name without an prefix but the DSDeployer binds to
      // java: prefix by default. So we prefix the java: to this returned normalized jndi name
      String internalJndiNameWithoutNamespace = DataSourceDeployerHelper.normalizeJndiName(dsJndiName, unit, informer);
      String targetJndiName = "java:/" + internalJndiNameWithoutNamespace;
      
      // the binder which binds to the internal JBoss specific jndi name
      String binderName = "jboss.jca:name=" + internalJndiNameWithoutNamespace + ",service=DataSourceBinding";
      
      // create and return the resource
      return new DataSourceResource(targetJndiName, binderName);
   }
}
