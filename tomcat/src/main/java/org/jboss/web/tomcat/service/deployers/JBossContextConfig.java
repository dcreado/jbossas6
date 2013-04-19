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
package org.jboss.web.tomcat.service.deployers;

// $Id: JBossContextConfig.java 104399 2010-05-03 20:50:38Z remy.maucherat@jboss.com $

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.HttpConstraintElement;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.xml.namespace.QName;

import org.apache.catalina.Container;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.Manager;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.Multipart;
import org.apache.catalina.deploy.SessionCookie;
import org.apache.catalina.deploy.jsp.FunctionInfo;
import org.apache.catalina.deploy.jsp.TagAttributeInfo;
import org.apache.catalina.deploy.jsp.TagFileInfo;
import org.apache.catalina.deploy.jsp.TagInfo;
import org.apache.catalina.deploy.jsp.TagLibraryInfo;
import org.apache.catalina.deploy.jsp.TagLibraryValidatorInfo;
import org.apache.catalina.deploy.jsp.TagVariableInfo;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.ContextConfig;
import org.apache.naming.resources.FileDirContext;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.annotation.javaee.Icon;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.plugins.bootstrap.basic.KernelConstants;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefsMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.metadata.web.jboss.JBossAnnotationsMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.AnnotationMetaData;
import org.jboss.metadata.web.spec.AttributeMetaData;
import org.jboss.metadata.web.spec.AuthConstraintMetaData;
import org.jboss.metadata.web.spec.CookieConfigMetaData;
import org.jboss.metadata.web.spec.DispatcherType;
import org.jboss.metadata.web.spec.ErrorPageMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.FunctionMetaData;
import org.jboss.metadata.web.spec.HttpMethodConstraintMetaData;
import org.jboss.metadata.web.spec.JspConfigMetaData;
import org.jboss.metadata.web.spec.JspPropertyGroupMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.LocaleEncodingMetaData;
import org.jboss.metadata.web.spec.LocaleEncodingsMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.jboss.metadata.web.spec.MimeMappingMetaData;
import org.jboss.metadata.web.spec.MultipartConfigMetaData;
import org.jboss.metadata.web.spec.SecurityConstraintMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletSecurityMetaData;
import org.jboss.metadata.web.spec.SessionConfigMetaData;
import org.jboss.metadata.web.spec.SessionTrackingModeType;
import org.jboss.metadata.web.spec.TagFileMetaData;
import org.jboss.metadata.web.spec.TagMetaData;
import org.jboss.metadata.web.spec.TaglibMetaData;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.metadata.web.spec.TransportGuaranteeType;
import org.jboss.metadata.web.spec.VariableMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionsMetaData;
import org.jboss.metadata.web.spec.WelcomeFileListMetaData;
import org.jboss.util.StringPropertyReplacer;
import org.jboss.util.xml.JBossEntityResolver;
import org.jboss.vfs.VirtualFile;
import org.jboss.web.deployers.MergedJBossWebMetaDataDeployer;
import org.jboss.web.deployers.ServletContainerInitializerDeployer;
import org.jboss.web.deployers.SharedJBossWebMetaDataDeployer;
import org.jboss.web.tomcat.metadata.ContextMetaData;
import org.jboss.web.tomcat.metadata.ManagerMetaData;
import org.jboss.web.tomcat.metadata.ParameterMetaData;
import org.jboss.web.tomcat.service.session.AbstractJBossManager;
import org.jboss.web.tomcat.service.session.JBossCacheManager;
import org.jboss.web.tomcat.service.session.distributedcache.spi.ClusteringNotSupportedException;
import org.jboss.xb.binding.Unmarshaller;
import org.jboss.xb.binding.UnmarshallerFactory;
import org.jboss.xb.binding.sunday.unmarshalling.SchemaBinding;
import org.jboss.xb.builder.JBossXBBuilder;

@SuppressWarnings("unchecked")
public class JBossContextConfig extends ContextConfig
{
   public static ThreadLocal<JBossWebMetaData> metaDataLocal = new ThreadLocal<JBossWebMetaData>();

   public static ThreadLocal<Map<String, TldMetaData>> tldMetaDataMapLocal = new ThreadLocal<Map<String, TldMetaData>>();

   public static ThreadLocal<Kernel> kernelLocal = new ThreadLocal<Kernel>();
   public static ThreadLocal<DeploymentUnit> deploymentUnitLocal = new ThreadLocal<DeploymentUnit>();

   public static ThreadLocal<DeployerConfig> deployerConfig = new ThreadLocal<DeployerConfig>();

   private static Logger log = Logger.getLogger(JBossContextConfig.class);

   private Set<String> overlays = new HashSet<String>();
   
   private boolean runDestroy = false;

   /**
    * <p>
    * Creates a new instance of {@code JBossContextConfig}.
    * </p>
    */
   public JBossContextConfig()
   {
      super();
      try
      {
         Map authMap = this.getAuthenticators();
         if (authMap.size() > 0)
            customAuthenticators = authMap;
      }
      catch (Exception e)
      {
         log.debug("Failed to load the customized authenticators", e);
      }
      runDestroy = deployerConfig.get().isDeleteWorkDirs();
   }

   public void lifecycleEvent(LifecycleEvent event) {
      if (event.getType().equals(Lifecycle.AFTER_START_EVENT)) {
         // Invoke ServletContainerInitializer
         Set<ServletContainerInitializer> scis = (Set<ServletContainerInitializer>) 
            deploymentUnitLocal.get().getAttachment(ServletContainerInitializerDeployer.SCI_ATTACHMENT_NAME);
         Map<ServletContainerInitializer, Set<Class<?>>> handlesTypes = (Map<ServletContainerInitializer, Set<Class<?>>>) 
            deploymentUnitLocal.get().getAttachment(ServletContainerInitializerDeployer.SCI_HANDLESTYPES_ATTACHMENT_NAME);
         if (scis != null)
         {
            for (ServletContainerInitializer sci : scis)
            {
               try
               {
                  sci.onStartup(handlesTypes.get(sci), context.getServletContext());
               }
               catch (Throwable t)
               {
                  log.error("Error calling onStartup for servlet container initializer: " + sci.getClass().getName(), t);
                  ok = false;
               }
            }
         }
         // Post order
         List<String> order = (List<String>) 
            deploymentUnitLocal.get().getAttachment(MergedJBossWebMetaDataDeployer.WEB_ORDER_ATTACHMENT_NAME);
         if (deploymentUnitLocal.get().getAttachment(MergedJBossWebMetaDataDeployer.WEB_NOORDER_ATTACHMENT_NAME) != Boolean.TRUE)
         {
            context.getServletContext().setAttribute(ServletContext.ORDERED_LIBS, order);
         }
      }
      super.lifecycleEvent(event);
   }
   
