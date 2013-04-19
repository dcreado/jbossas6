package org.jboss.weld.integration.ejb;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.ejbref.resolver.spi.EjbReferenceResolver;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.metadata.ejb.jboss.jndi.resolver.impl.JNDIPolicyBasedJNDINameResolverFactory;
import org.jboss.metadata.ejb.jboss.jndi.resolver.spi.SessionBean31JNDINameResolver;
import org.jboss.metadata.ejb.jboss.jndipolicy.plugins.DefaultJNDIBindingPolicyFactory;
import org.jboss.metadata.ejb.jboss.jndipolicy.spi.DefaultJndiBindingPolicy;

public class JBossSessionBean31DescriptorAdaptor<T> extends JBossSessionBeanDescriptorAdaptor<T>
{

   private final String localJndiName;
   private final boolean noInterfaceView;
   private final boolean singleton;

   public JBossSessionBean31DescriptorAdaptor(JBossSessionBean31MetaData sessionBeanMetaData, DeploymentUnit deploymentUnit, EjbReferenceResolver resolver)
   {
      super(sessionBeanMetaData, deploymentUnit, resolver);
      if (sessionBeanMetaData.isNoInterfaceBean())
      {
         getLocalBusinessInterfaces().add(new JBossBussinessInterfaceDescriptorAdaptor<T>(sessionBeanMetaData.getEjbClass(), sessionBeanMetaData.getEjbName(), deploymentUnit, resolver));
         this.localJndiName = getJndiName(sessionBeanMetaData);
         this.noInterfaceView = true;
      }
      else
      {
         this.localJndiName = null;
         this.noInterfaceView = false;
      }
      singleton = sessionBeanMetaData.isSingleton();
   }

   @Override
   public String getLocalJndiName()
   {
      if (localJndiName == null)
      {
         return super.getLocalJndiName();
      }
      else
      {
         return localJndiName;
      }
   }

   @Override
   public boolean isNoInterfaceView()
   {
      return noInterfaceView;
   }

   @Override
   public boolean isSingleton()
   {
      return singleton;
   }

   private static String getJndiName(JBossSessionBean31MetaData sessionBean)
   {
      DefaultJndiBindingPolicy jndiBindingPolicy = DefaultJNDIBindingPolicyFactory.getDefaultJNDIBindingPolicy();
      // get a jndi name resolver for this session bean, based on a jndi binding
      // policy
      SessionBean31JNDINameResolver jndiNameResolver = JNDIPolicyBasedJNDINameResolverFactory.getJNDINameResolver(sessionBean, jndiBindingPolicy);
      // no-interface view jndi name
      return jndiNameResolver.resolveNoInterfaceJNDIName(sessionBean);
   }
}