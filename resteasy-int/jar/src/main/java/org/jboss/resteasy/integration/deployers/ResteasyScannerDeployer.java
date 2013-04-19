package org.jboss.resteasy.integration.deployers;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.resteasy.plugins.server.servlet.Filter30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrapClasses;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.scanning.annotations.spi.AnnotationIndex;
import org.jboss.scanning.annotations.spi.AnnotationRepository;
import org.jboss.scanning.annotations.spi.Element;
import org.jboss.scanning.hierarchy.spi.HierarchyIndex;

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Use cases:
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResteasyScannerDeployer extends AbstractRealDeployer
{
   private static final Logger LOGGER = Logger.getLogger(ResteasyScannerDeployer.class);
   public static final Set<String> BOOT_CLASSES = new HashSet<String>();

   static
   {
      for (String clazz : ResteasyBootstrapClasses.BOOTSTRAP_CLASSES)
      {
         BOOT_CLASSES.add(clazz);
      }
   }

   public ResteasyScannerDeployer()
   {
      super();

      addRequiredInput(JBossWebMetaData.class);
      addInput(AnnotationRepository.class);
      addOutput(JBossWebMetaData.class);
      addOutput(ResteasyDeploymentData.class);
      setStage(DeploymentStages.PRE_REAL); // TODO -- right stage?
   }

   protected void internalDeploy(DeploymentUnit du) throws DeploymentException
   {
      JBossWebMetaData webdata = du.getAttachment(JBossWebMetaData.class);
      scan(du, webdata);

   }

   /**
    * If any servlet/filter classes are declared, then we probably don't want to scan.
    *
    * @param du
    * @param webdata
    * @return
    * @throws DeploymentException
    */
   protected boolean hasBootClasses(DeploymentUnit du, JBossWebMetaData webdata) throws DeploymentException
   {
      if (webdata.getServlets() != null)
      {
         for (ServletMetaData servlet : webdata.getServlets())
         {
            String servletClass = servlet.getServletClass();
            if (BOOT_CLASSES.contains(servletClass)) return true;
         }
      }
      if (webdata.getFilters() != null)
      {
         for (FilterMetaData filter : webdata.getFilters())
         {
            if (BOOT_CLASSES.contains(filter.getFilterClass())) return true;
         }
      }
      return false;

   }

   protected void scan(DeploymentUnit du, JBossWebMetaData webdata)
           throws DeploymentException
   {
      ResteasyDeploymentData resteasyDeploymentData = new ResteasyDeploymentData();
      du.addAttachment(ResteasyDeploymentData.class, resteasyDeploymentData);

      // If there is a resteasy boot class in web.xml, then the default should be to not scan
      // make sure this call happens before checkDeclaredApplicationClassAsServlet!!!
      boolean hasBoot = hasBootClasses(du, webdata);
      resteasyDeploymentData.setBootClasses(hasBoot);

      Class declaredApplicationClass = checkDeclaredApplicationClassAsServlet(du, webdata);
      // Assume that checkDeclaredApplicationClassAsServlet created the dispatcher
      if (declaredApplicationClass != null) resteasyDeploymentData.setDispatcherCreated(true);


      // set scanning on only if there are no boot classes
      if (hasBoot == false && !webdata.isMetadataComplete())
      {
         resteasyDeploymentData.setScanAll(true);
         resteasyDeploymentData.setScanProviders(true);
         resteasyDeploymentData.setScanResources(true);
      }

      // check resteasy configuration flags


      List<ParamValueMetaData> contextParams = webdata.getContextParams();

      if (contextParams != null)
      {
         for (ParamValueMetaData param : contextParams)
         {
            if (param.getParamName().equals(ResteasyContextParameters.RESTEASY_SCAN))
            {
               resteasyDeploymentData.setScanAll(Boolean.valueOf(param.getParamValue()));
            }
            else if (param.getParamName().equals(ResteasyContextParameters.RESTEASY_SCAN_PROVIDERS))
            {
               resteasyDeploymentData.setScanProviders(Boolean.valueOf(param.getParamValue()));
            }
            else if (param.getParamName().equals(ResteasyContextParameters.RESTEASY_SCAN_RESOURCES))
            {
               resteasyDeploymentData.setScanResources(Boolean.valueOf(param.getParamValue()));
            }
            else if (param.getParamName().equals(ResteasyContextParameters.RESTEASY_UNWRAPPED_EXCEPTIONS))
            {
               resteasyDeploymentData.setUnwrappedExceptionsParameterSet(true);
            }
         }
      }

      if (!resteasyDeploymentData.shouldScan())
      {
         return;
      }

      // look for Application class.  Don't scan for one if there is a declared one.
      if (declaredApplicationClass == null)
      {
         HierarchyIndex hier = du.getAttachment(HierarchyIndex.class);
         if (hier != null)
         {
            Set<Class<? extends Application>> applicationClass = hier.getSubClassesOf(null, Application.class);
            if (applicationClass != null)
            {
               if (applicationClass.size() > 1)
               {
                  StringBuilder builder = new StringBuilder("Only one JAX-RS Application Class allowed.");
                  Set<Class<? extends Application>> aClasses = new HashSet<Class<? extends Application>>();
                  for (Class c : applicationClass)
                  {
                     if (!c.isAnonymousClass() && !Modifier.isAbstract(c.getModifiers()))
                     {
                        aClasses.add(c);
                     }
                     builder.append(" ").append(c.getName());
                  }
                  if (aClasses.size() > 1) throw new DeploymentException(builder.toString());
                  else if (aClasses.size() == 1)
                  {
                     Class<? extends Application> aClass = applicationClass.iterator().next();
                     resteasyDeploymentData.setScannedApplicationClass(aClass);
                  }
               }
               else if (applicationClass.size() == 1)
               {
                  Class<? extends Application> aClass = applicationClass.iterator().next();
                  resteasyDeploymentData.setScannedApplicationClass(aClass);
               }
            }
         }
         else
         {
            LOGGER.debug("Expecting HierarchyIndex class for scanning WAR for JAX-RS Application class");
         }
      }

      // Looked for annotated resources and providers
      AnnotationIndex env = du.getAttachment(AnnotationIndex.class);
      if (env == null)
      {
         LOGGER.debug("Expecting AnnotationRepository class for scanning WAR for JAX-RS classes");
         return;
      }


      Set<Element<Path, Class<?>>> resources = null;
      Set<Element<Provider, Class<?>>> providers = null;
      if (resteasyDeploymentData.isScanResources())
      {
         resources = env.classIsAnnotatedWith(Path.class);
      }
      if (resteasyDeploymentData.isScanProviders())
      {
         providers = env.classIsAnnotatedWith(Provider.class);
      }

      if ((resources == null || resources.isEmpty()) && (providers == null || providers.isEmpty())) return;

      if (resources != null)
      {
         for (Element e : resources)
         {
            if (e.getOwner().isInterface())
            {
               continue;
            }
            resteasyDeploymentData.getScannedResourceClasses().add(e.getOwnerClassName());
         }
      }
      if (providers != null)
      {
         for (Element e : providers)
         {
            if (e.getOwner().isInterface()) continue;
            resteasyDeploymentData.getScannedProviderClasses().add(e.getOwnerClassName());
         }
      }

      Set<String> strings = env.classesImplementingInterfacesAnnotatedWith(Path.class.getName());
      resteasyDeploymentData.getScannedResourceClasses().addAll(strings);
   }

   protected Class checkDeclaredApplicationClassAsServlet(DeploymentUnit du, JBossWebMetaData webdata) throws DeploymentException
   {
      ClassLoader loader = du.getClassLoader();
      if (webdata.getServlets() == null) return null;

      for (ServletMetaData servlet : webdata.getServlets())
      {
         String servletClass = servlet.getServletClass();
         if (servletClass == null) continue;
         Class clazz = null;
         try
         {
            clazz = loader.loadClass(servletClass);
         }
         catch (ClassNotFoundException e)
         {
            throw new DeploymentException(e);
         }
         if (Application.class.isAssignableFrom(clazz))
         {
            servlet.setServletClass(HttpServlet30Dispatcher.class.getName());
            servlet.setAsyncSupported(true);
            ParamValueMetaData param = new ParamValueMetaData();
            param.setParamName("javax.ws.rs.Application");
            param.setParamValue(servletClass);
            List<ParamValueMetaData> params = servlet.getInitParam();
            if (params == null)
            {
               params = new ArrayList<ParamValueMetaData>();
               servlet.setInitParam(params);
            }
            params.add(param);

            try
            {
               Thread.currentThread().getContextClassLoader().loadClass(ResteasyIntegrationDeployer.CDI_INJECTOR_FACTORY_CLASS);
               // don't set this param if it is not in classpath
               if (((VFSDeploymentUnit) du).getMetaDataFile("beans.xml") != null)
               {
                  ResteasyIntegrationDeployer.setContextParameter(webdata, "resteasy.injector.factory", ResteasyIntegrationDeployer.CDI_INJECTOR_FACTORY_CLASS);
               }
            }
            catch (ClassNotFoundException ignored)
            {
            }
            return clazz;
         }
      }
      return null;
   }

   

}
