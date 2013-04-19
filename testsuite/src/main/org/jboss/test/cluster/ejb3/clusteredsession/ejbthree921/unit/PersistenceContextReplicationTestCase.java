/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
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
package org.jboss.test.cluster.ejb3.clusteredsession.ejbthree921.unit;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.Test;

import org.jboss.test.JBossClusteredTestCase;
import org.jboss.test.cluster.ejb3.clusteredsession.ejbthree921.MyStateful;
import org.jboss.test.cluster.ejb3.clusteredsession.ejbthree921.Person;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class PersistenceContextReplicationTestCase extends JBossClusteredTestCase
{

   public PersistenceContextReplicationTestCase(String name)
   {
      super(name);
   }

   protected InitialContext getInitialContext(int node) throws Exception {
      // Connect to the server0 JNDI
      String[] urls = getNamingURLs();
      Properties env1 = new Properties();
      env1.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
      env1.setProperty(Context.PROVIDER_URL, urls[node]);
      return new InitialContext(env1);
   }
   
   public void test1() throws Exception
   {
      MyStateful stateful = (MyStateful) getInitialContext(0).lookup("MyStatefulBean/remote");
      Person p = new Person("Brian");
      stateful.save(p);
      String expected = "Changing SFSB state";
      stateful.setDescription(expected);
      stateful.setUpFailover("once");
      try
      {
         String actual = stateful.getDescription();
         assertEquals(expected, actual);
      }
      catch(Exception e)
      {
         Throwable cause = e;
         while(cause.getCause() != null) cause = cause.getCause();
         throw (Exception) cause;
      }
      stateful.remove(p);
      stateful.done();
   }
   
   public static Test suite() throws Exception
   {
      return getDeploySetup(PersistenceContextReplicationTestCase.class, "ejbthree921.jar");
   }

}
