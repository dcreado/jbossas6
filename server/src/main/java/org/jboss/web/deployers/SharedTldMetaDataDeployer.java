/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.web.deployers;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;
import org.jboss.xb.binding.Unmarshaller;
import org.jboss.xb.binding.UnmarshallerFactory;
import org.jboss.xb.binding.sunday.unmarshalling.SchemaBindingResolver;
import org.jboss.xb.binding.sunday.unmarshalling.SingletonSchemaResolverFactory;

/**
 * A deployer that processes the shared TLDs.
 * 
 * @author Remy Maucherat
 * @version $Revision: 93820 $
 */
public class SharedTldMetaDataDeployer extends AbstractDeployer
{
   public static final String SHARED_TLDS_ATTACHMENT_NAME = "shared."+TldMetaData.class.getName();

   private List<URL> tldJars = null;
   private List<TldMetaData> sharedTldMetaData = null;

   public List<URL> getTldJars()
   {
      return tldJars;
   }

   public void setTldJars(List<URL> tldJars)
   {
      this.tldJars = tldJars;
   }

   public SharedTldMetaDataDeployer()
   {
      setStage(DeploymentStages.POST_CLASSLOADER);
      addOutput(SHARED_TLDS_ATTACHMENT_NAME);
   }

   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      if (!unit.getSimpleName().endsWith(".war"))
      {
         return;
      }
      
      synchronized (this)
      {
         if (sharedTldMetaData == null && tldJars != null)
         {
            UnmarshallerFactory factory = UnmarshallerFactory.newInstance();
            Unmarshaller unmarshaller = factory.newUnmarshaller();
            SchemaBindingResolver resolver = SingletonSchemaResolverFactory
                  .getInstance().getSchemaBindingResolver();

            // Parse shared JARs for TLDs
            sharedTldMetaData = new ArrayList<TldMetaData>();
            if (tldJars != null)
            {
               VirtualFileFilter tldFilter = new SuffixMatchFilter(".tld", VisitorAttributes.DEFAULT);
               for (URL tldJar : tldJars)
               {
                  try
                  {
                     VirtualFile virtualFile = VFS.getChild(tldJar);
                     VirtualFile metaInf = virtualFile.getChild("META-INF");
                     if (metaInf != null)
                     {
                        List<VirtualFile> tlds = metaInf.getChildren(tldFilter);
                        for (VirtualFile tld : tlds)
                        {
                           TldMetaData tldMetaData = (TldMetaData) unmarshaller.unmarshal(tld.toURL().toString(), resolver);
                           sharedTldMetaData.add(tldMetaData);
                        }
                     }
                  }
                  catch (Exception e)
                  {
                     throw new DeploymentException("Error processing TLDs for JAR: " + tldJar, e);
                  }
               }
            }
         }
      }
      if (sharedTldMetaData != null)
      {
         List<TldMetaData> clone = new ArrayList<TldMetaData>();
         clone.addAll(sharedTldMetaData);
         unit.addAttachment(SHARED_TLDS_ATTACHMENT_NAME, clone);
      }

   }

}
