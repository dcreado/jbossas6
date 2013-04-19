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

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.jmx.adaptor.snmp.config.attribute.ManagedBean;
import org.jboss.jmx.adaptor.snmp.config.attribute.MappedAttribute;
import org.jboss.logging.Logger;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;

/**
 * @author jean.deruelle@gmail.com
 * 
 */
public class TableMapper {

	/**
	 * keep an index of the OID from attributes.xml for mbean name defined a
	 * pattern (with a wildcard in it) and the corresponding ManagedBean. The
	 * OID will be the OID defined in the oid-prefix minus the last .X as this
	 * will be used as the entry
	 */
	private SortedMap<OID, ManagedBean> tables = new TreeMap<OID, ManagedBean>();
	private SortedMap<OID, BindEntry> tableBindings = new TreeMap<OID, BindEntry>();
	private SortedMap<OID, OID> tableIndexes = new TreeMap<OID, OID>();
	private SortedMap<OID, Variable> objectNameIndexes = new TreeMap<OID, Variable>();

	private MBeanServer server;
	private Logger log;

	public TableMapper(MBeanServer server, Logger log) {
		this.server = server;
		this.log = log;
	}
	
	/**
	 * 
	 * @param oid
	 * @return
	 */
	public BindEntry getTableBinding(OID oid) {
		return tableBindings.get(oid);
	}
	
	/**
	 * 
	 * @param oid
	 * @return
	 */
	public OID getNextTable(OID oid) {
		return tableIndexes.get(oid);
	}
	
	/**
	 * 
	 * @param oid
	 * @return
	 */
	public Variable getObjectNameIndexValue(OID oid) {		
		return objectNameIndexes.get(oid);
	}

	/**
	 * 
	 * @param mmb
	 * @param oname
	 */
	public void addTableMapping(ManagedBean mmb, ObjectName oname) {
		tables.put(
				new OID(mmb.getOidPrefix().substring(0,
								mmb.getOidPrefix().lastIndexOf("."))), mmb);
		// get all ObjectNames of MBeans matched by the given name.
		// they should be treated as Rows of the table defined which will have the oid
		// oidPrefix.
		Set<ObjectName> mbeanNames = server.queryNames(oname, null);
		if(mbeanNames.size() > 0) {
			createMappings(mbeanNames, mmb.getAttributes(), mmb.getOidPrefix());
		}
	}

