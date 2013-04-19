/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.cluster.ejb3.clusteredsession.ejbthree921;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.jboss.ejb3.annotation.CacheConfig;
import org.jboss.ejb3.annotation.Clustered;
import org.jboss.logging.Logger;

/**
 * @author carlo
 *
 */
@Clustered
@CacheConfig(name = "${jbosstest.cluster.sfsb.cache.config:sfsb-cache}")
@Stateful
@Remote(MyStateful.class)
@Interceptors({ExplicitFailoverInterceptor.class})
public class MyStatefulBean implements MyStateful
{
   private static final Logger log = Logger.getLogger(MyStatefulBean.class);
   
   @PersistenceContext(type=PersistenceContextType.EXTENDED)
   private EntityManager em;
   
   private String description;
   
   @Remove
   public void done()
   {
      
   }
   
   public String getDescription()
   {
      return description;
   }
   
   @PostActivate
   protected void postActivate()
   {
      log.info("postActivate");
   }
   
   @PrePassivate
   protected void prePassivate()
   {
      log.info("prePassivate");
   }
   
   public void remove(Person p)
   {
      em.remove(p);
   }
   
   public void save(Person p)
   {
      em.persist(p);
   }
   
   public void setDescription(String s)
   {
      this.description = s;
   }
   
   public void setUpFailover(String failover)
   {
      // To setup the failover property
      log.info("Setting up failover property: " +failover);
      System.setProperty ("JBossCluster-DoFail", failover);
   }
   
   public Person update(Person p)
   {
      return em.merge(p);
   }
}
