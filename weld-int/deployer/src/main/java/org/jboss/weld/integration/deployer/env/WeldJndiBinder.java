/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.weld.integration.deployer.env;

import javax.enterprise.inject.spi.BeanManager;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;

/**
 * This singleton bean for the deployer is responsible for binding an object
 * factory to the required name(s) in JNDI. Once bound, all deployed
 * applications can obtain the Web Beans manager from JNDI.
 *
 * @author David Allen
 *
 */
public class WeldJndiBinder
{
   private String jndiName = null;

   /**
    * Start the service.
    *
    * @param jndiContextPath the jndi context path
    * @param managerObjectFactoryClass the manager object factory
    * @throws Exception for any error
    */
   public void startService(String jndiContextPath, String managerObjectFactoryClass) throws Exception
   {
      jndiName = jndiContextPath;
      Reference managerReference = new Reference(BeanManager.class.getName(), managerObjectFactoryClass, null);
      bind(jndiContextPath, managerReference);
   }

   /**
    * Stop the service.
    *
    * @throws Exception for any error
    */
   public void stopService() throws Exception
   {
      Context initialContext = new InitialContext();
      initialContext.unbind(jndiName);
   }

   /**
    * Bind object to jndi.
    *
    * @param key the key
    * @param binding the object to bind
    * @throws Exception for any error
    */
   protected void bind(String key, Object binding) throws Exception
   {
      Context initialContext = new InitialContext();
      try
      {
         String[] parts = key.split("/");
         int length = parts.length;
         Context context = initialContext;
         Context nextContext;
         for (int i = 0; i < length - 1; i++)
         {
            try
            {
               nextContext = (Context) context.lookup(parts[i]);
            }
            catch (NamingException e)
            {
               nextContext = context.createSubcontext(parts[i]);
            }
            context = nextContext;
         }
         context.bind(parts[length - 1], binding);
      }
      catch (NamingException e)
      {
         throw new RuntimeException("Cannot bind " + binding + " to " + key, e);
      }
   }
}