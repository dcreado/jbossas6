/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.deployment;

import org.jboss.aop.microcontainer.aspects.jmx.JMX;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractNameIgnoreMechanism;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.deployer.SchemaResolverDeployer;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.ear.spec.ModulesMetaData;

/**
 * An ObjectModelFactoryDeployer for translating jboss-app.xml descriptors into
 * JBossAppMetaData instances.
 * 
 * @author Scott.Stark@jboss.org
 * @author Anil.Saldhana@redhat.com
 * @author adrian@jboss.org
 * @author ales.justin@jboss.org
 * @version $Revision: 105583 $
 */
@JMX(name="jboss.j2ee:service=EARDeployer", exposedInterface=JBossAppParsingDeployerMBean.class)
public class JBossAppParsingDeployer extends SchemaResolverDeployer<JBossAppMetaData> implements JBossAppParsingDeployerMBean
{
   private boolean callByValue = false;
   
   private String unauthenticatedIdentity = null;

   /**
    * Create a new JBossAppParsingDeployer.
    */
   public JBossAppParsingDeployer()
   {
      super(JBossAppMetaData.class);
      addInput(EarMetaData.class);
      addInput(JBossAppMetaData.class); // EarContentsDeployer can produce it
      setName("jboss-app.xml");
      setTopLevelOnly(true);
      setNameIgnoreMechanism(new NonEarJBossAppNameIgnoreMechanism()); // JBAS-7974
   }

   @Override
   protected boolean allowsReparse()
   {
      return true; // EarContentsDeployer can produce it already
   }

   /**
    * Get the virtual file path for the application descriptor in the
    * DeploymentContext.getMetaDataPath.
    * 
    * @return the current virtual file path for the application descriptor
    */
   public String getAppXmlPath()
   {
      return getName();
   }
   /**
    * Set the virtual file path for the application descriptor in the
    * DeploymentContext.getMetaDataLocation. The standard path is jboss-app.xml
    * to be found in the META-INF metdata path.
    * 
    * @param appXmlPath - new virtual file path for the application descriptor
    */
   public void setAppXmlPath(String appXmlPath)
   {
      setName(appXmlPath);
   }
   
   /**
    * @return whether ear deployments should be call by value
    */
   public boolean isCallByValue()
   {
      return callByValue;
   }
   
   /**
    * @param callByValue whether ear deployments should be call by value
    */
   public void setCallByValue(boolean callByValue)
   {
      this.callByValue = callByValue;
   }
  
   /**
    * Obtain an unauthenticated identity
    * 
    * @return the unauthenticated identity
    */
   public String getUnauthenticatedIdentity()
   {
      return unauthenticatedIdentity;
   }

   /**
    * Specify an unauthenticated identity
    * @param unauthenticatedIdentity ui flag
    */
   public void setUnauthenticatedIdentity(String unauthenticatedIdentity)
   {
      this.unauthenticatedIdentity = unauthenticatedIdentity;
   }

   // FIXME This should all be in a seperate deployer
   @Override
   protected void createMetaData(DeploymentUnit unit, String name, String suffix) throws DeploymentException
   {
      EarMetaData specMetaData = unit.getAttachment(EarMetaData.class);
      JBossAppMetaData metaData = unit.getAttachment(JBossAppMetaData.class); // from ear contents deployer

      // do parse
      super.createMetaData(unit, name, suffix);
      // new parsed metadata
      JBossAppMetaData parsed = unit.getAttachment(JBossAppMetaData.class);
      if (metaData != null && parsed != null)
      {
         ModulesMetaData mmd = metaData.getModules();
         if (mmd != null && mmd.isEmpty() == false)
         {
            ModulesMetaData parsedMMD = parsed.getModules();
            if (parsedMMD == null)
            {
               parsedMMD = new ModulesMetaData();
               parsed.setModules(parsedMMD);
            }
            parsedMMD.merge(parsedMMD, mmd);
         }
      }
      // parsed is the one we use after merged modules
      metaData = parsed;

      if(specMetaData == null && metaData == null)
         return;

      // If there no JBossMetaData was created from a jboss-app.xml, create one
      if (metaData == null)
         metaData = new JBossAppMetaData();

      // Create a merged view
      JBossAppMetaData mergedMetaData = new JBossAppMetaData();
      mergedMetaData.merge(metaData, specMetaData);
      // Set the merged as the output
      unit.getTransientManagedObjects().addAttachment(JBossAppMetaData.class, mergedMetaData);
      // Keep the raw parsed metadata as well
      unit.addAttachment("Raw"+JBossAppMetaData.class.getName(), metaData, JBossAppMetaData.class);
      // Pass the ear callByValue setting
      if (isCallByValue())
         unit.addAttachment("EAR.callByValue", Boolean.TRUE, Boolean.class);
      //Pass the unauthenticated identity
      if (this.unauthenticatedIdentity != null)
         unit.addAttachment("EAR.unauthenticatedIdentity", this.unauthenticatedIdentity, String.class);
   }

   private static class NonEarJBossAppNameIgnoreMechanism extends AbstractNameIgnoreMechanism
   {
      public boolean ignoreName(DeploymentUnit unit, String name)
      {
         return false;
      }

      public boolean ignorePath(DeploymentUnit unit, String path)
      {
         String simpleName = unit.getSimpleName();
         return simpleName.endsWith(".ear") == false; // it's not EAR, ignore it
      }
   }
}
