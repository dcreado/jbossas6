///*
// * JBoss, Home of Professional Open Source.
// * Copyright 2008, Red Hat Middleware LLC, and individual contributors
// * as indicated by the @author tags. See the copyright.txt file in the
// * distribution for a full listing of individual contributors.
// *
// * This is free software; you can redistribute it and/or modify it
// * under the terms of the GNU Lesser General Public License as
// * published by the Free Software Foundation; either version 2.1 of
// * the License, or (at your option) any later version.
// *
// * This software is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// * Lesser General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public
// * License along with this software; if not, write to the Free
// * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
// * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
// */
//package org.jboss.test.deployers.test;
//
//import java.net.URL;
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.Iterator;
//
//import org.jboss.shrinkwrap.api.ShrinkWrap;
//import org.jboss.shrinkwrap.api.spec.JavaArchive;
//import org.jboss.test.deployers.support.CheckableBootstrap;
//import org.jboss.test.deployers.support.MockEjbServices;
//import org.jboss.test.deployers.support.MockEmptyEjbServices;
//import org.jboss.test.deployers.support.MockTransactionServices;
//import org.jboss.test.deployers.support.MockWeldBootstrap;
//import org.jboss.test.deployers.support.WeldDEWrapper;
//import org.jboss.test.deployers.support.crm.CrmWebBean;
//import org.jboss.test.deployers.support.ejb.BusinessInterface;
//import org.jboss.test.deployers.support.ejb.MySLSBean;
//import org.jboss.test.deployers.support.ext.ExternalWebBean;
//import org.jboss.test.deployers.support.jar.PlainJavaBean;
//import org.jboss.test.deployers.support.jsf.NotWBJsfBean;
//import org.jboss.test.deployers.support.ui.UIWebBean;
//import org.jboss.test.deployers.support.util.SomeUtil;
//import org.jboss.test.deployers.support.web.ServletWebBean;
//import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
//import org.jboss.weld.bootstrap.spi.Deployment;
//
///**
// * JBossDeployment environment test case.
// *
// * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
// * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
// */
//public class DeploymentEnvironmentTestCase extends AbstractSingleArchiveTest<BeanDeploymentArchive>
//{
//   public DeploymentEnvironmentTestCase(String name)
//   {
//      super(name);
//   }
//
//   public static Test suite()
//   {
//      return suite(DeploymentEnvironmentTestCase.class);
//   }
//
//   @Override
//   public void setUp() throws Exception
//   {
//      super.setUp();
//      JavaArchive mockJar = ShrinkWrap.create(JavaArchive.class, "mock.jar");
//      mockJar.addClass(CheckableBootstrap.class);
//      mockJar.addClass(MockEjbServices.class);
//      mockJar.addClass(MockEmptyEjbServices.class);
//      mockJar.addClass(MockTransactionServices.class);
//      mockJar.addClass(MockWeldBootstrap.class);
//      mockJar.addClass(WeldDEWrapper.class);
//      assertDeploy(mockJar);
//   }
//
//   @Override
//   protected void assertWarsInEar()
//   {
//      Deployment deployment = (Deployment) getBean(Deployment.class);
//      Collection<BeanDeploymentArchive> bdas = getBDAs(deployment);
//      assertEquals(2, bdas.size());
//      Iterator<BeanDeploymentArchive> iterator = bdas.iterator();
//      BeanDeploymentArchive bda = iterator.next();
//      BeanDeploymentArchive bda1, bda2;
//      if (bda.getId().contains("simple1.war"))
//      {
//         bda1 = bda;
//         bda2 = iterator.next();
//      }
//      else
//      {
//         bda2 = bda;
//         bda1 = iterator.next();
//      }
//      assertBDAId(bda1, "warinear.ear/simple1.war");
//      assertExpectedClasses(bda1, ServletWebBean.class);
//      assertExpectedWarResources(bda1, "simple1.war", true);
//
//      assertBDAId(bda2, "warinear.ear/simple2.war");
//      assertExpectedClasses(bda2, NotWBJsfBean.class);
//      assertExpectedWarResources(bda2, "simple2.war", true);
//
//      Collection<BeanDeploymentArchive> reachableBDAs = getReachableBDAs(bda1);
//      assertTrue(reachableBDAs.isEmpty());
//      reachableBDAs = getReachableBDAs(bda2);
//      assertTrue(reachableBDAs.isEmpty());
//   }
//
//   @Override
//   protected void assertBasicEar()
//   {
//      Deployment deployment = (Deployment) getBean(Deployment.class);
//      Collection<BeanDeploymentArchive> bdas = getBDAs(deployment);
//      assertEquals(3, bdas.size());
//      BeanDeploymentArchive earBDA = null, simpleWarBDA = null, crmWarBDA = null;
//      for (BeanDeploymentArchive bda: bdas)
//      {
//         if (bda.getId().contains("simple.war"))
//         {
//            simpleWarBDA = bda;
//            assertBDAId(simpleWarBDA, "top-level.ear/simple.war");
//            assertExpectedClasses(simpleWarBDA, UIWebBean.class, ServletWebBean.class);
//            assertExpectedWarResources(simpleWarBDA, "top-level.ear/simple.war", true, "ui.jar");
//         }
//         else if (bda.getId().contains("crm.war"))
//         {
//            crmWarBDA = bda;
//            assertBDAId(crmWarBDA, "top-level.ear/crm.war");
//            assertExpectedClasses(crmWarBDA, CrmWebBean.class);
//            assertExpectedResources(crmWarBDA, "crm.jar");
//         }
//         else
//         {
//            earBDA = bda;
//            assertBDAId(earBDA, "top-level.ear");
//            assertExpectedClasses(earBDA, BusinessInterface.class, MySLSBean.class,
//                     ExternalWebBean.class, PlainJavaBean.class);
//            assertExpectedResources(earBDA, "top-level.ear/ejbs.jar",
//                     "top-level.ear/lib/ext.jar", "top-level.ear/simple.jar");
//         }
//      }
//      Collection<BeanDeploymentArchive> reachableBDAs = getReachableBDAs(earBDA);
//      assertTrue(reachableBDAs.isEmpty());
//      reachableBDAs = getReachableBDAs(simpleWarBDA);
//      assertEquals(1, reachableBDAs.size());
//      assertSame(earBDA, reachableBDAs.iterator().next());
//   }
//
//   @Override
//   protected void assertBasicEarFullCDI()
//   {
//      Deployment deployment = (Deployment) getBean(Deployment.class);
//      Collection<BeanDeploymentArchive> bdas = getBDAs(deployment);
//      assertEquals(3, bdas.size());
//      BeanDeploymentArchive earBDA = null, simpleWarBDA = null, crmWarBDA = null;
//      for (BeanDeploymentArchive bda: bdas)
//      {
//         if (bda.getId().contains("simple.war"))
//         {
//            simpleWarBDA = bda;
//            assertBDAId(simpleWarBDA, "top-level.ear/simple.war");
//            assertExpectedClasses(simpleWarBDA, UIWebBean.class, ServletWebBean.class);
//            assertExpectedWarResources(simpleWarBDA, "top-level.ear/simple.war", true, "ui.jar");
//         }
//         else if (bda.getId().contains("crm.war"))
//         {
//            crmWarBDA = bda;
//            assertBDAId(crmWarBDA, "top-level.ear/crm.war");
//            assertExpectedClasses(crmWarBDA, CrmWebBean.class, NotWBJsfBean.class);
//            assertExpectedWarResources(crmWarBDA, "top-level.ear/crm.war", true, "crm.jar");
//         }
//         else
//         {
//            earBDA = bda;
//            assertBDAId(earBDA, "top-level.ear");
//            assertExpectedClasses(earBDA, BusinessInterface.class, MySLSBean.class,
//                     ExternalWebBean.class, PlainJavaBean.class, SomeUtil.class);
//            assertExpectedResources(earBDA, "top-level.ear/lib/util.jar",
//                     "top-level.ear/lib/ext.jar", "top-level.ear/simple.jar",
//                     "top-level.ear/ejbs.jar");
//         }
//      }
//
//      Collection<BeanDeploymentArchive> reachableBDAs = getReachableBDAs(earBDA);
//      assertTrue(reachableBDAs.isEmpty());
//      reachableBDAs = getReachableBDAs(simpleWarBDA);
//      assertEquals(1, reachableBDAs.size());
//      assertSame(earBDA, reachableBDAs.iterator().next());
//   }
//
//   @Override
//   protected void assertBasicEarWithoutXml()
//   {
//      assertEmptyEnvironment();
//   }
//
//   @Override
//   protected void assertEmptyEnvironment()
//   {
//      assertNoBean(Deployment.class);
//   }
//
//   @Override
//   protected BeanDeploymentArchive assertSingleEnvironment(String name)
//   {
//      Deployment deployment = (Deployment) getBean(Deployment.class);
//      return assertOneBDA(deployment, name);
//   }
//
//   @Override
//   protected Collection<Class<?>> getClasses(BeanDeploymentArchive bda)
//   {
//      return bda.getBeanClasses();
//   }
//
//   @Override
//   protected Collection<URL> getResources(BeanDeploymentArchive bda)
//   {
//      //return bda.getBeansXml();
//      // TODO Fix this
//      return null;
//   }
//
//   private BeanDeploymentArchive assertOneBDA(Deployment deployment, String name)
//   {
//      Collection<BeanDeploymentArchive> bdas = getBDAs(deployment);
//      assertEquals(1, bdas.size());
//      BeanDeploymentArchive bda = bdas.iterator().next();
//      assertBDAId(bda, name);
//      assertTrue(getReachableBDAs(bda).isEmpty());
//      return bda;
//   }
//
//   private Collection<BeanDeploymentArchive> getBDAs(Deployment deployment)
//   {
//      assertNotNull(deployment);
//      Collection<BeanDeploymentArchive> bdas = deployment.getBeanDeploymentArchives();
//      assertNotNull(bdas);
//      return bdas;
//   }
//
//   private void assertBDAId(BeanDeploymentArchive bda, String name)
//   {
//      assertTrue("BDA id \""  + bda.getId() + "\" expected to end with suffix \"" + name + "/}\"",
//               bda.getId().endsWith(name + "/}"));
//   }
//
//   private Collection<BeanDeploymentArchive> getReachableBDAs(BeanDeploymentArchive bda)
//   {
//      Collection<BeanDeploymentArchive> result = new HashSet<BeanDeploymentArchive>();
//      getReachableBDAs(bda, result);
//      result.remove(bda);
//      return result;
//   }
//
//   private void getReachableBDAs(BeanDeploymentArchive bda, Collection<BeanDeploymentArchive> result)
//   {
//      if (result.contains(bda))
//      {
//         return;
//      }
//      result.add(bda);
//      for (BeanDeploymentArchive reachableBDA: bda.getBeanDeploymentArchives())
//      {
//         getReachableBDAs(reachableBDA, result);
//      }
//   }
//}
