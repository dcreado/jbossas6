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
package org.jboss.system.server.jmx;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;
import java.io.ObjectInputStream;
import java.util.Set;


/**
 * wrap the MBeanServer and minimize NotSerializableException
 * 
 * @author Scott Marlow smarlow@redhat.com
 *
 */
public class MBeanServerWrapper implements MBeanServer {

   private MBeanServer x;

   public  MBeanServerWrapper( MBeanServer delegateTo) {
      this.x = delegateTo;
   }

   public MBeanInfo getMBeanInfo(ObjectName name)
      throws InstanceNotFoundException, IntrospectionException, ReflectionException {

      MBeanInfo info = x.getMBeanInfo(name);
      return  new MBeanInfo(
         info.getClassName(),
         info.getDescription(),
         deepCopy(info.getAttributes()), // Strip the Descriptors
         info.getConstructors(),
         info.getOperations(),
         info.getNotifications());
   }

   public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
      return x.createMBean(className, name);
   }

   public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
      return x.createMBean(className, name, loaderName);
   }

   public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
      return x.createMBean(className, name, params, signature);
   }

   public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
      return x.createMBean(className, name, loaderName, params, signature);
   }

   public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
      return x.registerMBean(object, name);
   }

   public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
      x.unregisterMBean(name);
   }

   public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
      return x.getObjectInstance(name);
   }

   public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
      return x.queryMBeans(name, query);
   }

   public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
      return x.queryNames(name, query);
   }

   public boolean isRegistered(ObjectName name) {
      return x.isRegistered(name);
   }

   public Integer getMBeanCount() {
      return x.getMBeanCount();
   }

   public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
      return x.getAttribute(name, attribute);
   }

   public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
      return x.getAttributes(name, attributes);
   }

   public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
      x.setAttribute(name, attribute);
   }

   public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
      return x.setAttributes(name, attributes);
   }

   public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
      return x.invoke(name, operationName, params, signature);
   }

   public String getDefaultDomain() {
      return x.getDefaultDomain();
   }

   public String[] getDomains() {
      return x.getDomains();
   }

   public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
      x.addNotificationListener(name, listener, filter, handback);
   }

   public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
      x.addNotificationListener(name, listener, filter, handback);
   }

   public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
      x.removeNotificationListener(name, listener);
   }

   public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
      x.removeNotificationListener(name, listener, filter, handback);
   }

   public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
      x.removeNotificationListener(name, listener);
   }

   public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
      x.removeNotificationListener(name, listener, filter, handback);
   }


   public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
      return x.isInstanceOf(name, className);
   }

   public Object instantiate(String className) throws ReflectionException, MBeanException {
      return x.instantiate(className);
   }

   public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException, InstanceNotFoundException {
      return x.instantiate(className, loaderName);
   }

   public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
      return x.instantiate(className, params, signature);
   }

   public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, MBeanException, InstanceNotFoundException {
      return x.instantiate(className, loaderName, params, signature);
   }

   public ObjectInputStream deserialize(ObjectName name, byte[] data) throws InstanceNotFoundException, OperationsException {
      return x.deserialize(name, data);
   }

   public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
      return x.deserialize(className, data);
   }

   public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) throws InstanceNotFoundException, OperationsException, ReflectionException {
      return x.deserialize(className, loaderName, data);
   }

   public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
      return x.getClassLoaderFor(mbeanName);
   }

   public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
      return x.getClassLoader(loaderName);
   }

   public ClassLoaderRepository getClassLoaderRepository() {
      return x.getClassLoaderRepository();
   }

   private MBeanAttributeInfo[] deepCopy(MBeanAttributeInfo[] attrs)
   {
      MBeanAttributeInfo[] copy = new MBeanAttributeInfo[attrs.length];
      for (int i = 0; i < attrs.length; i++)
      {
         MBeanAttributeInfo attr = attrs[i];
         copy[i] = new MBeanAttributeInfo(
               attr.getName(),
               attr.getType(),
               attr.getDescription(),
               attr.isReadable(),
               attr.isWritable(),
               attr.isIs());
      }
      return copy;
   }
}
