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
package org.jboss.test.ejb3.ejbthree7376.unit;

import javax.naming.InitialContext;
import javax.validation.ConstraintViolationException;

import junit.framework.Test;

import org.jboss.test.JBossTestCase;
import org.jboss.test.ejb3.ejbthree7376.Player;
import org.jboss.test.ejb3.ejbthree7376.SoccerFacade;


/**
 * JPA 2 testing
 *
 * @author Scott Marlow
 */
public class JPAUnitTestCase extends JBossTestCase
{

   public static Test suite() throws Exception
   {
      return getDeploySetup(JPAUnitTestCase.class, "ejbthree7376.jar");
   }

   public JPAUnitTestCase(String name)
   {
      super(name);
   }

   public void testJPA() throws Exception
   {
     getLog().info("testJPA start");
     InitialContext ctx = this.getInitialContext();
     SoccerFacade stats = (SoccerFacade) ctx.lookup("ejb3/ejbthree7376/SoccerFacade");
     getLog().info("testJPA about to add player");
     stats.addPlayer(0,"Heather Mitts","Boston Breakers", 0); // 2009 season
     getLog().info("testJPA player added.  About to look up player");
     Player p = stats.getPlayer(0);
     assertNotNull("player is null" , p);
     assertEquals(p.getName(), "Heather Mitts");
     assertEquals(p.getTeam(), "Boston Breakers");
     getLog().info("testJPA end");
   }
 
   public void testJPABeanValidation() throws Exception
   {
     getLog().info("testJPABeanValidation start");
     InitialContext ctx = this.getInitialContext();
     SoccerFacade stats = (SoccerFacade) ctx.lookup("ejb3/ejbthree7376/SoccerFacade");
     try {
       stats.addPlayer(1, "Christine Lily","Boston Breakers", 9999); // should be invalid number of goals 
       fail("failed to throw ConstraintViolationException");
     }
     catch(ConstraintViolationException expected)
     {
       // expected path
     }

     getLog().info("testJPABeanValidation ended");
   }

  public void testJPAReadLock() throws Exception
  {
     getLog().info("testJPA start");
     InitialContext ctx = this.getInitialContext();
     SoccerFacade stats = (SoccerFacade) ctx.lookup("ejb3/ejbthree7376/SoccerFacade");
     getLog().info("testJPA about to add player");
     stats.addPlayer(2,"Fabiana","Boston Breakers", 5); // 2009 season
     assertNotNull("player is null", stats.getPlayer(2));
     getLog().info("testJPA player added.  About to look up player");
     Player p = stats.getPlayerReadLock(2);
     assertNotNull("player is null" , p);
     assertEquals(p.getName(), "Fabiana");
     assertEquals(p.getTeam(), "Boston Breakers");
     getLog().info("testJPA end");
  }

  public void testJPAOptimisticLock() throws Exception
  {
     getLog().info("testJPA start");
     InitialContext ctx = this.getInitialContext();
     SoccerFacade stats = (SoccerFacade) ctx.lookup("ejb3/ejbthree7376/SoccerFacade");
     getLog().info("testJPA about to add player");
     stats.addPlayer(3,"Kelly Smith","Boston Breakers", 10); // 2009 season
     assertNotNull("player is null", stats.getPlayer(3));
     getLog().info("testJPA player added.  About to look up player");
     Player p = stats.getPlayerOptimisticLock(3);
     assertNotNull("player is null" , p);
     assertEquals(p.getName(), "Kelly Smith");
     assertEquals(p.getTeam(), "Boston Breakers");
     getLog().info("testJPA end");

  }


}
