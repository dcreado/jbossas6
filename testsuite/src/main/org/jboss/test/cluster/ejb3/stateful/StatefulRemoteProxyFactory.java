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
package org.jboss.test.cluster.ejb3.stateful;

import org.jboss.aop.Advisor;
import org.jboss.ejb3.proxy.impl.factory.session.stateful.StatefulSessionRemoteProxyFactory;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;

/**
 * Comment
 *
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 * @version $Revision: 108925 $
 */
public class StatefulRemoteProxyFactory extends StatefulSessionRemoteProxyFactory
{
   @SuppressWarnings("unused")
   private static final Logger log = Logger.getLogger(StatefulRemoteProxyFactory.class);

   public StatefulRemoteProxyFactory(final String name, final String containerName, final String containerGuid,
         final JBossSessionBeanMetaData metadata, final ClassLoader classloader, final String url,
         final Advisor advisor, final String interceptorStackName)
   {
      super(name, containerName, containerGuid, metadata, classloader, url, advisor, interceptorStackName);
   }
}
