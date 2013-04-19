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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.management.Notification;

import org.jboss.bootstrap.api.as.config.JBossASServerConfig;
import org.jboss.jmx.adaptor.snmp.config.manager.Manager;
import org.jboss.jmx.adaptor.snmp.config.notification.Mapping;
import org.jboss.jmx.adaptor.snmp.config.notification.VarBind;
import org.jboss.jmx.adaptor.snmp.config.notification.VarBindList;
import org.jboss.jmx.adaptor.snmp.config.user.User;
import org.jboss.logging.Logger;
import org.jboss.xb.binding.GenericObjectModelFactory;
import org.jboss.xb.binding.MappingObjectModelFactory;
import org.jboss.xb.binding.ObjectModelFactory;
import org.jboss.xb.binding.Unmarshaller;
import org.jboss.xb.binding.UnmarshallerFactory;
import org.jboss.xb.binding.UnmarshallingContext;
import org.snmp4j.CommunityTarget;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.UserTarget;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.AbstractTransportMapping;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.xml.sax.Attributes;

/**
 * <tt>TrapEmitter</tt> is a class that manages SNMP trap emission.
 *
 * Currently, it allows to send V1 or V2 traps to one or more subscribed SNMP
 * managers defined by their IP address, listening port number and expected
 * SNMP version.
 *
 * @version $Revision: 112094 $
 *
 * @author  <a href="mailto:spol@intracom.gr">Spyros Pollatos</a>
 * @author  <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
**/
public class TrapEmitter
{
   /** The logger object */
   private static final Logger log = Logger.getLogger(TrapEmitter.class);
   
   /** Reference to the utilised trap factory*/
   private TrapFactory trapFactory = null;
   
   private SnmpAgentService snmpAgentService;
   
   /** Holds the manager subscriptions. Accessed through synch'd wrapper */
   private Set managers = Collections.synchronizedSet(new HashSet());  
    
   /** Contains the read in mappings */
   private ArrayList notificationMapList = null;
    
   /** Contains the compiled regular expression type specifications */
   private ArrayList mappingRegExpCache = null;
   
   /** Contains instances of the notification wrappers */
   private ArrayList notificationWrapperCache = null;
   
   /**
    * Builds a TrapEmitter object for sending SNMP V1 or V2 traps. <P>
   **/
   public TrapEmitter(SnmpAgentService snmpAgentService) {
      this.snmpAgentService = snmpAgentService;
   }
    
   /**
    * Complete emitter initialisation
   **/               
   public void start()
      throws Exception
   {
      // Load persisted manager subscriptions
      load();
      
      // Instantiate the trap factory
      this.trapFactory = (TrapFactory) Class.forName(this.snmpAgentService.getTrapFactoryClassName(),
                                                     true,
                                                     this.getClass().getClassLoader()).newInstance();
      
      // Initialise
      this.trapFactory.set(this.snmpAgentService.getClock(),
                           this.snmpAgentService.getTrapCounter(),
                           this.snmpAgentService.getRequestHandler());
      
      // Start the trap factory
      this.trapFactory.start();
   }
    
   /**
    * Perform shutdown
   **/
   public void stop()
      throws Exception
   {
      synchronized(this.managers) {
            
         // Drop all held manager records
         this.managers.clear();
      }
   }
    