	/**
	 * hacked together method that iterates through a list of object names and
	 * adds metrics to the bind entry set
	 * 
	 * @param mbeanNames
	 *            Set of ObjectNames associated with a wildcard ObjectName
	 * @param attrs
	 *            List of attributes that we want to know about for each entry
	 *            in mbeanNames
	 * @param oidPrefix
	 *            the oidPrefix for each of these, because Ideally we are
	 *            creating a table.
	 */
	private void createMappings(Set<ObjectName> mbeanNames,
			List<MappedAttribute> attrs, String tableOid) {
		boolean firstColumnIndexSet = false;
		SortedSet<String> onameStrings = new TreeSet<String>();
		for (ObjectName oname : mbeanNames) {
			onameStrings.add(oname.toString());
		}
		String previousMBeanName = null;
		String lastMBeanName = onameStrings.last();
//		OID rowIndexOID = null;
//		OID previousRowIndexOID = null;
		OID firstOID = null;
//		int rowIndex = 1;
		for (String mbeanRealName : onameStrings) {
			String previousAttribute = null;
			for (MappedAttribute ma : attrs) {
				String oid = tableOid;
				String columnOid = oid + ma.getOid();
				String previousOid = null;
				String fullOid = columnOid + ".'" + mbeanRealName + "'";
				if(previousMBeanName != null) {
					previousOid = columnOid + ".'" + previousMBeanName + "'";
				}
				OID coid = new OID(fullOid);
				// adding entry for the given OID
				addBindEntry(coid, mbeanRealName, ma.getName(), ma.isReadWrite());
				// adding mapping between the oid and the previous oid with the same attribute in the table
				if(previousOid != null) {
					OID previousOID = new OID(previousOid);
					tableIndexes.put(previousOID, coid);
				}				
				if(firstOID == null) {
					firstOID = coid;
				}
				// By issuing a GETNEXT request with the bare MIB name of one of the columns, the agent will return that entry from the first row of the table:
				if(!firstColumnIndexSet) {					
					// adding mapping between the table oid  and table entry oid and the first OID in the table
//					tableIndexes.put(new OID(oid + ".'" + ma.getName() + "'"), coid);
					tableIndexes.put(new OID(oid + ma.getOid()), coid);
					if(previousAttribute != null) {
						String lastRowOID = oid + previousAttribute +  ".'" + lastMBeanName + "'";
						tableIndexes.put(new OID(lastRowOID), coid);
					}
				}		
				previousAttribute = ma.getOid();
			}	
//			rowIndexOID = new OID(tableOid + ".1." + rowIndex);			
//			if(previousRowIndexOID == null) {
//				// adding mapping between the table oid  and table entry oid and the first OID in the table
//				tableIndexes.put(new OID(tableOid), rowIndexOID);
//				tableIndexes.put(new OID(tableOid.substring(0,
//						tableOid.lastIndexOf("."))), rowIndexOID);
//				objectNameIndexes.put(rowIndexOID, new OctetString(mbeanRealName));
//				previousRowIndexOID = rowIndexOID;
//			} else {
//				tableIndexes.put(previousRowIndexOID, rowIndexOID);
//				objectNameIndexes.put(rowIndexOID, new OctetString(mbeanRealName));
//				previousRowIndexOID = rowIndexOID;
//			}
//			rowIndex++;
			firstColumnIndexSet = true;
			previousMBeanName = mbeanRealName;
		}
		tableIndexes.put(new OID(tableOid), firstOID);
		tableIndexes.put(new OID(tableOid.substring(0,
				tableOid.lastIndexOf("."))), firstOID);
//		if(firstOID != null && previousRowIndexOID != null) {
//			tableIndexes.put(previousRowIndexOID, firstOID);
//		}
	}

	/**
	 * 
	 * @param oid
	 *            The OID bound to this particular attribute
	 * @param mmb
	 *            the name of the MBean server
	 * @param ma
	 *            the name of the MBeam attribute the OID is concerning
	 * @param rw
	 *            indicates whether this Attribute is read-write or not
	 *            (readonly if false)
	 */
	private void addBindEntry(OID coid, String mmb, String ma, boolean rw) {
		BindEntry be = new BindEntry(coid, mmb, ma);
		be.setReadWrite(rw);

		if (log.isTraceEnabled())
			log.trace("New bind entry   " + be);
		if (tableBindings.containsKey(coid)) {
			log.info("Duplicate oid " + coid + RequestHandlerImpl.SKIP_ENTRY);
		}
		if (mmb == null || mmb.equals("")) {
			log.info("Invalid mbean name for oid " + coid + RequestHandlerImpl.SKIP_ENTRY);
		}
		if (ma == null || ma.equals("")) {
			log.info("Invalid attribute name " + ma + " for oid " + coid
					+ RequestHandlerImpl.SKIP_ENTRY);
		}
		tableBindings.put(coid, be);
	}

	// To be optimized to call only the app needed
	public void checkTables(OID oid) {				
		for (Entry<OID, ManagedBean> tableEntry : tables.entrySet()) {
			ManagedBean managedBean = tableEntry.getValue();
			if (oid.startsWith(tableEntry.getKey()) && tableBindings.tailMap(tableEntry.getKey()).isEmpty()) {
				ObjectName oname = null;
				try {
					oname = new ObjectName(managedBean.getName());
				} catch (Exception e) {
				}
				// get all ObjectNames of MBeans matched by the given name.
				// they should be treated as Rows of the table defined which will have the oid
				// oidPrefix.
				Set<ObjectName> mbeanNames = server.queryNames(oname, null); 
				createMappings(mbeanNames, managedBean.getAttributes(),
						managedBean.getOidPrefix());
			}			
		}
	}

	public boolean belongsToTable(OID oid) {		
		return tableBindings.get(oid) != null;
	}

	public void removeTableMapping(ManagedBean mmb, ObjectName oname) {
		
	}
}