   protected void applicationWebConfig()
   {
      processWebMetaData(metaDataLocal.get());
      processContextParameters();
   }

   protected void defaultWebConfig()
   {
      JBossWebMetaData sharedJBossWebMetaData = (JBossWebMetaData)
         deploymentUnitLocal.get().getAttachment(SharedJBossWebMetaDataDeployer.SHARED_JBOSSWEB_ATTACHMENT_NAME);
      if (sharedJBossWebMetaData != null)
      {
         processWebMetaData(sharedJBossWebMetaData);
      }

      ServletContext servletContext = context.getServletContext();
      Kernel kernel = kernelLocal.get();
      DeploymentUnit unit = deploymentUnitLocal.get();
      log.debug("Setting MC attributes, kernel: " + kernel + ", unit: " + unit);
      servletContext.setAttribute(KernelConstants.KERNEL_NAME, kernel);
      servletContext.setAttribute(DeploymentUnit.class.getName(), unit);
   }

   protected void processWebMetaData(JBossWebMetaData metaData)
   {
      if (context instanceof StandardContext)
      {
         ((StandardContext)context).setReplaceWelcomeFiles(true);
      }

      // Version
      context.setVersion(metaData.getVersion());

      // SetPublicId
      if (metaData.is30())
         context.setPublicId("/javax/servlet/resources/web-app_3_0.dtd");
      else if (metaData.is25())
         context.setPublicId("/javax/servlet/resources/web-app_2_5.dtd");
      else if (metaData.is24())
         context.setPublicId("/javax/servlet/resources/web-app_2_4.dtd");
      else if (metaData.is23())
         context.setPublicId(org.apache.catalina.startup.Constants.WebDtdPublicId_23);
      else
         context.setPublicId(org.apache.catalina.startup.Constants.WebDtdPublicId_22);
      
      // Display name
      DescriptionGroupMetaData dg = metaData.getDescriptionGroup();
      if (dg != null)
      {
         String displayName = dg.getDisplayName();
         if (displayName != null)
         {
            context.setDisplayName(displayName);
         }
      }

      // Distributable
      if (metaData.getDistributable() != null)
         context.setDistributable(true);

      // Error pages
      List<ErrorPageMetaData> errorPages = metaData.getErrorPages();
      if (errorPages != null)
      {
         for (ErrorPageMetaData value : errorPages)
         {
            org.apache.catalina.deploy.ErrorPage errorPage = new org.apache.catalina.deploy.ErrorPage();
            errorPage.setErrorCode(value.getErrorCode());
            errorPage.setExceptionType(value.getExceptionType());
            errorPage.setLocation(value.getLocation());
            context.addErrorPage(errorPage);
         }
      }

      // Filter definitions
      FiltersMetaData filters = metaData.getFilters();
      if (filters != null)
      {
         for (FilterMetaData value : filters)
         {
            org.apache.catalina.deploy.FilterDef filterDef = new org.apache.catalina.deploy.FilterDef();
            filterDef.setFilterName(value.getName());
            filterDef.setFilterClass(value.getFilterClass());
            if (value.getInitParam() != null)
               for (ParamValueMetaData param : value.getInitParam())
               {
                  filterDef.addInitParameter(param.getParamName(), param.getParamValue());
               }
            filterDef.setAsyncSupported(value.isAsyncSupported());
            context.addFilterDef(filterDef);
         }
      }

      // Filter mappings
      List<FilterMappingMetaData> filtersMappings = metaData.getFilterMappings();
      if (filtersMappings != null)
      {
         for (FilterMappingMetaData value : filtersMappings)
         {
            org.apache.catalina.deploy.FilterMap filterMap = new org.apache.catalina.deploy.FilterMap();
            filterMap.setFilterName(value.getFilterName());
            List<String> servletNames = value.getServletNames();
            if (servletNames != null)
            {
               for (String name : servletNames)
                  filterMap.addServletName(name);
            }
            List<String> urlPatterns = value.getUrlPatterns();
            if (urlPatterns != null)
            {
               for (String pattern : urlPatterns)
                  filterMap.addURLPattern(pattern);
            }
            List<DispatcherType> dispatchers = value.getDispatchers();
            if (dispatchers != null)
            {
               for (DispatcherType type : dispatchers)
                  filterMap.setDispatcher(type.name());
            }
            context.addFilterMap(filterMap);
         }
      }

      // Listeners
      List<ListenerMetaData> listeners = metaData.getListeners();
      if (listeners != null)
      {
         for (ListenerMetaData value : listeners)
         {
            context.addApplicationListener(value.getListenerClass());
         }
      }

      // Login configuration
      LoginConfigMetaData loginConfig = metaData.getLoginConfig();
      if (loginConfig != null)
      {
         org.apache.catalina.deploy.LoginConfig loginConfig2 = new org.apache.catalina.deploy.LoginConfig();
         loginConfig2.setAuthMethod(loginConfig.getAuthMethod());
         loginConfig2.setRealmName(loginConfig.getRealmName());
         if (loginConfig.getFormLoginConfig() != null)
         {
            loginConfig2.setLoginPage(loginConfig.getFormLoginConfig().getLoginPage());
            loginConfig2.setErrorPage(loginConfig.getFormLoginConfig().getErrorPage());
         }
         context.setLoginConfig(loginConfig2);
      }

      // MIME mappings
      List<MimeMappingMetaData> mimes = metaData.getMimeMappings();
      if (mimes != null)
      {
         for (MimeMappingMetaData value : mimes)
         {
            context.addMimeMapping(value.getExtension(), value.getMimeType());
         }
      }

      // Security constraints
      List<SecurityConstraintMetaData> scs = metaData.getSecurityConstraints();
      if (scs != null)
      {
         for (SecurityConstraintMetaData value : scs)
         {
            org.apache.catalina.deploy.SecurityConstraint constraint = new org.apache.catalina.deploy.SecurityConstraint();
            TransportGuaranteeType tg = value.getTransportGuarantee();
            constraint.setUserConstraint(tg.name());
            AuthConstraintMetaData acmd = value.getAuthConstraint();
            constraint.setAuthConstraint(acmd != null);
            if (acmd != null)
            {
               if (acmd.getRoleNames() != null)
                  for (String role : acmd.getRoleNames())
                  {
                     constraint.addAuthRole(role);
                  }
            }
            WebResourceCollectionsMetaData wrcs = value.getResourceCollections();
            if (wrcs != null)
            {
               for (WebResourceCollectionMetaData wrc : wrcs)
               {
                  org.apache.catalina.deploy.SecurityCollection collection2 = new org.apache.catalina.deploy.SecurityCollection();
                  collection2.setName(wrc.getName());
                  List<String> methods = wrc.getHttpMethods();
                  if (methods != null)
                  {
                     for (String method : wrc.getHttpMethods())
                     {
                        collection2.addMethod(method);
                     }
                  }
                  List<String> methodOmissions = wrc.getHttpMethodOmissions();
                  if (methodOmissions != null)
                  {
                     for (String method : wrc.getHttpMethodOmissions())
                     {
                        collection2.addMethodOmission(method);
                     }
                  }
                  List<String> patterns = wrc.getUrlPatterns();
                  if (patterns != null)
                  {
                     for (String pattern : patterns)
                     {
                        collection2.addPattern(pattern);
                     }
                  }
                  constraint.addCollection(collection2);
               }
            }
            context.addConstraint(constraint);
         }
      }

      // Security roles
      SecurityRolesMetaData roles = metaData.getSecurityRoles();
      if (roles != null)
      {
         for (SecurityRoleMetaData value : roles)
         {
            context.addSecurityRole(value.getRoleName());
         }
      }

      // Servlet
      JBossServletsMetaData servlets = metaData.getServlets();
      if (servlets != null)
      {
         for (JBossServletMetaData value : servlets)
         {
            org.apache.catalina.Wrapper wrapper = context.createWrapper();
            wrapper.setName(value.getName());
            wrapper.setServletClass(value.getServletClass());
            if (value.getJspFile() != null)
            {
               wrapper.setJspFile(value.getJspFile());
            }
            wrapper.setLoadOnStartup(value.getLoadOnStartupInt());
            if (value.getRunAs() != null)
            {
               wrapper.setRunAs(value.getRunAs().getRoleName());
            }
            List<ParamValueMetaData> params = value.getInitParam();
            if (params != null)
            {
               for (ParamValueMetaData param : params)
               {
                  wrapper.addInitParameter(param.getParamName(), param.getParamValue());
               }
            }
            SecurityRoleRefsMetaData refs = value.getSecurityRoleRefs();
            if (refs != null)
            {
               for (SecurityRoleRefMetaData ref : refs)
               {
                  wrapper.addSecurityReference(ref.getRoleName(), ref.getRoleLink());
               }
            }
            wrapper.setAsyncSupported(value.isAsyncSupported());
            wrapper.setEnabled(value.isEnabled());
            // Multipart configuration
            if (value.getMultipartConfig() != null)
            {
               MultipartConfigMetaData multipartConfigMetaData = value.getMultipartConfig();
               Multipart multipartConfig = new Multipart();
               multipartConfig.setLocation(multipartConfigMetaData.getLocation());
               multipartConfig.setMaxRequestSize(multipartConfigMetaData.getMaxRequestSize());
               multipartConfig.setMaxFileSize(multipartConfigMetaData.getMaxFileSize());
               multipartConfig.setFileSizeThreshold(multipartConfigMetaData.getFileSizeThreshold());
               wrapper.setMultipartConfig(multipartConfig);
             }
            context.addChild(wrapper);
         }
      }

      // Servlet mapping
      List<ServletMappingMetaData> smappings = metaData.getServletMappings();
      if (smappings != null)
      {
         for (ServletMappingMetaData value : smappings)
         {
            List<String> urlPatterns = value.getUrlPatterns();
            if (urlPatterns != null)
            {
               for (String pattern : urlPatterns)
                  context.addServletMapping(pattern, value.getServletName());
            }
         }
      }

      // JSP Config
      JspConfigMetaData config = metaData.getJspConfig();
      if (config != null)
      {
         // JSP Property groups
         List<JspPropertyGroupMetaData> groups = config.getPropertyGroups();
         if (groups != null)
         {
            for (JspPropertyGroupMetaData group : groups)
            {
               org.apache.catalina.deploy.JspPropertyGroup jspPropertyGroup = 
                  new org.apache.catalina.deploy.JspPropertyGroup();
               for (String pattern : group.getUrlPatterns())
               {
                  jspPropertyGroup.addUrlPattern(pattern);
               }
               jspPropertyGroup.setElIgnored(group.getElIgnored());
               jspPropertyGroup.setPageEncoding(group.getPageEncoding());
               jspPropertyGroup.setScriptingInvalid(group.getScriptingInvalid());
               jspPropertyGroup.setIsXml(group.getIsXml());
               if (group.getIncludePreludes() != null)
               {
                  for (String includePrelude : group.getIncludePreludes())
                  {
                     jspPropertyGroup.addIncludePrelude(includePrelude);
                  }
               }
               if (group.getIncludeCodas() != null)
               {
                  for (String includeCoda : group.getIncludeCodas())
                  {
                     jspPropertyGroup.addIncludeCoda(includeCoda);
                  }
               }
               jspPropertyGroup.setDeferredSyntaxAllowedAsLiteral(group.getDeferredSyntaxAllowedAsLiteral());
               jspPropertyGroup.setTrimDirectiveWhitespaces(group.getTrimDirectiveWhitespaces());
               jspPropertyGroup.setDefaultContentType(group.getDefaultContentType());
               jspPropertyGroup.setBuffer(group.getBuffer());
               jspPropertyGroup.setErrorOnUndeclaredNamespace(group.getErrorOnUndeclaredNamespace());
               context.addJspPropertyGroup(jspPropertyGroup);
            }
         }
         // Taglib
         List<TaglibMetaData> taglibs = config.getTaglibs();
         if (taglibs != null)
         {
            for (TaglibMetaData taglib : taglibs)
            {
               context.addTaglib(taglib.getTaglibUri(), taglib.getTaglibLocation());
            }
         }
      }

      // Locale encoding mapping
      LocaleEncodingsMetaData locales = metaData.getLocalEncodings();
      if (locales != null)
      {
         for (LocaleEncodingMetaData value : locales.getMappings())
         {
            context.addLocaleEncodingMappingParameter(value.getLocale(), value.getEncoding());
         }
      }

      // Welcome files
      WelcomeFileListMetaData welcomeFiles = metaData.getWelcomeFileList();
      if (welcomeFiles != null)
      {
         for (String value : welcomeFiles.getWelcomeFiles())
            context.addWelcomeFile(value);
      }

      // Session timeout
      SessionConfigMetaData scmd = metaData.getSessionConfig();
      if (scmd != null)
      {
         context.setSessionTimeout(scmd.getSessionTimeout());
         if (scmd.getSessionTrackingModes() != null)
         {
            for (SessionTrackingModeType stmt : scmd.getSessionTrackingModes())
            {
               context.addSessionTrackingMode(stmt.toString());
            }
         }
         if (scmd.getCookieConfig() != null)
         {
            CookieConfigMetaData value = scmd.getCookieConfig();
            org.apache.catalina.deploy.SessionCookie cookieConfig = 
               new org.apache.catalina.deploy.SessionCookie();
            cookieConfig.setName(value.getName());
            cookieConfig.setDomain(value.getDomain());
            cookieConfig.setPath(value.getPath());
            cookieConfig.setComment(value.getComment());
            cookieConfig.setHttpOnly(value.getHttpOnly());
            cookieConfig.setSecure(value.getSecure());
            cookieConfig.setMaxAge(value.getMaxAge());
            context.setSessionCookie(cookieConfig);
         }
      }
   }