   /**
    * Intercepts the notification and after translating it to a trap sends it
    * along.
    *
    * @param n notification to be sent
    * @throws Exception if an error occurs during the preparation or
    * sending of the trap
   **/    
   public void send(Notification n) throws Exception {
      // Beeing paranoid
      synchronized(this.trapFactory) {
         if(this.trapFactory == null) {
            log.error("Received notifications before trap factory set. Discarding.");
            return;     
         }
      }
      
      // Locate mapping for incomming event
      int index = findMappingIndex(n);
      if(index < 0) {
    	  log.debug("No SNMP notifications configured for Notification " + n.getType() + " doing nothing");
    	  return;
      }
        
      Mapping m = (Mapping)this.notificationMapList.get(index);
      // Get the coresponding wrapper to get access to notification payload
      NotificationWrapper wrapper =
         (NotificationWrapper)this.notificationWrapperCache.get(index);     
      
      // Cache the translated notification
      PDUv1 v1TrapPdu = null; 
      PDU v2cTrapPdu = null; 
      ScopedPDU v3TrapPdu = null;
      
      if(managers.size() > 0) {
          
	      // Send trap. Synchronise on the subscription collection while 
	      // iterating 
	      synchronized(this.managers) {     	  
	         // Iterate over sessions and emit the trap on each one
	         Iterator i = this.managers.iterator();
	         while (i.hasNext()) {

	        	Target t = (Target)i.next();	        		        	
	        	
	            try {  	            	
	            	switch (t.getVersion()) {
	            		case SnmpConstants.version1:
		                    if (v1TrapPdu == null)
		                    	v1TrapPdu = this.trapFactory.generateV1Trap(n, m, wrapper);
		                     
		                    sendTrap(v1TrapPdu, t, m.getSecurityName());
		                    break;
		                  
	               		case SnmpConstants.version2c:
	               			if (v2cTrapPdu == null)
	               				v2cTrapPdu = this.trapFactory.generateV2cTrap(n, m, wrapper);
	                     
	               			sendTrap(v2cTrapPdu, t, m.getSecurityName());
	               			break;
	                     
	               		case SnmpConstants.version3:
	                        if (v3TrapPdu == null)
	                            v3TrapPdu = this.trapFactory.generateV3Trap(n, m, wrapper);
	        	                        
	                        sendTrap(v3TrapPdu, t, m.getSecurityName());
	                        
	                	 break;
	                     
	                  default:    
	                     log.error("Skipping session: Unknown SNMP version found");    
	               }        
	            } catch(MappingFailedException e) {
	              log.error("Translating notification failed ", e);
	            } catch(Exception e) {
	              log.error("SNMP send error for " + t.getAddress().toString(), e);                    
	            }
	         }
	      }
      } else {
    	 log.warn("No SNMP managers to send traps to");
      }
   }
   
   void sendTrap(PDU trap, Target target, String securityName) throws IOException {
	   // Advance the trap counter
       this.snmpAgentService.getTrapCounter().advance();
       
	   if(target instanceof UserTarget) {
		   if(securityName != null) {
			   if(snmpAgentService.getUserMap().get(securityName) == null) {
		        	 throw new IllegalArgumentException("Notification Security Name " +securityName + " doesn't match any user defined in users.xml");
		       } else {			   
				   OctetString userSecurityName = new OctetString(securityName);
				   
				   ((UserTarget)target).setSecurityName(userSecurityName);
				   ((UserTarget)target).setSecurityLevel(snmpAgentService.getUserMap().get(securityName).getSecurityLevel());
				   ((UserTarget)target).setSecurityModel(SecurityModel.SECURITY_MODEL_USM);	
				   
				   if(trap.getType() == PDU.INFORM) { 
					   User user = snmpAgentService.getUserMap().get(securityName);        	 
					   UsmUser usmUser = new UsmUser(userSecurityName,
			                 user.getAuthenticationProtocolID(),
			                 new OctetString(user.getAuthenticationPassphrase()),
			                 user.getPrivacyProtocolID(),
			                 new OctetString(user.getPrivacyPassphrase()));
					   Snmp snmp = createSnmpSession(target.getAddress());
					   byte[] authorativeEngine = snmp.discoverAuthoritativeEngineID(target.getAddress(), 8000);
					   if(authorativeEngine != null) {		      
						   OctetString authorativeEngineID = new OctetString(authorativeEngine);
						   snmp.getUSM().addUser(usmUser.getSecurityName(), authorativeEngineID, usmUser);
						   ((UserTarget)target).setAuthoritativeEngineID(authorativeEngine);					   
						   
						   snmp.send(trap, target);	    		
			    	  } else {
			    		  log.warn("Couldn't discover the AuthoritativeEngineID for INFORM notification " + trap);
			    	  }
			    	  snmp.close();
			    	  return;
				   }
			   }
		   }
	   }
	   // Send v2 traps or inform without users
	   snmpAgentService.getSession().send(trap, target);
   }
   
   
   /**
    * Locate mapping applicable for the incoming notification. Key is the
    * notification's type
    *
    * @param n the notification to be examined
    * @return the index of the mapping
    * @throws IndexOutOfBoundsException if no mapping found
   **/ 
   private int findMappingIndex(Notification n)
      throws IndexOutOfBoundsException
   {
      // Sequentially check the notification type against the compiled 
      // regular expressions. On first match return the coresponding mapping
      // index
      for (int i = 0; i < notificationMapList.size(); i++)
      {
         Pattern p = (Pattern) this.mappingRegExpCache.get(i);
            
         if (p != null)
         {
            Matcher m = p.matcher(n.getType());
            
            if (m.matches())
            {
               if (log.isTraceEnabled())
                  log.trace("Match for '" + n.getType() + "' on mapping " + i);
               return i;
            }
         }
      }
      return -1;
   }

