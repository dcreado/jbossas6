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
package org.jboss.jmx.adaptor.snmp.generator;


import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;

import org.jboss.jmx.adaptor.snmp.generator.metrics.MappedAttribute;
import org.jboss.jmx.adaptor.snmp.generator.metrics.AttributeMappings;
import org.jboss.jmx.adaptor.snmp.generator.metrics.ManagedBean;
import org.jboss.jmx.adaptor.snmp.generator.metrics.Mapping;
import org.jboss.jmx.adaptor.snmp.generator.metrics.VarBind;
import org.jboss.jmx.adaptor.snmp.generator.exceptions.NotEnoughInformationException;

import javax.management.ObjectName;


/** 
* This class will be able to generate MIBs or XMLs, depending on the invocation by the user. 
* It will utilize mibble libraries to parse MIBs to generate xmls from them, and will use 
* objects gathered from the parsing of XMLs to generate MIBs. Types are determined by either an
* attribute in the attributes.xml (snmp-type), or, if missing, a check on the attribute's type after 
* querying the server. 
*
*  @author <a href="mailto:tom.hauser@gmail.com>Tom Hauser</a>
**/

public class Generator {
	private String outputResName; // the name of the output file (.mib or .xml)
	private String moduleName; // the name of the output SNMP module
	private ArrayList<MIBObject> miboList; // internal list of MIBObjects
	private ArrayList<MIBNotification> mibnList;
	private ArrayList<MIBTable> tableList;
	private ArrayList<MappedAttribute> maList; // list of Mapped Attributes we care about
	private ArrayList<Mapping> nmList; // list of notification mappings we care about
	private ArrayList<VarBind> vbList;
	private AttributeMappings mbList;
	
	private HashMap<String, OIDDef> oidDefMap;	

	/** 
	 * Default constructor. Nulls all around.
	 */
	
	public Generator(){
		this.outputResName = null;
		this.mbList = null;
		this.miboList = null;
		this.maList = null;
	}
	
	/**
	 * Constructor for use when also parsing Notifications.xml
	 * @param outputResName the output filename
	 * @param maList list of MappedAttributes received from the parser 
	 * @param mbList list of ManagedBeans, can be null if we're generating an XML from an MIB
	 * @param nmList list of notifications received from the parser
	 */
	
	public Generator(String outputResName, String moduleName, ArrayList<MappedAttribute> maList, AttributeMappings mbList, ArrayList<Mapping> nmList){
		this.outputResName = outputResName;
		this.moduleName = moduleName;
		this.mbList = mbList;
		this.miboList = new ArrayList<MIBObject>();
		this.maList = maList;
		this.oidDefMap = new HashMap<String, OIDDef>();
		this.nmList = nmList;
		this.mibnList = new ArrayList<MIBNotification>();
		this.tableList = new ArrayList<MIBTable>();
	}
	
	//mutators
	public String getOutputResName(){
		return this.outputResName;
	}
	
	public void setOutputResName(String outputResName){
		this.outputResName = outputResName;
	}
	
	public ArrayList<MappedAttribute> getMaList(){
		return this.maList;
	}
	
	public void setMaList(ArrayList<MappedAttribute> maList){
		this.maList = maList;
	}
	
	public AttributeMappings getMbList(){
		return this.mbList;
	}
	
	public void setMbList(AttributeMappings mbList){
		this.mbList = mbList;
	}
	
	public HashMap<String, OIDDef> getOidDefMap(){
		return this.oidDefMap;
	}
	
	public void setOidDefMap(HashMap<String, OIDDef> oidDefMap){
		this.oidDefMap = oidDefMap;
	}

	/** 
	 * Entry point method. Called by MIBGenerator class after all of the parsing is completed. 
	 * Does nothing really itself.
	 */

