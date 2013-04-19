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
import javax.xml.bind.annotation.XmlElement;

/**
 * Simple POJO class to model XML data
 * 
 * @author  <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 * 
 * @version $Revision: 112094 $
 */
public class Mapping
{
	private String notificationType;
	private int generic;
	private int specific;
	private String enterprise;
	private boolean inform;
	private String securityName;
	private VarBindList varBindList;

	// Constructors -------------------------------------------------

	/**
	 * Default CTOR
	 */
	public Mapping() {
		// empty
	}

	// Accessors/Modifiers -------------------------------------------

	public String getEnterprise() {
		return enterprise;
	}

	public int getGeneric() {
		return generic;
	}

	public String getNotificationType() {
		return notificationType;
	}

	public int getSpecific() {
		return specific;
	}

	public VarBindList getVarBindList() {
		return varBindList;
	}
	
	@XmlAttribute(name="enterprise")
	public void setEnterprise(String enterprise) {
		this.enterprise = enterprise;
	}

	@XmlAttribute(name="generic")
	public void setGeneric(int generic) {
		this.generic = generic;
	}

	@XmlAttribute(name="notification-type")
	public void setNotificationType(String notificationType) {
		this.notificationType = notificationType;
	}

	@XmlAttribute(name="specific")
	public void setSpecific(int specific) {
		this.specific = specific;
	}

	@XmlElement(name="var-bind-list")
	public void setVarBindList(VarBindList varBindList) {
		this.varBindList = varBindList;
	}

	/**
	 * @param inform
	 *            the inform to set
	 */
	@XmlAttribute(name="inform")
	public void setInform(boolean inform) {
		this.inform = inform;
	}

	/**
	 * @return the inform
	 */
	public boolean isInform() {
		return inform;
	}

	/**
	 * @param securityName
	 *            the securityName to set
	 */
	@XmlAttribute(name="security-name")
	public void setSecurityName(String securityName) {
		this.securityName = securityName;
	}

	/**
	 * @return the securityName
	 */
	public String getSecurityName() {
		return securityName;
	}

	// Object overrides ----------------------------------------------

	public String toString() {
		StringBuffer sbuf = new StringBuffer(256);

		sbuf.append('[').append("notificationType=").append(notificationType)
				.append(", generic=").append(generic).append(", specific=")
				.append(specific).append(", enterprise=").append(enterprise)
				.append(", inform=").append(inform).append(", securityName=")
				.append(securityName).append(", varBindList=")
				.append(varBindList).append(']');

		return sbuf.toString();
	}   
}
