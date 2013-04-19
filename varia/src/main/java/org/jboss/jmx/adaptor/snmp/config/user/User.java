/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.jmx.adaptor.snmp.config.user;

import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES192;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.smi.OID;

/**
 * Simple POJO class to model XML data
 * 
 * @author  <a href="mailto:jean.deruelle@gmail.com">Jean Deruelle</a>
 * 
 */
public class User
{
   // Private Data --------------------------------------------------
   
   private String  securityName;
   private String  authenticationProtocol;
   private String  authenticationPassphrase;
   private String  privacyProtocol;
   private String  privacyPassphrase;
   private int securityLevel;
   
   // Constructors -------------------------------------------------
    
   /**
    * Default CTOR
    */
   public User()
   {
      // empty
   }
   
   // Accessors/Modifiers -------------------------------------------
   /**
    * @param securityName the securityName to set
    */
   public void setSecurityName(String securityName) {
   	this.securityName = securityName;
   }

   /**
    * @return the securityName
    */
   public String getSecurityName() {
   	return securityName;
   }

   /**
    * @param authenticationProtocol the authenticationProtocol to set
    */
   public void setAuthenticationProtocol(String authenticationProtocol) {
   	this.authenticationProtocol = authenticationProtocol;
   }

   /**
    * @return the authenticationProtocol
    */
   public String getAuthenticationProtocol() {
   	return authenticationProtocol;
   }
   
   /**
    * @return the authenticationProtocol
    */
   public OID getAuthenticationProtocolID() {
		AuthenticationProtocol authenticationProtocolID = AuthenticationProtocol.valueOf(authenticationProtocol.trim()); 
		switch (authenticationProtocolID) {
			case MD5:
				return AuthMD5.ID;
			case SHA:
				return AuthSHA.ID;
			default:
				return null;
		}
   }

   /**
    * @param authenticationPassphrase the authenticationPassphrase to set
    */
   public void setAuthenticationPassphrase(String authenticationPassphrase) {
   	this.authenticationPassphrase = authenticationPassphrase;
   }

   /**
    * @return the authenticationPassphrase
    */
   public String getAuthenticationPassphrase() {
   	return authenticationPassphrase;
   }

   /**
    * @param privacyProtocol the privacyProtocol to set
    */
   public void setPrivacyProtocol(String privacyProtocol) {
   	this.privacyProtocol = privacyProtocol;
   }

   /**
    * @return the privacyProtocol
    */
   public String getPrivacyProtocol() {
   	return privacyProtocol;
   }
   
   /**
    * @return the authenticationProtocol
    */
   public OID getPrivacyProtocolID() {
	   PrivacyProtocol privacyProtocolID = PrivacyProtocol.valueOf(privacyProtocol.trim()); 
		switch (privacyProtocolID) {
			case DES:
				return PrivDES.ID;
			case TRIPLE_DES:
				return Priv3DES.ID;
			case AES128:
				return PrivAES128.ID;
			case AES192:
				return PrivAES192.ID;
			case AES256:
				return PrivAES256.ID;
			default:
				return null;
		}
   }

   /**
    * @param privacyPassphrase the privacyPassphrase to set
    */
   public void setPrivacyPassphrase(String privacyPassphrase) {
   	this.privacyPassphrase = privacyPassphrase;
   }

   /**
    * @return the privacyPassphrase
    */
   public String getPrivacyPassphrase() {
   	return privacyPassphrase;
   }
   /**
    * Returns the given security level depending on te authentication protocol and privacy protocol chosen
    * @return
    */
   public int getSecurityLevel() {
	   if(authenticationProtocol == null) {
		   return SecurityLevel.NOAUTH_NOPRIV;
	   } else if(privacyProtocol == null) {
		   return SecurityLevel.AUTH_NOPRIV;
	   } else {
		   return SecurityLevel.AUTH_PRIV;
	   }
	   
   }
   
   // Object overrides ----------------------------------------------
   
   public String toString()
   {
      StringBuffer sbuf = new StringBuffer(256);
      
      sbuf.append('[')
      .append("securityName=").append(securityName)
      .append(", authenticationProtocol=").append(authenticationProtocol)
      .append(", authenticationPassphrase=").append(authenticationPassphrase)
      .append(", privacyProtocol=").append(privacyProtocol)
      .append(", privacyPassphrase=").append(privacyPassphrase)
      .append(']');
      
      return sbuf.toString();      
   }
}
