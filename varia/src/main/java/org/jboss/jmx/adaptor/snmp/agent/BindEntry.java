/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import javax.management.Attribute;
import javax.management.ObjectName;

import org.snmp4j.smi.OID;

/**
 * @author jean.deruelle@gmail.com
 *
 */
/**
 * An entry containing the mapping between oid and mbean/attribute
 * 
 * @author <a href="mailto:pilhuhn@user.sf.net>">Heiko W. Rupp</a>
 */
public class BindEntry implements Comparable {
	private final OID oid;

	private ObjectName mbean;
	private Attribute attr;
	private String mName;
	private String aName;
	private boolean isReadWrite = false;
	private boolean isTable = false;
	private OID tableIndexOID;

	/**
	 * Constructs a new BindEntry
	 * 
	 * @param oid
	 *            The SNMP-oid, this entry will use.
	 * @param mbName
	 *            The name of an MBean with attribute to query
	 * @param attrName
	 *            The name of the attribute to query
	 */
//	BindEntry(final String oidString, final String mbName, final String attrName) {
//		this(new OID(oidString), mbName, attrName);
//	}

	/**
	 * Constructs a new BindEntry.
	 * 
	 * @param coid
	 *            The SNMP-oid, this entry will use.
	 * @param mbName
	 *            The name of an MBean with attribute to query
	 * @param attrName
	 *            The name of the attribute to query
	 */
	BindEntry(final OID coid, final String mbName, final String attrName) {
		oid = coid;
		this.mName = mbName;
		this.aName = attrName;
		try {
			setMbean(new ObjectName(mbName));
			setAttr(new Attribute(attrName, null));

		} catch (Exception e) {
//			log.warn(e.toString());
			mName = "-unset-";
			aName = "-unset-";
		}
	}

	/**
	 * A string representation of this BindEntry
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("[oid=");
		buf.append(oid).append(", mbean=");
		buf.append(mName).append(", attr=");
		buf.append(aName).append(", rw=");
		buf.append(", table=").append(isTable());
		buf.append(isReadWrite()).append("]");

		return buf.toString();
	}

	public Attribute getAttr() {
		return attr;
	}

	public ObjectName getMbean() {
		return mbean;
	}

	public OID getOid() {
		return oid;
	}

	/**
	 * Compare two BindEntries. Ordering is defined at oid-level.
	 * 
	 * @param other
	 *            The BindEntry to compare to.
	 * @return 0 on equals, 1 if this is bigger than other
	 */
	public int compareTo(Object other) {
		if (other == null)
			throw new NullPointerException("Can't compare to NULL");

		if (!(other instanceof BindEntry))
			throw new ClassCastException("Parameter is no BindEntry");

		// trivial case
		if (this.equals(other))
			return 0;

		BindEntry obe = (BindEntry) other;
		// if (getOid().equals(obe.getOid()))
		// return 0;

		int res = oid.compareTo(obe.getOid());
		return res;
	}

	/**
	 * @param isReadWrite the isReadWrite to set
	 */
	public void setReadWrite(boolean isReadWrite) {
		this.isReadWrite = isReadWrite;
	}

	/**
	 * @return the isReadWrite
	 */
	public boolean isReadWrite() {
		return isReadWrite;
	}

	/**
	 * @param mbean the mbean to set
	 */
	public void setMbean(ObjectName mbean) {
		this.mbean = mbean;
	}

	/**
	 * @param attr the attr to set
	 */
	public void setAttr(Attribute attr) {
		this.attr = attr;
	}

	/**
	 * @param isTable the isTable to set
	 */
	public void setTable(boolean isTable) {
		this.isTable = isTable;
	}

	/**
	 * @return the isTable
	 */
	public boolean isTable() {
		return isTable;
	}

	/**
	 * @param tableIndexOID the tableIndexOID to set
	 */
	public void setTableIndexOID(OID tableIndexOID) {
		this.tableIndexOID = tableIndexOID;
	}

	/**
	 * @return the tableIndexOID
	 */
	public OID getTableIndexOID() {
		return tableIndexOID;
	}

	protected BindEntry clone(){
		BindEntry bindEntry = new BindEntry(oid, mName, aName);
		bindEntry.setAttr(attr);
		bindEntry.setMbean(mbean);
		bindEntry.setReadWrite(isReadWrite);
		bindEntry.setTable(isTable);
		return bindEntry;
	}
}
