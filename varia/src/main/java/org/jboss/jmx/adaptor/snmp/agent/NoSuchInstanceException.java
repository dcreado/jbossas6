package org.jboss.jmx.adaptor.snmp.agent;

import org.snmp4j.smi.OID;

/** This exception is thrown when a manager attempts to retrieve an instance of an object  
 *  that does not exist.
 *  
 *  @author Thomas Hauser <a href="mailto:thauser@redhat.com"></a>
 */

public class NoSuchInstanceException extends Exception {
	
	public NoSuchInstanceException(){
		super("Error: Requested an Instance of an Object that does not exist.");
	}

	public NoSuchInstanceException(OID oid){
		super("Error: The following Instance does not exist: "+oid);
	}
}
