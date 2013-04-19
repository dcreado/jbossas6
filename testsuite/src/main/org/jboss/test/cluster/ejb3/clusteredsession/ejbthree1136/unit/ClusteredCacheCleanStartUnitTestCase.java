/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.test.cluster.ejb3.clusteredsession.ejbthree1136.unit;

import javax.management.ObjectName;

import junit.framework.Test;

import org.jboss.test.cluster.ejb3.clusteredsession.ejbthree1136.SFSBCacheManipulator;
import org.jboss.test.JBossClusteredTestCase;

/**
 * Tests that StatefulTreeCache properly cleans up state when initializing.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1.1 $
 */
public class ClusteredCacheCleanStartUnitTestCase extends JBossClusteredTestCase
{
   /**
    * Create a new ClusteredCacheCleanupTestCase.
    * 
    * @param name
    */
   public ClusteredCacheCleanStartUnitTestCase(String name)
   {
      super(name);
   }
   
   public static Test suite() throws Exception
   {
      return getDeploySetup(ClusteredCacheCleanStartUnitTestCase.class, "ejbthree1136.sar");
   }

   public void testClusteredCacheCleanStart() throws Exception
   {
      assertEquals("Extraneous data present before deploy", SFSBCacheManipulator.VALUE, getExtraneousData());
      
      deploy("ejbthree1136-bean.jar");

      try
      {
         assertEquals("Extraneous data removed", null, getExtraneousData());
      }
      finally
      {
         undeploy("ejbthree1136-bean.jar");
      }
   }
   
   private Object getExtraneousData() throws Exception
   {
      ObjectName on = new ObjectName("jboss.test:service=Ejb3SFSBCacheManipulator");
      return invoke(on, "lookup", new Object[]{}, new String[]{});
   }
}
