package org.jboss.jmx.adaptor.snmp.agent;

import org.snmp4j.smi.OID;

/** This exception is thrown when a manager attempts to retrieve information 
 *  about an OID that does not exist in the attributes.xml
 *  
 *  @author Thomas Hauser <a href="mailto:thauser@redhat.com"></a>
 */

public class NoSuchObjectException extends Exception {
	
	public NoSuchObjectException(){
		super("Error: Requested an OID that does not exist.");
	}

	public NoSuchObjectException(OID oid){
		super("Error: The following OID does not exist: "+oid);
	}
}
