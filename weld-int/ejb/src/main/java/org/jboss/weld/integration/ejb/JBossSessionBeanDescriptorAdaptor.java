package org.jboss.weld.integration.ejb;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.ejbref.resolver.spi.EjbReferenceResolver;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;
import org.jboss.metadata.ejb.spec.RemoveMethodMetaData;
import org.jboss.weld.ejb.spi.BusinessInterfaceDescriptor;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.integration.util.Reflections;

public class JBossSessionBeanDescriptorAdaptor<T> extends JBossEJBDescriptorAdaptor<T> implements EjbDescriptor<T>
{

   private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

   private final List<BusinessInterfaceDescriptor<?>> localBusinessInterfaces;
   private final List<BusinessInterfaceDescriptor<?>> remoteBusinessInterfaces;

   private final List<Method> removeMethods;
   private final boolean stateful;
   private final boolean stateless;
   private final boolean singleton;
   private final String localJndiName;

   public JBossSessionBeanDescriptorAdaptor(JBossSessionBeanMetaData sessionBeanMetaData, DeploymentUnit deploymentUnit, EjbReferenceResolver resolver)
   {
      this(sessionBeanMetaData, deploymentUnit, resolver, sessionBeanMetaData.getLocalJndiName());
   }

   public JBossSessionBeanDescriptorAdaptor(JBossSessionBeanMetaData sessionBeanMetaData, DeploymentUnit deploymentUnit, EjbReferenceResolver resolver, String jndiName)
   {
      super(sessionBeanMetaData, deploymentUnit, resolver);
      this.localBusinessInterfaces = new ArrayList<BusinessInterfaceDescriptor<?>>();
      if (sessionBeanMetaData.getBusinessLocals() != null)
      {
         for (String interfaceName : sessionBeanMetaData.getBusinessLocals())
         {
            this.localBusinessInterfaces.add(new JBossBussinessInterfaceDescriptorAdaptor<Object>(interfaceName, getEjbName(), deploymentUnit, resolver));
         }
      }
      
      this.remoteBusinessInterfaces = new ArrayList<BusinessInterfaceDescriptor<?>>();
      if (sessionBeanMetaData.getBusinessRemotes() != null)
      {
         for (String interfaceName : sessionBeanMetaData.getBusinessRemotes())
         {
            this.remoteBusinessInterfaces.add(new JBossBussinessInterfaceDescriptorAdaptor<Object>(interfaceName, getEjbName(), deploymentUnit, resolver));
         }
      }

      this.removeMethods = new ArrayList<Method>();

      if (sessionBeanMetaData.getRemoveMethods() != null)
      {
         for (RemoveMethodMetaData removeMethodMetaData : sessionBeanMetaData.getRemoveMethods())
         {
            Method removeMethod;
            try
            {
               List<String> methodParameters = removeMethodMetaData.getBeanMethod().getMethodParams();
               List<Class<?>> parameterTypes = new ArrayList<Class<?>>();
               for (String methodParameter : methodParameters)
               {
                  try
                  {
                     parameterTypes.add(Reflections.classForName(methodParameter, deploymentUnit.getClassLoader()));
                  }
                  catch (ClassNotFoundException e)
                  {
                     throw new IllegalStateException("Cannot load EJB remove method parameter class interface for " + removeMethodMetaData.toString(), e);
                  }
               }
               removeMethod = getBeanClass().getMethod(removeMethodMetaData.getBeanMethod().getMethodName(), parameterTypes.toArray(EMPTY_CLASS_ARRAY));
               removeMethods.add(removeMethod);
            }
            catch (SecurityException e)
            {
               throw new RuntimeException("Unable to access EJB remove method", e);
            }
            catch (NoSuchMethodException e)
            {
               throw new RuntimeException("Unable to access EJB remove method", e);
            }

         }
      }

      this.stateful = sessionBeanMetaData.isStateful();
      this.stateless = sessionBeanMetaData.isStateless();
      this.singleton = false;
      this.localJndiName = jndiName;
   }

   public Collection<BusinessInterfaceDescriptor<?>> getLocalBusinessInterfaces()
   {
      return localBusinessInterfaces;
   }
   
   public Collection<BusinessInterfaceDescriptor<?>> getRemoteBusinessInterfaces() 
   {
		return remoteBusinessInterfaces;
   }


   public Collection<Method> getRemoveMethods()
   {
      return removeMethods;
   }

   public boolean isSingleton()
   {
      return singleton;
   }

   public boolean isStateful()
   {
      return stateful;
   }

   public boolean isStateless()
   {
      return stateless;
   }

   public boolean isMessageDriven()
   {
      return false;
   }
   
   public String getLocalJndiName()
   {
      return localJndiName;
   }

   public boolean isNoInterfaceView()
   {
      return false;
   }

}