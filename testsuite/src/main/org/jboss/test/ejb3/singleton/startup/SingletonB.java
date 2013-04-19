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

package org.jboss.test.ejb3.singleton.startup;

import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Singleton;

/**
 * User: jpai
 */
@Singleton
@Remote (SingletonBeanRemoteView.class)
@RemoteBinding (jndiBinding = SingletonB.JNDI_NAME)
public class SingletonB implements SingletonBeanRemoteView {

    public static final String JNDI_NAME = "SingletonB-remote-view";

    private static Logger logger = Logger.getLogger(SingletonB.class);

    @EJB (name = "SLSBTwo")
    private DoSomethingView slsbTwo;

    @PostConstruct
    public void onConstruct() throws Exception {
        slsbTwo.doSomething();
    }
    
    public void doSomething() {
        logger.info(this.getClass().getName() + "#doSomething()");
    }

    @Override
    public String echo(String msg) {
        logger.info("Echo " + msg);
        return msg;
    }
}
