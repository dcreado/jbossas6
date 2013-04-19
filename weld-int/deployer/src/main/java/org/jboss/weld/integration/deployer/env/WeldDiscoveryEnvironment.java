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
package org.jboss.weld.integration.deployer.env;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.classloading.spi.visitor.ResourceVisitor;

/**
 * WBD env impl.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class WeldDiscoveryEnvironment
{
   private Set<String> classes = new HashSet<String>();
   private Set<URL> urls = new HashSet<URL>();

   /**
    * Create visitor.
    *
    * @return the weld discovery visitor
    */
   public ResourceVisitor visitor()
   {
      return new ArchiveDiscoveryDeployer.WBDiscoveryVisitor(this);
   }

   /**
    * Add weld class.
    *
    * @param className the weld class
    */
   public void addWeldClass(String className)
   {
      classes.add(className);
   }

   /**
    * Add weld xml url.
    *
    * @param url the weld xml url
    */
   public void addWeldXmlURL(URL url)
   {
      urls.add(url);
   }

   /**
    * Get weld classes.
    *
    * @return the weld classes
    */
   @Deprecated
   public Collection<String> getWeldClasses()
   {
      // FIXME WELDINT-1 old classes that use this method should get an Unmodifiable
      // collection; if those classes are not deleted this method needs to be reviewed
      return classes;
   }

   /**
    * Get weld xmls.
    *
    * @return the weld xmls
    */
   public Collection<URL> getWeldXml()
   {
      return Collections.unmodifiableCollection(urls);
   }
}