   /**
    * Load manager subscriptions
   **/ 
   private void load() throws Exception
   {
      log.debug("Reading resource: '" + this.snmpAgentService.getManagersResName() + "'");
      
      // configure ObjectModelFactory for mapping XML to POJOs
      // we'll be simply getting an ArrayList of Manager objects
      MappingObjectModelFactory momf = new MappingObjectModelFactory();
      momf.mapElementToClass("manager-list", ArrayList.class);
      momf.mapElementToClass("manager", Manager.class);

      ArrayList managerList = null;
      InputStream is = null;
      try
      {
         // locate managers.xml
         final String resName = this.snmpAgentService.getManagersResName();
         is = SecurityActions.getThreadContextClassLoaderResource(resName);
         
         // create unmarshaller
         Unmarshaller unmarshaller = UnmarshallerFactory.newInstance()
               .newUnmarshaller();
         
         // let JBossXB do it's magic using the MappingObjectModelFactory
         managerList = (ArrayList)unmarshaller.unmarshal(is, momf, null);         
      }
      catch (Exception e)
      {
         log.error("Accessing resource '" + snmpAgentService.getManagersResName() + "'");
         throw e;
      }
      finally
      {
         if (is != null)
         {
            // close the XML stream
            is.close();            
         }
      }
      log.debug("Found " + managerList.size() + " monitoring managers");        
        
      for (Iterator i = managerList.iterator(); i.hasNext(); )
      {
         // Read the monitoring manager's particulars
         Manager m = (Manager)i.next();
         fixManagerVersion(m);                
         
    	 Target target = createTarget(m);
    	 if (target == null){
    		 log.warn("createTarget: manager m: "+m.toString() + " is null!");
    	 	continue;
    	 }    	 
    	                 
    	 // Add the record to the list of monitoring managers. If 
    	 // successfull open the session to the manager as well.
    	 if (this.managers.add(target) == false)
    	 {
           log.warn("Ignoring duplicate manager: " + m);  
    	 }            
      }
      
      log.debug("Reading resource: '" + snmpAgentService.getNotificationMapResName() + "'");
      
      ObjectModelFactory omf = new NotificationBinding();
      try
      {
         // locate notifications.xml
         final String resName = snmpAgentService.getNotificationMapResName();
         is = SecurityActions.getThreadContextClassLoaderResource(resName);
         
         // create unmarshaller
         Unmarshaller unmarshaller = UnmarshallerFactory.newInstance().newUnmarshaller();

         // let JBossXB do it's magic using the MappingObjectModelFactory
         List<Mapping> notifications = (List<Mapping>)unmarshaller.unmarshal(is, omf, null);     
         log.debug("Found " + notifications.size() + " notification mappings");   
         
         // Initialise the cache with the compiled regular expressions denoting 
         // notification type specifications
         this.mappingRegExpCache = 
            new ArrayList(notifications.size());
           
         // Initialise the cache with the instantiated notification wrappers
         this.notificationWrapperCache =
            new ArrayList(notifications.size());
         
         this.notificationMapList =
             new ArrayList(notifications.size());
           
         addNotifications(notifications);
      } catch (Exception e) {
         log.error("Accessing resource '" + snmpAgentService.getNotificationMapResName() + "'");
         throw e;
      } finally {
         if (is != null) {
            // close the XML stream
            is.close();            
         }
      }      
   }

