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
package org.jboss.system.server.profileservice.repository.clustered;

import org.jboss.profileservice.profile.metadata.plugin.FarmingProfileMetaData;
import org.jboss.profileservice.repository.legacy.DelegateProfile;
import org.jboss.profileservice.spi.DeploymentRepository;
import org.jboss.profileservice.spi.ProfileFactory;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.profileservice.spi.ProfileRepository;

/**
 * The clustered profile factory.
 * 
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision$
 */
public class ClusteredProfileFactory implements ProfileFactory<FarmingProfileMetaData, DelegateProfile>
{

   /** The farming profile meta data. */
   private static final String[] TYPES = new String[] { FarmingProfileMetaData.class.getName() };
   
   /** The profile repository. */
   private ProfileRepository profileRepository;
   

   public ProfileRepository getProfileRepository()
   {
      return profileRepository;
   }
   
   public void setProfileRepository(ProfileRepository profileRepository)
   {
      this.profileRepository = profileRepository;
   }
   

   public String[] getTypes()
   {
      return TYPES;
   }
   
   public DelegateProfile createProfile(ProfileKey key, FarmingProfileMetaData metaData) throws Exception
   {
      DeploymentRepository repository = profileRepository.createProfileDeploymentRepository(key, metaData);
      return new DelegateProfile(repository, key);
   }
   
   public void destroyProfile(FarmingProfileMetaData metaData, DelegateProfile profile)
   {
      // 
   }
   
}

