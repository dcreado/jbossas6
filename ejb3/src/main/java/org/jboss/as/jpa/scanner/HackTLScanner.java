/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.jpa.scanner;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Set;

import org.hibernate.ejb.packaging.NamedInputStream;
import org.hibernate.ejb.packaging.NativeScanner;
import org.hibernate.ejb.packaging.Scanner;

/**
 * Try hack thread local scanner.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class HackTLScanner implements Scanner
{
   private Scanner delegate;

   private Scanner getDelegate()
   {
      if (delegate == null)
      {
         Scanner scanner = HackCEMFBuilder.getScanner();
         if (scanner != null)
            delegate = scanner;
         else
            delegate = new NativeScanner();
      }
      return delegate;
   }

   public Set<Package> getPackagesInJar(URL jartoScan, Set<Class<? extends Annotation>> annotationsToLookFor)
   {
      return getDelegate().getPackagesInJar(jartoScan, annotationsToLookFor);
   }

   public Set<Class<?>> getClassesInJar(URL jartoScan, Set<Class<? extends Annotation>> annotationsToLookFor)
   {
      return getDelegate().getClassesInJar(jartoScan, annotationsToLookFor);
   }

   public Set<NamedInputStream> getFilesInJar(URL jartoScan, Set<String> filePatterns)
   {
      return getDelegate().getFilesInJar(jartoScan, filePatterns);
   }

   public Set<NamedInputStream> getFilesInClasspath(Set<String> filePatterns)
   {
      return getDelegate().getFilesInClasspath(filePatterns);
   }

   public String getUnqualifiedJarName(URL jarUrl)
   {
      return getDelegate().getUnqualifiedJarName(jarUrl);
   }
}