   /**
    * <p>
    * Retrieves the map of authenticators according to the settings made available by {@code TomcatService}.
    * </p>
    * 
    * @return a {@code Map} containing the authenticator that must be used for each authentication method.
    * @throws Exception if an error occurs while getting the authenticators.
    */
   protected Map getAuthenticators() throws Exception
   {
      Map authenticators = new HashMap();
      ClassLoader tcl = Thread.currentThread().getContextClassLoader();

      Properties authProps = this.getAuthenticatorsFromJndi();
      if (authProps != null)
      {
         Set keys = authProps.keySet();
         Iterator iter = keys != null ? keys.iterator() : null;
         while (iter != null && iter.hasNext())
         {
            String key = (String)iter.next();
            String authenticatorStr = (String)authProps.get(key);
            Class authClass = tcl.loadClass(authenticatorStr);
            authenticators.put(key, authClass.newInstance());
         }
      }
      if (log.isTraceEnabled())
         log.trace("Authenticators plugged in::" + authenticators);
      return authenticators;
   }

   /**
    * <p>
    * Get the key-pair of authenticators from the JNDI.
    * </p>
    * 
    * @return a {@code Properties} object containing the authenticator class name for each authentication method.
    * @throws NamingException if an error occurs while looking up the JNDI.
    */
   private Properties getAuthenticatorsFromJndi() throws NamingException
   {
      return (Properties)new InitialContext().lookup("TomcatAuthenticators");
   }

