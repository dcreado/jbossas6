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
package org.jboss.jmx.adaptor.snmp.config.notification;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Simple POJO class to model XML data
 * 
 * @author  <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 * 
 * @version $Revision: 112094 $
 */
public class VarBind {
	// Private Data --------------------------------------------------

	private String tag;
	private String oid;
	private String type;

	// Constructors -------------------------------------------------

	/**
	 * Default CTOR
	 */
	public VarBind() {
		// empty
	}

	// Accessors/Modifiers -------------------------------------------

	public String getOid() {
		return oid;
	}

	public String getTag() {
		return tag;
	}

	@XmlAttribute(name = "oid")
	public void setOid(String oid) {
		this.oid = oid;
	}

	@XmlAttribute(name = "tag")
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	@XmlAttribute(name = "type")
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	// Object overrides ----------------------------------------------

	public String toString() {
		StringBuffer sbuf = new StringBuffer(256);

		sbuf.append('[').append("tag=").append(tag).append(", oid=")
				.append(oid).append(", type=")
				.append(type).append(']');

		return sbuf.toString();
	}
}
