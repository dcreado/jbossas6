/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc. and individual contributors
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
package org.jboss.test.deployers;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.metadata.spi.MetaData;
import org.jboss.metadata.spi.MutableMetaData;
import org.jboss.metadata.spi.scope.ScopeKey;
import org.jboss.system.deployers.LegacyPrefixDeploymentContextComparator;
import org.jboss.test.JBossTestCase;

import org.jboss.dependency.spi.DependencyInfo;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.DeploymentState;
import org.jboss.deployers.spi.attachments.Attachments;
import org.jboss.deployers.spi.attachments.MutableAttachments;
import org.jboss.deployers.spi.deployer.DeploymentStage;
import org.jboss.deployers.structure.spi.ClassLoaderFactory;
import org.jboss.deployers.structure.spi.DeploymentContext;
import org.jboss.deployers.structure.spi.DeploymentContextVisitor;
import org.jboss.deployers.structure.spi.DeploymentResourceLoader;
import org.jboss.deployers.structure.spi.DeploymentUnit;

/**
 * Tests for the {@link org.jboss.system.deployers.LegacyPrefixDeploymentContextComparator}.
 * (Test for JBPAPP-3496.)
 *
 * @author <a href="mailto:miclark@redhat.com">Mike M. Clark</a>
 * 
 * @version $Revision: $
 */
public class LegacyPrefixDeploymentContextComparatorTestCase extends JBossTestCase
{
   private Comparator<DeploymentContext> comparator = null;
   
   public LegacyPrefixDeploymentContextComparatorTestCase(String name)
   {
      super(name);
   }
   
   public void setUp()
   {
      LegacyPrefixDeploymentContextComparator prefixComparator = new LegacyPrefixDeploymentContextComparator();
      prefixComparator.create();
      comparator = prefixComparator;
   }

   /**
    * Verifies leading zeros are ignored.
    */
   public void testLeadingZerosIgnored()
   {
      DeploymentContext first = new TestDeploymentContext("001test.ear");
      DeploymentContext second = new TestDeploymentContext("21test.ear");
      DeploymentContext third = new TestDeploymentContext("0132test.ear");
      
      
   }
   
   /**
    * Verifies straight-forward ordering.
    */
   public void testPrefixOrdering()
   {
      DeploymentContext first = new TestDeploymentContext("1test.ear");
      DeploymentContext second = new TestDeploymentContext("21test.ear");
      DeploymentContext third = new TestDeploymentContext("132test.ear");
      
      ValidateFirstSecondThird(first, second, third);
   }
   
   /**
    * Verifies that a prefix tie is ordered based on the suffix.
    */
   public void testSuffixFallback()
   {
      DeploymentContext first = new TestDeploymentContext("123test.sar");
      DeploymentContext second = new TestDeploymentContext("123test.ear");
      
      assertTrue("Second comes before first", comparator.compare(first, second) < 0);
      assertTrue("First comes after second", comparator.compare(second, first) > 0);
   }
   
   /**
    * Verifies suffix configuration.
    */
   public void testSuffixOrderChange()
   {
      Map<String, Integer> suffixOrder = new HashMap<String, Integer>();
      suffixOrder.put(".sar", 700);
      LegacyPrefixDeploymentContextComparator prefixComparator = new LegacyPrefixDeploymentContextComparator();
      prefixComparator.setSuffixOrder(suffixOrder);
      prefixComparator.create();
      
      Comparator<DeploymentContext> changedSuffix = prefixComparator;
      
      DeploymentContext first = new TestDeploymentContext("test.ear");
      DeploymentContext second = new TestDeploymentContext("test.sar");
      
      assertTrue("Second comes before first", changedSuffix.compare(first, second) < 0);
      assertTrue("First comes after second", changedSuffix.compare(second, first) > 0);
   }
   
   /**
    * Verifies order indicated in 
    * {@link org.jboss.system.deployers.LegacyPrefixDeploymentContextComparator}
    * javadoc.
    */
   public void testJavaDocExample()
   {
      DeploymentContext first = new TestDeploymentContext("test.sar");
      DeploymentContext second = new TestDeploymentContext("component.ear");
      DeploymentContext third = new TestDeploymentContext("001test.jar");
      DeploymentContext fourth = new TestDeploymentContext("5test.rar");
      DeploymentContext fifth = new TestDeploymentContext("5foo.jar");
      DeploymentContext sixth = new TestDeploymentContext("120bar.jar");
      
      assertTrue("Second comes before first", comparator.compare(first, second) < 0);
      assertTrue("Third comes before second", comparator.compare(second, third) < 0);
      assertTrue("Fourth comes before third", comparator.compare(third, fourth) < 0);
      assertTrue("Fifth comes before fourth", comparator.compare(fourth, fifth) < 0);
      assertTrue("Sixth comes before fifth", comparator.compare(fifth, sixth) < 0);
   }
   
   /**
    * Verifies non-prefixed deployments occur before prefixed deployments
    */
   public void testNonPrefixPrefixOrdering()
   {
      DeploymentContext first = new TestDeploymentContext("test.sar");
      DeploymentContext second = new TestDeploymentContext("test.ear");
      DeploymentContext third = new TestDeploymentContext("132test.ear");
      
      ValidateFirstSecondThird(first, second, third);
   }
   
   private void ValidateFirstSecondThird(DeploymentContext first, DeploymentContext second, DeploymentContext third) 
   {
      assertTrue("Second comes before first", comparator.compare(first, second) < 0);
      assertTrue("Third comes before first", comparator.compare(first, third) < 0);
      assertTrue("Third comes before second", comparator.compare(second, third) < 0);
      assertTrue("First comes after second", comparator.compare(second, first) > 0);
      assertTrue("First comes after third", comparator.compare(third, first) > 0);
      assertTrue("Second comes after third", comparator.compare(third, second) > 0);
      assertTrue("First does not tie itself", comparator.compare(first, first) == 0);
   }
   
