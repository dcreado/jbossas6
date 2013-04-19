package org.jboss.jmx.adaptor.snmp.generator.exceptions;
/** This exception is thrown when there is not enough information inside the input files 
 *  to create a complete, valid MIB.
 *  
 * @author Thomas Hauser <a href="mailto:thauser@redhat.com"></a>
 *
 */
public class NotEnoughInformationException extends Exception {

	public NotEnoughInformationException(){
		super("Incomplete information in the parsed files. MIB cannot be created.");
	}
	
	public NotEnoughInformationException(String m){
		super(m);
	}
	
}