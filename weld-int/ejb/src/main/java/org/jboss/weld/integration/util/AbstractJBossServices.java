package org.jboss.weld.integration.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.integration.vdf.DeploymentUnitAware;

/**
 * Abstract JBoss Weld services.
 *
 * @author Pete Muir
 * @author ales.justin@jboss.org
 */
public class AbstractJBossServices implements Service, DeploymentUnitAware
{
   protected DeploymentUnit topLevelDeploymentUnit;
   protected JBossEjb jbossEjb;
   protected final Context context;

   public AbstractJBossServices() throws NamingException
   {
      context = new InitialContext();
   }

   public void setDeploymentUnit(DeploymentUnit du)
   {
      if (du == null)
      {
         throw new IllegalArgumentException("Null deployment unit.");
      }

      topLevelDeploymentUnit = du.getTopLevel();
   }

   public void setJbossEjb(JBossEjb jbossEjb)
   {
      this.jbossEjb = jbossEjb;
   }

   public void cleanup()
   {
   }
}