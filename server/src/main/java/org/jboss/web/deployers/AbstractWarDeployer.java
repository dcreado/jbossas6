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
package org.jboss.web.deployers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.beans.metadata.plugins.builder.BeanMetaDataBuilderFactory;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.dependency.plugins.AbstractDependencyItem;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer;
import org.jboss.deployers.spi.deployer.helpers.AttachmentLocator;
import org.jboss.deployers.spi.deployer.managed.ManagedObjectCreator;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.structure.spi.main.MainDeployerInternals;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.injection.injector.EEInjector;
import org.jboss.injection.injector.metadata.EnvironmentEntryType;
import org.jboss.injection.injector.metadata.InjectionTargetType;
import org.jboss.injection.injector.metadata.JndiEnvironmentRefsGroup;
import org.jboss.injection.manager.spi.InjectionManager;
import org.jboss.injection.manager.spi.Injector;
import org.jboss.injection.mc.metadata.JndiEnvironmentImpl;
import org.jboss.jpa.resolvers.PersistenceUnitDependencyResolver;
import org.jboss.kernel.plugins.bootstrap.basic.KernelConstants;
import org.jboss.managed.api.ManagedObject;
import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.managed.api.annotation.ManagementPropertyFactory;
import org.jboss.managed.api.annotation.ViewUse;
import org.jboss.managed.api.factory.ManagedObjectFactory;
import org.jboss.managed.plugins.ManagedPropertyImpl;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.metadata.ear.spec.WebModuleMetaData;
import org.jboss.metadata.javaee.spec.Environment;
import org.jboss.metadata.javaee.spec.PersistenceContextReferenceMetaData;
import org.jboss.metadata.javaee.spec.PersistenceContextReferencesMetaData;
import org.jboss.metadata.web.jboss.ClassLoadingMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.metatype.api.values.SimpleValueSupport;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEComponentInformer;
import org.jboss.switchboard.spi.Barrier;
import org.jboss.system.metadata.ServiceAttributeMetaData;
import org.jboss.system.metadata.ServiceConstructorMetaData;
import org.jboss.system.metadata.ServiceDependencyMetaData;
import org.jboss.system.metadata.ServiceInjectionValueMetaData;
import org.jboss.system.metadata.ServiceMetaData;
import org.jboss.system.metadata.ServiceMetaDataVisitor;
import org.jboss.system.microcontainer.ServiceControllerContext;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileVisitor;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.web.WebApplication;

/** A template pattern class for web container integration into JBoss. This class
 * should be subclassed by web container providers wishing to integrate their
 * container into a JBoss server. The sole method to implement is:
 * {@link #getDeployment(DeploymentUnit, WebMetaData)}. This is called from
 * within {@linkplain #deploy(DeploymentUnit, WebMetaData)} to translate the
 * WebMetaData into a AbstractWarDeployment bean that will be passed to the
 * {@link org.jboss.system.deployers.ServiceDeployer} by creating ServiceMetaData
 * for the AbstractWarDeployment in
 * {@link #deployWebModule(DeploymentUnit, WebMetaData, AbstractWarDeployment)}
 *
 * The output of this deployer is a ServiceMetaData attachment. When this is
 * translated into a service instance by the ServiceDeployer, the
 * AbstractWarDeployment start/stop trigger the actual deployment/undeployment of
 * the web application.
 *
 * @see org.jboss.web.deployers.AbstractWarDeployment
 *
 * @author  Scott.Stark@jboss.org
 * @author  Christoph.Jung@infor.de
 * @author  Thomas.Diesler@arcor.de
 * @author  adrian@jboss.org
 * @author  ales.justin@jboss.org
 * @version $Revision: 111971 $
 */
