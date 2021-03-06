/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.jmx.adaptor.snmp.agent;

import org.snmp4j.smi.OID;

/**
 * This exception is thrown when an entry is read only and
 * a client wants to write to it.
 * 
 * @author <a href="mailto:hwr@pilhuhn.de">Heiko W. Rupp</a>
 * @version $Revision: 112094 $
 */
public class ReadOnlyException extends Exception
{
   public ReadOnlyException(final String oid)
   {
      super("OID " + oid + " is read only");
   }
   
   public ReadOnlyException(OID oid)
   {
	   super("OID " + oid + " is read only");
   }
}
