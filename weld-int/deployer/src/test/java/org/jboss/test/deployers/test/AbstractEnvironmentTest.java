/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.deployers.test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.deployers.support.MockArchiveManifest;
import org.jboss.test.deployers.support.crm.CrmWebBean;
import org.jboss.test.deployers.support.jar.PlainJavaBean;
import org.jboss.test.deployers.support.ui.UIWebBean;
import org.jboss.test.deployers.support.web.ServletWebBean;

/**
 * Abstract Environment test case.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @param E the type of environment this test applies to
 */
public abstract class AbstractEnvironmentTest<E> extends AbstractWeldTest
{
   protected static final String EAR_NAME = "simple.ear";
   protected static final String EJB_JAR_NAME = "ejb.jar";
   protected static final String WAR_NAME = "simple.war";
   
   public AbstractEnvironmentTest(String name)
   {
      super(name);
   }

   private static void addExpectedClass(Set<String> expected, Class<?> clazz)
   {
      expected.add(clazz.getName());
   }

   private void assertExpectedClasses(E environment, Set<String> expected)
   {
      Collection<String> weldClasses = getClasses(environment);
      assertNotNull(weldClasses);
      assertTrue("Unexpected empty weld classes collection", expected.isEmpty() || !weldClasses.isEmpty());
      for (String className : weldClasses)
      {
         assertTrue("Found unexpected class: " + className, expected.remove(className));
      }
      assertEmpty("Should be emtpy, missing " + expected, expected);
   }

   protected void assertExpectedClasses(E environment, Class<?>... classes)
   {
      Set<String> expected = new HashSet<String>();
      for(Class<?> clazz: classes)
      {
         addExpectedClass(expected, clazz);
      }
      assertExpectedClasses(environment, expected);
   }

   protected JavaArchive createEjbJar(boolean jarCDI)
   {
      return createEjbJar(EJB_JAR_NAME, jarCDI, PlainJavaBean.class);
   }
   
   protected JavaArchive createEjbJar(String jarName, boolean jarCDI, Class<?>... classes)
   {
      JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, jarName);
      for (Class<?> clazz: classes)
      {
         ejbJar.addClass(clazz);
      }
      MockArchiveManifest.addManifest(ejbJar, jarCDI);
      return ejbJar;
   }

   protected WebArchive createWar(boolean warCDI)
   {
      return createWar(WAR_NAME, warCDI, ServletWebBean.class);
   }

   protected WebArchive createWar(String warName, boolean warCDI, Class<?>... classes)
   {
      WebArchive war = ShrinkWrap.create(WebArchive.class, warName);
      for (Class<?> clazz: classes)
      {
         war.addClass(clazz);
      }
      MockArchiveManifest.addManifest(war, warCDI);
      return war;
   }

   protected WebArchive createWarWithLib(boolean warCDI, boolean libCDI)
   {
      WebArchive war = createWar(warCDI);
      createLib(war, "lib.jar", libCDI, UIWebBean.class);
      return war;
   }

   protected WebArchive createWarWithLibs(boolean warCDI, boolean lib1CDI, boolean lib2CDI)
   {
      WebArchive war = createWar(warCDI);
      createLib(war, "lib1.jar", lib1CDI, UIWebBean.class);
      createLib(war, "lib2.jar", lib2CDI, CrmWebBean.class);
      return war;
   }

   protected void createLib(LibraryContainer<?> archive, String libName, boolean cdi, Class<?>... classes)
   {
      JavaArchive lib = ShrinkWrap.create(JavaArchive.class, libName);
      if (cdi)
      {
         MockArchiveManifest.addCDIManifest(lib);
      }
      for (Class<?> libClass: classes)
      {
         lib.addClass(libClass);
      }
      archive.addLibrary(lib);
   }

   /**
    * Returns the classes recorded in the environment.
    * 
    * @param environment the environment
    * @return the list of classes that have been found in the environment
    */
   protected abstract Collection<String> getClasses(E environment);
}
