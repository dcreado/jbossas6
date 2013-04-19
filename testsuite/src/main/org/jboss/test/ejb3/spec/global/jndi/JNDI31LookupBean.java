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
package org.jboss.test.ejb3.spec.global.jndi;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.NamingException;

import org.jboss.ejb3.annotation.RemoteBinding;

/**
 * JNDILookupBean
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Stateless
@Remote(JNDI31Lookup.class)
@RemoteBinding(jndiBinding = JNDI31LookupBean.JNDI_NAME)
public class JNDI31LookupBean extends AbstractJNDILookup implements JNDI31Lookup
{

   public static final String JNDI_NAME = "ejb31-portable-jndi-name-delegate-bean";

   public JNDI31LookupBean()
   {
      this.moduleName = "ejb31-portable-jndi-names";
   }

   @Override
   public void lookupPortableSFSBJNDINames() throws NamingException
   {
      this.lookupPortableJNDINames(SimpleSFSB.class, false, true);
   }

   @Override
   public void lookupPortableSLSBJNDINames() throws NamingException
   {
      this.lookupPortableJNDINames(SimpleSLSB.class, false, true);
   }

   @Override
   public void lookupPortableSingletonBeanJNDINames() throws NamingException
   {
      this.lookupPortableJNDINames(SimpleSingleton.class, true, true);

   }

}
