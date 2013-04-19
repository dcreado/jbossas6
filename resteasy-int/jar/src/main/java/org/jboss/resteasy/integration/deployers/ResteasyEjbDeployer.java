package org.jboss.resteasy.integration.deployers;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.ejb.deployers.MergedJBossMetaDataDeployer;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.resteasy.util.GetRestful;

import javax.ejb.Local;
import javax.ejb.LocalHome;
import javax.ejb.Remote;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResteasyEjbDeployer extends AbstractRealDeployer
{
   private static final Logger LOGGER = Logger.getLogger(ResteasyEjbDeployer.class);

   public ResteasyEjbDeployer()
   {
      super();

      addRequiredInput(JBossWebMetaData.class);
      addRequiredInput(ResteasyDeploymentData.class);
      addRequiredInput(MergedJBossMetaDataDeployer.EJB_MERGED_ATTACHMENT_NAME);
      addOutput(JBossWebMetaData.class);
      setStage(DeploymentStages.PRE_REAL); // TODO -- right stage?
   }

   protected void internalDeploy(DeploymentUnit du) throws DeploymentException
   {
      ResteasyDeploymentData resteasy = du.getAttachment(ResteasyDeploymentData.class);

      /*
      log.info("*******************");
      log.info("*** Attachments ***");
      log.info("*******************");
      for (String attachment : du.getAttachments().keySet())
      {
         log.info(">>> " + attachment);
      }

      if (true) return;
      */

      // right now I only support resources
      if (!resteasy.isScanResources()) return;
      if (((VFSDeploymentUnit) du).getMetaDataFile("beans.xml") != null) return;

      JBossMetaData ejbs = (JBossMetaData) du
              .getAttachment(MergedJBossMetaDataDeployer.EJB_MERGED_ATTACHMENT_NAME);
      ClassLoader loader = du.getClassLoader();
      for (final JBossEnterpriseBeanMetaData ejb : ejbs.getEnterpriseBeans())
      {
         Class ejbClass = null;
         try
         {
            ejbClass = loader.loadClass(ejb.getEjbClass());
         }
         catch (ClassNotFoundException e)
         {
            throw new RuntimeException(e);
         }
         if (!GetRestful.isRootResource(ejbClass)) continue;
         String jndiName = getLocalJndiName(ejb, ejbClass);
         log.debug("Found JAX-RS EJB: " + ejbClass.getName() + " local jndi name: " + jndiName);
         StringBuffer buf = new StringBuffer();
         buf.append(jndiName).append(";").append(ejbClass.getName()).append(";").append("true");

         resteasy.getScannedJndiComponentResources().add(buf.toString());
         // make sure its removed from list
         resteasy.getScannedResourceClasses().remove(ejbClass.getName());
      }
   }

   private static String getLocalJndiName(JBossEnterpriseBeanMetaData ejb, Class<?> ejbClass)
   {
      // See if local binding is explicitly-defined
      LocalBinding localBinding = ejbClass.getAnnotation(LocalBinding.class);

      // If none specified
      if (localBinding == null || (localBinding.jndiBinding() != null && localBinding.jndiBinding().trim().length() == 0))
      {
         String name = ejb.getLocalJndiName();
         return name;
      }
      // Local Binding was explicitly-specified, use it
      else
      {
         return localBinding.jndiBinding();
      }
   }


   public static Set<Class<?>> getBusinessInterfaces(Class<?> beanClass)
   {
      // Obtain all business interfaces implemented by this bean class and its superclasses
      return getBusinessInterfaces(beanClass, new HashSet<Class<?>>());
   }

   /**
    * Resolve the potential business interfaces on an enterprise bean.
    * Returns all interfaces implemented by this class and, optionally, its supers which
    * are potentially a business interface.
    * <p/>
    * Note: for normal operation call container.getBusinessInterfaces().
    *
    * @param beanClass     the EJB implementation class
    * @param includeSupers Whether or not to include superclasses of the specified beanClass in this check
    * @return a list of potential business interfaces
    * @see org.jboss.ejb3.EJBContainer#getBusinessInterfaces()
    */
   public static Set<Class<?>> getBusinessInterfaces(Class<?> beanClass, boolean includeSupers)
   {
      // Obtain all business interfaces implemented by this bean class and optionally, its superclass
      return getBusinessInterfaces(beanClass, new HashSet<Class<?>>(), includeSupers);
   }

   private static Set<Class<?>> getBusinessInterfaces(Class<?> beanClass, Set<Class<?>> interfaces)
   {
      return getBusinessInterfaces(beanClass, interfaces, true);
   }

   private static Set<Class<?>> getBusinessInterfaces(Class<?> beanClass, Set<Class<?>> interfaces,
                                                      boolean includeSupers)
   {
      /*
       * 4.6.6:
       * The following interfaces are excluded when determining whether the bean class has
       * more than one interface: java.io.Serializable; java.io.Externalizable;
       * any of the interfaces defined by the javax.ejb package.
       */
      for (Class<?> intf : beanClass.getInterfaces())
      {
         if (intf.equals(java.io.Externalizable.class))
            continue;
         if (intf.equals(java.io.Serializable.class))
            continue;
         if (intf.getName().startsWith("javax.ejb"))
            continue;

         // FIXME Other aop frameworks might add other interfaces, this should really be configurable
         if (intf.getName().startsWith("org.jboss.aop"))
            continue;

         interfaces.add(intf);
      }

      // If there's no superclass, or we shouldn't check the superclass, return
      if (!includeSupers || beanClass.getSuperclass() == null)
      {
         return interfaces;
      }
      else
      {
         // Include any superclasses' interfaces
         return getBusinessInterfaces(beanClass.getSuperclass(), interfaces);
      }
   }


   public static Class<?>[] getLocalInterfaces(Class<?> beanClass)
   {
      // Initialize
      Set<Class<?>> localAndBusinessLocalInterfaces = new HashSet<Class<?>>();

      // Obtain @Local
      Local localAnnotation = beanClass.getAnnotation(Local.class);

      // Obtain @LocalHome
      LocalHome localHomeAnnotation = beanClass.getAnnotation(LocalHome.class);

      // Obtain @Remote
      Remote remoteAnnotation = beanClass.getAnnotation(Remote.class);

      // Obtain Remote and Business Remote interfaces
      //Class<?>[] remoteAndBusinessRemoteInterfaces = ProxyFactoryHelper.getRemoteAndBusinessRemoteInterfaces(container);

      // Obtain all business interfaces from the bean class
      Set<Class<?>> businessInterfacesImplementedByBeanClass = getBusinessInterfaces(beanClass);

      // Obtain all business interfaces directly implemented by the bean class (not including supers)
      Set<Class<?>> businessInterfacesDirectlyImplementedByBeanClass = getBusinessInterfaces(
              beanClass, false);

      // For each of the business interfaces implemented by the bean class
      for (Class<?> clazz : businessInterfacesImplementedByBeanClass)
      {
         // If @Local is on the interface
         if (clazz.isAnnotationPresent(Local.class))
         {
            // Add to the list of locals
            localAndBusinessLocalInterfaces.add(clazz);
         }
      }

      // EJBTHREE-1062
      // EJB 3 Core Specification 4.6.6
      // If bean class implements a single interface, that interface is assumed to be the
      // business interface of the bean. This business interface will be a local interface unless the
      // interface is designated as a remote business interface by use of the Remote
      // annotation on the bean class or interface or by means of the deployment descriptor.
      if (businessInterfacesDirectlyImplementedByBeanClass.size() == 1 && localAndBusinessLocalInterfaces.size() == 0)
      {
         // Obtain the implemented interface
         Class<?> singleInterface = businessInterfacesDirectlyImplementedByBeanClass.iterator().next();

         // If not explicitly marked as @Remote, and is a valid business interface
         if (remoteAnnotation == null && singleInterface.getAnnotation(Remote.class) == null)
         {
            // Return the implemented interface, adding to the container
            Class<?>[] returnValue = new Class[]
                    {singleInterface};
            return returnValue;
         }
      }

      // @Local was defined
      if (localAnnotation != null)
      {
         // If @Local has no value or empty value
         if (localAnnotation.value() == null || localAnnotation.value().length == 0)
         {
            // If @Local is defined with no value and there are no business interfaces
            if (businessInterfacesImplementedByBeanClass.size() == 0)
            {
               return new Class<?>[]
                       {};
            }
            // If more than one business interface is directly implemented by the bean class
            else if (businessInterfacesImplementedByBeanClass.size() > 1)
            {
               return new Class<?>[]
                       {};
            }
            // JIRA EJBTHREE-1062
            // EJB 3 4.6.6
            // If the bean class implements only one business interface, that
            //interface is exposed as local business if not denoted as @Remote
            else
            {
               // If not explicitly marked as @Remote
               if (remoteAnnotation == null)
               {
                  // Return the implemented interface and add to container
                  Class<?>[] returnValue = businessInterfacesImplementedByBeanClass.toArray(new Class<?>[]
                          {});
                  return returnValue;
               }
            }
         }
         // @Local has value
         else
         {
            // For each of the interfaces in @Local.value
            for (Class<?> clazz : localAnnotation.value())
            {
               // Add to the list of locals
               localAndBusinessLocalInterfaces.add(clazz);
            }

            // For each of the business interfaces implemented by the bean class
            for (Class<?> clazz : businessInterfacesImplementedByBeanClass)
            {
               // If @Local is on the interface
               if (clazz.isAnnotationPresent(Local.class))
               {
                  // Add to the list of locals
                  localAndBusinessLocalInterfaces.add(clazz);
               }
            }
         }
      }

      // If local interfaces have been defined/discovered
      if (localAndBusinessLocalInterfaces.size() > 0)
      {
         // Return local interfaces, first adding to the container
         Class<?>[] rtn = localAndBusinessLocalInterfaces.toArray(new Class<?>[]
                 {});
         return rtn;
      }

      // No local or business local interfaces discovered
      return new Class<?>[]
              {};
   }


}