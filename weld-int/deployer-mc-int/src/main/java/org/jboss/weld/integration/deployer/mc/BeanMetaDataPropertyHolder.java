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
package org.jboss.weld.integration.deployer.mc;

import org.jboss.beans.metadata.spi.BeanMetaData;

/**
 * Due to http://www.jboss.org/index.html?module=bb&op=viewtopic&p=4263782#4263782
 * BeanMetaData cannot be used directly as a property of a bean. Create this intermediate 
 * class instead
 *  
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class BeanMetaDataPropertyHolder
{
   private final BeanMetaData beanMetaData;
   
   /**
    * Constructor
    * @param beanMetaData The bean metadata to wrap
    * @throws IllegalArgumentException if beanMetaData is null
    */
   public BeanMetaDataPropertyHolder(BeanMetaData beanMetaData)
   {
      if (beanMetaData == null)
         throw new IllegalArgumentException("Null bean metadata");
      this.beanMetaData = beanMetaData;
   }

   /**
    * Get the bean metadata
    * @return the bean metadata
    */
   public BeanMetaData getBeanMetaData()
   {
      return beanMetaData;
   }
}