	public void addNotifications(List<Mapping> notifications) {
		for (Iterator<Mapping> i = notifications.iterator(); i.hasNext();) {
			Mapping mapping = i.next();

			// Compile and add the regular expression
			String notificationType = mapping.getNotificationType();

			try {
				Pattern re = Pattern.compile(notificationType);
				this.mappingRegExpCache.add(re);
			} catch (PatternSyntaxException e) {
				// Fill the slot to keep index count correct
				this.mappingRegExpCache.add(null);

				log.warn("Error compiling notification mapping for type: "
						+ notificationType, e);
			}

			// Instantiate and add the wrapper
			// Read wrapper class name
			String wrapperClassName = mapping.getVarBindList()
					.getWrapperClass();

			log.debug("notification wrapper class: " + wrapperClassName);

			try {
				NotificationWrapper wrapper = (NotificationWrapper) Class
						.forName(wrapperClassName, true,
								this.getClass().getClassLoader()).newInstance();

				// Initialise it
				wrapper.set(snmpAgentService.getClock(),
						snmpAgentService.getTrapCounter());

				// Add the wrapper to the cache
				this.notificationWrapperCache.add(wrapper);
			} catch (Exception e) {
				// Fill the slot to keep index count correct
				this.notificationWrapperCache.add(null);

				log.warn("Error compiling notification mapping for type: "
						+ notificationType, e);
			}
			
			notificationMapList.add(mapping);
		}
	}
	
	public void removeNotifications(List<Mapping> notifications) {
		for (Iterator<Mapping> i = notifications.iterator(); i.hasNext();) {
			Mapping mapping = i.next();
			notificationMapList.remove(mapping);
		}
	}
   
   /**
    * Function used to change the SNMP versions received from managers
    * config file (1,2,3) to the actual versions used to distinguish in
    * snmp4j (0,1,3)
    * @param m the Manager who's version we want to fix
    */
   private void fixManagerVersion(Manager m){
	  if (m != null){
		  switch (m.getVersion()){
		  case 1:
			  m.setVersion(SnmpConstants.version1);
			  break;
		  case 2:
			  m.setVersion(SnmpConstants.version2c);
			  break;
		  case 3: 
			  m.setVersion(SnmpConstants.version3);
		  }
		  
	  }
   }

   /**
    * cater for possible global -b option, if no override has been specified
    */
   private InetAddress toInetAddressWithDefaultBinding(String host)
      throws UnknownHostException
   {
      if (host == null || host.length() == 0) {
         
         String defaultBindAddress = System.getProperty(JBossASServerConfig.PROP_KEY_JBOSSAS_BIND_ADDRESS);
         if (defaultBindAddress != null && !defaultBindAddress.equals("0.0.0.0"))
            return InetAddress.getByName(defaultBindAddress);
         else
            return InetAddress.getLocalHost();
      }
      else
         return InetAddress.getByName(host);
   }
   
   private Target createTarget(Manager m){
	   Target target = null;
  
	   int version = m.getVersion();
	   try{
		   if (version == SnmpConstants.version1 || version == SnmpConstants.version2c){
			   if (m.getAddress() != null){
				   target = new CommunityTarget(new UdpAddress(InetAddress.getByName(m.getAddress()), m.getPort()), new OctetString(m.getCommunityString()));
				   //try defining retries/timeout period in notifications.xml
				   //Timeout and Retries needs to be a field later on
				   //target.setRetries(3);
				   target.setTimeout(8000);
			   }
		   }
		   else if (version == SnmpConstants.version3) {
			   target = new UserTarget();
			   target.setRetries(1);
			   target.setTimeout(8000);
			   target.setAddress(new UdpAddress(InetAddress.getByName(m.getAddress()), m.getPort()));			   
		   }
		   else {
			   //unrecognized version
			   throw new IllegalArgumentException("version " + version + " is not supported in managers.xml, only 1, 2 and 3 are valid values");
		   }
	   } catch (UnknownHostException e) {
		   log.error("A problem occured creating the target towards " + m.getAddress() + ":" + m.getPort(), e);
	   } //something goes here
	   if (target != null){
		   target.setVersion(version);
	   }
	   return target;
   }
   
   private Snmp createSnmpSession(Address address) throws IOException {
	    AbstractTransportMapping transport;
	    if (address instanceof TcpAddress) {
	      transport = new DefaultTcpTransportMapping();
	    } else {
	      transport = new DefaultUdpTransportMapping();
	    }
	    // Could save some CPU cycles:
	    // transport.setAsyncMsgProcessingSupported(false);
	    
	   	MessageDispatcherImpl dispatcher = new MessageDispatcherImpl();
	    Snmp snmp =  new Snmp(dispatcher, transport);
	    OctetString localEngineID= new OctetString(MPv3.createLocalEngineID());	    
	    USM usm = new USM(SecurityProtocols.getInstance(), localEngineID, 0);
	    MPv3 mpv3 = new MPv3(usm);			   		   	    
		dispatcher.addMessageProcessingModel(new MPv2c());
		dispatcher.addMessageProcessingModel(mpv3);		
		
		snmp.listen();
		
	    return snmp;
	  }

