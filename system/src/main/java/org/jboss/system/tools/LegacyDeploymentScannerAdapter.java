/*
* JBoss, Home of Professional Open Source
* Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.system.tools;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.profileservice.deployment.hotdeploy.HDScannerFactory;
import org.jboss.profileservice.deployment.hotdeploy.Scanner;

/**
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @version $Revision$
 */
public class LegacyDeploymentScannerAdapter extends ProfileServiceToolsFacade implements DeploymentRepositoryAdapter
{

   /** The HDScanner factory. */
   private HDScannerFactory hdFactory;
   
   /** Internal list which scanners we stopped. */
   private List<Scanner> stoppedScanners = new ArrayList<Scanner>();
   
   public HDScannerFactory getHdFactory()
   {
      return hdFactory;
   }
   
   public void setHdFactory(HDScannerFactory hdFactory)
   {
      this.hdFactory = hdFactory;
   }
   
   /**
    * {@inheritDoc}
    */
   public void resume()
   {
      synchronized (stoppedScanners)
      {
         final Iterator<Scanner> scanners = stoppedScanners.iterator();
         while(scanners.hasNext())
         {
            final Scanner scanner = scanners.next();
            try
            {
               synchronized(scanner)
               {
                  scanner.start();
               }
            }
            catch(Exception e)
            {
               // log
            }
            finally
            {
               scanners.remove();
            }
         }
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public void scan()
   {
      synchronized(stoppedScanners)
      {
         // TODO only scan the profiles registered here ?
         for(final Scanner scanner : hdFactory.getRegisteredScanners())
         {
            synchronized(scanner)
            {
               if(scanner.isScheduled())
               {
                  try
                  {
                     scanner.scan();
                  }
                  catch(Exception e)
                  {
                     // log
                  }
               }  
            }
         }
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public void suspend()
   {
      synchronized(stoppedScanners)
      {
         for(final Scanner scanner : hdFactory.getRegisteredScanners())
         {
            synchronized(scanner)
            {
               if(scanner.isScheduled())
               {
                  scanner.stop();
                  stoppedScanners.add(scanner);
               }
            }      
         }         
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addURL(URL url) throws URISyntaxException
   {
      super.addURI(url.toURI());
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasURL(URL url) throws URISyntaxException
   {
      return super.managesURI(url.toURI());
   }

   /**
    * {@inheritDoc}
    */
   public void removeURL(URL url) throws URISyntaxException
   {
      super.removeURI(url.toURI());
   }
   
}

