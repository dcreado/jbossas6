/*
 * Copyright (c) 2003,  Intracom S.A. - www.intracom.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * This package and its source code is available at www.jboss.org
**/
package org.jboss.jmx.adaptor.snmp.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

import javax.management.Notification;
import org.jboss.jmx.adaptor.snmp.agent.EventTypes;
import org.jboss.system.ServiceMBeanSupport;
import org.snmp4j.CommunityTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 * <tt>NotificationProducerService</tt> is a test class with an MBean interface
 * used to produce simple JMX notifications to be intercepted and mapped to SNMP
 * traps by the snmp JMX adaptor
 * 
 * @version $Revision: 112094 $
 *
 * @author  <a href="mailto:spol@intracom.gr">Spyros Pollatos</a>
 * @author  <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 *
 * @jmx:mbean
 *    extends="org.jboss.system.ServiceMBean"
**/
public class NotificationProducerService  
   extends ServiceMBeanSupport
   implements NotificationProducerServiceMBean
{

	/** Notification types for testing */ 
	   public final String V1_TEST_NOTIFICATION = "jboss.snmp.agent.v1test";
	   public final String V2_TEST_NOTIFICATION = "jboss.snmp.agent.v2test";
	   public final String V3_TEST_NOTIFICATION = "jboss.snmp.agent.v3test";
	   
   /**
    * Sends a test Notification of type "V1"
    *
    * @jmx:managed-operation
   **/    
   public void sendV1()
      throws Exception
   {
	   log.debug("Sending SnmpV1 test notification");
	   
	   sendNotification(
         new Notification(V1_TEST_NOTIFICATION, this, getNextNotificationSequenceNumber(),
                          "V1 test notifications")); 
   }

   /**
    * Sends a test Notification of type "V2"
    *
    * @jmx:managed-operation
   **/          
   public void sendV2()
      throws Exception
   {
	   log.debug("Sending SnmpV2 test notification");
	   sendNotification(
         new Notification(V2_TEST_NOTIFICATION, this, getNextNotificationSequenceNumber(),
                          "V2 test notifications"));        
   }
   
   /**
    * Sends a test Notification of type "V3"
    *
    * @jmx:managed-operation
   **/          
   public void sendV3()
      throws Exception
   {
	   log.debug("Sending SnmpV3 test notification");
	   sendNotification(
         new Notification(V3_TEST_NOTIFICATION, this, getNextNotificationSequenceNumber(),
                          "V3 test notifications"));        
   }
   
   public void getBulk(){
	   log.debug("Executing GETBULK...");
	   PDU pdu = new PDU();
		pdu.setType(PDU.GETBULK);
		pdu.add(new VariableBinding(new OID("1.2.3.4.1.1")));
		pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.1.0")));
		pdu.add(new VariableBinding(new OID("1.3.1.1")));
		pdu.setMaxRepetitions(7);
		pdu.setNonRepeaters(1);
		CommunityTarget target = new CommunityTarget();
		OctetString community = new OctetString("public");
		target.setCommunity(community);
		target.setVersion(SnmpConstants.version2c);
		Address targetAddress = GenericAddress.parse("udp:127.0.0.1/1161");
		target.setAddress(targetAddress);
		target.setRetries(2);
		target.setTimeout(2000);
	
		try {
			DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping();
			transport.listen();
			System.out.println("READY: "+System.currentTimeMillis());
			Snmp snmp = new Snmp(transport);
			ResponseEvent responseEvent = snmp.send(pdu, target);
			System.out.println("response " + responseEvent.toString());

			PDU responsePDU = responseEvent.getResponse();
			if (responsePDU == null){
				System.out.println("Request timed out");
			}
			else{
				System.out.println("Received response "+responsePDU);
			}
				System.out.println("Peer Address: "+responseEvent.getPeerAddress());
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e) {
			System.out.println("Some Other exception!!");
		}
	   	   
   }
   
   /**
    * Sends a test GETNEXT request
    */
   public void getNext(){
		PDU pdu = new PDU();
		pdu.setType(PDU.GETNEXT);
		pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.1")));	
		
		
		CommunityTarget target = new CommunityTarget();
		OctetString community = new OctetString("public");
		target.setCommunity(community);
		target.setVersion(SnmpConstants.version2c);
		Address targetAddress = GenericAddress.parse("udp:127.0.0.1/1161");
		target.setAddress(targetAddress);
		target.setRetries(2);
		target.setTimeout(2000);
		try {
			DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping();
			transport.listen();
			System.out.println("READY: "+System.currentTimeMillis());
			Snmp snmp = new Snmp(transport);
			ResponseEvent responseEvent = snmp.send(pdu, target);
			System.out.println("response " + responseEvent.toString());
			PDU responsePDU = responseEvent.getResponse();
			if (responsePDU == null){
				System.out.println("Request timed out");
			}
			else{
				System.out.println("Received response "+responsePDU);
			}
				System.out.println("Peer Address: "+responseEvent.getPeerAddress());
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e) {
			System.out.println("Some Other exception!!");
		}
	   }
   
   /**
    * Sends a test GET request
    * 
    * @jmx:managed-operation
    */
   public void get(){
	   PDU pdu = new PDU();
	   pdu.setType(PDU.GET);
	   pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.1.4")));	
	   	   
	   CommunityTarget target = new CommunityTarget();
	   target.setCommunity(new OctetString("public"));
	   target.setVersion(SnmpConstants.version2c);
	   Address targetAddress = GenericAddress.parse("udp:127.0.0.1/1161");	   
	   target.setAddress(targetAddress);
       target.setRetries(2);
       target.setTimeout(2000);

	   try {
			DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping();

			transport.listen();
			System.out.println("READY: "+System.currentTimeMillis());
			Snmp snmp = new Snmp(transport);
			ResponseEvent responseEvent = snmp.send(pdu, target);
			System.out.println("response " + responseEvent.toString());
	        PDU responsePDU = responseEvent.getResponse();
	        if (responsePDU == null) {
            System.out.println("Request timed out");
	        }
	        else {
	    //        System.out.println("Received response "+response);
	        }
			System.out.println("Peer Address: "+responseEvent.getPeerAddress());
			System.out.println("responsePdu = " + responsePDU);

	} catch (UnknownHostException e1) {
		e1.printStackTrace();
	} catch (IOException e1) {
		e1.printStackTrace();
	} catch (Exception e) {
		System.out.println("Some Other exception!!" + e);
	}
	
   }
   
   /**
    * Sends a test SET request
    * 
    * @jmx:managed-operation
    */
   public void set(){
	   PDU pdu = new PDU();
       pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.1.1"), new OctetString("MBean Set Test")));	

       pdu.setType(PDU.SET);

       CommunityTarget target = new CommunityTarget();
       target.setCommunity(new OctetString("private"));
       target.setVersion(SnmpConstants.version2c);
       Address targetAddress = GenericAddress.parse("udp:127.0.0.1/1161");
       target.setAddress(targetAddress);
       target.setRetries(2);
       target.setTimeout(1000);
	try{
		DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping();
		transport.listen();
		System.out.println("READY: "+System.currentTimeMillis());
		Snmp snmp = new Snmp(transport);	
        ResponseEvent responseEvent = snmp.set(pdu, target);
        System.out.println("response " + responseEvent.toString());
	
	PDU responsePDU = responseEvent.getResponse();

	if (responsePDU == null){
		System.out.println("Request timed out.");
	}
	else {
		System.out.println("Received response "+responsePDU);
            }
                    System.out.println("Peer Address: "+responseEvent.getPeerAddress());


	} catch (UnknownHostException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} catch (Exception e) {
		System.out.println("Some Other exception!!");
	}
	
	   
   }
   
} // class NotificationProducerService

