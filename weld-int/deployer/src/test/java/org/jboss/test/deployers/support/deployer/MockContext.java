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
package org.jboss.test.deployers.support.deployer;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * Pure mock, no logic used when hacking this ;-)
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class MockContext implements Context
{
   private Hashtable<Object, Object> env = new Hashtable<Object, Object>();
   private Map<Object, Object> map = new HashMap<Object, Object>();

   public Object lookup(Name name) throws NamingException
   {
      return map.get(name);
   }

   public Object lookup(String name) throws NamingException
   {
      return map.get(name);
   }

   public void bind(Name name, Object obj) throws NamingException
   {
      map.put(name, obj);
   }

   public void bind(String name, Object obj) throws NamingException
   {
      map.put(name, obj);
   }

   public void rebind(Name name, Object obj) throws NamingException
   {
      map.put(name, obj);
   }

   public void rebind(String name, Object obj) throws NamingException
   {
      map.put(name, obj);
   }

   public void unbind(Name name) throws NamingException
   {
      map.remove(name);
   }

   public void unbind(String name) throws NamingException
   {
      map.remove(name);
   }

   public void rename(Name oldName, Name newName) throws NamingException
   {
      Object previous = map.remove(oldName);
      map.put(newName, previous);
   }

   public void rename(String oldName, String newName) throws NamingException
   {
      Object previous = map.remove(oldName);
      map.put(newName, previous);
   }

   private <T> NamingEnumeration<T> empty()
   {
      return new NamingEnumeration<T>()
      {
         public T next() throws NamingException
         {
            return null;
         }

         public boolean hasMore() throws NamingException
         {
            return false;
         }

         public void close() throws NamingException
         {
         }

         public boolean hasMoreElements()
         {
            return false;
         }

         public T nextElement()
         {
            return null;
         }
      };
   }

   public NamingEnumeration<NameClassPair> list(Name name) throws NamingException
   {
      return empty();
   }

   public NamingEnumeration<NameClassPair> list(String name) throws NamingException
   {
      return empty();
   }

   public NamingEnumeration<Binding> listBindings(Name name) throws NamingException
   {
      return empty();
   }

   public NamingEnumeration<Binding> listBindings(String name) throws NamingException
   {
      return empty();
   }

   public void destroySubcontext(Name name) throws NamingException
   {
      map.remove(name);
   }

   public void destroySubcontext(String name) throws NamingException
   {
      map.remove(name);
   }

   public Context createSubcontext(Name name) throws NamingException
   {
      Context context = new MockContext();
      map.put(name, context);
      return context;
   }

   public Context createSubcontext(String name) throws NamingException
   {
      Context context = new MockContext();
      map.put(name, context);
      return context;
   }

   public Object lookupLink(Name name) throws NamingException
   {
      return map.get(name);
   }

   public Object lookupLink(String name) throws NamingException
   {
      return map.get(name);
   }

   public NameParser getNameParser(Name name) throws NamingException
   {
      return NameParser.class.cast(map.get(name));
   }

   public NameParser getNameParser(String name) throws NamingException
   {
      return NameParser.class.cast(map.get(name));
   }

   public Name composeName(Name name, Name prefix) throws NamingException
   {
      return new CompositeName().addAll(prefix).addAll(name);
   }

   public String composeName(String name, String prefix) throws NamingException
   {
      return prefix + "/" + name;
   }

   public Object addToEnvironment(String propName, Object propVal) throws NamingException
   {
      return env.put(propName, propVal);
   }

   public Object removeFromEnvironment(String propName) throws NamingException
   {
      return env.remove(propName);
   }

   public Hashtable<?, ?> getEnvironment() throws NamingException
   {
      return env;
   }

   public void close() throws NamingException
   {
      map.clear();
   }

   public String getNameInNamespace() throws NamingException
   {
      return null;
   }
}