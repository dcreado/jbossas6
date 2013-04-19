/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.webservices.integration.metadata;

import java.util.LinkedList;
import java.util.List;

import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMessageDrivenBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;
import org.jboss.metadata.javaee.spec.PortComponent;
import org.jboss.wsf.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.j2ee.EJBArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBSecurityMetaData;
import org.jboss.wsf.spi.metadata.j2ee.MDBMetaData;
import org.jboss.wsf.spi.metadata.j2ee.SLSBMetaData;

/**
 * Builds container independent meta data from EJB21 container meta data. 
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
final class MetaDataBuilderEJB21 extends AbstractMetaDataBuilderEJB
{
   /**
    * Constructor.
    */
   MetaDataBuilderEJB21()
   {
      super();
   }

   /**
    * @see AbstractMetaDataBuilderEJB#buildEnterpriseBeansMetaData(Deployment, EJBArchiveMetaData)
    * 
    * @param dep webservice deployment
    * @param ejbArchiveMD EJB archive meta data
    */
   @Override
   protected void buildEnterpriseBeansMetaData(final Deployment dep, final EJBArchiveMetaData ejbArchiveMD)
   {
      final JBossMetaData jbossMetaData = WSHelper.getRequiredAttachment(dep, JBossMetaData.class);
      final List<EJBMetaData> wsEjbsMD = new LinkedList<EJBMetaData>();

      for (final JBossEnterpriseBeanMetaData jbossEjbMD : jbossMetaData.getEnterpriseBeans())
      {
         this.buildEnterpriseBeanMetaData(wsEjbsMD, jbossEjbMD);
      }

      ejbArchiveMD.setEnterpriseBeans(wsEjbsMD);

      final String securityDomain = jbossMetaData.getSecurityDomain();
      this.log.debug("Setting security domain: " + securityDomain);
      ejbArchiveMD.setSecurityDomain(securityDomain);
   }

   /**
    * Builds JBoss agnostic EJB meta data.
    *
    * @param wsEjbsMD jboss agnostic EJBs meta data
    * @param jbossEjbMD jboss specific EJB meta data
    */
   private void buildEnterpriseBeanMetaData(final List<EJBMetaData> wsEjbsMD,
         final JBossEnterpriseBeanMetaData jbossEjbMD)
   {
      final EJBMetaData wsEjbMD = this.newEjbMetaData(jbossEjbMD);

      if (wsEjbMD != null)
      {
         // set EJB name and class
         wsEjbMD.setEjbName(jbossEjbMD.getEjbName());
         wsEjbMD.setEjbClass(jbossEjbMD.getEjbClass());

         if (jbossEjbMD.isSession())
         {
            final JBossSessionBeanMetaData sessionEjbMD = (JBossSessionBeanMetaData) jbossEjbMD;

            // set home interfaces
            wsEjbMD.setServiceEndpointInterface(sessionEjbMD.getServiceEndpoint());
            wsEjbMD.setHome(sessionEjbMD.getHome());
            wsEjbMD.setLocalHome(sessionEjbMD.getLocalHome());

            // set JNDI names
            wsEjbMD.setJndiName(sessionEjbMD.determineJndiName());
            wsEjbMD.setLocalJndiName(jbossEjbMD.determineLocalJndiName());

            final PortComponent portComponentMD = sessionEjbMD.getPortComponent();
            if (portComponentMD != null)
            {
               // set port component meta data
               wsEjbMD.setPortComponentName(portComponentMD.getPortComponentName());
               wsEjbMD.setPortComponentURI(portComponentMD.getPortComponentURI());

               // set security meta data
               final EJBSecurityMetaData smd = new EJBSecurityMetaData();
               smd.setAuthMethod(portComponentMD.getAuthMethod());
               smd.setTransportGuarantee(portComponentMD.getTransportGuarantee());
               smd.setSecureWSDLAccess(portComponentMD.getSecureWSDLAccess());
               wsEjbMD.setSecurityMetaData(smd);
            }
         }

         wsEjbsMD.add(wsEjbMD);
      }
   }

   /**
    * Creates new JBoss agnostic EJB bean meta data model.
    *
    * @param jbossEjbMD jboss EJB meta data
    * @return webservices EJB meta data
    */
   private EJBMetaData newEjbMetaData(final JBossEnterpriseBeanMetaData jbossEjbMD)
   {
      if (jbossEjbMD.isSession())
      {
         this.log.debug("Creating JBoss agnostic EJB21 meta data for session bean: " + jbossEjbMD.getEjbClass());
         return new SLSBMetaData();
      }
      else if (jbossEjbMD.isMessageDriven())
      {
         this.log.debug("Creating JBoss agnostic EJB21 meta data for message driven bean: " + jbossEjbMD.getEjbClass());
         final MDBMetaData mdbMD = new MDBMetaData();

         final JBossMessageDrivenBeanMetaData jbossMessageBean = (JBossMessageDrivenBeanMetaData) jbossEjbMD;
         ((MDBMetaData) mdbMD).setDestinationJndiName(jbossMessageBean.getDestinationJndiName());

         return mdbMD;
      }

      return null;
   }
}
