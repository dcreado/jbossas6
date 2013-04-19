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
package org.jboss.iiop;

import java.util.List;

import javax.ejb.spi.HandleDelegate;

import org.jboss.metadata.IorSecurityConfigMetaData;
import org.jboss.security.SecurityDomain;
import org.omg.CORBA.ORB;

/**
 *   Mbean interface for the JBoss CORBA ORB service.
 *      
 *   @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 *   @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 *   @version $Revision: 108275 $
 */
public interface CorbaORBServiceMBean
{
   public ORB getORB();

   public HandleDelegate getHandleDelegate();
   
   public String getORBClass();
   public void setORBClass(String orbClass);

   public String getORBSingletonClass();
   public void setORBSingletonClass(String orbSingletonClass);

   public String getORBSingletonDelegate();
   public void setORBSingletonDelegate(String orbSingletonDelegate);

   public void setORBPropertiesFileName(String orbPropertiesFileName);
   public String getORBPropertiesFileName();

   public List<String> getPortableInterceptorInitializers();
   public void setPortableInterceptorInitializers(
                                      List<String> portableInterceptorInitializers);

   public IorSecurityConfigMetaData getDefaultIORSecurityConfig();
   public void setDefaultIORSecurityConfig(IorSecurityConfigMetaData config);
   
   public void setPort(int port);
   public int getPort();

   public void setSSLPort(int sslPort);
   public int getSSLPort();

   public void setSecurityDomain(SecurityDomain sslDomain);
   public SecurityDomain getSecurityDomain();

   boolean getSSLComponentsEnabled();
   void setSSLComponentsEnabled(boolean sslComponentsEnabled);

   boolean getSendSASAcceptWithExceptionEnabled();
   void setSendSASAcceptWithExceptionEnabled(boolean value);

   boolean getOTSContextPropagationEnabled();
   void setOTSContextPropagationEnabled(boolean value);

   boolean getSunJDK14IsLocalBugFix();
   void setSunJDK14IsLocalBugFix(boolean sunJDK14IsLocalBugFix);
}

