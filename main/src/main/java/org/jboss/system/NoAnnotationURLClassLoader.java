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
package org.jboss.system;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

/**
 * A URL classloader to avoid URL annotation of classes in RMI
 *
 * @author <a href="mailto:Adrian.Brock@HappeningTimes.com">Adrian Brock</a>
 * @version $Revision: 82920 $
 */
public class NoAnnotationURLClassLoader
   extends URLClassLoader
{
   /** The value returned by {@link getURLs}. */
   private static final URL[] EMPTY_URL_ARRAY = {};

   /**
    * Create a classloader with the context classloader as parent
    * 
    * @param urls the urls
    * @return the classloader
    */
   public static NoAnnotationURLClassLoader createClassLoader(URL... urls)
   {
      return createClassLoader(Thread.currentThread().getContextClassLoader(), urls);
   }

   /**
    * Create a classloader
    *
    * @param parent the parent classloader
    * @param urls the urls
    * @return the classloader
    */
   public static NoAnnotationURLClassLoader createClassLoader(ClassLoader parent, URL... urls)
   {
      return new NoAnnotationURLClassLoader(urls, parent);
   }
   
   /**
    * Construct a <tt>URLClassLoader</tt>
    *
    * @param urls   the URLs to load classes from.
    */
   public NoAnnotationURLClassLoader(URL[] urls)
   {
      super(urls);
   }

   /**
    * Construct a <tt>URLClassLoader</tt>
    *
    * @param urls   the URLs to load classes from.
    * @param parent the parent classloader.
    */
   public NoAnnotationURLClassLoader(URL[] urls, ClassLoader parent)
   {
      super(urls, parent);
   }

   /**
    * Construct a <tt>URLClassLoader</tt>
    *
    * @param urls    the URLs to load classes from.
    * @param parent  the parent classloader.
    * @param factory the url stream factory.
    */
   public NoAnnotationURLClassLoader(URL[] urls, ClassLoader parent,
                                     URLStreamHandlerFactory factory)
   {
      super(urls, parent, factory);
   }

   /**
   * Return all library URLs
   *
   * <p>Do not remove this method without running the WebIntegrationTestSuite
   * @return the urls
   */
   public URL[] getAllURLs()
   {
      return super.getURLs();
   }

   /**
   * Return an empty URL array to force the RMI marshalling subsystem to
   * use the <tt>java.server.codebase</tt> property as the annotated codebase.
   *
   * <p>Do not remove this method without discussing it on the dev list.
   *
   * @return Empty URL[]
   */
   public URL[] getURLs()
   {
      return EMPTY_URL_ARRAY;
   }
}
