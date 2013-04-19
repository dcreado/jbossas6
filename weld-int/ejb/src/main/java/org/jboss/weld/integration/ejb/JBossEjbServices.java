package org.jboss.weld.integration.ejb;

import javax.naming.NamingException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.common.deployers.spi.AttachmentNames;
import org.jboss.ejb3.ejbref.resolver.spi.EjbReferenceResolver;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMessageDrivenBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;
import org.jboss.weld.ejb.api.SessionObjectReference;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.jboss.weld.integration.util.AbstractJBossServices;

/**
 * An implementation of EjbServices for JBoss EJB3
 *
 * @author Pete Muir
 * @author ales.justin@jboss.org
 * @author Marius Bogoevici
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class JBossEjbServices extends AbstractJBossServices implements EjbServices
{
   private EjbReferenceResolver resolver;
   private final List<EjbDescriptor<?>> ejbs = new ArrayList<EjbDescriptor<?>>();
   private final List<String> ejbContainerNames = new ArrayList<String>();
   private Map<String, InterceptorBindings> interceptorBindings = new ConcurrentHashMap<String, InterceptorBindings>();
   private Map<EjbDescriptor<?>, DeploymentUnit> deploymentUnitMap = new ConcurrentHashMap<EjbDescriptor<?>, DeploymentUnit>();

   public JBossEjbServices() throws NamingException
   {
      super();
   }

   public void setResolver(EjbReferenceResolver resolver)
   {
      this.resolver = resolver;
   }

   @Override
   public void setDeploymentUnit(DeploymentUnit du)
   {
      super.setDeploymentUnit(du);
      discoverEjbs(du.getTopLevel());
   }

   public SessionObjectReference resolveEjb(EjbDescriptor<?> ejbDescriptor)
   {
      try
      {
         return new JBossSessionObjectReference(ejbDescriptor, deploymentUnitMap.get(ejbDescriptor), context);
      }
      catch (NamingException e)
      {
         throw new RuntimeException("Error retreiving EJB from JNDI " + ejbDescriptor, e);
      }
   }

   public void registerInterceptors(EjbDescriptor<?> ejbDescriptor, InterceptorBindings interceptorBindings)
   {
      this.interceptorBindings.put(ejbDescriptor.getEjbName(), interceptorBindings);
   }

   public Object resolveRemoteEjb(String jndiName, String mappedName, String ejbLink)
   {
      if (mappedName != null)
      {
         try
         {
            return context.lookup(mappedName);
         }
         catch (NamingException e)
         {
            throw new RuntimeException("Error retreiving EJB from JNDI with mappedName " + mappedName, e);
         }
      }
      else if (jndiName != null)
      {
         try
         {
            return context.lookup(jndiName);
         }
         catch (NamingException e)
         {
            throw new RuntimeException("Error retreiving EJB from JNDI with mappedName " + jndiName, e);
         }
      }
      else
      {
         throw new IllegalArgumentException("jndiName, mappedName and ejbLink are null");
      }
   }

   /**
    * Discover ejbs.
    *
    * @param du the deployment unit
    */
   protected void discoverEjbs(DeploymentUnit du)
   {
      // Ensure it's an EJB3 DU (by looking for the processed metadata)
      if (du.getAttachment(AttachmentNames.PROCESSED_METADATA, JBossMetaData.class) != null && du.getAttachment(JBossMetaData.class).isEJB3x())
      {
         JBossMetaData jBossMetaData = du.getAttachment(JBossMetaData.class);
         for (JBossEnterpriseBeanMetaData enterpriseBeanMetaData : jBossMetaData.getEnterpriseBeans())
         {
            if (enterpriseBeanMetaData.isSession() && enterpriseBeanMetaData instanceof JBossSessionBean31MetaData)
            {
               JBossSessionBean31MetaData sessionBeanMetaData = (JBossSessionBean31MetaData) enterpriseBeanMetaData;
               EjbDescriptor<?> ejbDescriptor = new JBossSessionBean31DescriptorAdaptor<Object>(sessionBeanMetaData, du, resolver);
               addEjbDescriptor(ejbDescriptor, du);
            }
            else if (enterpriseBeanMetaData.isSession())
            {
               JBossSessionBeanMetaData sessionBeanMetaData = (JBossSessionBeanMetaData) enterpriseBeanMetaData;
               EjbDescriptor<?> ejbDescriptor = new JBossSessionBeanDescriptorAdaptor<Object>(sessionBeanMetaData, du, resolver);
               addEjbDescriptor(ejbDescriptor, du);
            }
            else if (enterpriseBeanMetaData.isMessageDriven())
            {
               JBossMessageDrivenBeanMetaData messageDrivenBeanMetaData = (JBossMessageDrivenBeanMetaData) enterpriseBeanMetaData;
               EjbDescriptor<?> ejbDescriptor = new JBossMessageDrivenBeanDescriptorAdaptor<Object>(messageDrivenBeanMetaData, du, resolver);
               addEjbDescriptor(ejbDescriptor, du);
            }
            if (enterpriseBeanMetaData.getContainerName() != null)
            {
               ejbContainerNames.add(enterpriseBeanMetaData.getContainerName());
            }
            else
            {
               ejbContainerNames.add(enterpriseBeanMetaData.getGeneratedContainerName());
            }
         }
      }

      List<DeploymentUnit> children = du.getChildren();
      if (children != null && children.isEmpty() == false)
      {
         for (DeploymentUnit childDu : children)
         {
            discoverEjbs(childDu);
         }
      }
   }

   private void addEjbDescriptor(EjbDescriptor<?> ejbDescriptor, DeploymentUnit deploymentUnit)
   {
      deploymentUnitMap.put(ejbDescriptor, deploymentUnit);
      ejbs.add(ejbDescriptor);
   }

   public Iterable<EjbDescriptor<?>> getEjbs()
   {
      return ejbs;
   }

   /**
    * Get the names of all ejb container.
    *
    * @return all ejb container names
    */
   public Iterable<String> getEjbContainerNames()
   {
      return Collections.unmodifiableCollection(ejbContainerNames);
   }

   public InterceptorBindings getInterceptorBindings(String ejbName)
   {
      return interceptorBindings.get(ejbName);
   }

   @Override
   public void cleanup()
   {
      ejbContainerNames.clear();
      ejbs.clear();
      resolver = null;
   }
}