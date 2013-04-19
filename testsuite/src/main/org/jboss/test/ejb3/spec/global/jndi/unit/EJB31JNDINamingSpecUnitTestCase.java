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
package org.jboss.test.ejb3.spec.global.jndi.unit;

import junit.framework.Test;

import org.jboss.test.JBossTestCase;
import org.jboss.test.ejb3.spec.global.jndi.JNDI31Lookup;
import org.jboss.test.ejb3.spec.global.jndi.JNDI31LookupBean;

/**
 * EJB31JNDINamingSpecUnitTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class EJB31JNDINamingSpecUnitTestCase extends JBossTestCase
{

   public EJB31JNDINamingSpecUnitTestCase(String name)
   {
      super(name);
   }
   
   public static Test suite() throws Exception
   {
      return getDeploySetup(EJB31JNDINamingSpecUnitTestCase.class, "ejb31-portable-jndi-names.jar");
   }

   public void testJNDINamesForSLSB() throws Exception
   {
      JNDI31Lookup delegateBean = (JNDI31Lookup) this.getInitialContext().lookup(JNDI31LookupBean.JNDI_NAME);
      
      delegateBean.lookupPortableSLSBJNDINames();

   }

   public void testJNDINamesForSFSB() throws Exception
   {
      JNDI31Lookup delegateBean = (JNDI31Lookup) this.getInitialContext().lookup(JNDI31LookupBean.JNDI_NAME);
      
      delegateBean.lookupPortableSFSBJNDINames();

   }

   public void testJNDINamesForSingleton() throws Exception
   {
      JNDI31Lookup delegateBean = (JNDI31Lookup) this.getInitialContext().lookup(JNDI31LookupBean.JNDI_NAME);
      
      delegateBean.lookupPortableSingletonBeanJNDINames();

   }

}
