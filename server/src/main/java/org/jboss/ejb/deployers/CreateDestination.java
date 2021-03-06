/*
* JBoss, Home of Professional Open Source
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
package org.jboss.ejb.deployers;

/**
 * CreateDestination.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 102506 $
 */
public class CreateDestination
{
   /** The matcher */
   private CreateDestinationMatcher matcher;
   
   /** The factory */
   private CreateDestinationFactory factory;

   /**
    * Create a new CreateDestination.
    */
   public CreateDestination()
   {
   }
   
   /**
    * Create a new CreateDestination.
    * 
    * @param matcher the matcher
    * @param factory the factory
    * @throws IllegalArgumentException for a null parameter
    */
   public CreateDestination(CreateDestinationMatcher matcher, CreateDestinationFactory factory)
   {
      if (matcher == null)
         throw new IllegalArgumentException("Null matcher");
      if (factory == null)
         throw new IllegalArgumentException("Null factory");
      this.matcher = matcher;
      this.factory = factory;
   }

   /**
    * Get the matcher.
    * 
    * @return the matcher.
    */
   public CreateDestinationMatcher getMatcher()
   {
      return matcher;
   }

   /**
    * Set the matcher.
    * 
    * @param matcher the matcher.
    */
   public void setMatcher(CreateDestinationMatcher matcher)
   {
      this.matcher = matcher;
   }

   /**
    * Get the factory.
    * 
    * @return the factory.
    */
   public CreateDestinationFactory getFactory()
   {
      return factory;
   }

   /**
    * Set the factory.
    * 
    * @param factory the factory.
    */
   public void setFactory(CreateDestinationFactory factory)
   {
      this.factory = factory;
   }
   
   public Class<?> getOutput()
   {
       return factory.getOutput();
   }

   /**
    * Validate the parameters
    */
   public void create()
   {
      if (matcher == null)
         throw new IllegalStateException("No matcher has been set");
      if (factory == null)
         throw new IllegalStateException("No factory has been set");
   }
}
