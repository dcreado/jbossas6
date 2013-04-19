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
package org.jboss.weld.integration.deployer.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.vfs.VirtualFile;

/**
 * Post web.xml weld deployer.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class PostWebMetadataDeployer extends WeldAwareMetadataDeployer<JBossWebMetaData>
{
   private final ListenerMetaData WBL;
   private final ListenerMetaData JIL;
   private final FilterMetaData CPF;
   private final FilterMappingMetaData CPFM;

   public PostWebMetadataDeployer()
   {
      super(JBossWebMetaData.class, true);
      addInput("merged." + JBossWebMetaData.class.getName());
      addInput("order."+ WebMetaData.class.getName());
      addInput("overlays."+WebMetaData.class.getName());
      addInput("localscis."+WebMetaData.class.getName());
      
      setStage(DeploymentStages.POST_CLASSLOADER);
      setOptionalWeldXml(true);
      // create wbl listener
      WBL = new ListenerMetaData();
      WBL.setListenerClass("org.jboss.weld.servlet.WeldListener");
      JIL = new ListenerMetaData();
      JIL.setListenerClass("org.jboss.weld.integration.webtier.jsp.JspInitializationListener");
      CPF = new FilterMetaData();
      CPF.setFilterName("Weld Conversation Propagation Filter");
      CPF.setFilterClass("org.jboss.weld.servlet.ConversationPropagationFilter");
      CPF.setAsyncSupported(true);
      CPFM = new FilterMappingMetaData();
      CPFM.setFilterName("Weld Conversation Propagation Filter");
      CPFM.setUrlPatterns(Arrays.asList("/*"));
      addOutput("merged." + JBossWebMetaData.class.getName());
   }

   protected void internalDeploy(VFSDeploymentUnit unit, JBossWebMetaData deployment, Collection<VirtualFile> wbXml) throws DeploymentException
   {
      if (wbXml != null)
      {
         List<ListenerMetaData> listeners = deployment.getListeners();
         if (listeners == null)
         {
            listeners = new ArrayList<ListenerMetaData>();
            deployment.setListeners(listeners);
         }
         listeners.add(0, WBL);
         listeners.add(1, JIL);

         FiltersMetaData filters = deployment.getFilters();
         if (filters == null)
         {
            filters = new FiltersMetaData();
            deployment.setFilters(filters);
         }
         filters.add(CPF);

         List<FilterMappingMetaData> filterMappings = deployment.getFilterMappings();
         if (filterMappings == null)
         {
            filterMappings = new ArrayList<FilterMappingMetaData>();
            deployment.setFilterMappings(filterMappings);
         }
         filterMappings.add(CPFM);

      }
   }
}
