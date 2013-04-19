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
package org.jboss.deployment;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.dependency.spi.DependencyInfo;
import org.jboss.dependency.spi.DependencyItem;
import org.jboss.deployers.client.spi.main.MainDeployer;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.attachments.MutableAttachments;
import org.jboss.deployers.spi.deployer.DeploymentStage;
import org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer;
import org.jboss.deployers.spi.structure.MetaDataTypeFilter;
import org.jboss.deployers.structure.spi.ClassLoaderFactory;
import org.jboss.deployers.structure.spi.DeploymentResourceLoader;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.structure.spi.DeploymentUnitVisitor;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentResourceLoader;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.jpa.spec.PersistenceUnitMetaData;
import org.jboss.metadata.spi.MetaData;
import org.jboss.metadata.spi.MutableMetaData;
import org.jboss.metadata.spi.scope.ScopeKey;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

/**
 * Hack to work around PersistenceUnitDeployment usage of DeploymentUnit::getMetaDataFile
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class PUHackDeployer extends AbstractSimpleRealDeployer<PersistenceUnitMetaData>
{
   private AbstractSimpleRealDeployer<PersistenceUnitMetaData> delegate;

   public PUHackDeployer(AbstractSimpleRealDeployer<PersistenceUnitMetaData> delegate)
   {
      super(PersistenceUnitMetaData.class);
      if (delegate == null)
         throw new IllegalArgumentException("Null delegate deployer");
      this.delegate = delegate;

      setComponentsOnly(delegate.isComponentsOnly());
      addOutput(BeanMetaData.class);
   }

   @Override
   public void deploy(DeploymentUnit unit, PersistenceUnitMetaData deployment) throws DeploymentException
   {
      DeploymentUnit wrapper = wrap(unit);
      delegate.deploy(wrapper, deployment);
   }

   protected static DeploymentUnit wrap(DeploymentUnit unit)
   {
      if (unit instanceof VFSDeploymentUnit)
         return new VFSDUDelegate((VFSDeploymentUnit)unit);
      else
         return new DUDelegate(unit);
   }

   private static class DUDelegate implements DeploymentUnit
   {
      private DeploymentUnit delegate;

      private DUDelegate(DeploymentUnit delegate)
      {
         this.delegate = delegate;
      }

      public String getName()
      {
         return delegate.getName();
      }

      public String getSimpleName()
      {
         return delegate.getSimpleName();
      }

      public String getRelativePath()
      {
         return delegate.getRelativePath();
      }

      public ScopeKey getScope()
      {
         return delegate.getScope();
      }

      public void setScope(ScopeKey key)
      {
         delegate.setScope(key);
      }

      public ScopeKey getMutableScope()
      {
         return delegate.getMutableScope();
      }

      public void setMutableScope(ScopeKey key)
      {
         delegate.setMutableScope(key);
      }

      public MetaData getMetaData()
      {
         return delegate.getMetaData();
      }

      public MutableMetaData getMutableMetaData()
      {
         return delegate.getMutableMetaData();
      }

      public ClassLoader getClassLoader()
      {
         return delegate.getClassLoader();
      }

      public boolean createClassLoader(ClassLoaderFactory factory) throws DeploymentException
      {
         return delegate.createClassLoader(factory);
      }

      public void removeClassLoader(ClassLoaderFactory factory)
      {
         delegate.removeClassLoader(factory);
      }

      public <T> Set<? extends T> getAllMetaData(Class<T> type)
      {
         return delegate.getAllMetaData(type);
      }

      public MutableAttachments getTransientManagedObjects()
      {
         return delegate.getTransientManagedObjects();
      }

      public boolean isTopLevel()
      {
         return delegate.isTopLevel();
      }

      public DeploymentUnit getTopLevel()
      {
         DeploymentUnit top = delegate.getTopLevel();
         return wrap(top);
      }

      public DeploymentUnit getParent()
      {
         DeploymentUnit parent = delegate.getParent();
         return wrap(parent);
      }

      public List<DeploymentUnit> getChildren()
      {
         return delegate.getChildren();
      }

      public List<DeploymentUnit> getComponents()
      {
         return delegate.getComponents();
      }

      public boolean isComponent()
      {
         return delegate.isComponent();
      }

      public DeploymentUnit addComponent(String name)
      {
         return wrap(delegate.addComponent(name));
      }

      public DeploymentUnit getComponent(String name)
      {
         return wrap(delegate.getComponent(name));
      }

      public boolean removeComponent(String name)
      {
         return delegate.removeComponent(name);
      }

      public DeploymentResourceLoader getResourceLoader()
      {
         return delegate.getResourceLoader();
      }

      public ClassLoader getResourceClassLoader()
      {
         return delegate.getResourceClassLoader();
      }

      public void visit(DeploymentUnitVisitor visitor) throws DeploymentException
      {
         delegate.visit(visitor);
      }

      public MainDeployer getMainDeployer()
      {
         return delegate.getMainDeployer();
      }

      public Object getControllerContextName()
      {
         return delegate.getControllerContextName();
      }

      public DeploymentStage getRequiredStage()
      {
         return delegate.getRequiredStage();
      }

      public void setRequiredStage(DeploymentStage stage)
      {
         delegate.setRequiredStage(stage);
      }

      public DependencyInfo getDependencyInfo()
      {
         return delegate.getDependencyInfo();
      }

      public void addIDependOn(DependencyItem dependency)
      {
         delegate.addIDependOn(dependency);
      }

      public void removeIDependOn(DependencyItem dependency)
      {
         delegate.removeIDependOn(dependency);
      }

      public Set<Object> getControllerContextNames()
      {
         return delegate.getControllerContextNames();
      }

      public void addControllerContextName(Object name)
      {
         delegate.addControllerContextName(name);
      }

      public void removeControllerContextName(Object name)
      {
         delegate.removeControllerContextName(name);
      }

      public Object addAttachment(String name, Object attachment)
      {
         return delegate.addAttachment(name, attachment);
      }

      public <T> T addAttachment(String name, T attachment, Class<T> expectedType)
      {
         return delegate.addAttachment(name, attachment, expectedType);
      }

      public <T> T addAttachment(Class<T> type, T attachment)
      {
         return delegate.addAttachment(type, attachment);
      }

      public Object removeAttachment(String name)
      {
         return delegate.removeAttachment(name);
      }

      public <T> T removeAttachment(String name, Class<T> expectedType)
      {
         return delegate.removeAttachment(name, expectedType);
      }

      public <T> T removeAttachment(Class<T> type)
      {
         return delegate.removeAttachment(type);
      }

      public void setAttachments(Map<String, Object> map)
      {
         delegate.setAttachments(map);
      }

      public void clear()
      {
         delegate.clear();
      }

      public int getChangeCount()
      {
         return delegate.getChangeCount();
      }

      public void clearChangeCount()
      {
         delegate.clearChangeCount();
      }

      public Map<String, Object> getAttachments()
      {
         return delegate.getAttachments();
      }

      public Object getAttachment(String name)
      {
         return delegate.getAttachment(name);
      }

      public <T> T getAttachment(String name, Class<T> expectedType)
      {
         return delegate.getAttachment(name, expectedType);
      }

      public <T> T getAttachment(Class<T> type)
      {
         return delegate.getAttachment(type);
      }

      public boolean isAttachmentPresent(String name)
      {
         return delegate.isAttachmentPresent(name);
      }

      public boolean isAttachmentPresent(String name, Class<?> expectedType)
      {
         return delegate.isAttachmentPresent(name, expectedType);
      }

      public boolean isAttachmentPresent(Class<?> type)
      {
         return delegate.isAttachmentPresent(type);
      }

      public boolean hasAttachments()
      {
         return delegate.hasAttachments();
      }
   }

   private static class VFSDUDelegate extends DUDelegate implements VFSDeploymentUnit
   {
      private VFSDeploymentUnit delegate;

      private VFSDUDelegate(VFSDeploymentUnit delegate)
      {
         super(delegate);
         this.delegate = delegate;
      }

      public VFSDeploymentResourceLoader getResourceLoader()
      {
         return delegate.getResourceLoader();
      }

      public VFSDeploymentUnit getParent()
      {
         return delegate.getParent();
      }

      public VFSDeploymentUnit getTopLevel()
      {
         return delegate.getTopLevel();
      }

      public VirtualFile getMetaDataFile(String name)
      {
         return getMetaDataFile(name, MetaDataTypeFilter.ALL);
      }

      public VirtualFile getMetaDataFile(String name, MetaDataTypeFilter filter)
      {
         return delegate.getMetaDataFile(name, filter);
      }

      public List<VirtualFile> getMetaDataFiles(String name, String suffix)
      {
         return getMetaDataFiles(name, suffix, MetaDataTypeFilter.ALL);
      }

      public List<VirtualFile> getMetaDataFiles(String name, String suffix, MetaDataTypeFilter filter)
      {
         return delegate.getMetaDataFiles(name, suffix, filter);
      }

      public List<VirtualFile> getMetaDataFiles(VirtualFileFilter filter)
      {
         return getMetaDataFiles(filter, MetaDataTypeFilter.ALL);
      }

      public List<VirtualFile> getMetaDataFiles(VirtualFileFilter filter, MetaDataTypeFilter mdtf)
      {
         return delegate.getMetaDataFiles(filter, mdtf);
      }

      public void prependMetaDataLocation(VirtualFile... locations)
      {
         delegate.prependMetaDataLocation(locations);
      }

      public void appendMetaDataLocation(VirtualFile... locations)
      {
         delegate.appendMetaDataLocation(locations);
      }

      public void removeMetaDataLocation(VirtualFile... locations)
      {
         delegate.removeMetaDataLocation(locations);
      }

      public VirtualFile getFile(String path)
      {
         return delegate.getFile(path);
      }

      public VirtualFile getRoot()
      {
         return delegate.getRoot();
      }

      public List<VirtualFile> getClassPath()
      {
         return delegate.getClassPath();
      }

      public void setClassPath(List<VirtualFile> classPath)
      {
         delegate.setClassPath(classPath);
      }

      public void prependClassPath(VirtualFile... files)
      {
         delegate.prependClassPath(files);
      }

      public void prependClassPath(List<VirtualFile> files)
      {
         delegate.prependClassPath(files);
      }

      public void appendClassPath(VirtualFile... files)
      {
         delegate.appendClassPath(files);
      }

      public void appendClassPath(List<VirtualFile> files)
      {
         delegate.appendClassPath(files);
      }

      public void addClassPath(VirtualFile... files)
      {
         delegate.addClassPath(files);
      }

      public void addClassPath(List<VirtualFile> files)
      {
         delegate.addClassPath(files);
      }

      public void removeClassPath(VirtualFile... files)
      {
         delegate.removeClassPath(files);
      }

      public List<VFSDeploymentUnit> getVFSChildren()
      {
         return delegate.getVFSChildren();
      }
   }
}