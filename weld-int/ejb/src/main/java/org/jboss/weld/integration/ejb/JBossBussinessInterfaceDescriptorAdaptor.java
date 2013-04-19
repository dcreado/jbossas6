package org.jboss.weld.integration.ejb;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.ejbref.resolver.spi.EjbReferenceResolver;
import org.jboss.weld.ejb.spi.BusinessInterfaceDescriptor;
import org.jboss.weld.integration.util.Reflections;

public class JBossBussinessInterfaceDescriptorAdaptor<T> implements BusinessInterfaceDescriptor<T>
{
   private Class<T> type;

   public JBossBussinessInterfaceDescriptorAdaptor(String interfaceName, String ejbName, DeploymentUnit deploymentUnit, EjbReferenceResolver resolver)
   {
      try
      {
         type = (Class<T>) Reflections.classForName(interfaceName, deploymentUnit.getClassLoader());
      }
      catch (ClassCastException e)
      {
         throw new IllegalStateException("Error loading EJB Session bean interface", e);
      }
      catch (ClassNotFoundException e)
      {
         throw new IllegalStateException("Cannot load EJB Session bean interface", e);
      }
   }

   public Class<T> getInterface()
   {
      return type;
   }

   @Override
   public String toString()
   {
      return "Business interface: " + getInterface();
   }
}