	public void writeFile(){
		if (outputResName == null){
			System.out.println("No output file location given. Aborting.");
			System.exit(2);
		}
		try{
			
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputResName)));
			createEntries();
			writeMibHeader(out);
			writeMibImports(out);
			writeMibObjectDefinitions(out);
			writeEntries(out);			
			out.print("END");
			out.close();
		}
		catch (Exception e){e.printStackTrace();}
	}
	
	/**
	 * Simple method that writes the header to the MIB file.  
	 * TODO: make the name JBOSS-AS-MIB configurable.
	 * @param out the PrintWriter to output to.
	 */
	
	private void writeMibHeader(PrintWriter out){
		out.println("-- This MIB Generated by the JBoss MIB Generator");
		out.println();
		out.println(this.moduleName+" DEFINITIONS ::=BEGIN");
		out.println();
		// maybe add more here, if not we don't need this to be a seperate method.
	}
	
	/**
	 * Another simple method that outputs the Imports of the generated MIB.
	 * The current imports will be recognized by any standard snmp tool (notably net-snmp)
	 * TODO: Make these imports configurable. 
	 * @param out
	 */
	
	private void writeMibImports(PrintWriter out){
		out.println("IMPORTS");
		out.println("\tOBJECT-TYPE,");
		out.println("\tNOTIFICATION-TYPE,");
		out.println("\tCounter32,");
		out.println("\tGauge32,");
		out.println("\tCounter64,");
		out.println("\tTimeTicks");
		out.println("\t\tFROM SNMPv2-SMI");
		out.println("\tDisplayString,");
		out.println("\tTruthValue");
		out.println("\t\tFROM SNMPv2-TC;");	
		out.println();
	}
	
	/** 
	 *  This method outputs all of the Object OID definitions we need in order for a manager
	 *  that loads the generated MIB to be able to know the names of metrics it is getting back.
	 *  TODO: enable exact configuration of this section
	 * @param out The PrintWriter that writes to the file we need it to
	 * @author <a href="mailto:tom.hauser@gmail.com>Tom Hauser</a>
	 */	
	private void writeMibObjectDefinitions(PrintWriter out){
		Set<String> oidKeySet = oidDefMap.keySet();
		Iterator<String> oidIt = oidKeySet.iterator();
		while (oidIt.hasNext()){
			out.println(oidDefMap.get(oidIt.next()));
		}
		out.println();
	}
	
	/**
	 * Outputs all of the objects to the output file
	 * @param out
	 */
	
	private void writeEntries(PrintWriter out){
		for (MIBObject mibo : miboList){
			out.println(mibo);
		}
		for (MIBNotification noti : mibnList){
			out.println(noti);
		}
		for (MIBTable table : tableList){
			out.println(table);
		}
	}
	

	
	/**
	 * This method generates an appropriate MIBObject from a given MappedAttribute, and adds
	 * it to this instance's list of mibObjects for writing to the file.
	 */
	
	private void createEntries(){
		if (maList != null){
			for (MappedAttribute ma : maList){
				try{
					if (ma.isAttributeTable())
						tableList.add(new MIBTable(ma));
					else{
						MIBObject mibo = new MIBObject(ma);
						miboList.add(mibo);
					}
				}
				catch (NotEnoughInformationException e){
					e.printStackTrace();
					System.err.println(e.getMessage());
					System.exit(1);
				}
			}
		}
		else {
			maList = new ArrayList<MappedAttribute>(0);
			System.out.println("Attribute parsing was skipped. No attribute MIB definitions written.");
		}
		if (mbList != null){
			for(ManagedBean mb : mbList){
				// we only want to make a table if the MBean's name is a wildcard. 
				ObjectName test = null;
				try{
				test = new ObjectName(mb.getName());
				}
				catch(Exception e){
					e.printStackTrace();
					System.exit(1);
				}
				if (test != null && test.isPattern()){
					try{
						MIBTable mibT = new MIBTable(mb);
						tableList.add(mibT);
					}
					catch (NotEnoughInformationException e){
						e.printStackTrace();
						System.err.println(e.getMessage());
						System.exit(1);
					}
				}
			}
		}
		else { 
			mbList = new AttributeMappings();
			System.out.println("Attribute parsing was skipped. No list of MBeans is available.");
		}
		if (nmList != null){
			for(Mapping nm : nmList){
				try{
					MIBNotification mibN = new MIBNotification(nm);
					mibnList.add(mibN);
				}
				catch (NotEnoughInformationException e){
					e.printStackTrace();
					System.err.println(e.getMessage());
					System.exit(1);
				}
			}
		}
		else {
			nmList = new ArrayList<Mapping>(0);
			System.out.println("Notification parsing was skipped. No notification MIB definitions written.");
		}
	}	

	/* Internal Classes ----- */
	
	
	/** 
	 * Internal class used to represent a single MIB Object. Created by reading the required 
	 * data from a MappedAttribute object, and filling in the rest.
	 */
	private class MIBObject extends MIBEntity{
		private String fullOid;
		private boolean rowEntry; //internally used to avoid adding an OIDDef for a member of an entry
	
		MIBObject(String name, VarBind vb) throws NotEnoughInformationException {
			super();
			this.name = name;
			this.syntax = (vb.getType()!=null) ? vb.getType() : "DisplayString";
			this.maxAccess = "not-accessible";
			this.status = "current";
			this.description = (vb.getDesc()!=null) ? vb.getDesc() : "";
			this.fullOid = vb.getOid();
			String[] temp = fullOid.split("\\.");
			this.objectId = temp[temp.length-1];
			String oidPrefix="";
			for (int i = 0; i<temp.length-1;i++){
				if(i==temp.length-2)
					oidPrefix+=temp[i];
				else
					oidPrefix+=temp[i]+".";
			}
			setOidDef(oidPrefix, null);


		}
		
		MIBObject(MappedAttribute ma) throws NotEnoughInformationException{
			this(ma, false);
		}
			
		MIBObject(MappedAttribute ma, boolean rowEntry) throws NotEnoughInformationException{
			// names must begin with lowercase, or the manager complains / gives warnings
			this.name = ma.getName().substring(0,1).toLowerCase() + ma.getName().substring(1);
			
			this.rowEntry = rowEntry; // default value with this constructor.
			//if the ma has an snmpType defined, we use that as the type. Otherwise we cannot know it, use
			//the general OCTET STRING type.
			this.syntax = (ma.getSnmpType()!=null) ? ma.getSnmpType() : "OCTET STRING (SIZE(0..255))";
					
			//there are more values possible here for an MIB, but not for a JMX attr; we only 
			//need to worry about these possibilities
			this.maxAccess = ma.getMaxAccess();
			
			if (this.maxAccess == null){
				if (ma.isReadWrite()) 
					maxAccess = "read-write";
				else
					maxAccess = "read-only";
			}
			
			this.status = (ma.getStatus()!=null) ? ma.getStatus() : "current";
			
			// perhaps have this as an optional attribute in the attributes.xml? Left as blank 
			// because there is no way to query the server about a given attribute
			this.description = (ma.getSnmpDesc()!= null) ? ma.getSnmpDesc() : "";
			
			String[] temp = ma.getOid().split("\\."); // this will contain the full numerical OID.
			// We need the last element in the array to be the registered OID, so we can put the correct 
			// name in the output MIB
			this.objectId = temp[temp.length-1];
			this.fullOid = ma.getOidPrefix()+"."+this.objectId;
			// check if there is already an OID definition for this prefix. If there is make this MIBObject's 
			// oidDef reflect that
			if (this.rowEntry)
				this.oidDef = ma.getOidDefName();
			else
				setOidDef(ma.getOidPrefix(), ma.getOidDefName());
		}
		
		public String getName(){
			return this.name;
		}
		
		public String getFullOid(){
			return this.fullOid;
		}
		
		public boolean equals(Object o){
			return super.equals(o);
		}
			
		@Override
		public String toString(){
			StringBuffer buf = new StringBuffer();
			buf.append(this.name+" OBJECT-TYPE\n");
			buf.append("\tSYNTAX ").append(this.syntax);
			buf.append("\n");
			buf.append("\tACCESS ").append(this.maxAccess);
			buf.append("\n");
			buf.append("\tSTATUS ").append(this.status);
			buf.append("\n");
			buf.append("\tDESCRIPTION ");
			buf.append("\n\t\t");
			buf.append("\""+this.description+"\"");
			buf.append("\n");
			buf.append("::= {").append(" ");
			buf.append(this.oidDef + " " + this.objectId  + " }");
			buf.append("\n");
			return buf.toString();
		}
	}
	
	/** 
	 * Internal class for generating an MIB Notification from a notification mapping parsed from the notification.xml
	 * @author <a href="mailto:tom.hauser@gmail.com>Tom Hauser 
	 */
	
		private class MIBNotification extends MIBEntity{
			private ArrayList<String> objects;
			
			public MIBNotification(Mapping mp) throws NotEnoughInformationException {
				this.objects = new ArrayList<String>();
				
				this.name = mp.getName();
				if (this.name==null){
					throw new NotEnoughInformationException("The notification "+mp.getNotificationType()+" has no valid name for the MIB.");
				}
				this.status = (mp.getStatus()!=null) ? mp.getStatus() : "current";
				this.description = (mp.getDesc()!=null) ? mp.getDesc() : "";
				ArrayList<VarBind> vbList = (ArrayList<VarBind>)mp.getVarBindList().getVarBindList();
				setObjects(vbList);
				// the OID of a v2 trap = <enterpriseid>.0.<specificid>
				// for predefined traps, see RFC1907 / 3413 / 3418 / http://www.oid-info.com/get/1.3.6.1.6.3.13
				String oidPrefix = mp.getEnterprise()+"."+String.valueOf(mp.getGeneric());
				this.objectId = String.valueOf(mp.getSpecific());
				// check if there is already an OID definition for this prefix. If there is make this MIBObject's 
				// oidDef reflect that
				setOidDef(oidPrefix, mp.getOidDef());

			}
			
			public void setObjects(ArrayList<VarBind> vbList) throws NotEnoughInformationException{
				// there are special conditions in this section that we need to account for, and 
				// define more MIBObjects. They are all of the n:tags and u:tags
				// more changes need to be done 
				Iterator<VarBind>vbIt = vbList.iterator();
				ArrayList<String> oids = new ArrayList<String>(10);
				while(vbIt.hasNext()){
					VarBind vb = vbIt.next();
					oids.add(vb.getOid());			
					if (vb.getTag().matches("^n:.*") || vb.getTag().matches("^u:.*")){
						createNotifPayloadMibo(vb);
					}
				}					
				// have all the oids. compare these to the MIBObject's full OID. if it fails, 
				// put OID into the Objects ArrayList.
				int index = 0;
				Iterator<String> oidIt = oids.iterator();
 
				nextOid: while (oidIt.hasNext()){
					String oidString = oidIt.next();
					for(index=0; index<miboList.size();index++){
						if (oidString.equals(miboList.get(index).getFullOid())){
							this.objects.add(miboList.get(index).getName());
							continue nextOid;
						}
					}
					// if we get here; there is no matching MIBObject for this oid. put UNKNOWNOBJECT here instead.
					//this.objects.add("UNKNOWNOBJECT");
					throw new NotEnoughInformationException("The notification "+this.name+" contains an OID that does not exist in the MIB.");
				}
			}

			//if we found a tag that matches n:.*, we create a new MIBObject so that this object
			//is included in the mib.
			
			public void createNotifPayloadMibo(VarBind vb) throws NotEnoughInformationException{
				String[] tag = vb.getTag().split(":");
				//hacky. maybe a cleaner way to do this later?
				String name = "jbossJmxNotification"+tag[tag.length-1].substring(0,1).toUpperCase()+tag[tag.length-1].substring(1);
				// maybe better way to glean it
				miboList.add(new MIBObject(name,vb));
			}
						
			public String printObjects(){
				StringBuffer buf = new StringBuffer();
				buf.append("{\n");
				for( int index = 0; index < this.objects.size(); index++){
					if (index == this.objects.size()-1)
						buf.append("\t\t"+this.objects.get(index));
					else
						buf.append("\t\t"+this.objects.get(index)).append(",\n");
				}
				
				buf.append("\n\t\t}\n");
				return buf.toString();
			
			}
			
			@Override
			public String toString(){
				StringBuffer buf = new StringBuffer();
				buf.append(this.name+" NOTIFICATION-TYPE\n");
				buf.append("\tOBJECTS ").append(printObjects());
				buf.append("\tSTATUS ").append(this.status);
				buf.append("\n");
				buf.append("\tDESCRIPTION ");
				buf.append("\n\t\t");
				buf.append("\""+this.description+"\"");
				buf.append("\n");
				buf.append("::= { ");
				buf.append(this.oidDef + " " + this.objectId  + " }");
				buf.append("\n");
				return buf.toString();
			}
		}
	
	/**
	 * Internal class for keeping track of an OID Definition (used at the top of an MIB for naming purposes)
	 * see http://www.simpleweb.org/ietf/mibs/modules/IETF/txt/SNMPv2-SMI 
	 * @author thauser
	 *
	 */
		private class OIDDef{
			private String name; // the name of the OID definition
			private String definition; // the OID with '.' replaced by ' ', ready to be output into the MIB
			private String rawOid; // the full dotted-string oid, untouched.
			
			public OIDDef(String name, String oid){
				this.name = name;
				setRawOid(oid);
				setDefinition();
			}
			
			//mutators
			public String getName(){
				return this.name;	
			}

			public void setDefinition(){
				String temp = "{ ";
				temp += this.rawOid;
				temp += "}";
				this.definition = temp;
			}
			
			public void setRawOid(String oid){
				String [] tokens = oid.split("\\.");
				this.rawOid = replaceDottedOid(tokens).trim()+ " ";		
			}
			
			private String replaceDottedOid(String [] tokens){
				String temp = "";
				for (int i = 0; i < tokens.length; i++){
					temp+=tokens[i].trim()+" ";
				}
				return temp;
			}
			
			@Override
			public String toString(){
				StringBuffer buf = new StringBuffer();
				buf.append(this.name);
				buf.append("\t\tOBJECT IDENTIFIER ::= ");
				buf.append(this.definition);
				return buf.toString();
			}
			
			@Override
			public boolean equals(Object o){
				if (this == o){
					return true;
				}
				
				if (o == null){
					return false;
				}
				
				OIDDef that = (OIDDef) o;
				
				return (this.rawOid == that.rawOid);
			}
		}//end OIDDef
	

		/** Internal class representing a Table entry in the MIB **/
		private class MIBTable extends MIBEntity{
			private String tablePrefix;
			private String rowName;
			private MIBTableRow row; // the row created from information contained in 
									 // this MIBTable
			//creating a table out of an attribute 
			public MIBTable(MappedAttribute ma) throws NotEnoughInformationException {
				this.tablePrefix=ma.getName().substring(0,1).toLowerCase() + ma.getName().substring(1);
				this.name = this.tablePrefix+"Table";
				this.maxAccess = "not-accessible";

				this.description = (ma.getSnmpDesc()!=null) ? ma.getSnmpDesc() : "";
				this.rowName = this.tablePrefix+"Entry";
				this.syntax = this.rowName.substring(0,1).toUpperCase() + this.rowName.substring(1);
				this.status = (ma.getStatus()!=null) ? ma.getStatus() : "current";
				setOidDef(ma.getOidPrefix(), ma.getOidDefName());
				String [] temp = ma.getOid().split("\\.");
				// TODO: make this more elegant. 
							
				// HACKS::
				if (temp.length == 3){ // .x.y, .x == TableOid, .y = RowOid
					this.objectId = temp[1];
					row = new MIBTableRow(this.name, this.tablePrefix, this.rowName, temp[2], ma.getMode());
				}
				else{
					this.objectId = temp[temp.length-1];
					row = new MIBTableRow(this.name, this.tablePrefix, this.rowName, "1", ma.getMode());
				}
				
			}
			
			
			// if we find a managedbean with a wildcard object name, we make a table
			public MIBTable(ManagedBean mb) throws NotEnoughInformationException{
				this.tablePrefix = mb.getTableName();
				if (this.tablePrefix == null){
					throw new NotEnoughInformationException("The mbean "+mb.getName()+" has no table-name attribute defined. MIB Generation failed.");
				}
				this.name = this.tablePrefix+"Table";
				this.maxAccess = "not-accessible";
				String oid = mb.getOidPrefix();
				if (oid == null){
					throw new NotEnoughInformationException("The mbean "+mb.getName()+" has no oid-prefix attribute defined. MIB Generation failed.");
				}
				String oidPrefix = "";
				String [] temp = oid.split("\\.");
				this.objectId = temp[temp.length-2];
				for (int i = 0; i < temp.length-2; i++){
					if (i==temp.length-3)
						oidPrefix += temp[i];
					else if (temp[i].equals("."))
						continue;
					else
						oidPrefix += temp[i]+".";
				}
				this.description = (mb.getDesc()!=null) ? mb.getDesc() : "";
				this.rowName = this.tablePrefix+"Entry";
				this.syntax = this.rowName.substring(0,1).toUpperCase() + this.rowName.substring(1);
				this.status = (mb.getStatus()!=null) ? mb.getStatus() : "current";
				setOidDef(oidPrefix, mb.getOidDefinition());
				row = new MIBTableRow(this.name, this.tablePrefix, this.rowName, temp[temp.length-1], (ArrayList<MappedAttribute>)mb.getAttributes());
			}
			
			@Override 
			public String toString(){
				StringBuffer buf = new StringBuffer();
				buf.append(this.name).append(" OBJECT-TYPE").append("\n")
				   .append("\tSYNTAX\tSEQUENCE OF ").append(this.syntax).append("\n")
				   .append("\tMAX-ACCESS\t").append(this.maxAccess).append("\n")
				   .append("\tSTATUS\t").append(this.status).append("\n")
				   .append("\tDESCRIPTION\n\t\t").append("\""+this.description+"\"").append("\n")
				   .append("::= { ").append(this.oidDef+" " +this.objectId+" }\n\n")
				   .append(row);
				return buf.toString();
			}
			
		}
		
		/**Internal class representing a Table Row entry in the MIB **/
	
		private class MIBTableRow extends MIBEntity{
			private ArrayList<MIBObject> rowObjects;
			private String tableName;
			private String tablePrefix;
			private String typeName;
			private String indexName;
			
			//build from a table=true attribute index and contents are generic.
			MIBTableRow(String tableName, String tablePrefix, String rowName, String rowOid, String mode) throws NotEnoughInformationException{
				this.tableName = tableName;
				this.tablePrefix = tablePrefix;
				this.name = rowName;
				this.objectId = rowOid;
				this.typeName = this.name.substring(0,1).toUpperCase() + this.name.substring(1);
				this.syntax = rowName;
				this.maxAccess="not-accessible";
				this.status = "current";
				this.description = "";
				this.rowObjects = new ArrayList<MIBObject>(1);
				MappedAttribute index = new MappedAttribute();
				index.setSnmpType("DisplayString");
				index.setName("index");
				index.setOid(".1");
				index.setMode("ro");
				index.setOidDefName(this.name);
				index.setMaxAccess("not-accessible");
				index.setStatus("current");
				rowObjects.add(new MIBObject(index,true));
				this.indexName = "index";
				MappedAttribute element = new MappedAttribute();
				element.setSnmpType("DisplayString");
				element.setName("element");
				element.setOid(".2");
				element.setMode(mode);
				element.setOidDefName(this.name);
				if (element.isReadWrite())
					element.setMaxAccess("read-write");
				
				else
					element.setMaxAccess("read-only");
				element.setStatus("current");
				rowObjects.add(new MIBObject(element,true));
			}

			// build from ManagedBean, so we have an attrlist.
			MIBTableRow(String tableName, String tablePrefix, String rowName, String rowOid, ArrayList<MappedAttribute> attrList) throws NotEnoughInformationException{
				this.tableName = tableName;
				this.tablePrefix = tablePrefix;
				this.name = rowName;
				this.objectId = rowOid;
				this.typeName = this.name.substring(0,1).toUpperCase() + this.name.substring(1);
				this.syntax = rowName;
				this.maxAccess = "not-accessible";
				this.status = "current"; // doesn't make sense to define a table that is not current 
				this.description = "";
				this.indexName = this.tablePrefix+"ObjectName";
				setRowObjects(attrList);				
			}
			
			private void setRowObjects(ArrayList<MappedAttribute> attrList) throws NotEnoughInformationException{
				this.rowObjects = new ArrayList<MIBObject>(1);
				// check if there's an invalid OID in the attrList, ie: one defined with a .1 oid
				for (MappedAttribute ma: attrList){
					if (ma.getOid().equals(".1")){
						System.out.println("The attribute '"+ma.getName()+"' has an OID of 1. This value is reserved for the index in a table. MIB Generation failed.");
						System.exit(1);
					}
				}
				MappedAttribute objectName = new MappedAttribute();
				objectName.setSnmpType("DisplayString");
				objectName.setName(this.tablePrefix+"ObjectName");
				objectName.setOid(".1");
				objectName.setMode("ro");
				objectName.setOidDefName(this.name);
				objectName.setMaxAccess("not-accessible");
				objectName.setStatus("current");
				rowObjects.add(new MIBObject(objectName, true));
				for (MappedAttribute ma : attrList){
					ma.setOidDefName(this.name);
					ma.setName(this.tablePrefix+ma.getName().substring(0,1).toUpperCase() + ma.getName().substring(1));
					rowObjects.add(new MIBObject(ma,true));
				}
			}
						
			private String writeRowDefinition(){
				StringBuffer buf = new StringBuffer();
				buf.append(this.typeName + " ::= SEQUENCE {\n");
				for (int i = 0; i < rowObjects.size(); i++){
					if(i == rowObjects.size()-1)
						buf.append("\t"+rowObjects.get(i).getName()+"\t"+rowObjects.get(i).getSyntax());	
					else
						buf.append("\t"+rowObjects.get(i).getName()+"\t"+rowObjects.get(i).getSyntax()+",\n");
				}
				buf.append("\n}\n");
				return buf.toString();
			}
			
			@Override 
			public String toString(){
				StringBuffer buf = new StringBuffer();
				buf.append(this.name)          .append(" OBJECT-TYPE")  .append("\n")
				   .append("\tSYNTAX\t")       .append(this.typeName)       .append("\n")
				   .append("\tMAX-ACCESS\t")   .append(this.maxAccess)  .append("\n")
				   .append("\tSTATUS\t")	   .append(this.status)     .append("\n")
				   .append("\tDESCRIPTION\n\t\t").append("\""+this.description+"\"\n")
				   .append("\n\t\t").append("INDEX\t{\n\t\tIMPLIED "+this.indexName+"\n\t\t}\n")         
				   .append("::= { ").append(this.tableName).append(" "+this.objectId)
				   .append(" }\n\n");
				buf.append(writeRowDefinition()+"\n");
				// print the rest of the objects
				for (MIBObject object : rowObjects){
					buf.append(object).append("\n");
				}
				return buf.toString();				
			}
		}
		
		/** Abstract class containing all the different common fields of an entry in the MIB **/
		private abstract class MIBEntity{
			String name;
			String syntax;
			String maxAccess;
			String status;
			String description;
			String oidDef;
			String objectId;
			
			MIBEntity(){
				name="";
				syntax="";
				maxAccess="";
				status="";
				description="";
				oidDef="";
				objectId="";
			}
			
			public String getSyntax(){
				return this.syntax;
			}
			
			public abstract String toString();
				
			public void setOidDef(String oidPrefix, String oidDefName) throws NotEnoughInformationException{
				if (oidDefMap.containsKey(oidPrefix)){
					this.oidDef = oidDefMap.get(oidPrefix).getName();
				}
				else{
					if (oidDefName != null){
						// the name to be used is in the attributes.xml. use it.
						OIDDef newOidDef = new OIDDef(oidDefName, oidPrefix);
						oidDefMap.put(oidPrefix, newOidDef);
						this.oidDef=oidDefName;
					}
					else { // everything failed. the attributes.xml doesn't specify a name for the oid definition to be used,
						   // nor do we already know about it.
						throw new NotEnoughInformationException("There was no definition-name for oid-prefix: "+oidPrefix);
					}
				}
			}
			
			/** 
			 * Compares two MIBEntities, in order to allow us to know if there are duplicates or not, or maybe even use SortedSet.
			 * If the OIDDef and objectId are not equal, then we return false. This is because, no MIB Object can have the same OIDDef and the same objectId.
			 * 
			 * @param o Object to compare to
			 * @return true if equal, false otherwise
			 */
			@Override
			public boolean equals(Object o){
				if (this == o)
					return true;
								
				if (o == null) 
					return false;
				
				MIBEntity test = (MIBEntity) o;
				
				if (this.objectId != null ? !this.objectId.equals(test.objectId) : test.objectId != null) 
					return false;
				
				if (this.oidDef != null ? !this.oidDef.equals(test.oidDef) : test.oidDef != null) 
					return false;
				
				return true;

			}
		}

}// end MIB Generator