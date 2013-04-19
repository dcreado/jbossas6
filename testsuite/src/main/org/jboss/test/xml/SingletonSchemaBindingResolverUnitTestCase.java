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
package org.jboss.test.xml;

import junit.framework.Test;

import org.jboss.beans.metadata.plugins.AbstractBeanMetaData;
import org.jboss.test.xml.initializer.Container;
import org.jboss.test.xml.initializer.ContainerInitializer;
import org.jboss.xb.binding.resolver.MutableSchemaResolver;
import org.jboss.xb.binding.sunday.unmarshalling.SingletonSchemaResolverFactory;
import org.jboss.xb.binding.sunday.unmarshalling.SchemaBindingResolver;

/**
 * SingletonSchemaBindingResolverUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 99904 $
 */
public class SingletonSchemaBindingResolverUnitTestCase extends AbstractJBossXBTest
{
   public static SchemaBindingResolver initResolver() throws Exception
   {
      MutableSchemaResolver resolver = SingletonSchemaResolverFactory.getInstance().getSchemaBindingResolver();
      Class clazz = SingletonSchemaBindingResolverUnitTestCase.class;
      resolver.mapSchemaInitializer(ContainerInitializer.NS, ContainerInitializer.class.getName());
      String location = getSchemaLocation(clazz, "SchemaBindingInitializerUnitTestCaseContainer.xsd");
      resolver.mapSchemaLocation(ContainerInitializer.NS, location);
      return resolver;
   }

   public void testBean() throws Exception
   {
      Container container = (Container) unmarshal(rootName + "ContainerStrictBean.xml", Container.class);
      Object object = container.getValue();
      assertNotNull("Should have a value", object);
      assertTrue(object instanceof AbstractBeanMetaData);
      AbstractBeanMetaData bean = (AbstractBeanMetaData) object;
      assertEquals("TestClass", bean.getBean());
   }

   protected void configureLogging()
   {
      //enableTrace("org.jboss.test.xml");
   }

   /**
    * Setup the test
    * 
    * @return the test
    */
   public static Test suite()
   {
      return suite(SingletonSchemaBindingResolverUnitTestCase.class);
   }

   /**
    * Create a new SingletonSchemaBindingResolverUnitTestCase.
    * 
    * @param name the test name
    */
   public SingletonSchemaBindingResolverUnitTestCase(String name)
   {
      super(name);
   }
}
