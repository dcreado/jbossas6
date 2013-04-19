package org.jboss.weld.integration.ejb;
import java.io.Serializable;
import java.lang.reflect.Proxy;

import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.common.registrar.spi.Ejb3Registrar;
import org.jboss.ejb3.common.registrar.spi.Ejb3RegistrarLocator;
import org.jboss.ejb3.endpoint.Endpoint;
import org.jboss.ejb3.endpoint.deployers.EndpointResolver;
import org.jboss.ejb3.proxy.impl.handler.session.SessionProxyInvocationHandler;
import org.jboss.logging.Logger;
import org.jboss.weld.ejb.api.SessionObjectReference;
import org.jboss.weld.ejb.spi.EjbDescriptor;


public class JBossSessionObjectReference implements SessionObjectReference
{

   private static Logger log = Logger.getLogger(JBossSessionObjectReference.class);
   
   private static final String MC_BIND_NAME_ENDPOINT_RESOLVER = "EJB3EndpointResolver";

   private static final long serialVersionUID = 8227728506645839338L;

   private final Object reference;
   private final Serializable id;
   private final String ejbClassName;
   private final String jndiName;
   private final boolean stateful;

   boolean removed = false;
   private final String endpointMcBindName;

   public JBossSessionObjectReference(EjbDescriptor<?> descriptor, DeploymentUnit deploymentUnit, Context context) throws NamingException
   {
      if (!(descriptor instanceof JBossSessionBeanDescriptorAdaptor<?>))
      {
         throw new IllegalArgumentException("Can only operate on JBoss EJB3");
      }
      this.jndiName = ((JBossSessionBeanDescriptorAdaptor<?>) descriptor).getLocalJndiName();
      reference = context.lookup(jndiName);
      if (descriptor instanceof JBossSessionBean31DescriptorAdaptor<?> && ((JBossSessionBean31DescriptorAdaptor<?>) descriptor).isNoInterfaceView())
      {
         this.id = null;
      }
      else
      {
         SessionProxyInvocationHandler handler = (SessionProxyInvocationHandler) Proxy.getInvocationHandler(reference);
         id = (Serializable) handler.getTarget();
      }

      @Deprecated
      Ejb3Registrar registrar = Ejb3RegistrarLocator.locateRegistrar();

      // Get the resolver
      EndpointResolver resolver = registrar.lookup(MC_BIND_NAME_ENDPOINT_RESOLVER, EndpointResolver.class);
      this.ejbClassName = descriptor.getBeanClass().getSimpleName();
      endpointMcBindName = resolver.resolve(deploymentUnit, ejbClassName);
      this.stateful = descriptor.isStateful();
   }

   @SuppressWarnings("unchecked")
   public <S> S getBusinessObject(Class<S> businessInterfaceType)
   {
      return (S) reference;
   }

   public void remove()
   {
      if (id == null && stateful)
      {
         log.warn("Cannot remove EJB, id unknown (likely because this is a no-interface view!)");
         removed = true;
         return;
      }
      else if (stateful)
      {
         getEndpoint().getSessionFactory().destroySession(id);
         removed = true;
      }
      else
      {
         throw new UnsupportedOperationException("Can only remove stateful beans " + this );
      }
   }

   private Endpoint getEndpoint()
   {
      @Deprecated
      Ejb3Registrar registrar = Ejb3RegistrarLocator.locateRegistrar();
      return registrar.lookup(endpointMcBindName, Endpoint.class);
   }

   public boolean isRemoved()
   {
      // TODO Doesn't account for the case the EJB container removes the EJB without WB!
      return removed;
   }

   @Override
   public String toString()
   {
      return "Session bean reference: " + jndiName + " with id: " + id;
   }

}