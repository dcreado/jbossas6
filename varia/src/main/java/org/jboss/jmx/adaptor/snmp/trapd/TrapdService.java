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
package org.jboss.jmx.adaptor.snmp.trapd;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Vector;

import org.jboss.bootstrap.api.as.config.JBossASServerConfig;
import org.jboss.system.ServiceMBeanSupport;

import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 * MBean wrapper class that acts as an SNMP trap receiver/logger.
 * It logs traps as INFO messages - change log4j configuration to
 * redirect logging output. To reconfigure the listening port
 * the MBean needs to be stopped and re-started.
 *
 * @jmx:mbean
 *    extends="org.jboss.system.ServiceMBean"
 *
 * @version $Revision: 112094 $
 *
 * @author  <a href="mailto:spol@intracom.gr">Spyros Pollatos</a>
 * @author  <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
**/
public class TrapdService 
   extends ServiceMBeanSupport
   implements TrapdServiceMBean, CommandResponder
{
   /** The listening port */
   private int port;

   /** The interface to bind, useful for multi-homed hosts */
   private InetAddress bindAddress;
   
   /** The snmp session used to receive the traps*/
   protected Snmp snmp;
    
   /**
    * Empty CTOR
   **/
   public TrapdService()
   {
       // empty
   }
        
   /**
    * Sets the port that will be used to receive traps
    *
    * @param port the port to listen for traps
    *
    * @jmx:managed-attribute
   **/
   public void setPort(int port)
   {
      this.port = port;
   }

   /**
    * Gets the port that will be used to receive traps
    *
    * @return the port to listen for traps
    *
    * @jmx:managed-attribute
   **/    
   public int getPort()
   {
      return this.port;
   }

   /**
    * Sets the interface that will be bound
    *
    * @param host the interface to bind
    *
    * @jmx:managed-attribute
   **/   
   public void setBindAddress(String host)
      throws UnknownHostException
   {
      this.bindAddress = toInetAddress(host);
   }

   /**
    * Gets the interface that will be bound
    *
    * @return the interface to bind
    * 
    * @jmx:managed-attribute
   **/      
   public String getBindAddress()
   {
      String address = null;
      
      if (this.bindAddress != null)
         address = this.bindAddress.getHostAddress();
      
      return address;
   }
   
   /**
    * Performs service start-up by instantiating an SnmpTrapSession
   **/
   protected void startService()
      throws Exception
   {
      // Create the SNMP trap receiving session with the logging handler,
      // using Logger inherited from ServiceMBeanSupport
      
         // cater for possible global -b option, if no override has been specified
         InetAddress address = this.bindAddress != null ? this.bindAddress :
               toInetAddress(System.getProperty(JBossASServerConfig.PROP_KEY_JBOSSAS_BIND_ADDRESS));
         
         MessageDispatcher mtDispatcher = new MessageDispatcherImpl();
//
//         // add message processing models
         mtDispatcher.addMessageProcessingModel(new MPv1());
         mtDispatcher.addMessageProcessingModel(new MPv2c());
         mtDispatcher.addMessageProcessingModel(new MPv3());
         TransportMapping transport =
        	    new DefaultUdpTransportMapping(new UdpAddress(address,port));
         
         this.snmp = new Snmp(mtDispatcher,transport);
         
         //start test
//       OctetString localEngineID =
//       	      new OctetString(snmp.getLocalEngineID());
//       USM usm = new USM(SecurityProtocols.getInstance(), localEngineID, 0);
//       SecurityModels.getInstance().addSecurityModel(usm);
       //end test
        	   
//        	this.snmp = new Snmp(mtDispatcher, transport);

        	snmp.addCommandResponder(this);
        	snmp.listen();
        	log.debug("listening on: "+address);
   }
    
   /**
    * Performs service shutdown by stopping SnmpTrapSession
   **/
   protected void stopService()
      throws Exception
   {
	   snmp.close();
   }
   
   /**
    * Safely convert a host string to InetAddress or null
    */
   private InetAddress toInetAddress(String host)
      throws UnknownHostException
   {
      if (host == null || host.length() == 0)
         return null;
      else
         return InetAddress.getByName(host);
   }

	@Override
	public void processPdu(CommandResponderEvent e) {
		PDU pdu = e.getPDU();
		if (pdu != null){
			
			if (pdu instanceof PDUv1){
				processPDUv1((PDUv1)pdu);
			}
			else if (pdu instanceof ScopedPDU) {
				processScopedPDU((ScopedPDU)pdu);
			} 
			else if (pdu instanceof PDU){
				processPDUv2c(pdu);
			}
			else {
				log.warn("Unknown PDU type: " + PDU.getTypeString(pdu.getType()));
			}
			
		}
	}
	
	private void processPDUv2c(PDU pdu){
	      StringBuffer sbuf = new StringBuffer();
	      sbuf.append("\nV2 Trap from agent ").append(this.bindAddress.toString());
	      sbuf.append(" on port ").append(this.port);
	      sbuf.append("\n");
		
	      Vector bindings = pdu.getVariableBindings();
	      Iterator it = bindings.iterator();
	      int counter = 0;
	      while (it.hasNext())
	      {
	         VariableBinding vb = (VariableBinding)it.next();
	         if (vb != null) {
		         sbuf.append("Varbind[").append(counter++).append("] := ");
		         sbuf.append(vb.getOid().toString()).append(" --> ");
		         sbuf.append(vb.getVariable().toString()).append("\n");
	         }
	      }
	      sbuf.append("\nLength............. ").append(counter);
	      log.debug(sbuf.toString());
	}

	private void processScopedPDU(ScopedPDU pdu) {
		//****
	      StringBuffer sbuf = new StringBuffer();
	      sbuf.append("\nV3 Trap from agent ").append(this.bindAddress.toString());
	      sbuf.append(" on port ").append(this.port);
	      sbuf.append("\n");
		
	      Vector bindings = pdu.getVariableBindings();
	      Iterator it = bindings.iterator();
	      int counter = 0;
	      while (it.hasNext())
	      {
	         VariableBinding vb = (VariableBinding)it.next();
	         if (vb != null) {
		         sbuf.append("Varbind[").append(counter++).append("] := ");
		         sbuf.append(vb.getOid().toString()).append(" --> ");
		         sbuf.append(vb.getVariable().toString()).append("\n");
	         }
	      }
	      sbuf.append("\nLength............. ").append(counter);
	      log.debug(sbuf.toString());
		
	}

	private void processPDUv1(PDUv1 pdu) {
	      StringBuffer sbuf = new StringBuffer();
	      sbuf.append("\nV1 Trap from agent ").append(this.bindAddress.toString());
	      sbuf.append(" on port ").append(this.port);
	      sbuf.append("\nIP Address......... ").append(pdu.getAgentAddress().toString());
	      sbuf.append("\nEnterprise Id...... ").append(pdu.getEnterprise());
	      sbuf.append("\nGeneric ........... ").append(pdu.getGenericTrap());
	      sbuf.append("\nSpecific .......... ").append(pdu.getSpecificTrap());
	      sbuf.append("\nTimeStamp ......... ").append(pdu.getTimestamp());
	      
	      sbuf.append("\n");
		
	      Vector bindings = pdu.getVariableBindings();
	      Iterator it = bindings.iterator();
	      int counter = 0;
	      while (it.hasNext())
	      {
	         VariableBinding vb = (VariableBinding)it.next();
	         if (vb != null) {
		         sbuf.append("Varbind[").append(counter++).append("] := ");
		         sbuf.append(vb.getOid().toString()).append(" --> ");
		         sbuf.append(vb.getVariable().toString()).append("\n");
	         }
	      }
	      sbuf.append("\nLength............. ").append(counter);
	      log.debug(sbuf.toString());
	}
    
} // class TrapdService
