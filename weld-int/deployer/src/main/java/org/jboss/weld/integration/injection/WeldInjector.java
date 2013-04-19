package org.jboss.weld.integration.injection;

import org.jboss.ejb3.BeanContext;
import org.jboss.injection.Injector;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.integration.deployer.env.helpers.BootstrapBean;

/**
 * Injector for injecting Web Beans into Servlets and other Web Artifacts
 *
 * @author Marius Bogoevici
 */
public class WeldInjector implements Injector
{
   private BootstrapBean bootstrapBean;

   private String beanDeploymentArchiveId;

   public WeldInjector(BootstrapBean bootstrapBean, String beanDeploymentArchiveId)
   {
      if (bootstrapBean == null)
         throw new IllegalArgumentException("Null bootstrap bean");
      if (beanDeploymentArchiveId == null)
         throw new IllegalArgumentException("Null bean deployment archive id");

      this.bootstrapBean = bootstrapBean;
      this.beanDeploymentArchiveId = beanDeploymentArchiveId;
   }

   public Class getInjectionClass()
   {
      return null;
   }

   public void inject(BeanContext ctx)
   {
      //no-op
   }

   public void inject(Object instance)
   {
      BeanDeploymentArchive foundBeanDeploymentArchive = null;
      for (BeanDeploymentArchive beanDeploymentArchive: bootstrapBean.getDeployment().getBeanDeploymentArchives())
      {
         if (beanDeploymentArchive.getId().equals(beanDeploymentArchiveId))
         {
            foundBeanDeploymentArchive = beanDeploymentArchive;
         }
      }
      if (foundBeanDeploymentArchive == null)
      {
         throw new IllegalStateException("Cannot find BeanManager for BeanDeploymentArchive with id=" + beanDeploymentArchiveId);
      }
      NonContextualObjectInjectionHelper.injectNonContextualInstance(instance, bootstrapBean.getBootstrap().getManager(foundBeanDeploymentArchive));

   }

   public void cleanup()
   {
      bootstrapBean = null;
   }
}