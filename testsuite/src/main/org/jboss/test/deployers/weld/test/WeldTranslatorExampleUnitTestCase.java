/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.deployers.weld.test;

import junit.framework.Test;
import org.jboss.test.deployers.weld.translator.ejb.SentenceTranslator;

import javax.naming.InitialContext;
import java.net.URL;

/**
 * Test WELD Translator example.
 *
 */
public class WeldTranslatorExampleUnitTestCase extends WeldExampleTest
{
   public WeldTranslatorExampleUnitTestCase(String test)
   {
      super(test);
      setTestExpected(true);
   }

    @Override
    protected URL getBaseURL() throws Exception {
        return new URL(getBaseURLString() + getWebContextName() + "/home.jsf");
    }

    public static Test suite() throws Exception
   {
      return deploy(WeldTranslatorExampleUnitTestCase.class, false);
   }
}

