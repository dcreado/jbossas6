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

import java.util.Collection;
import java.util.logging.Logger;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * JbossianRegistrarBean
 * 
 * EJB3 SLSB to register/manage JBossians
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
@Stateless
@Local(JbossianRegistrarLocalBusiness.class)
public class JbossianRegistrarBean implements JbossianRegistrarLocalBusiness
{
   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(JbossianRegistrarBean.class.getName());

   private static final String EJBQL_ALL_JBOSSIANS = "SELECT o FROM " + Jbossian.class.getSimpleName() + " o";

   //-------------------------------------------------------------------------------------||
   // Instance Members -------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * JPA integration
    */
   @PersistenceContext
   private EntityManager em;

   //-------------------------------------------------------------------------------------||
   // Required Implementations -----------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /* (non-Javadoc)
    * @see org.jboss.embedded.testsuite.fulldep.ejb3.entity.JbossianRegistrarLocalBusiness#add(org.jboss.embedded.testsuite.fulldep.ejb3.entity.Jbossian)
    */
   @Override
   public void add(final Jbossian jbossian) throws IllegalArgumentException
   {
      // Precondition check
      if (jbossian == null)
      {
         throw new IllegalArgumentException("jbossian must be specified");
      }

      // Persist
      log.info("Persisting: " + jbossian);
      em.persist(jbossian);
   }

   /* (non-Javadoc)
    * @see org.jboss.embedded.testsuite.fulldep.ejb3.entity.JbossianRegistrarLocalBusiness#getAllJbossians()
    */
   @Override
   @SuppressWarnings("unchecked")
   public Collection<Jbossian> getAllJbossians()
   {
      // Get all
      final Collection<Jbossian> list;
      try
      {
         list = em.createQuery(EJBQL_ALL_JBOSSIANS).getResultList();
      }
      catch (final ClassCastException cce)
      {
         throw new RuntimeException(
               "Error in setup, only objects of type " + Jbossian.class.getName() + " are allowed", cce);
      }
      log.info("Returning " + list.size() + " " + Jbossian.class.getSimpleName() + "(s)");
      return list;
   }
}
