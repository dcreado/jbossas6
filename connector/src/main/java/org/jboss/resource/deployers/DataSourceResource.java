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

import java.util.Collection;
import java.util.Collections;

import javax.naming.LinkRef;

import org.jboss.switchboard.spi.Resource;

/**
 * {@link Resource} for a data-source/@DataSourceDefinition reference
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class DataSourceResource implements Resource
{

   
   /**
    * The JBoss internal datasource binder which binds the datasource to an internal
    * jndi name 
    */
   private String dataSourceBinderName;
   
   /**
    * The internal JBoss specific jndi name of the datasource
    */
   private String targetJndiName;
   
   /**
    * 
    * @param internalJndiName JBoss specific internal jndi name for the datasource
    * @param datasourceBinderName The binder which binds the datasource to the JBoss specific internal jndi name
    */
   public DataSourceResource(String internalJndiName, String datasourceBinderName)
   {
      this.targetJndiName = internalJndiName;
      this.dataSourceBinderName = datasourceBinderName;
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public Object getDependency()
   {
      // a LinkRef doesn't need to depend on anything *during bind time*
      return null;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<?> getInvocationDependencies()
   {
      if (this.dataSourceBinderName == null || this.dataSourceBinderName.trim().isEmpty())
      {
         return null;
      }
      // this DataSourceResource, for lookup/invocation, depends on the binder which binds the datasource to internal
      // JNDI name
      return Collections.singleton(this.dataSourceBinderName);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object getTarget()
   {
      // return the LinkRef to internal JBoss specific jndi name of the datasource
      return new LinkRef(this.targetJndiName);
   }
   
   @Override
   public String toString()
   {
      return "DataSourceResource[jndiname=" + this.targetJndiName + " ,binderName=" + this.dataSourceBinderName + "]";
   }
}
