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
package org.jboss.test.ejb3.ejbthree7376;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

/**
 * @author Scott Marlow
 *
 */
@Stateless(mappedName="ejb3/ejbthree7376/Soccer") 
public class SoccerBean implements Soccer
{
   @PersistenceContext
   private EntityManager em;

   public void addPlayer(int id, String name, String team, int goals)
   {

     Player p = new Player(id, name, team, goals);
     em.persist(p);
     em.flush();
   }

   public Player getPlayer(int id)
   {
     Player p = em.find(Player.class, id);
     return p;
   }

   public Player getPlayerReadLock(int id)
   {
     Player p = em.find(Player.class, id);
     if(p != null)
        em.lock(p, LockModeType.READ);
     return p;
  }

   public Player getPlayerOptimisticLock(int id)
   {
     Player p = em.find(Player.class, id);
     if(p != null)
        em.lock(p, LockModeType.OPTIMISTIC);
     return p;
  }

}
