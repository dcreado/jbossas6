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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.bootstrap.api.as.config.JBossASBasedServerConfig;
import org.jboss.bootstrap.spi.as.JBossASBootstrap;
import org.jboss.bootstrap.spi.as.server.MCJBossASBasedServerProvider;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.spi.deployment.KernelDeployment;
import org.jboss.logging.Logger;
import org.jboss.profileservice.Hack;
import org.jboss.profileservice.bootstrap.AbstractProfileServiceBootstrap;
import org.jboss.profileservice.config.ProfileServiceConfig;
import org.jboss.profileservice.deployment.hotdeploy.HDScannerFactory;
import org.jboss.profileservice.domain.AbstractDomainMetaData;
import org.jboss.profileservice.domain.spi.DomainMetaData;
import org.jboss.profileservice.domain.spi.DomainMetaDataRepository;
import org.jboss.profileservice.metadata.ProfilesMetaDataFactory;
import org.jboss.profileservice.profile.metadata.CommonProfileNameSpaces;
import org.jboss.profileservice.profile.metadata.FeatureCapability;
import org.jboss.profileservice.profile.metadata.plugin.EmptyProfileMetaData;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.profileservice.spi.metadata.ProfileMetaData;
import org.jboss.profileservice.spi.metadata.ProfileMetaDataVisitorNode;

/**
 * The basic profile service bootstrap.
 *
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision$
 */
public class BasicProfileServiceBootstrap<K extends MCJBossASBasedServerProvider<K, T>, T extends JBossASBasedServerConfig<T>>
   extends AbstractProfileServiceBootstrap implements JBossASBootstrap<K,T>
{

   private static final Logger log = Logger.getLogger("ProfileServiceBootstrap");

   /** Default domain name, since we don't have a domain right now. */
   private static final String DOMAIN_NAME = ProfileKey.DEFAULT;

   private URL profilesLocation;

   /** The domain meta data. */
   private AbstractDomainMetaData domainMetaData;

   /** The domain meta data repository. */
   private DomainMetaDataRepository profileFactory;

   /** The hd scanner factory. */
   private HDScannerFactory hdScannerFactory;

   /** The bootstrap deployments. */
   // TODO this should be removed
   private Map<String, KernelDeployment> bootstrapDeployments;

   public BasicProfileServiceBootstrap(Kernel kernel, ProfileServiceConfig config)
   {
      super(kernel, config);
   }

   public DomainMetaDataRepository getProfileFactory()
   {
      return profileFactory;
   }

   public void setProfileFactory(DomainMetaDataRepository profileFactory)
   {
      this.profileFactory = profileFactory;
   }

   public HDScannerFactory getHdScannerFactory()
   {
      return hdScannerFactory;
   }

   public void setHdScannerFactory(HDScannerFactory hdScannerFactory)
   {
      this.hdScannerFactory = hdScannerFactory;
   }

   public URL getProfilesLocation()
   {
      return profilesLocation;
   }

   public void setProfilesLocation(URL profilesLocation)
   {
      this.profilesLocation = profilesLocation;
   }

   public Map<String, KernelDeployment> getBootstrapDeployments()
   {
      return bootstrapDeployments;
   }

   public void start(K server) throws Exception
   {
      // Create the domain meta data
      this.domainMetaData = createDomainMetaData(server.getConfiguration().getServerName());
      // FIXME this should not be needed
      this.bootstrapDeployments = server.getDeployments();
      //
      try
      {
         createProfileService(domainMetaData);
         populateProfileRepository();
         createWorkaroundProfile();
         start(domainMetaData);
         try
         {
            getConfig().getDeployerRegistry().checkAllComplete();
         }
         catch(Exception e)
         {
            log.error("Failed to load profile:", e);
         }
         hdScannerFactory.enableScanning();
      }
      catch(Throwable t)
      {
         log.error("failed to start server", t);
      }
   }

   public void prepareShutdown(K server)
   {
      super.prepareShutdown();
      hdScannerFactory.disabledScanning();
      getConfig().getDeployerRegistry().prepareShutdown();
   }

   public void shutdown(K server)
   {
      try
      {
         stop(domainMetaData);
      }
      finally
      {
         getConfig().getDeployerRegistry().shutdown();
      }
   }

   protected void createWorkaroundProfile()
   {
      // FIXME
      EmptyProfileMetaData metaData = new EmptyProfileMetaData("workaround");
      List<ProfileMetaDataVisitorNode> nodes = new ArrayList<ProfileMetaDataVisitorNode>();
      nodes.add(new FeatureCapability(CommonProfileNameSpaces.PROFILE_NAMESPACE));
      nodes.add(new FeatureCapability(CommonProfileNameSpaces.IMMUTABLE_PROFILE_NAMESPACE));
      nodes.add(new FeatureCapability(CommonProfileNameSpaces.FARMING_PROFILE_NAMESPACE));
      nodes.add(new FeatureCapability(CommonProfileNameSpaces.HOTDEPLOY_PROFILE_NAMESPACE));
      metaData.setFeatures(nodes);
      profileRepository.getDomainRepository().registerMetaData(metaData);
   }

   protected AbstractDomainMetaData createDomainMetaData(String serverName)
   {
      // The delegate
      DomainMetaData delegate = getProfileFactory().getDomainMetaData();
      // Temp bootstrap domain meta data
      return new BootstrapDomainMetaData(DOMAIN_NAME, serverName, delegate);
   }

   protected void populateProfileRepository() throws Exception
   {
      if(getProfilesLocation() != null)
      {
         ProfilesMetaDataFactory metadataFactory = new ProfilesMetaDataFactory();
         metadataFactory.parse(getProfilesLocation());
         Map<String, ProfileMetaData> profiles = metadataFactory.getProfiles();
         for(ProfileMetaData metaData : profiles.values())
         {
            profileRepository.getProfileRepository().registerMetaData(metaData);
         }
      }
   }

}

