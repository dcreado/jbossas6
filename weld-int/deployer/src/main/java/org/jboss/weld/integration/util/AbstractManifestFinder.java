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
package org.jboss.weld.integration.util;

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.jar.Manifest;

import org.jboss.logging.Logger;

/**
 * Find manifest info from class - abstract from code source.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
abstract class AbstractManifestFinder implements ManifestFinder
{
   protected Logger log = Logger.getLogger(getClass());

   public Manifest findManifest(Class<?> clazz) throws Exception
   {
      ProtectionDomain domain = clazz.getProtectionDomain();
      CodeSource source = domain.getCodeSource();
      URL location = source.getLocation();
      return findManifest(location);
   }

   /**
    * Find manifest from base url.
    *
    * @param url the url
    * @return manifest or null
    * @throws Exception for any error
    */
   protected abstract Manifest findManifest(URL url) throws Exception;
}