   /**
    * Process the context parameters. Let a user application
    * override the sharedMetaData values.
    */
   protected void processContextParameters()
   {
      JBossWebMetaData local = metaDataLocal.get();
      JBossWebMetaData shared = (JBossWebMetaData)
         deploymentUnitLocal.get().getAttachment(SharedJBossWebMetaDataDeployer.SHARED_JBOSSWEB_ATTACHMENT_NAME);
      
      if (shared == null)
         return;

      Map<String, String> overrideParams = new HashMap<String, String>();

      List<ParamValueMetaData> params = local.getContextParams();
      if (params != null)
      {
         for (ParamValueMetaData param : params)
         {
            overrideParams.put(param.getParamName(), param.getParamValue());
         }
      }
      params = shared.getContextParams();
      if (params != null)
      {
         for (ParamValueMetaData param : params)
         {
            if (overrideParams.get(param.getParamName()) == null)
            {
               overrideParams.put(param.getParamName(), param.getParamValue());
            }
         }
      }

      for (String key : overrideParams.keySet())
      {
         context.addParameter(key, overrideParams.get(key));
      }

   }

   /**
    * Process a "init" event for this Context.
    */
   protected void init()
   {

      context.setConfigured(false);
      ok = true;

      if (!context.getOverride())
      {
         processContextConfig("context.xml", false);
         processContextConfig(getHostConfigPath(org.apache.catalina.startup.Constants.HostContextXml), false);
      }
      // This should come from the deployment unit
      processContextConfig("WEB-INF/context.xml", true);

   }

