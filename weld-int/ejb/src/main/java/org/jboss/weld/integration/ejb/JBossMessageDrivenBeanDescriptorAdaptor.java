package org.jboss.weld.integration.ejb;

import java.lang.reflect.Method;
import java.util.Collection;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.ejbref.resolver.spi.EjbReferenceResolver;
import org.jboss.metadata.ejb.jboss.JBossMessageDrivenBeanMetaData;
import org.jboss.weld.ejb.spi.BusinessInterfaceDescriptor;

public class JBossMessageDrivenBeanDescriptorAdaptor<T> extends JBossEJBDescriptorAdaptor<T>
{
   public JBossMessageDrivenBeanDescriptorAdaptor(JBossMessageDrivenBeanMetaData messageDrivenBeanMetaData, DeploymentUnit deploymentUnit, EjbReferenceResolver resolver)
   {
      super(messageDrivenBeanMetaData, deploymentUnit, resolver);
   }

   public Collection<BusinessInterfaceDescriptor<?>> getLocalBusinessInterfaces()
   {
      // Not relevant for MDBs
      return null;
   }
   
   public Collection<BusinessInterfaceDescriptor<?>> getRemoteBusinessInterfaces()
   {
      // Not relevant for MDBs
      return null;
   }


   public Collection<Method> getRemoveMethods()
   {
      // Not relevant for MDBs
      return null;
   }

   public boolean isSingleton()
   {
      return false;
   }

   public boolean isStateful()
   {
      return false;
   }

   public boolean isStateless()
   {
      return false;
   }

   public boolean isMessageDriven()
   {
      return true;
   }

}