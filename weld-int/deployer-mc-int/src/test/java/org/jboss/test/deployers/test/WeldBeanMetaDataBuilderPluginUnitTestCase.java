/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestSuite;

import org.jboss.beans.metadata.plugins.AbstractAnnotationMetaData;
import org.jboss.beans.metadata.plugins.AbstractBeanMetaData;
import org.jboss.beans.metadata.plugins.AbstractConstructorMetaData;
import org.jboss.beans.metadata.plugins.AbstractInstallMetaData;
import org.jboss.beans.metadata.plugins.AbstractParameterMetaData;
import org.jboss.beans.metadata.spi.AnnotationMetaData;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.InstallMetaData;
import org.jboss.beans.metadata.spi.ParameterMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.DependencyInfo;
import org.jboss.dependency.spi.DependencyItem;
import org.jboss.deployers.client.spi.main.MainDeployer;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.attachments.MutableAttachments;
import org.jboss.deployers.spi.deployer.DeploymentStage;
import org.jboss.deployers.structure.spi.ClassLoaderFactory;
import org.jboss.deployers.structure.spi.DeploymentResourceLoader;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.structure.spi.DeploymentUnitVisitor;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.plugins.bootstrap.basic.BasicBootstrap;
import org.jboss.kernel.weld.metadata.api.annotations.Weld;
import org.jboss.metadata.spi.MetaData;
import org.jboss.metadata.spi.MutableMetaData;
import org.jboss.metadata.spi.scope.ScopeKey;
import org.jboss.test.AbstractTestCaseWithSetup;
import org.jboss.test.AbstractTestDelegate;
import org.jboss.test.deployers.support.unit.McBeanWithConstructorAnnotation;
import org.jboss.test.deployers.support.unit.McBeanWithInstallAnnotation;
import org.jboss.test.deployers.support.unit.McBeanWithNoWeldAnnotations;
import org.jboss.test.deployers.support.unit.McBeanWithPropertyAnnotation;
import org.jboss.weld.integration.deployer.mc.WeldBeanMetaDataDeployerPlugin;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class WeldBeanMetaDataBuilderPluginUnitTestCase extends AbstractTestCaseWithSetup
{
   public WeldBeanMetaDataBuilderPluginUnitTestCase(String name)
   {
      super(name);
   }

   public static TestSuite suite()
   {
      return new TestSuite(WeldBeanMetaDataBuilderPluginUnitTestCase.class);
   }
   
   public static AbstractTestDelegate getDelegate(Class<?> clazz) throws Exception
   {
      return new WeldBeanMetaDataBuilderPluginTestDelegate(clazz);
   }
   
   public void testNonWeldMcBeanMinimalMetaData()
   {
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder("Test", McBeanWithNoWeldAnnotations.class.getName());
      checkMcBean(builder.getBeanMetaData(), false);
   }
   
   public void testNonWeldMcBeanMetaDataNoAnnotations()
   {
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder("Test", McBeanWithNoWeldAnnotations.class.getName());
      builder.addConstructorParameter(Object.class.getName(), "Test");
      builder.addInstall("install");
      builder.addPropertyMetaData("property", "Test");
      checkMcBean(builder.getBeanMetaData(), false);
   }
   
   public void testWeldMcBeanConstructorMetaDataAnnotation()
   {
      AbstractBeanMetaData bmd = new AbstractBeanMetaData("Test", McBeanWithNoWeldAnnotations.class.getName());
      AbstractConstructorMetaData cmd = new AbstractConstructorMetaData();
      AbstractParameterMetaData pmd = new AbstractParameterMetaData(Object.class.getName(), "Test");
      cmd.setParameters(Collections.singletonList((ParameterMetaData)pmd));
      cmd.setAnnotations(createWeldAnnotationMetaData());
      bmd.setConstructor(cmd);
      
      checkMcBean(bmd, true);
   }
   
   public void testWeldMcBeanInstallMetaDataAnnotation()
   {
      AbstractBeanMetaData bmd = new AbstractBeanMetaData("Test", McBeanWithNoWeldAnnotations.class.getName());
      AbstractInstallMetaData imd = new AbstractInstallMetaData();
      imd.setMethodName("install");
      imd.setAnnotations(createWeldAnnotationMetaData());
      bmd.setInstalls(Collections.singletonList((InstallMetaData)imd));
      
      checkMcBean(bmd, true);
   }
   
   public void testWeldMcPropertyMetaDataAnnotation()
   {
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder("Test", McBeanWithNoWeldAnnotations.class.getName());
      builder.addPropertyMetaData("property", "Test");
      builder.addPropertyAnnotation("property", "@" + Weld.class.getName());
      
      checkMcBean(builder.getBeanMetaData(), true);
   }
   
   public void testWeldConstructorSourceAnnotation()
   {
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder("Test", McBeanWithConstructorAnnotation.class.getName());
      checkMcBean(builder.getBeanMetaData(), true);
   }
   
   public void testWeldInstallSourceAnnotation()
   {
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder("Test", McBeanWithInstallAnnotation.class.getName());
      checkMcBean(builder.getBeanMetaData(), true);
   }
   
   public void testWeldPropertySourceAnnotation()
   {
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder("Test", McBeanWithPropertyAnnotation.class.getName());
      checkMcBean(builder.getBeanMetaData(), true);
   }
   
   private void checkMcBean(BeanMetaData bmd, boolean isWeld)
   {
      ControllerContext ctx = getPlugin().createContext(getController(), new MockDeploymentUnit(), bmd);
      if (isWeld) 
         assertNotNull(ctx);
      else
         assertNull(ctx);
   }
   
   private Set<AnnotationMetaData> createWeldAnnotationMetaData()
   {
      AbstractAnnotationMetaData amd = new AbstractAnnotationMetaData();
      amd.setAnnotation("@" + Weld.class.getName());
      return Collections.singleton((AnnotationMetaData)amd);
   }

   private WeldBeanMetaDataBuilderPluginTestDelegate getMyDelegate()
   {
      return (WeldBeanMetaDataBuilderPluginTestDelegate)getDelegate();
   }
   
   private Controller getController()
   {
      return getMyDelegate().kernel.getController();
   }
   
   private WeldBeanMetaDataDeployerPlugin getPlugin()
   {
      return getMyDelegate().plugin;
   }
   
   private static class WeldBeanMetaDataBuilderPluginTestDelegate extends AbstractTestDelegate
   {
      Kernel kernel;
      WeldBeanMetaDataDeployerPlugin plugin;
      
      public WeldBeanMetaDataBuilderPluginTestDelegate(Class<?> clazz)
      {
         super(clazz);
      }

      @Override
      public void setUp() throws Exception
      {
         super.setUp();
         BasicBootstrap bootstrap = new BasicBootstrap();
         bootstrap.run();
         kernel = bootstrap.getKernel();
         
         plugin = new WeldBeanMetaDataDeployerPlugin();
      }

      @Override
      public void tearDown() throws Exception
      {
         super.tearDown();
      }
   }

   private class MockDeploymentUnit implements DeploymentUnit
   {
      public DeploymentUnit addComponent(String name)
      {
         return null;
      }

      public void addControllerContextName(Object name)
      {
      }

      public void addIDependOn(DependencyItem dependency)
      {
      }

      public boolean createClassLoader(ClassLoaderFactory factory) throws DeploymentException
      {
         return false;
      }

      public <T> Set<? extends T> getAllMetaData(Class<T> type)
      {
         return null;
      }

      public List<DeploymentUnit> getChildren()
      {
         return null;
      }

      public ClassLoader getClassLoader()
      {
         return Thread.currentThread().getContextClassLoader();
      }

      public DeploymentUnit getComponent(String name)
      {
         return null;
      }

      public List<DeploymentUnit> getComponents()
      {
         return null;
      }

      public Object getControllerContextName()
      {
         return null;
      }

      public Set<Object> getControllerContextNames()
      {
         return null;
      }

      public DependencyInfo getDependencyInfo()
      {
         return null;
      }

      public MainDeployer getMainDeployer()
      {
         return null;
      }

      public MetaData getMetaData()
      {
         return null;
      }

      public MutableMetaData getMutableMetaData()
      {
         return null;
      }

      public ScopeKey getMutableScope()
      {
         return null;
      }

      public String getName()
      {
         return "Test";
      }

      public DeploymentUnit getParent()
      {
         return null;
      }

      public String getRelativePath()
      {
         return null;
      }

      public DeploymentStage getRequiredStage()
      {
         return null;
      }

      public ClassLoader getResourceClassLoader()
      {
         return null;
      }

      public DeploymentResourceLoader getResourceLoader()
      {
         return null;
      }

      public ScopeKey getScope()
      {
         return null;
      }

      public String getSimpleName()
      {
         return null;
      }

      public DeploymentUnit getTopLevel()
      {
         return this;
      }

      public MutableAttachments getTransientManagedObjects()
      {
         return null;
      }

      public boolean isComponent()
      {
         return false;
      }

      public boolean isTopLevel()
      {
         return false;
      }

      public void removeClassLoader(ClassLoaderFactory factory)
      {
      }

      public boolean removeComponent(String name)
      {
         return false;
      }

      public void removeControllerContextName(Object name)
      {
      }

      public void removeIDependOn(DependencyItem dependency)
      {
      }

      public void setMutableScope(ScopeKey key)
      {
      }

      public void setRequiredStage(DeploymentStage stage)
      {
      }

      public void setScope(ScopeKey key)
      {
      }

      public void visit(DeploymentUnitVisitor visitor) throws DeploymentException
      {
      }

      public Object addAttachment(String name, Object attachment)
      {
         return null;
      }

      public <T> T addAttachment(Class<T> type, T attachment)
      {
         return null;
      }

      public <T> T addAttachment(String name, T attachment, Class<T> expectedType)
      {
         return null;
      }

      public void clear()
      {
      }

      public void clearChangeCount()
      {
      }

      public int getChangeCount()
      {
         return 0;
      }

      public Object removeAttachment(String name)
      {
         return null;
      }

      public <T> T removeAttachment(Class<T> type)
      {
         return null;
      }

      public <T> T removeAttachment(String name, Class<T> expectedType)
      {
         return null;
      }

      public void setAttachments(Map<String, Object> map)
      {
      }

      public Object getAttachment(String name)
      {
         return null;
      }

      public <T> T getAttachment(Class<T> type)
      {
         return null;
      }

      public <T> T getAttachment(String name, Class<T> expectedType)
      {
         return null;
      }

      public Map<String, Object> getAttachments()
      {
         return null;
      }

      public boolean hasAttachments()
      {
         return false;
      }

      public boolean isAttachmentPresent(String name)
      {
         if (name.equals("Test_WeldBootstrapBean_BeanMetaData"))
            return true;
         return false;
      }

      public boolean isAttachmentPresent(Class<?> type)
      {
         return false;
      }

      public boolean isAttachmentPresent(String name, Class<?> expectedType)
      {
         return false;
      }
      
   }
   
}
