/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.weld.integration.deployer.jndi;

import javax.enterprise.inject.spi.BeanManager;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.structure.spi.DeploymentUnitVisitor;
import org.jboss.naming.Util;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEModuleInformer;
import org.jboss.reloaded.naming.service.NameSpaces;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.util.IdFactory;
import org.jboss.weld.integration.util.JndiUtils;

/**
 * This deployer intercepts BootstrapBean metadata,
 * and adds JndiBinder invocations to it.
 *
 * @author Pete Muir
 * @author <a href="mailto:stan.silvert@jboss.org">Stan Silvert</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Marius Bogoevici
 */
public class JndiBinder
{

   public static final String REFADDR_ID = "id";

   private Context beanManagerContext;
   private NameSpaces nameSpaces;
   private JavaEEModuleInformer moduleInformer;

   public void setModuleInformer(JavaEEModuleInformer moduleInformer)
   {
      this.moduleInformer = moduleInformer;
   }

   public void setNameSpaces(NameSpaces nameSpaces)
   {
      this.nameSpaces = nameSpaces;
   }

   // --- binding logic ---


   public void bind(DeploymentUnit unit) throws DeploymentException
   {
      if (DeployersUtils.checkForWeldFilesAcrossDeployment(unit))
      {
         unit.visit(new BinderVisitor(getBeanManagerContext()));
      }
   }

   public void unbind(DeploymentUnit unit)
   {
      BeanMetaData bbBMD = unit.getTopLevel().getAttachment(DeployersUtils.getBootstrapBeanAttachmentName(unit), BeanMetaData.class);

      if (bbBMD != null)
      {
         try
         {
            unit.visit(new UnbinderVisitor(getBeanManagerContext()));
         }
         catch (DeploymentException e)
         {
            throw new RuntimeException(e);
         }
      }

      // TODO: cleanup the remaining subcontexts if any (e.g. EAR/WAR etc)
   }

   protected Context createContext() throws NamingException
   {
      return nameSpaces.getGlobalContext();
   }

   public void create() throws Exception
   {
      Context context = createContext();
      beanManagerContext = context.createSubcontext(JndiUtils.BEAN_MANAGER_GLOBAL_SUBCONTEXT);
   }

   protected Context getBeanManagerContext()
   {
      return beanManagerContext;
   }

   public void destroy()
   {
      try
      {
         Context context = createContext();
         context.destroySubcontext(JndiUtils.BEAN_MANAGER_GLOBAL_SUBCONTEXT);
      }
      catch (Exception ignore)
      {
      }
   }

   private boolean hasJndiBoundBeanManager(DeploymentUnit deploymentUnit)
   {
      return DeployersUtils.checkForWeldFiles(deploymentUnit, false) && (moduleInformer.getModuleType(deploymentUnit).equals(JavaEEModuleInformer.ModuleType.EJB) ||
            moduleInformer.getModuleType(deploymentUnit).equals(JavaEEModuleInformer.ModuleType.WEB));
   }

   private class BinderVisitor implements DeploymentUnitVisitor
   {
      private final Context rootContext;

      private BinderVisitor(Context rootContext)
      {
         this.rootContext = rootContext;
      }

      public void visit(DeploymentUnit unit) throws DeploymentException
      {
         try
         {
            if (hasJndiBoundBeanManager(unit))
            {
               String path = JndiUtils.getJndiSubcontexPathForBeanManager(moduleInformer, unit);
               Context subcontext = Util.createSubcontext(rootContext, path);
               Reference reference = new Reference(BeanManager.class.getName(), "org.jboss.weld.integration.deployer.jndi.JBossBeanManagerObjectFactory", null);
               reference.add(new StringRefAddr(REFADDR_ID, IdFactory.getIdFromClassLoader(unit.getClassLoader())));
               subcontext.bind("BeanManager", reference);
            }
         }
         catch (NamingException e)
         {
            throw new DeploymentException(e);
         }
      }

      public void error(DeploymentUnit unit)
      {
         // do nothing
      }
   }

   private class UnbinderVisitor implements DeploymentUnitVisitor
   {
      private final Context rootContext;

      private UnbinderVisitor(Context rootContext)
      {
         this.rootContext = rootContext;
      }

      public void visit(DeploymentUnit unit) throws DeploymentException
      {
         try
         {
            if (hasJndiBoundBeanManager(unit))
            {
               String path = JndiUtils.getJndiSubcontexPathForBeanManager(moduleInformer, unit);
               Context subcontext = (Context) rootContext.lookup(path);
               subcontext.unbind("BeanManager");
               rootContext.destroySubcontext(path);
            }
         }
         catch (NamingException e)
         {
            throw new DeploymentException(e);
         }
      }

      public void error(DeploymentUnit unit)
      {
         // do nothing
      }
   }

}
