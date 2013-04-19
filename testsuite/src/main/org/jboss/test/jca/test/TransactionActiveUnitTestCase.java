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
package org.jboss.test.jca.test;

import junit.framework.Test;

import org.jboss.test.JBossTestCase;
import org.jboss.test.jca.interfaces.TransactionActiveHome;
import org.jboss.test.jca.interfaces.TransactionActiveRemote;

/**
 * TransactionActiveUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 82920 $
 */
public class TransactionActiveUnitTestCase extends JBossTestCase
{
   public TransactionActiveUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(TransactionActiveUnitTestCase.class, "jca-txactive-ejb.jar");
   }

   public void testJDBCTransactionActive() throws Exception
   {
      TransactionActiveHome home = (TransactionActiveHome) getInitialContext().lookup("test/ejbs/TxActiveBean");
      TransactionActiveRemote remote = home.create();
      remote.setupDatabase();
      remote.changeDatabase();
      remote.checkDatabase();
   }

   public void testJMSTransactionActive() throws Exception
   {
      TransactionActiveHome home = (TransactionActiveHome) getInitialContext().lookup("test/ejbs/TxActiveBean");
      TransactionActiveRemote remote = home.create();
      remote.setupQueue();
      remote.changeQueue();
      remote.checkQueue();
   }
}