   protected void processContextConfig(String resourceName, boolean local)
   {
      ClassLoader oldCl = SecurityActions.getContextClassLoader();
      SecurityActions.setContextClassLoader(this.getClass().getClassLoader());
      
      ContextMetaData contextMetaData = null;
      try
      {
         SchemaBinding schema = JBossXBBuilder.build(ContextMetaData.class);
         Unmarshaller u = UnmarshallerFactory.newInstance().newUnmarshaller();
         u.setSchemaValidation(false);
         u.setValidation(false);
         u.setEntityResolver(new JBossEntityResolver());
         
         InputStream is = null;
         try
         {
            if (local)
            {
               DeploymentUnit localUnit = deploymentUnitLocal.get();
               if (localUnit instanceof VFSDeploymentUnit)
               {
                  VFSDeploymentUnit vfsUnit = (VFSDeploymentUnit)localUnit;
                  VirtualFile vf = vfsUnit.getFile(resourceName);
                  if (vf != null)
                     is = vf.openStream();
               }
            }

            if (is == null)
               is = getClass().getClassLoader().getResourceAsStream(resourceName);

            if (is != null)
               contextMetaData = ContextMetaData.class.cast(u.unmarshal(is, schema));
         }
         finally
         {
            if (is != null)
            {
               try
               {
                  is.close();
               }
               catch (IOException e)
               {
                  // Ignore
               }
            }
         }
      }
      catch (Exception e)
      {
         log.error("XML error parsing: " + resourceName, e);
         ok = false;
         return;
      }
      finally
      {
         SecurityActions.setContextClassLoader(oldCl);
      }

      if (contextMetaData != null)
      {
         try
         {
            if (contextMetaData.getAttributes() != null)
            {
               Iterator<QName> names = contextMetaData.getAttributes().keySet().iterator();
               while (names.hasNext())
               {
                  QName name = names.next();
                  String value = (String)contextMetaData.getAttributes().get(name);
                  // FIXME: This should be done by XB
                  value = StringPropertyReplacer.replaceProperties(value);
                  IntrospectionUtils.setProperty(context, name.getLocalPart(), value);
               }
            }

            TomcatService.addLifecycleListeners(context, contextMetaData.getListeners());

            // Context/Realm
            if (contextMetaData.getRealm() != null)
            {
               context.setRealm((org.apache.catalina.Realm)TomcatService.getInstance(contextMetaData.getRealm(), null));
            }

            // Context/Valve
            TomcatService.addValves(context, contextMetaData.getValves());

            // Context/InstanceListener
            if (contextMetaData.getInstanceListeners() != null)
            {
               Iterator<String> listeners = contextMetaData.getInstanceListeners().iterator();
               while (listeners.hasNext())
               {
                  context.addInstanceListener(listeners.next());
               }
            }

            // Context/Loader
            if (contextMetaData.getLoader() != null)
            {
               // This probably won't work very well in JBoss
               context.setLoader((org.apache.catalina.Loader)TomcatService.getInstance(contextMetaData.getLoader(), "org.apache.catalina.loader.WebappLoader"));
            }

            // Context/Manager
            if (contextMetaData.getManager() != null)
            {
               Manager manager = initManager(contextMetaData.getManager());
               
               if (contextMetaData.getManager().getStore() != null)
               {
                  org.apache.catalina.Store store = (org.apache.catalina.Store)TomcatService.getInstance
                     (contextMetaData.getManager().getStore(), null);
                  try {
                     org.apache.catalina.session.PersistentManagerBase.class.getMethod("setStore", org.apache.catalina.Store.class)
                        .invoke(manager, store);
                  } catch (Throwable t) {
                     // Ignore
                     log.error("Could not set persistent store", t);
                  }
               }
               context.setManager(manager);
            }

            // Context/Parameter
            if (contextMetaData.getParameters() != null)
            {
               Iterator<ParameterMetaData> parameterMetaDatas = contextMetaData.getParameters().iterator();
               while (parameterMetaDatas.hasNext())
               {
                  ParameterMetaData parameterMetaData = parameterMetaDatas.next();
                  context.addApplicationParameter((org.apache.catalina.deploy.ApplicationParameter)TomcatService.getInstance(parameterMetaData, null));
               }
            }

            // Context/Resources
            if (contextMetaData.getResources() != null)
            {
               context.setResources((javax.naming.directory.DirContext)TomcatService.getInstance(contextMetaData.getResources(),
                     "org.apache.naming.resources.FileDirContext"));
            }

            // Context/SessionCookie
            if (contextMetaData.getSessionCookie() != null)
            {
               SessionCookie sessionCookie = new SessionCookie();
               sessionCookie.setComment(contextMetaData.getSessionCookie().getComment());
               sessionCookie.setDomain(contextMetaData.getSessionCookie().getDomain());
               sessionCookie.setHttpOnly(contextMetaData.getSessionCookie().getHttpOnly());
               sessionCookie.setPath(contextMetaData.getSessionCookie().getPath());
               sessionCookie.setSecure(contextMetaData.getSessionCookie().getSecure());
               context.setSessionCookie(sessionCookie);
            }

            // Context/WrapperLifecycle
            if (contextMetaData.getWrapperLifecycles() != null)
            {
               Iterator<String> listeners = contextMetaData.getWrapperLifecycles().iterator();
               while (listeners.hasNext())
               {
                  context.addWrapperLifecycle(listeners.next());
               }
            }

            // Context/WrapperListeners
            if (contextMetaData.getWrapperListeners() != null)
            {
               Iterator<String> listeners = contextMetaData.getWrapperListeners().iterator();
               while (listeners.hasNext())
               {
                  context.addWrapperListener(listeners.next());
               }
            }

            // Context/Overlay
            if (contextMetaData.getOverlays() != null)
            {
               overlays.addAll(contextMetaData.getOverlays());
            }

         }
         catch (Exception e)
         {
            log.error("Error processing: " + resourceName, e);
            ok = false;
         }
      }
   }

   protected void destroy()
   {
      if (runDestroy)
      {
         super.destroy();
      }
   }

