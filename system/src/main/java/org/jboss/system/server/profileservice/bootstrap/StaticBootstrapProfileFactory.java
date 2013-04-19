/*
* JBoss, Home of Professional Open Source
* Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.system.server.profileservice.bootstrap;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.profileservice.domain.AbstractDomainMetaData;
import org.jboss.profileservice.domain.spi.DomainMetaData;
import org.jboss.profileservice.domain.spi.DomainMetaDataFragment;
import org.jboss.profileservice.domain.spi.DomainMetaDataFragmentVisitor;
import org.jboss.profileservice.domain.spi.DomainMetaDataRepository;
import org.jboss.profileservice.profile.metadata.BasicSubProfileMetaData;
import org.jboss.profileservice.profile.metadata.helpers.AbstractProfileMetaData;
import org.jboss.profileservice.profile.metadata.helpers.ProfileMetaDataFactory;
import org.jboss.profileservice.profile.metadata.plugin.ScanPeriod;
import org.jboss.profileservice.spi.metadata.ProfileMetaData;
import org.jboss.profileservice.spi.metadata.ProfileMetaDataVisitorNode;

/**
 * The static bootstrap factory. This should get replaced with the
 * actual domain meta data exporting the profiles which have to
 * loaded during the bootstrap process.
 * 
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision$
 */
public class StaticBootstrapProfileFactory implements DomainMetaDataFragment, DomainMetaDataRepository
{

   /** The bindings profile name. */
   private String bindingsName = "jboss:profile=bindings";
   
   /** The bootstrap profile name. */
   private String bootstrapName = "jboss:profile=legacy-service-xml";
   
   /** The deployers profile name. */
   private String deployersName = "jboss:profile=deployers";
   
   /** The applications profile name. */
   private String applicationsName = "jboss:profile=applications";
   
   /** The bindings deployment name. */
   private String bindingsDeployment = "bindingservice.beans";
   
   /** The bootstrap deployment name. */
   private String bootstrapDeployment = "jboss-service.xml";
   
   /** The conf/ dir URI. */
   private URI confURI;
   
   /** The deployers uri. */
   private URI deployersURI;
   
   /** The application uris. */
   private List<URI> applicationURIs;
   
   /** The shared scan period. */
   private ScanPeriod scanPeriod = new ScanPeriod();
   
   /** The attachment store uri. */
   private File attachmentStoreRoot;

   public String getBootstrapName()
   {
      return bootstrapName;
   }
   
   public void setBootstrapName(String bootstrapName)
   {
      this.bootstrapName = bootstrapName;
   }
   
   public String getDeployersName()
   {
      return deployersName;
   }
   
   public void setDeployersName(String deployersName)
   {
      this.deployersName = deployersName;
   }
   
   public String getApplicationsName()
   {
      return applicationsName;
   }
   
   public void setApplicationsName(String applicationsName)
   {
      this.applicationsName = applicationsName;
   }

   public String getBindingsDeployment()
   {
      return bindingsDeployment;
   }
   
   public void setBindingsDeployment(String bindingsDeployment)
   {
      this.bindingsDeployment = bindingsDeployment;
   }
   
   public String getBootstrapDeployment()
   {
      return bootstrapDeployment;
   }
   
   public void setBootstrapDeployment(String bootstrapDeployment)
   {
      this.bootstrapDeployment = bootstrapDeployment;
   }
   
   public URI getConfURI()
   {
      return confURI;
   }
   
   public void setConfURI(URI confURI)
   {
      this.confURI = confURI;
   }
   
   public URI getDeployersURI()
   {
      return deployersURI;
   }

   public void setDeployersURI(URI deployersURI)
   {
      this.deployersURI = deployersURI;
   }

   public List<URI> getApplicationURIs()
   {
      if(applicationURIs == null)
         return Collections.emptyList();
      return applicationURIs;
   }
   
   public void setApplicationURIs(List<URI> applicationURIs)
   {
      this.applicationURIs = applicationURIs;
   }

   public ScanPeriod getScanPeriod()
   {
      return scanPeriod;
   }
   
   public void setScanPeriod(ScanPeriod scanPeriod)
   {
      this.scanPeriod = scanPeriod;
   }
   
