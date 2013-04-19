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
package org.jboss.jmx.adaptor.snmp.agent;

import java.util.ArrayList;
import java.util.List;

import javax.management.Notification;

import org.jboss.jmx.adaptor.snmp.config.notification.Mapping;
import org.jboss.jmx.adaptor.snmp.config.notification.VarBind;
import org.jboss.jmx.adaptor.snmp.config.notification.VarBindList;
import org.jboss.logging.Logger;
import org.jboss.xb.binding.GenericObjectModelFactory;
import org.jboss.xb.binding.UnmarshallingContext;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.ScopedPDU;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.VariableBinding;
import org.xml.sax.Attributes;

/**
 * <tt>TrapFactorySupport</tt> takes care of translation of Notifications
 * into SNMP V1 and V2 traps
 *
 * Data Structure Guide
 *
 * It looks complicated but it ain't. The mappings are read into a structure
 * that follows the outline defined in the Notification.xsd. Have a look
 * there and in the example notificationMap.xml and you should get the picture. 
 * As an optimization, 2 things are done:
 *
 * 1.   The "NotificationType" fields of all the mappings are 
 *      read, interpreted and compiled as regular expressions. All the 
 *      instances are placed in an array and made accessible in their compiled 
 *      form
 * 2.   The "wrapperClass" attribute is interpreted as a class name that 
 *      implements interface NotificationWrapper. An instance of each class is 
 *      created and similarly placed in an array 
 *
 * This results in 2 collections one of regular expressions and one of 
 * NotificationWrapper instances. The two collections have exactly the same
 * size as the collection of mappings. Obviously each read mapping has a "1-1"
 * correspondence with exactly 1 compiled regular expression and exactly 1
 * NotificationWrapper instance. The key for the correspondence is the index: 
 * regular expression i corresponds to mapping i that coresponds to 
 * NotificationWrapper instance i. The loading of the 2 collections is 
 * performed in method startService.
 * Checking for which mapping to apply (implemented in method findMapping) on a 
 * notification is simple: traverse the cached regular expressions and attempt 
 * to match the notification type against them. The FIRST match short circuits 
 * the search and the coresponding mapping index is returned.
 *
 * @version $Revision: 112094 $
 *
 * @author  <a href="mailto:spol@intracom.gr">Spyros Pollatos</a>
 * @author  <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
**/
public class TrapFactorySupport
   implements TrapFactory
{
   /** The logger object */
   private static final Logger log = Logger.getLogger(TrapFactorySupport.class);

   /** Reference to SNMP variable binding factory */
   private SnmpVarBindFactory snmpVBFactory = null;
   
   /** Uptime clock */
   private Clock clock = null;
   
   /** Trap counter */
   private Counter trapCount = null;   
   
   /** Request handler ref */
   private RequestHandler requestHandler = null;
   
   /**
    * Create TrapFactorySupport
   **/
   public TrapFactorySupport()
   {
      this.snmpVBFactory = new SnmpVarBindFactory();
   }

   /**
    * Sets the name of the file containing the notification/trap mappings,
    * the uptime clock and the trap counter
   **/ 
   public void set(Clock clock, Counter count)
   {
      this.clock = clock;
      this.trapCount = count;
   }
   
   public void set(Clock clock, Counter count, RequestHandler rh){
	   this.clock = clock;
	   this.trapCount = count;
	   this.requestHandler = rh;
   }
   /**
    * Populates the regular expression and wrapper instance collections. Note 
    * that a failure (e.g. to compile a regular expression or to instantiate a 
    * wrapper) generates an error message. Furthermore, the offending 
    * expression or class are skipped and the corresponding collection entry 
    * is null. It is the user's responsibility to track the reported errors in 
    * the logs and act accordingly (i.e. correct them and restart). If not the 
    * corresponding mappings are effectively void and will NOT have effect. 
   **/    
   public void start()
      throws Exception
   {
      
      log.debug("Trap factory going active");                                                       
   }
    
   /**
    * Traslates a Notification to an SNMP V1 trap.
   **/
   public PDUv1 generateV1Trap(Notification n, Mapping m, NotificationWrapper wrapper) 
      throws MappingFailedException
   {
      if (log.isTraceEnabled())
         log.trace("generateV1Trap");             
        
      // Create trap
      PDUv1 trapPdu = new PDUv1();
        
      trapPdu.setTimestamp(this.clock.uptime());
      trapPdu.setType(PDU.V1TRAP);
        
      // Organise the 'variable' payload 
      trapPdu.setGenericTrap(m.getGeneric());
      trapPdu.setSpecificTrap(m.getSpecific());
      trapPdu.setEnterprise(new OID(m.getEnterprise()));
        
      // Append the specified varbinds. Get varbinds from mapping and for
      // each one of the former use the wrapper to get the corresponding
      // values      
        
      if(wrapper != null)
      {
         // Prime the wrapper with the notification contents
         wrapper.prime(n);
            
         // Iterate through mapping specified varbinds and organise values
         // for each
         List vbList = m.getVarBindList().getVarBindList();
         
         for (int i = 0; i < vbList.size(); i++)
         {
            VarBind vb = (VarBind)vbList.get(i);
            
            if (vb.getTag().matches("^a:.*")){
            	OID oid = new OID(vb.getOid()+".0");
            	Variable var;
            	try{
            		var = requestHandler.getValueFor(oid);
            	}
            	catch (Exception e){
            		var = null;
            	}
            
            	if (var != null){
              		trapPdu.add(new VariableBinding(oid, var));
            	}
            	else { // we failed to find the variable with given oid here. we fall back onto the wrapper.
                 	trapPdu.add(
                 			this.snmpVBFactory.make(vb.getOid(), wrapper.get(vb.getTag())));
            	}
            }
            else{
                // Append the var bind. Interrogate read vb for OID and 
                // variable tag. The later is used as the key passed to the 
                // wrapper in order for it to locate the required value. That 
                // value and the aforementioned OID are used to generate the 
                // variable binding
                	trapPdu.add(
                		this.snmpVBFactory.make(vb.getOid(), wrapper.get(vb.getTag())));
            }
         }
      }
      else
      {
         throw new MappingFailedException(
            "Varbind mapping failure: null wrapper defined for " +
            " notification type '" + m.getNotificationType() + "'" );
      }
      return trapPdu;        
   }
    
   /**
    * Traslates a Notification to an SNMP V2c trap.
    *
    * TODO: how do you get timestamp, generic, and specific stuff in the trap
   **/
   public PDU generateV2cTrap(Notification n, Mapping m, NotificationWrapper wrapper) 
      throws MappingFailedException
   {
      if (log.isTraceEnabled())
         log.trace("generateV2cTrap");
      
      // Create trap
      PDU trapPdu = new PDU();
      if(m.isInform()) {
      	trapPdu.setType(ScopedPDU.INFORM);    	
      } else {
      	trapPdu.setType(ScopedPDU.TRAP);
      }
        
      // Those 2 Variable Bindings are mandatory for v2c and v3 traps and inform
      trapPdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(this.clock.uptime())));
      // For generic traps, values are defined in RFC 1907, for vendor specific traps snmpTrapOID is essentially a concatenation of the SNMPv1 Enterprise parameter and two additional sub-identifiers, '0', and the SNMPv1 Specific trap code parameter.
      trapPdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(m.getEnterprise() + ".0." + m.getSpecific())));
      
      // Append the specified varbinds. Get varbinds from mapping and for
      // each one of the former use the wrapper to get data from the 
      // notification
      if (wrapper != null)
      {
         // Prime the wrapper with the notification contents
         wrapper.prime(n);
            
         List vbList = m.getVarBindList().getVarBindList();
         
         for (int i = 0; i < vbList.size(); i++)
         {
             VarBind vb = (VarBind)vbList.get(i);
             
             if (vb.getTag().matches("^a:.*")){
             	OID oid = new OID(vb.getOid()+".0");
             	Variable var;
             	try{
             		var = requestHandler.getValueFor(oid);
             	}
             	catch (Exception e){
             		var = null;
             	}
             
             	if (var != null){
               		trapPdu.add(new VariableBinding(oid, var));
             	}
             	else { // we failed to find the variable with given oid here. we fall back onto the wrapper.
                  	trapPdu.add(
                  			this.snmpVBFactory.make(vb.getOid(), wrapper.get(vb.getTag())));
             	}
             }
             else{
                 // Append the var bind. Interrogate read vb for OID and 
                 // variable tag. The later is used as the key passed to the 
                 // wrapper in order for it to locate the required value. That 
                 // value and the aforementioned OID are used to generate the 
                 // variable binding
                 	trapPdu.add(
                 		this.snmpVBFactory.make(vb.getOid(), wrapper.get(vb.getTag())));
             }
         }
      }
      else
      {
         log.warn("Varbind mapping failure: null wrapper defined for " +
                  " notification type '" + m.getNotificationType() + "'" );
      }
      return trapPdu;
   }
   
   /**
    * Traslates a Notification to an SNMP V3 trap.
    *
    * TODO: how do you get timestamp, generic, and specific stuff in the trap
   **/
   public ScopedPDU generateV3Trap(Notification n, Mapping m, NotificationWrapper wrapper) 
      throws MappingFailedException
   {
	   if (log.isTraceEnabled())
       log.trace("generateV3Trap");
    
    // Create trap
    ScopedPDU trapPdu = new ScopedPDU();
    if(m.isInform()) {
    	trapPdu.setType(ScopedPDU.INFORM);    	
    } else {
    	trapPdu.setType(ScopedPDU.TRAP);
    }
      
    // Those 2 Variable Bindings are mandatory for v2c and v3 traps and inform
    trapPdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(this.clock.uptime())));
    // For generic traps, values are defined in RFC 1907, for vendor specific traps snmpTrapOID is essentially a concatenation of the SNMPv1 Enterprise parameter and two additional sub-identifiers, '0', and the SNMPv1 Specific trap code parameter.
    trapPdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(m.getEnterprise() + ".0." + m.getSpecific())));        
    
    // Append the specified varbinds. Get varbinds from mapping and for
    // each one of the former use the wrapper to get data from the 
    // notification
    if (wrapper != null) {
       // Prime the wrapper with the notification contents
       wrapper.prime(n);
          
       List vbList = m.getVarBindList().getVarBindList();
       
       for (int i = 0; i < vbList.size(); i++) {
           VarBind vb = (VarBind)vbList.get(i);
           
           if (vb.getTag().matches("^a:.*")){
           	OID oid = new OID(vb.getOid()+".0");
           	Variable var;
           	try{
           		var = requestHandler.getValueFor(oid);
           	}
           	catch (Exception e){
           		var = null;
           	}
           
           	if (var != null){
             		trapPdu.add(new VariableBinding(oid, var));
           	}
           	else { // we failed to find the variable with given oid here. we fall back onto the wrapper.
                	trapPdu.add(
                			this.snmpVBFactory.make(vb.getOid(), wrapper.get(vb.getTag())));
           	}
           }
           else{
               // Append the var bind. Interrogate read vb for OID and 
               // variable tag. The later is used as the key passed to the 
               // wrapper in order for it to locate the required value. That 
               // value and the aforementioned OID are used to generate the 
               // variable binding
               	trapPdu.add(
               		this.snmpVBFactory.make(vb.getOid(), wrapper.get(vb.getTag())));
           }
       }
    } else {
       log.warn("Varbind mapping failure: null wrapper defined for " +
                " notification type '" + m.getNotificationType() + "'" );
    }
    return trapPdu;
   }
   
} // class TrapFactorySupport