	/**
	 * Utility class used by JBossXB to help parse notifications.xml
	 */
	private static class NotificationBinding implements
			GenericObjectModelFactory {
		// GenericObjectModelFactory implementation ----------------------

		public Object completeRoot(Object root, UnmarshallingContext ctx,
				String uri, String name) {
			return root;
		}

		public Object newRoot(Object root, UnmarshallingContext navigator,
				String namespaceURI, String localName, Attributes attrs) {
			ArrayList notifList;

			if (root == null) {
				root = notifList = new ArrayList();
			} else {
				notifList = (ArrayList) root;
			}
			return root;
		}

		public Object newChild(Object parent, UnmarshallingContext navigator,
				String namespaceURI, String localName, Attributes attrs) {
			Object child = null;

			if ("mapping".equals(localName)) {
				Mapping m = new Mapping();
				
				String notificationType = attrs.getValue("notification-type");
				String generic = attrs.getValue("generic");
				String specific = attrs.getValue("specific");
				String enterprise = attrs.getValue("enterprise");
				String inform = attrs.getValue("inform");
				String securityName = attrs.getValue("security-name");
				m.setNotificationType(notificationType);
				m.setGeneric(Integer.parseInt(generic));
				m.setSpecific(Integer.parseInt(specific));
				m.setEnterprise(enterprise);
				m.setInform(Boolean.parseBoolean(inform));
				m.setSecurityName(securityName);
				
				child = m;
			} else if ("var-bind-list".equals(localName)) {
				VarBindList vblist = new VarBindList();
				child = vblist;
				if (attrs.getLength() > 0) {
					for (int i = 0; i < attrs.getLength(); i++) {
						if ("wrapper-class".equals(attrs.getLocalName(i))) {
							vblist.setWrapperClass(attrs.getValue(i));
						}
					}
				}
				// check that wrapper-class is set
				if (vblist.getWrapperClass() == null) {
					throw new RuntimeException(
							"'wrapper-class' must be set at 'var-bind-list' element");
				}
			} else if ("var-bind".equals(localName)) {
				VarBind vb = new VarBind();
				String oid = attrs.getValue("oid");
				String tag = attrs.getValue("tag");
				String type = attrs.getValue("type");
				vb.setOid(oid);
				vb.setTag(tag);
				vb.setType(type);
				
				child = vb;
			}
			return child;
		}

		public void addChild(Object parent, Object child,
				UnmarshallingContext navigator, String namespaceURI,
				String localName) {
			if (parent instanceof ArrayList) {
				ArrayList notifList = (ArrayList) parent;

				if (child instanceof Mapping) {
					notifList.add(child);
				}
			} else if (parent instanceof Mapping) {
				Mapping m = (Mapping) parent;

				if (child instanceof VarBindList) {
					m.setVarBindList((VarBindList) child);
				}
			} else if (parent instanceof VarBindList) {
				VarBindList vblist = (VarBindList) parent;

				if (child instanceof VarBind) {
					vblist.addVarBind((VarBind) child);
				}
			}
		}

		public void setValue(Object o, UnmarshallingContext navigator,
				String namespaceURI, String localName, String value) {
			if (o instanceof Mapping) {
				Mapping m = (Mapping) o;

				if ("notification-type".equals(localName)) {
					m.setNotificationType(value);
				} else if ("generic".equals(localName)) {
					m.setGeneric(Integer.parseInt(value));
				} else if ("specific".equals(localName)) {
					m.setSpecific(Integer.parseInt(value));
				} else if ("enterprise".equals(localName)) {
					m.setEnterprise(value);
				} else if ("inform".equals(localName)) {
					m.setInform(Boolean.parseBoolean(value));
				} else if ("security-name".equals(localName)) {
					m.setSecurityName(value);
				}
			} else if (o instanceof VarBind) {
				VarBind vb = (VarBind) o;

				if ("tag".equals(localName)) {
					vb.setTag(value);
				} else if ("oid".equals(localName)) {
					vb.setOid(value);
				}
			}
		}

		public Object completedRoot(Object root,
				UnmarshallingContext navigator, String namespaceURI,
				String localName) {
			return root;
		}
	}
} // class TrapEmitter
