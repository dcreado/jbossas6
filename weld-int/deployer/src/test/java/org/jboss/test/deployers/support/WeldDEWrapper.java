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
package org.jboss.test.deployers.support;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class WeldDEWrapper
{
   protected final Deployment deployment;

   public WeldDEWrapper(Deployment deployment)
   {
      this.deployment = deployment;
   }

   public Iterable<String> discoverWebBeanClasses()
   {
      Set<String> result = new HashSet<String>();
      Collection<BeanDeploymentArchive> bdas = deployment.getBeanDeploymentArchives();
      for (BeanDeploymentArchive bda : bdas)
      {
         for (String clazz : bda.getBeanClasses())
            result.add(clazz);
      }
      return result;
   }

   public Iterable<URL> discoverWeldXml()
   {
      Set<URL> result = new HashSet<URL>();
      Collection<BeanDeploymentArchive> bdas = deployment.getBeanDeploymentArchives();
      for (BeanDeploymentArchive bda : bdas)
      {
         // TODO Fix this
         //for (URL url : bda.getBeansXml())
         //   result.add(url);
      }
      return result;
   }
}