   private class TestDeploymentContext implements DeploymentContext
   {
      private int relativeOrder = 0;
      private String simpleName = null;
      
      public TestDeploymentContext(String simpleName)
      {
         this.simpleName = simpleName;
      }
      
      public void addChild(DeploymentContext child)
      {
         throw new UnsupportedOperationException();
      }

      public void addComponent(DeploymentContext component)
      {
         throw new UnsupportedOperationException();
      }

      public void addControllerContextName(Object name)
      {
         throw new UnsupportedOperationException();
      }

      public void cleanup()
      {
         throw new UnsupportedOperationException();
      }

      public boolean createClassLoader(ClassLoaderFactory factory) throws DeploymentException
      {
         throw new UnsupportedOperationException();
      }

      public void deployed()
      {
         throw new UnsupportedOperationException();
      }

      public List<DeploymentContext> getChildren()
      {
         throw new UnsupportedOperationException();
      }

      public ClassLoader getClassLoader()
      {
         throw new UnsupportedOperationException();
      }

      public Comparator<DeploymentContext> getComparator()
      {
         throw new UnsupportedOperationException();
      }

      public List<DeploymentContext> getComponents()
      {
         throw new UnsupportedOperationException();
      }

      public Object getControllerContextName()
      {
         throw new UnsupportedOperationException();
      }

      public Set<Object> getControllerContextNames()
      {
         throw new UnsupportedOperationException();
      }

      public DependencyInfo getDependencyInfo()
      {
         throw new UnsupportedOperationException();
      }

      public Deployment getDeployment()
      {
         throw new UnsupportedOperationException();
      }

      public DeploymentUnit getDeploymentUnit()
      {
         throw new UnsupportedOperationException();
      }

      public MetaData getMetaData()
      {
         throw new UnsupportedOperationException();
      }

      public MutableMetaData getMutableMetaData()
      {
         throw new UnsupportedOperationException();
      }

      public ScopeKey getMutableScope()
      {
         throw new UnsupportedOperationException();
      }

      public String getName()
      {
         throw new UnsupportedOperationException();
      }

      public DeploymentContext getParent()
      {
         throw new UnsupportedOperationException();
      }

      public Throwable getProblem()
      {
         throw new UnsupportedOperationException();
      }

      public int getRelativeOrder()
      {
         return relativeOrder;
      }

      public String getRelativePath()
      {
         throw new UnsupportedOperationException();
      }

      public DeploymentStage getRequiredStage()
      {
         throw new UnsupportedOperationException();
      }

      public ClassLoader getResourceClassLoader()
      {
         throw new UnsupportedOperationException();
      }

      public DeploymentResourceLoader getResourceLoader()
      {
         throw new UnsupportedOperationException();
      }

      public ScopeKey getScope()
      {
         throw new UnsupportedOperationException();
      }

      public String getSimpleName()
      {
         return simpleName;
      }

      public DeploymentState getState()
      {
         throw new UnsupportedOperationException();
      }

      public DeploymentContext getTopLevel()
      {
         throw new UnsupportedOperationException();
      }

      public boolean isComponent()
      {
         throw new UnsupportedOperationException();
      }

      public boolean isDeployed()
      {
         throw new UnsupportedOperationException();
      }

      public boolean isTopLevel()
      {
         throw new UnsupportedOperationException();
      }

      public boolean removeChild(DeploymentContext child)
      {
         throw new UnsupportedOperationException();
      }

      public void removeClassLoader()
      {
         throw new UnsupportedOperationException();
      }

      public void removeClassLoader(ClassLoaderFactory factory)
      {
         throw new UnsupportedOperationException();
      }

      public boolean removeComponent(DeploymentContext component)
      {
         throw new UnsupportedOperationException();
      }

      public void removeControllerContextName(Object name)
      {
         throw new UnsupportedOperationException();
      }

      public void setClassLoader(ClassLoader classLoader)
      {
         throw new UnsupportedOperationException();
      }

      public void setComparator(Comparator<DeploymentContext> comparator)
      {
         throw new UnsupportedOperationException();
      }

      public void setDeployment(Deployment deployment)
      {
         throw new UnsupportedOperationException();
      }

      public void setDeploymentUnit(DeploymentUnit unit)
      {
         throw new UnsupportedOperationException();
      }

      public void setMutableScope(ScopeKey key)
      {
         throw new UnsupportedOperationException();
      }

      public void setParent(DeploymentContext parent)
      {
         throw new UnsupportedOperationException();
      }

      public void setProblem(Throwable problem)
      {
         throw new UnsupportedOperationException();
      }

      public void setRelativeOrder(int relativeOrder)
      {
         this.relativeOrder = relativeOrder;
      }

      public void setRequiredStage(DeploymentStage stage)
      {
         throw new UnsupportedOperationException();
      }

      public void setScope(ScopeKey key)
      {
         throw new UnsupportedOperationException();
      }

      public void setState(DeploymentState state)
      {
         throw new UnsupportedOperationException();
      }

      public void visit(DeploymentContextVisitor visitor) throws DeploymentException
      {
         throw new UnsupportedOperationException();
      }

      public MutableAttachments getTransientAttachments()
      {
         throw new UnsupportedOperationException();
      }

      public MutableAttachments getTransientManagedObjects()
      {
         throw new UnsupportedOperationException();
      }

      public Attachments getPredeterminedManagedObjects()
      {
         throw new UnsupportedOperationException();
      }

      public void setPredeterminedManagedObjects(Attachments predetermined)
      {
         throw new UnsupportedOperationException();
      }
   }
   
}
