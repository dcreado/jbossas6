/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.deployers.test;

import java.util.List;

import junit.framework.Test;

import org.jboss.test.deployers.support.MockWeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.integration.deployer.env.bda.DeploymentImpl;

/**
 * JBoss Deployment test case.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class BasicEarJBossDeploymentTestCase extends AbstractDeploymentTest
{
   public BasicEarJBossDeploymentTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(BasicEarJBossDeploymentTestCase.class);
   }

   @Override
   protected int getExpectedArchives()
   {
      return 3;
   }

   @Override
   protected void initializeDeployment(Deployment deployment)
   {
      ((DeploymentImpl) deployment).initialize(new MockWeldBootstrap());
   }
   
   @Override
   protected void assertNewBeanDeploymentArchive(List<BeanDeploymentArchive> archives, BeanDeploymentArchive newBDA)
   {
      assertSame(newBDA, archives.iterator().next());
   }
}