   /**
    * Migrate TLD metadata to Catalina. This is separate, and is not subject to the order defined.
    */
   protected void applicationTldConfig()
   {
      
      Map<String, TldMetaData> tldMetaDataMap = tldMetaDataMapLocal.get();
      if (tldMetaDataMap == null)
      {
         return;
      }
      
      ArrayList<TagLibraryInfo> tagLibraries = new ArrayList<TagLibraryInfo>();

      Iterator<String> locationInterator = tldMetaDataMap.keySet().iterator();
      while (locationInterator.hasNext())
      {
         String relativeLocation = null;
         String jarPath = null;
         
         String location = locationInterator.next();
         TldMetaData tldMetaData = tldMetaDataMap.get(location);
         if (!location.startsWith("shared:"))
         {
            relativeLocation = "/" + location.substring(TldMetaData.class.getName().length() + 1);
            if (relativeLocation.startsWith("/WEB-INF/lib/"))
            {
               int pos = relativeLocation.indexOf('/', "/WEB-INF/lib/".length());
               if (pos > 0)
               {
                  jarPath = relativeLocation.substring(pos);
                  if (jarPath.startsWith("/"))
                  {
                     jarPath = jarPath.substring(1);
                  }
                  relativeLocation = relativeLocation.substring(0, pos);
               }
            }
         }

         TagLibraryInfo tagLibraryInfo = new TagLibraryInfo();
         tagLibraryInfo.setTlibversion(tldMetaData.getTlibVersion());
         if (tldMetaData.getJspVersion() == null)
            tagLibraryInfo.setJspversion(tldMetaData.getVersion());
         else
            tagLibraryInfo.setJspversion(tldMetaData.getJspVersion());
         tagLibraryInfo.setShortname(tldMetaData.getShortName());
         tagLibraryInfo.setUri(tldMetaData.getUri());
         if (tldMetaData.getDescriptionGroup() != null)
         {
            tagLibraryInfo.setInfo(tldMetaData.getDescriptionGroup().getDescription());
         }
         // Listener
         if (tldMetaData.getListeners() != null)
         {
            for (ListenerMetaData listener : tldMetaData.getListeners())
            {
               tagLibraryInfo.addListener(listener.getListenerClass());
            }
         }
         // Validator
         if (tldMetaData.getValidator() != null)
         {
            TagLibraryValidatorInfo tagLibraryValidatorInfo = new TagLibraryValidatorInfo();
            tagLibraryValidatorInfo.setValidatorClass(tldMetaData.getValidator().getValidatorClass());
            if (tldMetaData.getValidator().getInitParams() != null)
            {
               for (ParamValueMetaData paramValueMetaData : tldMetaData.getValidator().getInitParams())
               {
                  tagLibraryValidatorInfo.addInitParam(paramValueMetaData.getParamName(), paramValueMetaData.getParamValue());
               }
            }
            tagLibraryInfo.setValidator(tagLibraryValidatorInfo);
         }
         // Tag
         if (tldMetaData.getTags() != null)
         {
            for (TagMetaData tagMetaData : tldMetaData.getTags())
            {
               TagInfo tagInfo = new TagInfo();
               tagInfo.setTagName(tagMetaData.getName());
               tagInfo.setTagClassName(tagMetaData.getTagClass());
               tagInfo.setTagExtraInfo(tagMetaData.getTeiClass());
               if (tagMetaData.getBodyContent() != null)
                  tagInfo.setBodyContent(tagMetaData.getBodyContent().toString());
               tagInfo.setDynamicAttributes(tagMetaData.getDynamicAttributes());
               // Description group
               if (tagMetaData.getDescriptionGroup() != null)
               {
                  DescriptionGroupMetaData descriptionGroup = tagMetaData.getDescriptionGroup();
                  if (descriptionGroup.getIcons() != null && descriptionGroup.getIcons().value() != null
                        && (descriptionGroup.getIcons().value().length > 0))
                  {
                     Icon icon = descriptionGroup.getIcons().value()[0];
                     tagInfo.setLargeIcon(icon.largeIcon());
                     tagInfo.setSmallIcon(icon.smallIcon());
                  }
                  tagInfo.setInfoString(descriptionGroup.getDescription());
                  tagInfo.setDisplayName(descriptionGroup.getDisplayName());
               }
               // Variable
               if (tagMetaData.getVariables() != null)
               {
                  for (VariableMetaData variableMetaData : tagMetaData.getVariables())
                  {
                     TagVariableInfo tagVariableInfo = new TagVariableInfo();
                     tagVariableInfo.setNameGiven(variableMetaData.getNameGiven());
                     tagVariableInfo.setNameFromAttribute(variableMetaData.getNameFromAttribute());
                     tagVariableInfo.setClassName(variableMetaData.getVariableClass());
                     tagVariableInfo.setDeclare(variableMetaData.getDeclare());
                     if (variableMetaData.getScope() != null)
                        tagVariableInfo.setScope(variableMetaData.getScope().toString());
                     tagInfo.addTagVariableInfo(tagVariableInfo);
                  }
               }
               // Attribute
               if (tagMetaData.getAttributes() != null)
               {
                  for (AttributeMetaData attributeMetaData : tagMetaData.getAttributes())
                  {
                     TagAttributeInfo tagAttributeInfo = new TagAttributeInfo();
                     tagAttributeInfo.setName(attributeMetaData.getName());
                     tagAttributeInfo.setType(attributeMetaData.getType());
                     tagAttributeInfo.setReqTime(attributeMetaData.getRtexprvalue());
                     tagAttributeInfo.setRequired(attributeMetaData.getRequired());
                     tagAttributeInfo.setFragment(attributeMetaData.getFragment());
                     if (attributeMetaData.getDeferredValue() != null) {
                        tagAttributeInfo.setDeferredValue("true");
                        tagAttributeInfo.setExpectedTypeName(attributeMetaData.getDeferredValue().getType());
                     }
                     else
                     {
                        tagAttributeInfo.setDeferredValue("false");
                     }
                     if (attributeMetaData.getDeferredMethod() != null)
                     {
                        tagAttributeInfo.setDeferredMethod("true");
                        tagAttributeInfo.setMethodSignature(attributeMetaData.getDeferredMethod().getMethodSignature());
                     }
                     else
                     {
                        tagAttributeInfo.setDeferredMethod("false");
                     }
                     tagInfo.addTagAttributeInfo(tagAttributeInfo);
                  }
               }
               tagLibraryInfo.addTagInfo(tagInfo);
            }
         }
         // Tag files
         if (tldMetaData.getTagFiles() != null)
         {
            for (TagFileMetaData tagFileMetaData : tldMetaData.getTagFiles())
            {
               TagFileInfo tagFileInfo = new TagFileInfo();
               tagFileInfo.setName(tagFileMetaData.getName());
               tagFileInfo.setPath(tagFileMetaData.getPath());
               tagLibraryInfo.addTagFileInfo(tagFileInfo);
            }
         }
         // Function
         if (tldMetaData.getFunctions() != null)
         {
            for (FunctionMetaData functionMetaData : tldMetaData.getFunctions())
            {
               FunctionInfo functionInfo = new FunctionInfo();
               functionInfo.setName(functionMetaData.getName());
               functionInfo.setFunctionClass(functionMetaData.getFunctionClass());
               functionInfo.setFunctionSignature(functionMetaData.getFunctionSignature());
               tagLibraryInfo.addFunctionInfo(functionInfo);
            }
         }
         
         if (jarPath == null && relativeLocation == null)
         {
            context.addJspTagLibrary(tagLibraryInfo);
         }
         else if (jarPath == null)
         {
            tagLibraryInfo.setLocation("");
            tagLibraryInfo.setPath(relativeLocation);
            tagLibraries.add(tagLibraryInfo);
            context.addJspTagLibrary(tagLibraryInfo);
            context.addJspTagLibrary(relativeLocation, tagLibraryInfo);
         }
         else
         {
            tagLibraryInfo.setLocation(relativeLocation);
            tagLibraryInfo.setPath(jarPath);
            tagLibraries.add(tagLibraryInfo);
            context.addJspTagLibrary(tagLibraryInfo);
            if (jarPath.equals("META-INF/taglib.tld"))
            {
               context.addJspTagLibrary(relativeLocation, tagLibraryInfo);
            }
         }
      }

      // Add additional TLDs URIs from explicit web config
      String taglibs[] = context.findTaglibs();
      for (int i = 0; i < taglibs.length; i++) {
          String uri = taglibs[i];
          String path = context.findTaglib(taglibs[i]);
          String location = "";
          if (path.indexOf(':') == -1 && !path.startsWith("/")) {
              path = "/WEB-INF/" + path;
          }
          if (path.endsWith(".jar")) {
              location = path;
              path = "META-INF/taglib.tld";
          }
          for (int j = 0; j < tagLibraries.size(); j++) {
              TagLibraryInfo tagLibraryInfo = tagLibraries.get(j);
              if (tagLibraryInfo.getLocation().equals(location) && tagLibraryInfo.getPath().equals(path)) {
                  context.addJspTagLibrary(uri, tagLibraryInfo);
              }
          }
      }

   }

