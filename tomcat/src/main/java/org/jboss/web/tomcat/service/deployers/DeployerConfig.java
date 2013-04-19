/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.web.tomcat.service.deployers;

import java.util.List;
import java.util.Set;

import javax.management.ObjectName;

import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.security.plugins.JaasSecurityManagerServiceMBean;

/**
 * The tomcat war deployer configuration passed in from the web container.
 * 
 * @author Scott.Stark@jboss.org
 * @author Anil.Saldhana@redhat.com
 * @version $Revision: 104333 $
 */
public class DeployerConfig
{
   /**
    * The tomcat sar class loader
    */
   private ClassLoader serviceClassLoader;

   /**
    * The domain used for the tomcat mbeans
    */
   private String catalinaDomain = "Catalina";

   /**
    * The web context class to create
    */
   private String contextClassName;

   /**
    * The parent class loader first model flag
    */
   private boolean java2ClassLoadingCompliance = false;

   /**
    * A flag indicating if war archives should be unpacked
    */
   private boolean unpackWars = true;

   /**
    * If true, ejb-links that don't resolve don't cause an error (fallback to jndi-name)
    */
   private boolean lenientEjbLink = false;

   /**
    * The tomcat service JMX object name
    */
   private ObjectName serviceName;

   /**
    * A flag indicating if the working dir for a war deployment should be delete when the war is undeployed.
    */
   private boolean deleteWorkDirs = true;

   /**
    * Get the request attribute name under which the JAAS Subject is store
    */
   private String subjectAttributeName = null;

   /**
    * The default security-domain name to use
    */
   private String defaultSecurityDomain;

   /**
    * Flag indicating whether web-app specific context xmls may set the privileged flag.
    */
   private boolean allowSelfPrivilegedWebApps = false;

   /** The service used to flush authentication cache on session invalidation. */
   private JaasSecurityManagerServiceMBean secMgrService;

   /** FQN of the SecurityContext Class */
   private String securityContextClassName;

   /** Dynamic classloaders */
   private Set<String> dynamicClassloaders;

   /**
    * Standalone flag: Servlet + JSP only, no EE.
    */
   private boolean standalone = false;
   
   public ClassLoader getServiceClassLoader()
   {
      return serviceClassLoader;
   }

   public void setServiceClassLoader(ClassLoader serviceClassLoader)
   {
      this.serviceClassLoader = serviceClassLoader;
   }

   public String getCatalinaDomain()
   {
      return catalinaDomain;
   }

   public void setCatalinaDomain(String catalinaDomain)
   {
      this.catalinaDomain = catalinaDomain;
   }

   public String getContextClassName()
   {
      return contextClassName;
   }

   public void setContextClassName(String contextClassName)
   {
      this.contextClassName = contextClassName;
   }

   public boolean isJava2ClassLoadingCompliance()
   {
      return java2ClassLoadingCompliance;
   }

   public void setJava2ClassLoadingCompliance(boolean java2ClassLoadingCompliance)
   {
      this.java2ClassLoadingCompliance = java2ClassLoadingCompliance;
   }

   public boolean isUnpackWars()
   {
      return unpackWars;
   }

   public void setUnpackWars(boolean unpackWars)
   {
      this.unpackWars = unpackWars;
   }

   public boolean isLenientEjbLink()
   {
      return lenientEjbLink;
   }

   public void setLenientEjbLink(boolean lenientEjbLink)
   {
      this.lenientEjbLink = lenientEjbLink;
   }

   public ObjectName getServiceName()
   {
      return serviceName;
   }

   public void setServiceName(ObjectName serviceName)
   {
      this.serviceName = serviceName;
   }

   public boolean isDeleteWorkDirs()
   {
      return deleteWorkDirs;
   }

   public void setDeleteWorkDirs(boolean deleteWorkDirs)
   {
      this.deleteWorkDirs = deleteWorkDirs;
   }

   public String getSubjectAttributeName()
   {
      return subjectAttributeName;
   }

   public void setSubjectAttributeName(String subjectAttributeName)
   {
      this.subjectAttributeName = subjectAttributeName;
   }

   public boolean isStandalone()
   {
      return standalone;
   }

   public void setStandalone(boolean standalone)
   {
      this.standalone = standalone;
   }

   /**
    * Get the default security domain implementation to use if a war does not declare a security-domain.
    * 
    * @return jndi name of the security domain binding to use.
    * @jmx:managed-attribute
    */
   public String getDefaultSecurityDomain()
   {
      return defaultSecurityDomain;
   }

   /**
    * Set the default security domain implementation to use if a war does not declare a security-domain.
    * 
    * @param defaultSecurityDomain - jndi name of the security domain binding to use.
    * @jmx:managed-attribute
    */
   public void setDefaultSecurityDomain(String defaultSecurityDomain)
   {
      this.defaultSecurityDomain = defaultSecurityDomain;
   }

   public boolean isAllowSelfPrivilegedWebApps()
   {
      return allowSelfPrivilegedWebApps;
   }

   public void setAllowSelfPrivilegedWebApps(boolean allowSelfPrivilegedWebApps)
   {
      this.allowSelfPrivilegedWebApps = allowSelfPrivilegedWebApps;
   }

   public JaasSecurityManagerServiceMBean getSecurityManagerService()
   {
      return secMgrService;
   }

   public void setSecurityManagerService(JaasSecurityManagerServiceMBean mgr)
   {
      this.secMgrService = mgr;
   }

   public String getSecurityContextClassName()
   {
      return securityContextClassName;
   }

   public void setSecurityContextClassName(String securityContextClassName)
   {
      this.securityContextClassName = securityContextClassName;
   }

   public Set<String> getDynamicClassloaders()
   {
      return dynamicClassloaders;
   }

   public void setDynamicClassloaders(Set<String> dynamicClassloaders)
   {
      this.dynamicClassloaders = dynamicClassloaders;
   }   
   
}
