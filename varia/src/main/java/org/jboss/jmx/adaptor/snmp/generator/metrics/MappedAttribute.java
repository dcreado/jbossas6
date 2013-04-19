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
package org.jboss.jmx.adaptor.snmp.generator.metrics;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * An attribute mapping, by default readonly, and a blank description
 * 
 * @author <a href="mailto:hwr@pilhuhn.de">Heiko W. Rupp</a>
 * @version $Revision: 111649 $
 */
public class MappedAttribute
{
	private String name;
	private String oid;
	private String mode;
	private boolean isReadWrite = false;
	private String table;
	private boolean isAttributeTable = false;
	
	private String mbName = ""; //the name of the mBean this MappedAttribute is associated with
	private String snmpType = ""; //the type for the MIB we should use, if provided
	private String oidPrefix = ""; // all of these metrics are useful in the MIB Generator
	private String oidDefName = ""; 
	private String maxAccess = "";
	private String description = ""; 
	private String status = "";

	public MappedAttribute() {
	}

	/** Attribute name */
	public String getName() {
		return name;
	}

	@XmlAttribute(name="name")
	public void setName(String name) {
		this.name = name;
	}

	/** Attribute oid */
	public String getOid() {
		return oid;
	}

	@XmlAttribute(name="oid")
	public void setOid(String oid) {
		this.oid = oid;
	}

	/** Attribute mode (ro/rw) */
	public boolean isReadWrite() {
		return isReadWrite;
	}
	
	/** Attribute mode (ro/rw) */
	public String getMode() {
		return mode;
	}

	@XmlAttribute(name="mode")
	public void setMode(String mode) {
		this.mode = mode;
		if(mode != null && mode.equalsIgnoreCase("rw")) {
			isReadWrite = true;
		}
	}
	/** Attribute table  */
	public boolean isAttributeTable() {
		return isAttributeTable;
	}
	
	/** Attribute table  */
	public String getTable() {
		return table;
	}

	@XmlAttribute(name="table")
	public void setTable(String table) {
		this.table = table;
		if(table != null && Boolean.valueOf(table)) {
			isAttributeTable = true;
		}
	}
	
	public String getMbean(){
		return this.mbName;
	}
	
	@XmlAttribute(name="name")
	public void setMbean(String mbName){
		this.mbName = mbName;
	}
	
	public String getSnmpType(){
		return this.snmpType;
	}
	
	public void setSnmpType(String snmpType){
		this.snmpType=snmpType;
	}
	
	public String getOidPrefix(){
		return this.oidPrefix;
	}
	
	public void setOidPrefix(String oidPrefix){
		if (oidPrefix.charAt(0) == '.')
			this.oidPrefix = oidPrefix.substring(1);
		else
			this.oidPrefix = oidPrefix;
	}
	
	public String getOidDefName(){
		return this.oidDefName;
	}
	
	public void setOidDefName(String oidDefName){
		this.oidDefName = oidDefName;
	}
	
	public String getMaxAccess(){
		return this.maxAccess;
	}
	
	public void setMaxAccess(String maxAccess){
		this.maxAccess = maxAccess;
	}
	
	public String getSnmpDesc(){
		return this.description;
	}
	
	public void setSnmpDesc(String snmpDesc){
		this.description = snmpDesc;
	}
	
	public String getStatus(){
		return this.status;
	}
	
	public void setStatus(String status){
		this.status = status;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("[name=").append(name);
		buf.append(", oid=").append(oid);
		buf.append(", rw=").append(isReadWrite);
		buf.append("]");
		return buf.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if (this == o) 
			return true;
		
		if (o == null)
			return false;
		
		MappedAttribute that = (MappedAttribute) o;
		
		String fullOidThis = this.oidPrefix+"."+this.oid;
		String fullOidThat = that.oidPrefix+"."+that.oid;
		return (fullOidThis.equals(fullOidThat));
	}
	
	@Override 
	public int hashCode(){
		int result = 4;
		int PRIME = 31;
		String fullOid = this.oidPrefix+"."+this.oid;
		return PRIME * result + fullOid.hashCode();			
	}
}