   public void applicationServletContainerInitializerConfig()
   {
      // Do nothing here
   }
   
   protected void createFragmentsOrder()
   {
      // Do nothing here
   }

   protected void applicationExtraDescriptorsConfig()
   {
      // Do nothing here
   }

   protected void resolveAnnotations(JBossAnnotationsMetaData annotations)
   {
      if (annotations != null)
      {
         for (AnnotationMetaData annotation : annotations)
         {
            String className = annotation.getClassName();
            Container wrappers[] = context.findChildren();
            for (int i = 0; i < wrappers.length; i++) {
                Wrapper wrapper = (Wrapper) wrappers[i];
                if (className.equals(wrapper.getServletClass()))
                {
                   
                   // Merge @RunAs
                   if (annotation.getRunAs() != null && wrapper.getRunAs() == null)
                   {
                      wrapper.setRunAs(annotation.getRunAs().getRoleName());
                   }
                   // Merge @MultipartConfig
                   if (annotation.getMultipartConfig() != null && wrapper.getMultipartConfig() == null)
                   {
                      MultipartConfigMetaData multipartConfigMetaData = annotation.getMultipartConfig();
                      Multipart multipartConfig = new Multipart();
                      multipartConfig.setLocation(multipartConfigMetaData.getLocation());
                      multipartConfig.setMaxRequestSize(multipartConfigMetaData.getMaxRequestSize());
                      multipartConfig.setMaxFileSize(multipartConfigMetaData.getMaxFileSize());
                      multipartConfig.setFileSizeThreshold(multipartConfigMetaData.getFileSizeThreshold());
                      wrapper.setMultipartConfig(multipartConfig);
                   }
                   // Merge @ServletSecurity
                   if (annotation.getServletSecurity() != null && wrapper.getServletSecurity() == null)
                   {
                      ServletSecurityMetaData servletSecurityAnnotation = annotation.getServletSecurity();
                      Collection<HttpMethodConstraintElement> methodConstraints = null;
                      
                      EmptyRoleSemantic emptyRoleSemantic =  EmptyRoleSemantic.PERMIT;
                      if (servletSecurityAnnotation.getEmptyRoleSemantic() != null)
                      {
                         emptyRoleSemantic = EmptyRoleSemantic.valueOf(servletSecurityAnnotation.getEmptyRoleSemantic().toString());
                      }
                      TransportGuarantee transportGuarantee = TransportGuarantee.NONE;
                      if (servletSecurityAnnotation.getTransportGuarantee() != null)
                      {
                         transportGuarantee = TransportGuarantee.valueOf(servletSecurityAnnotation.getTransportGuarantee().toString());
                      }
                      String[] roleNames = servletSecurityAnnotation.getRolesAllowed().toArray(new String[0]);
                      HttpConstraintElement constraint = new HttpConstraintElement(emptyRoleSemantic, transportGuarantee, roleNames);
                      
                      if (servletSecurityAnnotation.getHttpMethodConstraints() != null)
                      {
                         methodConstraints = new HashSet<HttpMethodConstraintElement>();
                         for (HttpMethodConstraintMetaData annotationMethodConstraint : 
                            servletSecurityAnnotation.getHttpMethodConstraints())
                         {
                            emptyRoleSemantic =  EmptyRoleSemantic.PERMIT;
                            if (annotationMethodConstraint.getEmptyRoleSemantic() != null)
                            {
                               emptyRoleSemantic = EmptyRoleSemantic.valueOf(annotationMethodConstraint.getEmptyRoleSemantic().toString());
                            }
                            transportGuarantee = TransportGuarantee.NONE;
                            if (annotationMethodConstraint.getTransportGuarantee() != null)
                            {
                               transportGuarantee = TransportGuarantee.valueOf(annotationMethodConstraint.getTransportGuarantee().toString());
                            }
                            roleNames = annotationMethodConstraint.getRolesAllowed().toArray(new String[0]);
                            HttpConstraintElement constraint2 = 
                               new HttpConstraintElement(emptyRoleSemantic, transportGuarantee, roleNames);
                            HttpMethodConstraintElement methodConstraint = 
                               new HttpMethodConstraintElement(annotationMethodConstraint.getMethod(), constraint2);
                            methodConstraints.add(methodConstraint);
                         }
                      }
                      
                      ServletSecurityElement servletSecurity = new ServletSecurityElement(constraint, methodConstraints);
                      wrapper.setServletSecurity(servletSecurity);
                   }
                   
                }
            }
         }
      }
   }

