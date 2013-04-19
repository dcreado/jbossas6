package org.jboss.jmx.adaptor.snmp.agent;

/**	
 * This exception is thrown when a client attempts to either GET a variable that 
 * is of a type not supported by our SNMP adaptor (hopefully very few types eventually),
 * or if the client attempts to SET an attribute of type T to something of a type that is not T 
 * (ie a String to an int)
 * 
 * @author Thomas Hauser <a href="mailto:thauser@redhat.com"></a>
 *
 */

public class VariableTypeException extends Exception{
	
	public VariableTypeException(){
		super("Error: Variable type mismatch.");
	}

	public VariableTypeException(String m){
		super(m);
	}
}
