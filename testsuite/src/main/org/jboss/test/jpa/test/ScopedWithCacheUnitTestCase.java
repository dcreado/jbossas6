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
package org.jboss.test.jpa.test;

import java.util.Properties;

import javax.naming.InitialContext;

import junit.framework.Test;

import org.jboss.test.JBossTestCase;
import org.jboss.test.hibernate.test.HibernateIntgUnitTestCase;
import org.jboss.test.jpa.classloader.ejb.EntityTester;
import org.jboss.test.jpa.classloader.model.Account;
import org.jboss.test.jpa.classloader.model.AccountHolder;

/**
 * 
 * @author Brian Stansberry
 * @version $Id: EntityUnitTestCase.java 57207 2006-09-26 12:06:13Z dimitris@jboss.org $
 */
public class ScopedWithCacheUnitTestCase
extends JBossTestCase
{   
   protected org.jboss.logging.Logger log = getLog();

   protected static final AccountHolder SMITH = new AccountHolder("Smith", "1000");
   
   protected EntityTester sfsb0;
   
   public ScopedWithCacheUnitTestCase(String name)
   {
      super(name);
   }

   /** Setup the test suite.
    */
   public static Test suite() throws Exception
   {
      return getDeploySetup(ScopedWithCacheUnitTestCase.class, "jpa-classloader-test.ear");
   }
   
   protected void setUp() throws Exception
   {
      super.setUp();
      
      sfsb0 = getEntityTester(getJndiURL()); 
      sfsb0.cleanup();
   }
   
   protected EntityTester getEntityTester(String nodeJNDIAddress) throws Exception
   {
//      Properties prop1 = new Properties();
//      prop1.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
//      prop1.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
//      prop1.put("java.naming.provider.url", "jnp://" + nodeJNDIAddress + ":1099");
//   
//      log.info("===== Naming properties for " + nodeJNDIAddress + ": ");
//      log.info(prop1);
//      log.info("Create InitialContext for " + nodeJNDIAddress);
      InitialContext ctx1 = getInitialContext();
   
      log.info("Lookup sfsb from " + nodeJNDIAddress);
      EntityTester eqt = (EntityTester)ctx1.lookup("EntityTesterBean/remote");
      
      return eqt;
   }
    
   protected void tearDown() throws Exception
   {
      if (sfsb0 != null)
      {
         try
         {
            sfsb0.remove(true);
         }
         catch (Exception e) {}
      }
      
      sfsb0 = null;
   }
   
   public void testSimpleInsert() throws Exception
   {
      Integer id = Integer.valueOf(1001);
      Integer balance = Integer.valueOf(5);
      sfsb0.createAccount(SMITH, id, balance, "94536");
      Account acct = sfsb0.getAccount(id);
      assertNotNull(acct);
      AccountHolder holder = acct.getAccountHolder();
      assertNotNull(holder);
      assertEquals("Smith", holder.getLastName());
      assertEquals("1000", holder.getSsn());
      assertEquals(balance, acct.getBalance());
      assertEquals("94536", acct.getBranch());
   }
}