   protected Manager initManager(ManagerMetaData managerMetaData) throws Exception,
         ClusteringNotSupportedException, NoClassDefFoundError
   {
      JBossWebMetaData webMetaData = metaDataLocal.get();
      
      String defaultManagerClass = webMetaData.getDistributable() == null 
            ? StandardManager.class.getName() : JBossCacheManager.class.getName();      
      Manager manager = (Manager)TomcatService.getInstance(managerMetaData, defaultManagerClass);
      
      if (manager instanceof AbstractJBossManager)
      {
         // TODO next 10+ lines just to create a 'name' that the AbstractJBossManager
         // impls don't even use
         Host host = null;
         Container container = context;
         while (host == null && container != null)
         {
            container = container.getParent();
            if (container instanceof Host)
            {
               host = (Host) container;
            }
         }
         String hostName = host.getName();
         String name = "//" + ((hostName == null) ? "localhost" : hostName) + webMetaData.getContextRoot();
         
         try
         {
            ((AbstractJBossManager) manager).init(name, webMetaData);
         }
         catch (ClusteringNotSupportedException e)
         {
            if (managerMetaData.getClassName() == null)
            {  
               // JBAS-3513 Just log a WARN, not an ERROR
               log.warn("Failed to setup clustering, clustering disabled. ClusteringNotSupportedException: " + e.getMessage());
               manager = (Manager)TomcatService.getInstance(managerMetaData, StandardManager.class.getName());
            }
            else
            {
               throw e;
            }
         }
         catch (NoClassDefFoundError ncdf)
         {
            if (managerMetaData.getClassName() == null)
            {  
               // JBAS-3513 Just log a WARN, not an ERROR
               log.debug("Classes needed for clustered webapp unavailable", ncdf);
               log.warn("Failed to setup clustering, clustering disabled. NoClassDefFoundError: " + ncdf.getMessage());
               manager = (Manager)TomcatService.getInstance(managerMetaData, StandardManager.class.getName());
            }
            else
            {
               throw ncdf;
            }
            
         }
      }
      return manager;
   }
   
   protected void completeConfig() {

      JBossWebMetaData metaData = metaDataLocal.get();
      
      // Process Servlet API related annotations that were dependent on Servlet declarations
      if (ok && (metaData != null))
      {
         // Resolve type specific annotations to their corresponding Servlet components
         metaData.resolveAnnotations();
         // Same process for Catalina
         resolveAnnotations(metaData.getAnnotations());
      }

      if (ok)
      {
         resolveServletSecurity();
      }

      if (ok)
      {
         validateSecurityRoles();
      }
      
      if (ok && (metaData != null))
      {
         // Resolve run as
         metaData.resolveRunAs();
      }

      // Configure an authenticator if we need one
      if (ok)
      {
         authenticatorConfig();
      }

      // Find and configure overlays
      DeploymentUnit deploymentUnit = deploymentUnitLocal.get();
      if (ok && (deploymentUnit != null)) {
         Set<VirtualFile> overlays = (Set<VirtualFile>) 
            deploymentUnit.getAttachment(MergedJBossWebMetaDataDeployer.WEB_OVERLAYS_ATTACHMENT_NAME);
         if (overlays != null)
         {
            if (context.getResources() instanceof ProxyDirContext) {
               ProxyDirContext resources = (ProxyDirContext) context.getResources();
               for (VirtualFile overlay : overlays)
               {
                  // JBAS-7832: Replaced with FileDirContext for now
                  //VFSDirContext vfsDirContext = new VFSDirContext(overlay);
                  //resources.addOverlay(vfsDirContext);
                  FileDirContext dirContext = new FileDirContext();
                  try
                  {
                     dirContext.setDocBase(overlay.getPhysicalFile().getAbsolutePath());
                     resources.addOverlay(dirContext);
                  }
                  catch (IOException e)
                  {
                     log.error(sm.getString("contextConfig.noOverlay", context.getName()), e);
                     ok = false;
                     break;
                  }
               }
            }
            else if (overlays.size() > 0)
            {
               // Error, overlays need a ProxyDirContext to compose results
               log.error(sm.getString("contextConfig.noOverlay", context.getName()));
               ok = false;
            }
         }
      }
      
      // Add other overlays, if any
      if (ok)
      {
         for (String overlay : overlays)
         {
            if (context.getResources() instanceof ProxyDirContext)
            {
               ProxyDirContext resources = (ProxyDirContext) context.getResources();
               FileDirContext dirContext = new FileDirContext();
               dirContext.setDocBase(overlay);
               resources.addOverlay(dirContext);
            }
         }
      }

      // Make our application unavailable if problems were encountered
      if (!ok) {
         log.error(sm.getString("contextConfig.unavailable"));
         context.setConfigured(false);
      }

   }

}
