/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.ha.singleton;

import java.lang.reflect.Type;

import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.types.SimpleMetaType;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.SimpleValue;
import org.jboss.metatype.api.values.SimpleValueSupport;
import org.jboss.metatype.spi.values.MetaMapper;
import org.jboss.profileservice.spi.ProfileKey;

/**
 * A ProfileKey -> SimpleValue.STRING meta mapper. We only need the
 * profile name, since the domain and server information are shared
 * between all ProfileKeys in a server.
 * 
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision$
 */
public class ProfileKeyMetaMapper extends MetaMapper<ProfileKey>
{

   @Override
   public MetaType getMetaType()
   {
      return SimpleMetaType.STRING;
   }
   
   @Override
   public Type mapToType()
   {
      return ProfileKey.class;
   }
   
   @Override
   public MetaValue createMetaValue(MetaType metaType, ProfileKey key)
   {
      if(key == null)
      {
         throw new IllegalArgumentException("null profile key");
      }
      return SimpleValueSupport.wrap(key.getName());
   }

   @Override
   public ProfileKey unwrapMetaValue(MetaValue metaValue)
   {
      if(metaValue != null && SimpleMetaType.STRING.equals(metaValue.getMetaType()))
      {
         String profileName = String.class.cast(((SimpleValue) metaValue).getValue());
         return new ProfileKey(profileName);
      }
      throw new IllegalStateException("cannot recreate profile key");
   }

}

