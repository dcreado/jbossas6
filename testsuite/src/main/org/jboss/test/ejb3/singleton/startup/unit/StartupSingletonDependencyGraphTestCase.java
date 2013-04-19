/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.test.ejb3.singleton.startup.unit;

import org.jboss.test.JBossTestCase;
import org.jboss.test.ejb3.singleton.startup.SingletonB;
import org.jboss.test.ejb3.singleton.startup.SingletonBeanRemoteView;

import javax.naming.InitialContext;

/**
 * User: jpai
 */
public class StartupSingletonDependencyGraphTestCase extends JBossTestCase {

    public StartupSingletonDependencyGraphTestCase(String name) {
        super(name);
    }

    public void testStartupSingletonBeanAccess() throws Exception {
        this.deploy("ejbthree2227.ear");
        try {
            final SingletonBeanRemoteView singletonBean = InitialContext.doLookup(SingletonB.JNDI_NAME);
            singletonBean.doSomething();
            final String message = "Hello world!";
            final String reply = singletonBean.echo(message);
            assertEquals("Unexpected reply from singleton bean", message, reply);
        } finally {
            this.undeploy("ejbthree2227.ear");
        }

    }

}
