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

import javax.ejb.EJB;
import javax.ejb.Stateless;


/**
 * @author Scott Marlow
 *
 */
@Stateless(mappedName="ejb3/ejbthree7376/SoccerFacade") 
public class SoccerFacadeBean implements SoccerFacade
{
   @EJB
   private Soccer stats = null;

   public void addPlayer(int id, String name, String team, int goals)
   {
      stats.addPlayer(id, name, team, goals);
   }

   public Player getPlayer(int id)
   {
     return stats.getPlayer(id);
   }

   public Player getPlayerReadLock(int id)
   {
     return stats.getPlayerReadLock(id);
   }

   public Player getPlayerOptimisticLock(int id)
   {
     return stats.getPlayerOptimisticLock(id);
   }
}
