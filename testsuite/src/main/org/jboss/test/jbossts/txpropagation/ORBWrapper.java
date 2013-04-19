/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2009,
 * @author JBoss Inc.
 */
package org.jboss.test.jbossts.txpropagation;

import org.omg.CORBA.ORBPackage.InvalidName;

import com.arjuna.orbportability.ORB;
import com.arjuna.orbportability.RootOA;

/**
 * Simple class to start and stop an ORB for use with OTS based transactions.
 */
public class ORBWrapper
{
   ORB orb;
   RootOA oa;

   public void start() throws InvalidName
   {
      orb = com.arjuna.orbportability.ORB.getInstance("ClientSide");
      oa = com.arjuna.orbportability.OA.getRootOA(orb);
      
      orb.initORB(new String[] {}, null);
      oa.initOA();
      
      com.arjuna.ats.internal.jts.ORBManager.setORB(orb);
      com.arjuna.ats.internal.jts.ORBManager.setPOA(oa);
   }

   public void stop()
   {
      oa.destroy();
      orb.shutdown();
   }

}
