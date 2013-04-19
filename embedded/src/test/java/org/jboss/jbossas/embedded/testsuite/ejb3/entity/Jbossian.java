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
package org.jboss.jbossas.embedded.testsuite.ejb3.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * JBossian
 *
 * Test entity bean to represent a hacker
 * of JBoss projects
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
@Entity
public class Jbossian implements Serializable
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 1L;

   //-------------------------------------------------------------------------------------||
   // Instance Members -------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Primary key
    */
   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long id;

   /**
    * Name 
    */
   private String name;

   /**
    * Position / title
    */
   private String role;

   /**
    * Age
    */
   private int age;

   //-------------------------------------------------------------------------------------||
   // Constructor ------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Default, no-arg constructor
    */
   public Jbossian()
   {

   }

   /**
    * Constructor to set all properties
    */
   public Jbossian(final String name, final String role, final int age)
   {
      // Set properties
      this.setName(name);
      this.setRole(role);
      this.setAge(age);
   }

   //-------------------------------------------------------------------------------------||
   // Accessors / Mutators ---------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * @return the name
    */
   public String getName()
   {
      return name;
   }

   /**
    * @param name the name to set
    */
   public void setName(String name)
   {
      this.name = name;
   }

   /**
    * @return the role
    */
   public String getRole()
   {
      return role;
   }

   /**
    * @param role the role to set
    */
   public void setRole(String role)
   {
      this.role = role;
   }

   /**
    * @return the age
    */
   public int getAge()
   {
      return age;
   }

   /**
    * @param age the age to set
    */
   public void setAge(int age)
   {
      this.age = age;
   }

   /**
    * @return the id
    */
   public long getId()
   {
      return id;
   }

   /**
    * @param id the id to set
    */
   public void setId(long id)
   {
      this.id = id;
   }

   //-------------------------------------------------------------------------------------||
   // Overridden Implementations ---------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString()
   {
      return this.getClass().getSimpleName() + " [name=" + name + ", role=" + role + ", age=" + age + "]";
   }
}
