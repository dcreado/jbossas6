/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2010, Red Hat Middleware LLC, and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.weld.integration.instantiator;

import org.jboss.beans.metadata.api.model.InjectOption;
import org.jboss.beans.metadata.plugins.AbstractInjectionValueMetaData;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.instantiator.deployer.BeanInstantiatorDeployerBase;
import org.jboss.ejb3.instantiator.spi.BeanInstantiator;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.util.IdFactory;

public class Jsr299BeanInstantiatorDeployer extends BeanInstantiatorDeployerBase
{

   protected BeanInstantiator getBeanInstantiator(JBossEnterpriseBeanMetaData ebmd)
   {
      return new Jsr299BeanInstantiator(ebmd);
   }

   protected  BeanInstantiator getBeanInstantiator(JBossEnterpriseBeanMetaData ebmd, DeploymentUnit unit)
   {
      return getBeanInstantiator(ebmd);
   }

   @Override
   protected void processMetadata(BeanMetaDataBuilder beanMetaDataBuilder, DeploymentUnit unit, JBossEnterpriseBeanMetaData ejb)
   {
      beanMetaDataBuilder.setBean(Jsr299BeanInstantiator.class.getName());
      beanMetaDataBuilder.addPropertyMetaData("bdaId", IdFactory.getIdFromClassLoader(unit.getClassLoader()));
      AbstractInjectionValueMetaData bootstrapBean = new AbstractInjectionValueMetaData(DeployersUtils.getBootstrapBeanName(unit));
      beanMetaDataBuilder.addPropertyMetaData("bootstrapBean", bootstrapBean);
   }
}
