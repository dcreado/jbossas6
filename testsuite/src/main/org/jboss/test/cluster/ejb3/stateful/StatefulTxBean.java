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

import javax.annotation.security.RolesAllowed;
import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.TransactionManager;

import org.jboss.ejb3.annotation.JndiInject;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.logging.Logger;

/**
 * Comment
 *
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 * @version $Revision: 108925 $
 */
@Stateful
@Remote(StatefulTx.class)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED) 
@SecurityDomain("test")
public class StatefulTxBean implements StatefulTx
{
  private static final Logger log = Logger.getLogger(StatefulTxBean.class);
   
   @JndiInject(jndiName="java:/TransactionManager") private TransactionManager tm;
    
   @RolesAllowed("allowed")
   public boolean isGlobalTransacted() throws javax.transaction.SystemException
   {
      return (tm.getTransaction() != null);
   }
   
   @TransactionAttribute(TransactionAttributeType.REQUIRED)
   @RolesAllowed("allowed")
   public boolean isLocalTransacted() throws javax.transaction.SystemException
   {
      return (tm.getTransaction() != null);
   }
   
   @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
   @RolesAllowed("allowed")
   public boolean testNewTx() throws javax.transaction.SystemException
   {
      return (tm.getTransaction() != null);
   }
   
   @TransactionAttribute(TransactionAttributeType.REQUIRED)
   @RolesAllowed("allowed")
   public void testTxRollback()
   {
      throw new RuntimeException("test rollback");
   }
   
   @TransactionAttribute(value = TransactionAttributeType.MANDATORY)
   @RolesAllowed("allowed")
   public State testMandatoryTx(State o)
   {
      return o;
   }
}