public abstract class AbstractWarDeployer extends AbstractSimpleRealDeployer<JBossWebMetaData>
   implements ManagedObjectCreator
{
   public static final String DEPLOYER = "org.jboss.web.AbstractWebContainer.deployer";
   public static final String WEB_APP = "org.jboss.web.AbstractWebContainer.webApp";
   public static final String WEB_MODULE = "org.jboss.web.AbstractWebContainer.webModule";
   public static final String ERROR = "org.jboss.web.AbstractWebContainer.error";
   
   /** A mapping of deployed warUrl strings to the WebApplication object */
   protected HashMap deploymentMap = new HashMap();
   /** The parent class loader first model flag */
   protected boolean java2ClassLoadingCompliance = false;
   /** A flag indicating if war archives should be unpacked */
   protected boolean unpackWars = true;
   /** A flag indicating if local dirs with WEB-INF/web.xml should be treated as wars
    */
   protected boolean acceptNonWarDirs = false;

   /** If true, ejb-links that don't resolve don't cause an error (fallback to jndi-name) */
   protected boolean lenientEjbLink = false;

   /** The default security-domain name to use */
   protected String defaultSecurityDomain = "java:/jaas/other";
   /** The request attribute name under which the JAAS Subject is store */
   private String subjectAttributeName = null;
   /** Legacy support for MBeanServer */
   private MBeanServer server;
   private MainDeployerInternals mainDeployer;

   private PersistenceUnitDependencyResolver persistenceUnitDependencyResolver;
   
   private JavaEEComponentInformer componentInformer;

   /**
    * Create a new AbstractWarDeployer.
    */
   public AbstractWarDeployer()
   {
      super(JBossWebMetaData.class);
      addInput(Barrier.class);
      addInput(InjectionManager.class);
      setOutput(ServiceMetaData.class);
      setOutput(WarDeployment.class);
   }

   /** Get the flag indicating if the normal Java2 parent first class loading
    * model should be used over the servlet 2.3 web container first model.
    * @return true for parent first, false for the servlet 2.3 model
    * @jmx:managed-attribute
    */
   public boolean getJava2ClassLoadingCompliance()
   {
      return java2ClassLoadingCompliance;
   }

   /** Set the flag indicating if the normal Java2 parent first class loading
    * model should be used over the servlet 2.3 web container first model.
    * @param flag true for parent first, false for the servlet 2.3 model
    * @jmx:managed-attribute
    */
   public void setJava2ClassLoadingCompliance(boolean flag)
   {
      java2ClassLoadingCompliance = flag;
   }

   /** Set the flag indicating if war archives should be unpacked. This may
    * need to be set to false as long extraction paths under deploy can
    * show up as deployment failures on some platforms.
    *
    * @jmx:managed-attribute
    * @return true is war archives should be unpacked
    */
   public boolean getUnpackWars()
   {
      return unpackWars;
   }

   /** Get the flag indicating if war archives should be unpacked. This may
    * need to be set to false as long extraction paths under deploy can
    * show up as deployment failures on some platforms.
    *
    * @jmx:managed-attribute
    * @param flag , true is war archives should be unpacked
    */
   public void setUnpackWars(boolean flag)
   {
      this.unpackWars = flag;
   }

   /**
    * Get the flag indicating if local dirs with WEB-INF/web.xml should be
    * treated as wars
    * @return true if local dirs with WEB-INF/web.xml should be treated as wars
    * @jmx.managed-attribute
    */
   public boolean getAcceptNonWarDirs()
   {
      return acceptNonWarDirs;
   }

   /**
    * Set the flag indicating if local dirs with WEB-INF/web.xml should be
    * treated as wars
    * @param flag - true if local dirs with WEB-INF/web.xml should be treated as wars
    * @jmx.managed-attribute
    */
   public void setAcceptNonWarDirs(boolean flag)
   {
      this.acceptNonWarDirs = flag;
   }

   /**
    * Get the flag indicating if ejb-link errors should be ignored
    * in favour of trying the jndi-name in jboss-web.xml
    * @return the LenientEjbLink flag
    *
    * @jmx:managed-attribute
    */
   public boolean getLenientEjbLink()
   {
      return lenientEjbLink;
   }

   /**
    * Set the flag indicating if ejb-link errors should be ignored
    * in favour of trying the jndi-name in jboss-web.xml
    *
    * @jmx:managed-attribute
    */
   public void setLenientEjbLink(boolean flag)
   {
      lenientEjbLink = flag;
   }

   /** Get the default security domain implementation to use if a war
    * does not declare a security-domain.
    *
    * @return jndi name of the security domain binding to use.
    * @jmx:managed-attribute
    */
   public String getDefaultSecurityDomain()
   {
      return defaultSecurityDomain;
   }

   /** Set the default security domain implementation to use if a war
    * does not declare a security-domain.
    *
    * @param defaultSecurityDomain - jndi name of the security domain binding
    * to use.
    * @jmx:managed-attribute
    */
   public void setDefaultSecurityDomain(String defaultSecurityDomain)
   {
      this.defaultSecurityDomain = defaultSecurityDomain;
   }

   @Inject
   public void setPersistenceUnitDependencyResolver(PersistenceUnitDependencyResolver resolver)
   {
      this.persistenceUnitDependencyResolver = resolver;
   }
   
   @Inject
   public void setJavaEEComponentInformer(JavaEEComponentInformer informer)
   {
      this.componentInformer = informer;
   }
   
   /** Get the session attribute number under which the caller Subject is stored
    * @jmx:managed-attribute
    */
   public String getSubjectAttributeName()
   {
      return subjectAttributeName;
   }

   /** Set the session attribute number under which the caller Subject is stored
    * @jmx:managed-attribute
    */
   public void setSubjectAttributeName(String subjectAttributeName)
   {
      this.subjectAttributeName = subjectAttributeName;
   }

   public void start() throws Exception
   {
      // TODO: remove dependency on jmx
      this.server = MBeanServerLocator.locateJBoss();
   }

   public void stop() throws Exception
   {

   }

   /**
    * Get the AbstractWarDeployment bean for the deployment metadata. Subclasses
    * override this method to provide a AbstractWarDeployment bean whose
    * start/stop will control the deployment/undeployment of the web
    * application.
    *
    * @param unit - the deployment unit
    * @param metaData - the input web application metadata
    * @return the AbstractWarDeployment for the input WebMetaData
    * @throws Exception - thrown on any failure
    */
   public abstract AbstractWarDeployment getDeployment(DeploymentUnit unit, JBossWebMetaData metaData) throws Exception;

   /**
    * Deploy a web app based on the WebMetaData. This calls
    * {@link #getDeployment(DeploymentUnit, WebMetaData)} to obtain an
    * AbstractWarDeployment bean that is wrapped in a ServiceMetaData by
    * deployWebModule.
    *
    * This will set the WebMetaData.contextRoot if it has not been set based
    * on the war deployment name.
    *
    * @see #deployWebModule(DeploymentUnit, WebMetaData, AbstractWarDeployment)
    * @see #buildWebContext(DeploymentUnit, String, String, WebMetaData)
    *
    * @param unit - the war for the deployment
    * @param metaData - the metadata for the deployment
    */
   @Override
   public void deploy(DeploymentUnit unit, JBossWebMetaData metaData) throws DeploymentException
   {
      log.debug("Begin deploy, " + metaData);

      // Merge any settings from the ear level
      JBossAppMetaData earMetaData = AttachmentLocator.search(unit, JBossAppMetaData.class);
      if (earMetaData != null)
      {
         String path = unit.getRelativePath();
         ModuleMetaData webModule = earMetaData.getModule(path);
         if (webModule != null)
         {
            // Check for a context-root setting
            String contextRoot = metaData.getContextRoot();
            if (contextRoot == null)
            {
               WebModuleMetaData wmodule = (WebModuleMetaData)webModule.getValue();
               contextRoot = wmodule.getContextRoot();
               metaData.setContextRoot(contextRoot);
            }

            // Add any alt-dd setting
            metaData.setAlternativeDD(webModule.getAlternativeDD());
         }

         // Merge security domain/roles
         if (metaData.getSecurityDomain() == null && earMetaData.getSecurityDomain() != null)
            metaData.setSecurityDomain(earMetaData.getSecurityDomain());
         // TODO
         metaData.mergeSecurityRoles(earMetaData.getSecurityRoles());
      }

      try
      {
         /* Unpack wars to the tmp directory for now until tomcat can use the vfs directly. Since
          * the vfs deals with the distinction between a true directory, the only way we can tell from
          * this level of the api is to look for a url that ends in '/'. Here we assume that the name is
          * the root url.
          */
         String warName = unit.getName();

         /**
          * Ignore the jacc policy service bean
          */
         if (warName.startsWith("jboss:") && warName.contains("id="))
            return;

         if (unit instanceof VFSDeploymentUnit)
         {
            VFSDeploymentUnit vfsUnit = (VFSDeploymentUnit)unit;
            VirtualFile root = vfsUnit.getRoot();
            final URL expandedWarURL = root.getPhysicalFile().toURI().toURL();

            // Map
            String warPathName = root.getPathName();
            if (warPathName.endsWith("/") == false)
               warPathName += "/";
            List<VirtualFile> classpathVFs = vfsUnit.getClassPath();
            if (classpathVFs != null)
            {
               List<URL> classpath = new ArrayList<URL>();
               for (VirtualFile vf : classpathVFs)
               {
                  try
                  {
                     String path = vf.getPathName();
                     if (path.startsWith(warPathName))
                     {
                        path = path.substring(warPathName.length());
                        URL pathURL = new URL(expandedWarURL, path);
                        classpath.add(pathURL);
                     }
                     else
                     {
                        log.debug("Ignoring path element: " + vf);
                     }
                  }
                  catch (Exception e)
                  {
                     log.debug("Ignoring path element: " + vf, e);
                  }
               }
               unit.addAttachment("org.jboss.web.expandedWarClasspath", classpath);
            }

            // Indicate that an expanded URL exists
            unit.addAttachment("org.jboss.web.expandedWarURL", expandedWarURL, URL.class);

            // Resolve any ear relative alt-dd path to an expWarUrl/WEB-INF/alt-dd.xml file
            String altDDPath = metaData.getAlternativeDD();
            if (altDDPath != null)
            {
               // First see if this is already a war local dd
               VirtualFile altDD = vfsUnit.getMetaDataFile(altDDPath);
               if (altDD == null)
               {
                  // Pass absolute paths through
                  File file = new File(altDDPath);
                  if (!file.exists() || !file.isAbsolute())
                  {
                     // Should be an relative to the top deployment
                     VFSDeploymentUnit topUnit = vfsUnit.getTopLevel();
                     if (topUnit == unit)
                        throw new DeploymentException("Unable to resolve " + altDDPath + " as WEB-INF path");
                     altDD = topUnit.getFile(altDDPath);
                     if (altDD == null)
                        throw new DeploymentException("Unable to resolve " + altDDPath + " as a deployment path");
                     
                     VirtualFile altDDFile = root.getChild("WEB-INF/" + altDD.getName());
                     log.debug("Copying the altDD to: " + altDDFile);
                     VFSUtils.writeFile(altDDFile, altDD.openStream());
                     metaData.setAlternativeDD(altDDFile.getPathName());
                  }
               }
            }
         }

         ClassLoadingMetaData classLoading = metaData.getClassLoading();
         if (classLoading == null)
            classLoading = new ClassLoadingMetaData();
         // pass in the java2ClassLoadingCompliance if it was not set at the war level
         if (classLoading.wasJava2ClassLoadingComplianceSet() == false)
            classLoading.setJava2ClassLoadingCompliance(this.java2ClassLoadingCompliance);
         metaData.setClassLoading(classLoading);

         // Build the context root if its not been set or is specified at the ear
         String webContext = metaData.getContextRoot();
         webContext = buildWebContext(webContext, warName, metaData, unit);
         metaData.setContextRoot(webContext);

         AbstractWarDeployment deployment = getDeployment(unit, metaData);
         deployment.setMainDeployer(mainDeployer);
         // TODO: until deployment is a MC bean
         deployment.setPersistenceUnitDependencyResolver(persistenceUnitDependencyResolver);
         deployWebModule(unit, metaData, deployment);
      }
      catch (Exception e)
      {
         throw new DeploymentException("Failed to create web module", e);
      }
   }

   /**
    * Cleanup war deployer specifics.
    */
   @Override
   public void undeploy(DeploymentUnit unit, JBossWebMetaData metaData)
   {
      try
      {
         // Delete any expanded war
         URL warURL = unit.getAttachment("org.jboss.web.expandedWarURL", URL.class);
         if (warURL != null)
         {
            // use vfs to cleanup/delete - since we created it with vfs
            VirtualFile file = VFS.getChild(warURL);
         }
      }
      catch (Exception e)
      {
         log.debug("Failed to remove expanded war", e);
      }
      /* Clear class loader refs
       metaData.setContextLoader(null);
       metaData.setResourceClassLoader(null);
       metaData.setENCLoader(null);
       */
   }

   public void addDeployedApp(String warURL, WebApplication webApp)
   {
      deploymentMap.put(warURL, webApp);
   }

   /** Get the WebApplication object for a deployed war.
    @param warUrl the war url string as originally passed to deploy().
    @return The WebApplication created during the deploy step if the
    warUrl is valid, null if no such deployment exists.
    */
   public WebApplication getDeployedApp(String warUrl)
   {
      return (WebApplication)deploymentMap.get(warUrl);
   }

   public WebApplication removeDeployedApp(String warURL)
   {
      return (WebApplication)deploymentMap.remove(warURL);
   }

   /** Returns the applications deployed by the web container subclasses.
    @jmx:managed-attribute
    @return An Iterator of WebApplication objects for the deployed wars.
    */
   public Iterator getDeployedApplications()
   {
      return deploymentMap.values().iterator();
   }

   /** A utility method that uses reflection to access a URL[] getURLs method
    * so that non-URLClassLoader class loaders that support this method can
    * provide info.
    */
   public static URL[] getClassLoaderURLs(ClassLoader cl)
   {
      URL[] urls = {};
      try
      {
         Class returnType = urls.getClass();
         Class[] parameterTypes = {};
         Method getURLs = cl.getClass().getMethod("getURLs", parameterTypes);
         if (returnType.isAssignableFrom(getURLs.getReturnType()))
         {
            Object[] args = {};
            urls = (URL[])getURLs.invoke(cl, args);
         }
         if (urls == null || urls.length == 0)
         {
            getURLs = cl.getClass().getMethod("getAllURLs", parameterTypes);
            if (returnType.isAssignableFrom(getURLs.getReturnType()))
            {
               Object[] args = {};
               urls = (URL[])getURLs.invoke(cl, args);
            }
         }
      }
      catch (Exception ignore)
      {
      }
      return urls;
   }

   /** This method creates a context-root string from either the
    WEB-INF/jboss-web.xml context-root element is one exists, or the
    filename portion of the warURL. It is called if the deployment
    webContext value is null which indicates a standalone war deployment.
    A war name of ROOT.war is handled as a special case of a war that
    should be installed as the default web context.
    @param ctxPath - war level context-root
    @param warName -
    */
   protected String buildWebContext(String ctxPath, String warName, JBossWebMetaData metaData, DeploymentUnit unit)
   {
      // Build a war root context from the war name if one was not specified
      String webContext = ctxPath;

      // Build the context from the deployment name
      if (webContext == null)
      {
         // Build the context from the war name, strip the .war suffix
         webContext = warName;
         webContext = webContext.replace('\\', '/');
         if (webContext.endsWith("/"))
            webContext = webContext.substring(0, webContext.length() - 1);
         int prefix = webContext.lastIndexOf('/');
         if (prefix > 0)
            webContext = webContext.substring(prefix + 1);
         int suffix = webContext.lastIndexOf(".war");
         if (suffix > 0)
            webContext = webContext.substring(0, suffix);
         // Strip any '<int-value>.' prefix
         int index = 0;
         for (; index < webContext.length(); index++)
         {
            char c = webContext.charAt(index);
            if (Character.isDigit(c) == false && c != '.')
               break;
         }
         webContext = webContext.substring(index);
      }

      // Servlet containers are anal about the web context starting with '/'
      if (webContext.length() > 0 && webContext.charAt(0) != '/')
         webContext = "/" + webContext;
      // And also the default root context must be an empty string, not '/'
      else if (webContext.equals("/"))
         webContext = "";
      return webContext;
   }

   /**
    * TODO: The use of an MBeanServer needs to be removed
    * @return
    */
   @Deprecated
   protected MBeanServer getServer()
   {
      return server;
   }

   public MainDeployerInternals getMainDeployer()
   {
      return mainDeployer;
   }

   public void setMainDeployer(MainDeployerInternals mainDeployer)
   {
      this.mainDeployer = mainDeployer;
   }

   /**
    * Get the object name of the ServiceMetaData instance associated with
    * the WebMetaData. This uses the pattern:
    * "jboss.web.deployment:war="+metaData.getContextRoot()
    * if there are no virtual hosts, otherwise
    * "jboss.web.deployment:war="+metaData.getVirtualHosts()[0]+metaData.getContextRoot()
    * @param metaData - the web app metaData
    * @return the war object name
    */
   protected String getObjectName(JBossWebMetaData metaData)
   {
      // Obtain the war virtual host and context root to define a unique war name
      String virtualHost = "";
      List<String> hosts = metaData.getVirtualHosts();
      if (hosts != null && hosts.size() > 0)
      {
         virtualHost = hosts.get(0);
      }
      String ctxPath = metaData.getContextRoot();
      // The ctx path value cannot be empty in the object name
      if (ctxPath == null || ctxPath.length() == 0)
         ctxPath = "/";
      return "jboss.web.deployment:war=" + virtualHost + ctxPath;
   }

   /**
    * Called by deploy first to create a ServiceMetaData instance that wraps the
    * AbstractWarDeployment bean and then attach it to the deployment unit. The
    * presence of the ServiceMetaData attachment makes the deployment unit
    * "relevant" to the deployers that handle mbean services.
    *
    * @param unit - the deployment unit
    * @param metaData - the web app metadata passed to deploy
    * @param deployment - the web app deployment bean created by getDeployment
    * @throws Exception
    */
   protected void deployWebModule(DeploymentUnit unit, JBossWebMetaData metaData, AbstractWarDeployment deployment) throws Exception
   {
      log.debug("deployWebModule: " + unit.getName());
      try
      {
         ServiceMetaData webModule = new ServiceMetaData();
         String name = getObjectName(metaData);
         ObjectName objectName = new ObjectName(name);
         webModule.setObjectName(objectName);
         webModule.setCode(WebModule.class.getName());
         // WebModule(DeploymentUnit, AbstractWarDeployer, AbstractWarDeployment)
         ServiceConstructorMetaData constructor = new ServiceConstructorMetaData();
         constructor.setSignature(new String[] { DeploymentUnit.class.getName(), AbstractWarDeployer.class.getName(), AbstractWarDeployment.class.getName() });
         constructor.setParameters(new Object[] { unit, this, deployment });
         webModule.setConstructor(constructor);

         List<ServiceAttributeMetaData> attrs = new ArrayList<ServiceAttributeMetaData>();

         ServiceAttributeMetaData attr = new ServiceAttributeMetaData();
         attr.setName("SecurityManagement");
         ServiceInjectionValueMetaData injectionValue = new ServiceInjectionValueMetaData(deployment.getSecurityManagementName());
         attr.setValue(injectionValue);
         attrs.add(attr);

         ServiceAttributeMetaData attrPR = new ServiceAttributeMetaData();
         attrPR.setName("PolicyRegistration");
         ServiceInjectionValueMetaData injectionValuePR = new ServiceInjectionValueMetaData(deployment.getPolicyRegistrationName());
         attrPR.setValue(injectionValuePR);
         attrs.add(attrPR);

         ServiceAttributeMetaData attrKernel = new ServiceAttributeMetaData();
         attrKernel.setName("Kernel");
         ServiceInjectionValueMetaData injectionValueKernel = new ServiceInjectionValueMetaData(KernelConstants.KERNEL_NAME);
         attrKernel.setValue(injectionValueKernel);
         attrs.add(attrKernel);

         webModule.setAttributes(attrs);

         // Dependencies...Still have old jmx names here
         Collection<String> depends = metaData.getDepends();
         List<ServiceDependencyMetaData> dependencies = new ArrayList<ServiceDependencyMetaData>();
         if (depends != null && depends.isEmpty() == false)
         {
            if (log.isTraceEnabled())
               log.trace(name + " has dependencies: " + depends);

            for (String iDependOn : depends)
            {
               ServiceDependencyMetaData sdmd = new ServiceDependencyMetaData();
               sdmd.setIDependOn(iDependOn);
               dependencies.add(sdmd);
            }
         }
         
         // SwitchBoard
         Barrier switchBoard = unit.getAttachment(Barrier.class);
         if (switchBoard != null)
         {
            // Setup switchboard dependency on the deployment
            ServiceDependencyMetaData switchBoardDependency = new AnyStateServiceDependencyMetaData(switchBoard.getId(), ControllerState.START, ControllerState.INSTALLED);
            dependencies.add(switchBoardDependency);
            log.debug("Added switchboard dependency: " + switchBoard.getId() + " for web module: " + name);
         }
         // Injection Manager
         InjectionManager injectionManager = unit.getAttachment(InjectionManager.class);
         if (injectionManager != null)
         {
            // set the InjectionManager on the deployment
            deployment.setInjectionManager(injectionManager);
            // Setup the Injector
            Environment webEnvironment = metaData.getJndiEnvironmentRefsGroup();
            if (webEnvironment != null)
            {
               // convert JBMETA metadata to jboss-injection specific metadata
               JndiEnvironmentRefsGroup jndiEnvironment = new JndiEnvironmentImpl(webEnvironment, unit.getClassLoader());
               // For optimization, we'll create an Injector only if there's atleast one InjectionTarget
               if (this.hasInjectionTargets(jndiEnvironment))
               {
                  // create the injector
                  EEInjector eeInjector = new EEInjector(jndiEnvironment);
                  // add the injector the injection manager
                  injectionManager.addInjector(eeInjector);
                  // Deploy the Injector as a MC bean (so that the fully populated naming context (obtained via the SwitchBoard
                  // Barrier) gets injected.
                  String injectorMCBeanName = this.getInjectorMCBeanName(unit);
                  BeanMetaData injectorBMD = this.createInjectorBMD(injectorMCBeanName, eeInjector, switchBoard);
                  unit.addAttachment(BeanMetaData.class + ":" + injectorMCBeanName, injectorBMD);
                  
                  // Add the Injector dependency on the deployment (so that the DU doesn't
                  // get started till the Injector is available)
                  ServiceDependencyMetaData injectorDepdendency = new ServiceDependencyMetaData();
                  injectorDepdendency.setIDependOn(injectorMCBeanName);
                  dependencies.add(injectorDepdendency);
                  log.debug("Added Injector dependency: " + injectorMCBeanName + " for web module: " + name);
               }
            }
         }
         
         // TODO: We haven't yet integrated PC and EJB reference providers in SwitchBoard.
         // The following sections will be removed after the RPs are made available

         // JBAS-6795 Add dependency on PersistenceContext references
         PersistenceContextReferencesMetaData pcRefs = metaData.getPersistenceContextRefs();
         if (pcRefs != null)
         {
            for (PersistenceContextReferenceMetaData pcRef : metaData.getPersistenceContextRefs())
            {
               // TODO: this is a duplication of the logic in PersistenceContextHandler
               String persistenceUnitName = pcRef.getPersistenceUnitName();
               String beanName = persistenceUnitDependencyResolver.resolvePersistenceUnitSupplier(unit, persistenceUnitName);
               ServiceDependencyMetaData sdmd = new ServiceDependencyMetaData();
               sdmd.setIDependOn(beanName);
               dependencies.add(sdmd);
            }
         }

         webModule.setDependencies(dependencies);

         // Here's where a bit of magic happens. By attaching the ServiceMetaData
         // to the deployment, we now make the deployment "relevant" to
         // deployers that use ServiceMetaData as an input (e.g. the
         // org.jboss.system.deployers.ServiceDeployer). Those deployers
         // can now take over deploying the web module.

         unit.addAttachment("WarServiceMetaData", webModule, ServiceMetaData.class);
      }
      catch (Exception e)
      {
         throw DeploymentException.rethrowAsDeploymentException("Error creating web module " + unit.getName(), e);
      }
   }

   @ManagementObject(name = "ContextMO", componentType = @ManagementComponent(type = "WAR", subtype="Context"))
   public static class ContextMO
   {
      @ManagementPropertyFactory(ManagedPropertyImpl.class)
      @ManagementProperty(use = {ViewUse.RUNTIME}, readOnly = true)
      public String getContextRoot() { return ""; }
   }

   public void build(DeploymentUnit unit, Set<String> outputs,
         Map<String, ManagedObject> managedObjects) throws DeploymentException
   {
      JBossWebMetaData meta = unit.getAttachment(JBossWebMetaData.class);
      if (meta == null)
         return;

      ManagedObject mo = ManagedObjectFactory.getInstance().createManagedObject(ContextMO.class);
      if (mo == null)
         throw new DeploymentException("could not create managed object");

      mo.getProperty("contextRoot").setValue(SimpleValueSupport.wrap(meta.getContextRoot()));
      managedObjects.put("ContextMO", mo);
   }
   
   /**
    * 
    * Similar to {@link ServiceDependencyMetaData} except that this allows to specify the "whenRequired" and
    * "dependentState" {@link ControllerState ControllerState} 
    * 
    */
   private class AnyStateServiceDependencyMetaData extends ServiceDependencyMetaData
   {
      
      private ControllerState whenRequired;
      
      private ControllerState dependentState;
      
      public AnyStateServiceDependencyMetaData(String iDependOn, ControllerState whenRequired, ControllerState dependentState)
      {
         this.setIDependOn(iDependOn);
         this.whenRequired = whenRequired;
         this.dependentState = dependentState;
      }
      /**
       * @see ServiceDependencyMetaData#visit(ServiceMetaDataVisitor)
       * 
       */
      @Override
      public void visit(ServiceMetaDataVisitor visitor)
      {
         ServiceControllerContext context = visitor.getControllerContext();
         Object name = context.getName();
         Object dependency = this.getIDependOn();
         try
         {
            dependency = getIDependOnObjectName().getCanonicalName();
         }
         catch (MalformedObjectNameException ignored)
         {
         }
         visitor.addDependency(new AbstractDependencyItem(name, dependency, this.whenRequired, this.dependentState));
         visitor.visit(this);
      }
   }
   
   /**
    * Creates and returns {@link BeanMetaData} for the passed {@link EEInjector injector} and sets up
    * dependency on the passed {@link Barrier SwitchBoard barrier}.
    * 
    * @param injectorMCBeanName
    * @param injector
    * @param barrier
    * @return
    */
   protected BeanMetaData createInjectorBMD(String injectorMCBeanName, EEInjector injector, Barrier barrier)
   {
      BeanMetaDataBuilder builder = BeanMetaDataBuilderFactory.createBuilder(injectorMCBeanName, injector.getClass().getName());
      builder.setConstructorValue(injector);

      // add dependency on SwitchBoard Barrier
      builder.addDemand(barrier.getId(), ControllerState.CREATE, ControllerState.START, null);
      
      // return the Injector BMD
      return builder.getBeanMetaData();
   }
   

   /**
    * Returns the MC bean name for a {@link Injector injector} belonging to the 
    * {@link InjectionManager InjectionManager} for the passed {@link DeploymentUnit deployment unit}
    * 
    * @param unit
    * @return
    */
   protected String getInjectorMCBeanName(DeploymentUnit unit)
   {
      StringBuilder sb = new StringBuilder("jboss-injector:");
      String appName = this.componentInformer.getApplicationName(unit);
      if (appName != null)
      {
         sb.append("appName=");
         sb.append(appName);
         sb.append(",");
      }
      String moduleName = this.componentInformer.getModuleName(unit);
      sb.append("module=");
      sb.append(moduleName);

      return sb.toString();
   }
   
   /**
    * Returns true if the passed {@link JndiEnvironmentRefsGroup} has atleast one {@link EnvironmentEntryType environment entry}
    * with an {@link InjectionTargetType injection target}. Else, returns false
    *  
    * @param jndiEnv
    * @return
    */
   private boolean hasInjectionTargets(JndiEnvironmentRefsGroup jndiEnv)
   {
      Collection<EnvironmentEntryType> envEntries = jndiEnv.getEntries();
      if (envEntries == null || envEntries.isEmpty())
      {
         return false;
      }
      for (EnvironmentEntryType envEntry : envEntries)
      {
         Collection<InjectionTargetType> injectionTargets = envEntry.getInjectionTargets();
         if (injectionTargets != null && !injectionTargets.isEmpty())
         {
            return true;
         }
      }
      return false;   
   }
   
   
}
