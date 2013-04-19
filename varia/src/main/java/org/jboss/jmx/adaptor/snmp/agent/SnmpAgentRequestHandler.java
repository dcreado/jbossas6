package org.jboss.jmx.adaptor.snmp.agent;

import java.net.InetAddress;

import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.smi.OctetString;

/**
* <P>The SnmpAgentRequestHandler interface is implemented by an object that
* wishs to receive callbacks when an SNMP protocol data unit
* is received from a manager.</P>
*
*/
public interface SnmpAgentRequestHandler
{
/**
* <P>This method is defined to handle SNMP requests
* that are received by the session. The parameters
* allow the handler to determine the host, port, and
* community string of the received PDU</P>
*
* @param session The SNMP session
* @param manager The remote sender
* @param port    The remote senders port
* @param community  The community string
* @param pdu     The SNMP pdu
*
*/
void snmpReceivedPdu(Snmp session,
                 InetAddress      manager,
                 int              port,
                 OctetString  community,
                 PDU    pdu);

/**
* <P>This method is defined to handle SNMP Get requests
* that are received by the session. The request has already
* been validated by the system.  This routine will build a
* response and pass it back to the caller.</P>
*
* @param pdu     The SNMP pdu
* @param getNext The agent is requesting the lexically NEXT item after each
*                    item in the pdu. *** THIS IS NO LONGER REQUIRED. REMOVED.***
*
* @return PDU filled in with the proper response, or null if cannot process. PDU's
* version is based on the @param pdu.
*  		  
* NOTE: this might be changed to throw an exception.
*/
PDU snmpReceivedGet(PDU pdu);

/**
 * <P> This method handles SNMP Get Bulk requests received by the session
 * Builds a response PDU and passes it back to the caller.
 * </P
 * 
 * @param pdu The SNMP pdu
 * @return PDU filled with the proper response. This PDU is either V2c or V3.
 * 			this PDU will always try to ignore errors and fill with as much info
 * 			as possible.
 */

PDU snmpReceivedGetBulk(PDU pdu);

/**
* <P>This method is defined to handle SNMP Set requests
* that are received by the session. The request has already
* been validated by the system.  This routine will build a
* response and pass it back to the caller.</P>
*
* @param pdu     The SNMP pdu
*
* @return PDU filled in with the proper response, or null if cannot process
* NOTE: this might be changed to throw an exception.
*/
PDU snmpReceivedSet(PDU pdu);
//ResponseEvent maybe

///**
//* <P>This method is invoked if an error occurs in 
//* the session. The error code that represents
//* the failure will be passed in the second parameter,
//* 'error'. The error codes can be found in the class
//* SnmpAgentSession class.</P>
//*
//* <P>If a particular PDU is part of the error condition
//* it will be passed in the third parameter, 'pdu'. The
//* pdu will be of the type SnmpPduRequest or SnmpPduTrap
//* object. The handler should use the "instanceof" operator
//* to determine which type the object is. Also, the object
//* may be null if the error condition is not associated
//* with a particular PDU.</P>
//*
//* @param session The SNMP Session
//* @param error   The error condition value.
//* @param ref     The PDU reference, or potentially null.
//*                It may also be an exception.
//*/
//void SnmpAgentSessionError(Snmp session, 
//                       int              error,
//                       Object           ref);
}