package org.jboss.jmx.adaptor.snmp.agent;

/** This exception is thrown when a GETNEXT or GETBULK operation
 *  attempts to get an OID that is not in the MIB
 *  
 * @author Thomas Hauser <a href="mailto:thauser@redhat.com"></a>
 *
 */

public class EndOfMibViewException extends Exception {
	
	public EndOfMibViewException(){
		super("Traversal of the tree left the subtree.");
			
	}

	public EndOfMibViewException(String m){
		super(m);
	}
}
