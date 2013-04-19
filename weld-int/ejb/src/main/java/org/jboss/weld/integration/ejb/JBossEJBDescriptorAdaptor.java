package org.jboss.weld.integration.ejb;

import java.io.Serializable;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.ejbref.resolver.spi.EjbReferenceResolver;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.jboss.weld.integration.util.Reflections;

public abstract class JBossEJBDescriptorAdaptor<T> implements EjbDescriptor<T>, Serializable
{
   private final Class<T> beanClass;
   private final String ejbName;

   public JBossEJBDescriptorAdaptor(JBossEnterpriseBeanMetaData enterpriseBeanMetaData, DeploymentUnit deploymentUnit, EjbReferenceResolver resolver)
   {
      if (enterpriseBeanMetaData.getEjbClass() != null)
      {
         try
         {
            this.beanClass = (Class<T>) Reflections.classForName(enterpriseBeanMetaData.getEjbClass(), deploymentUnit.getClassLoader());
         }
         catch (ClassCastException e)
         {
            throw new IllegalStateException("Error loading EJB Session bean class", e);
         }
         catch (ClassNotFoundException e)
         {
            throw new IllegalStateException("Cannot load EJB Session bean class", e);
         }
      }
      else
      {
         throw new IllegalStateException("EJB class is null. EJB " + enterpriseBeanMetaData);
      }
      this.ejbName = enterpriseBeanMetaData.getEjbName();
   }

   public Class<T> getBeanClass()
   {
      return beanClass;
   }

   public String getEjbName()
   {
      return ejbName;
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append(getEjbName());
      if (isStateful())
      {
         builder.append(" (SFSB)");
      }
      if (isStateless())
      {
         builder.append(" (SLSB)");
      }
      if (isSingleton())
      {
         builder.append(" (Singleton)");
      }
      if (isMessageDriven())
      {
         builder.append(" (MDB)");
      }
      builder.append("; BeanClass: " + getBeanClass() + "; Local Business Interfaces: " + getLocalBusinessInterfaces());
      return builder.toString();
   }

   @Override
   public boolean equals(Object other)
   {
      if (other instanceof EjbDescriptor<?>)
      {
         EjbDescriptor<?> that = (EjbDescriptor<?>) other;
         return this.getEjbName().equals(that.getEjbName());
      }
      else
      {
         return false;
      }
   }

   @Override
   public int hashCode()
   {
      return getEjbName().hashCode();
   }
}