   public File getAttachmentStoreRoot()
   {
      return attachmentStoreRoot;
   }

   public void setAttachmentStoreRoot(File attachmentStoreRoot)
   {
      this.attachmentStoreRoot = attachmentStoreRoot;
   }
   
   protected DomainMetaDataFragment getFragment()
   {
      return this;
   }

   public DomainMetaData getDomainMetaData()
   {
      AbstractDomainMetaData domain = new AbstractDomainMetaData();
      domain.setFragments(Collections.singletonList(getFragment()));
      return domain;
   }
   
   public String getNameSpace()
   {
      return "";
   }
   
   public void visit(DomainMetaDataFragmentVisitor visitor)
   {
      for(ProfileMetaData profile : createProfiles()) {
         visitor.addProfileMetaData(profile);
      }
   }
   
   /**
    * Create the profiles.
    * 
    * @return the profiles
    */
   protected List<ProfileMetaData> createProfiles()
   {
      List<ProfileMetaData> profiles = new ArrayList<ProfileMetaData>();
      List<String> dependencies =  new ArrayList<String>();
      
      // conf/bindingservice.beans
      profiles.add(createFilteredProfileMetaData(bindingsName, getConfURI(), dependencies, getBindingsDeployment()));
      dependencies.add(bindingsName);
      
      // conf/jboss-service.xml
      profiles.add(createFilteredProfileMetaData(bootstrapName, getConfURI(), dependencies, getBootstrapDeployment()));
      dependencies.add(bootstrapName);
      
      // deployers/
      profiles.add(createScanningProfile(deployersName, getDeployersURI(), dependencies, false));
      dependencies.add(deployersName);

      // Create the application profiles
      createApplicationProfiles(profiles, dependencies);
      
      return profiles;
   }
   
   /**
    * Create the application profiles.
    * 
    * @param profiles the profiles list
    * @param dependencies the dependencies list
    */
   protected void createApplicationProfiles(List<ProfileMetaData> profiles, List<String> dependencies)
   {
      // the hot deployment profiles
      if(getApplicationURIs().isEmpty() == false) {
         for(int i = 0; i < getApplicationURIs().size(); i++) {
            final URI uri = getApplicationURIs().get(i);
            final String profileName = i == 0 ? applicationsName : applicationsName + i;
            profiles.add(createScanningProfile(profileName, uri, dependencies, true));
            dependencies.add(profileName);
         }
      }      
   }
   
   /**
    * Create a filtered profile meta data.
    * 
    * @param name the unique profile name
    * @param root the override root
    * @param deploymentNames the deployments contained in this profile
    * @return the profile meta data
    */
   protected ProfileMetaData createFilteredProfileMetaData(String name, URI root, Collection<String> dependencies, String... deploymentNames)
   {
      return processDependencies(ProfileMetaDataFactory.createFilteredProfileMetaData(name, root, deploymentNames),
            dependencies);
   }
   
   /**
    * Create a scanning profile
    * 
    * @param name the unique profile name
    * @param root the profile root
    * @param hotDeployment flag whether hot deployment scans should be enabled
    * @return the scanning profile meta data
    */
   protected ProfileMetaData createScanningProfile(String name, URI root, Collection<String> dependencies, boolean hotDeployment)
   {
      AbstractProfileMetaData profile;
      if(hotDeployment) {
         profile = ProfileMetaDataFactory.createHotDeploymentScanningProfile(name, root, getScanPeriod());
      } else {
         profile = ProfileMetaDataFactory.createImmutableScanningProfile(name, root);
      }
      // Dependencies
      return processDependencies(profile, dependencies);
   }

   /**
    * Process the dependencies.
    * 
    * @param metaData the profile meta data
    * @param names the dependencies
    */
   protected AbstractProfileMetaData processDependencies(AbstractProfileMetaData metaData, Collection<String> names)
   {
      List<ProfileMetaDataVisitorNode> nodes = new ArrayList<ProfileMetaDataVisitorNode>();
      for(String name : names) {
         nodes.add(new BasicSubProfileMetaData(name));
      }
      metaData.setFeatures(nodes);
      return metaData;
   }
   
}

