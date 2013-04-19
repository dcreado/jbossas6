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

package org.jboss.test.ejb3.singleton.security.unit;

import junit.framework.Assert;
import junit.framework.Test;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.jboss.test.JBossTestCase;
import org.jboss.test.ejb3.singleton.security.SecuredSingletonBean;
import org.jboss.test.ejb3.singleton.security.SingletonSecurity;

import javax.ejb.EJBAccessException;
import javax.naming.InitialContext;

/**
 * Tests that invocations on a secured singleton bean work as expected
 * <p/>
 * User: Jaikiran Pai
 */
public class SingletonSecurityTestCase extends JBossTestCase {

    public SingletonSecurityTestCase(String name) {
        super(name);
    }

    public static Test suite() throws Exception {
        return getDeploySetup(SingletonSecurityTestCase.class, "ejb3-singleton-security.jar");
    }

    /**
     * Test a method invocation on a singleton bean with the correct expected role.
     *
     * @throws Exception
     */
    public void testInvocationOnSecuredMethodWithCorrectRole() throws Exception {
        final SingletonSecurity securedSingleton = InitialContext.doLookup(SecuredSingletonBean.JNDI_NAME);
        final SecurityClient securityClient = SecurityClientFactory.getSecurityClient();
        securityClient.setSimple("user1", "pass1");
        try {
            // login
            securityClient.login();
            // expects role1, so should succeed
            securedSingleton.allowedForRole1();
        } finally {
            securityClient.logout();
        }

    }

    /**
     * Test a method invocation on a singleton bean with an incorrect role.
     *
     * @throws Exception
     */
    public void testInvocationOnSecuredMethodWithInCorrectRole() throws Exception {
        final SingletonSecurity securedSingleton = InitialContext.doLookup(SecuredSingletonBean.JNDI_NAME);
        final SecurityClient securityClient = SecurityClientFactory.getSecurityClient();
        securityClient.setSimple("user2", "pass2");
        try {
            // login
            securityClient.login();
            try {
                // expects role1, so should fail
                securedSingleton.allowedForRole1();
                Assert.fail("Call to secured method, with incorrect role, was expected to fail");
            } catch (EJBAccessException ejbae) {
                // expected
            }
        } finally {
            securityClient.logout();
        }

    }

    /**
     * Test a method invocation on a singleton bean without logging in.
     *
     * @throws Exception
     */
    public void testInvocationOnSecuredMethodWithoutLogin() throws Exception {
        final SingletonSecurity securedSingleton = InitialContext.doLookup(SecuredSingletonBean.JNDI_NAME);
        try {
            securedSingleton.allowedForRole1();
            Assert.fail("Call to secured method was expected to fail");
        } catch (EJBAccessException ejbae) {
            // expected
        }

    }
}
