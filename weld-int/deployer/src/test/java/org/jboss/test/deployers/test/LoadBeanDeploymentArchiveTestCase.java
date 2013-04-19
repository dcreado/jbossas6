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
package org.jboss.test.deployers.test;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.Test;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.deployers.support.CheckableBootstrap;
import org.jboss.test.deployers.support.MockArchiveManifest;
import org.jboss.test.deployers.support.MockEjbInjectionServices;
import org.jboss.test.deployers.support.MockEjbServices;
import org.jboss.test.deployers.support.MockEmptyEjbServices;
import org.jboss.test.deployers.support.MockTransactionServices;
import org.jboss.test.deployers.support.MockWeldBootstrap;
import org.jboss.test.deployers.support.WeldDEWrapper;
import org.jboss.test.deployers.support.crm.CrmWebBean;
import org.jboss.test.deployers.support.ejb.BusinessInterface;
import org.jboss.test.deployers.support.ejb.MySLSBean;
import org.jboss.test.deployers.support.ext.ExternalWebBean;
import org.jboss.test.deployers.support.jar.PlainJavaBean;
import org.jboss.test.deployers.support.jsf.NotWBJsfBean;
import org.jboss.test.deployers.support.ui.UIWebBean;
import org.jboss.test.deployers.support.util.SomeUtil;
import org.jboss.test.deployers.support.web.ServletWebBean;
import org.jboss.test.deployers.vfs.classloader.support.a.A;
import org.jboss.test.deployers.vfs.classloader.support.b.B;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.deployer.env.bda.DeploymentImpl;
import org.junit.Ignore;

/**
 * Deployment.loadBeanDeploymentArchive test case.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
@Ignore
public class LoadBeanDeploymentArchiveTestCase extends AbstractEnvironmentTest<BeanDeploymentArchive>
{
   private Bootstrap bootstrap = new MockWeldBootstrap();
   
   public LoadBeanDeploymentArchiveTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(LoadBeanDeploymentArchiveTestCase.class);
   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      JavaArchive mockJar = ShrinkWrap.create(JavaArchive.class, "mock.jar");
      mockJar.addClass(CheckableBootstrap.class);
      mockJar.addClass(MockEjbServices.class);
      mockJar.addClass(MockEmptyEjbServices.class);
      mockJar.addClass(MockEjbInjectionServices.class);
      mockJar.addClass(MockTransactionServices.class);
      mockJar.addClass(MockWeldBootstrap.class);
      mockJar.addClass(WeldDEWrapper.class);
      assertDeploy(mockJar);
   }

   public void testEjbJars() throws Exception
   {
      // ejb1.jar
      JavaArchive ejbJar1 = createEjbJar("ejb1.jar", true, PlainJavaBean.class);
      DeploymentUnit unit = assertDeploy(ejbJar1);
      Class<?> plainJavaBeanClass = getClass(PlainJavaBean.class, unit);
      Deployment deployment1 = initializeDeploymentBean(unit);
      
      // ejb2.jar
      JavaArchive ejbJar2 = createEjbJar("ejb2.jar", true,  MySLSBean.class, BusinessInterface.class);
      unit = assertDeploy(ejbJar2);
      Class<?> mySLSBeanClass = getClass(MySLSBean.class, unit);
      Class<?> businessInterface = getClass(BusinessInterface.class, unit);
      Deployment deployment2 =  initializeDeploymentBean(unit);
      
      assertNotSame(deployment1, deployment2);
      
      BeanDeploymentArchive bda1 = deployment1.getBeanDeploymentArchives().iterator().next();
      BeanDeploymentArchive bda2 = deployment2.getBeanDeploymentArchives().iterator().next();
      // double invocation should yield the same result
      assertSame(bda1, deployment1.loadBeanDeploymentArchive(plainJavaBeanClass));
      assertSame(bda1, deployment1.loadBeanDeploymentArchive(plainJavaBeanClass));
      
      assertSame(bda2, deployment1.loadBeanDeploymentArchive(mySLSBeanClass));
      assertSame(bda2, deployment1.loadBeanDeploymentArchive(businessInterface));
      assertSame(bda2, deployment1.loadBeanDeploymentArchive(mySLSBeanClass));
      assertSame(bda2, deployment1.loadBeanDeploymentArchive(businessInterface));
      
      assertSame(bda1, deployment2.loadBeanDeploymentArchive(plainJavaBeanClass));
      assertSame(bda1, deployment2.loadBeanDeploymentArchive(plainJavaBeanClass));
      assertSame(bda2, deployment2.loadBeanDeploymentArchive(mySLSBeanClass));
      assertSame(bda2, deployment2.loadBeanDeploymentArchive(mySLSBeanClass));
      assertSame(bda2, deployment2.loadBeanDeploymentArchive(businessInterface));
      assertSame(bda2, deployment2.loadBeanDeploymentArchive(businessInterface));
   }

   public void testMixedEjbJars() throws Exception
   {
      // ejb1.jar
      JavaArchive ejbJar1 = createEjbJar("ejb1.jar", true, PlainJavaBean.class);
      DeploymentUnit unit = assertDeploy(ejbJar1);
      Class<?> plainJavaBeanClass = getClass(PlainJavaBean.class, unit);
      Deployment deployment1 =  initializeDeploymentBean(unit);
      // ejb2.jar
      JavaArchive ejbJar2 = createEjbJar("ejb2.jar", false,  MySLSBean.class, BusinessInterface.class);
      unit = assertDeploy(ejbJar2);
      ClassLoader classLoader2 = unit.getClassLoader();
      Class<?> mySLSBeanClass = classLoader2.loadClass(MySLSBean.class.getName());
      Class<?> businessInterface = classLoader2.loadClass(BusinessInterface.class.getName());
      
      BeanDeploymentArchive bda1 = deployment1.getBeanDeploymentArchives().iterator().next();
      assertSame(bda1, deployment1.loadBeanDeploymentArchive(plainJavaBeanClass));
      // creation of bda2 on demand
      BeanDeploymentArchive bda2 = deployment1.loadBeanDeploymentArchive(mySLSBeanClass);
      assertBDAId(bda2, "ejb2.jar");
      assertExpectedClasses(bda2, MySLSBean.class);
      assertNoBeansXml(bda2);
      // double invocation
      assertSame(bda2, deployment1.loadBeanDeploymentArchive(mySLSBeanClass));
      assertBDAId(bda2, "ejb2.jar");
      assertExpectedClasses(bda2, MySLSBean.class);
      assertNoBeansXml(bda2);
      // inclusion of BusinessInterface
      assertSame(bda2, deployment1.loadBeanDeploymentArchive(businessInterface));
      assertBDAId(bda2, "ejb2.jar");
      assertExpectedClasses(bda2, MySLSBean.class, BusinessInterface.class);
      assertNoBeansXml(bda2);
      // double invocation
      assertSame(bda2, deployment1.loadBeanDeploymentArchive(businessInterface));
      assertBDAId(bda2, "ejb2.jar");
      assertExpectedClasses(bda2, MySLSBean.class, BusinessInterface.class);
      assertNoBeansXml(bda2);
   }

   public void testEjbJarsInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME);
      JavaArchive ejbJar1 = createEjbJar("ejbJar1.jar", true, PlainJavaBean.class);
      ear.addModule(ejbJar1);
      JavaArchive ejbJar2 = createEjbJar("ejbJar2.jar", true, MySLSBean.class, BusinessInterface.class);
      ear.addModule(ejbJar2);
      MockArchiveManifest.addManifest(ear);
      DeploymentUnit unit = assertDeploy(ear);
      Class<?> plainJavaBeanClass = getClass(PlainJavaBean.class, unit);
      Class<?> mySLSBeanClass = getClass(MySLSBean.class, unit);
      Class<?> businessInterface = getClass(BusinessInterface.class, unit);
      
      Deployment deployment =  initializeDeploymentBean();
      BeanDeploymentArchive bda = deployment.loadBeanDeploymentArchive(plainJavaBeanClass);
      assertBDAId(bda, EAR_NAME);
      assertExpectedClasses(bda, PlainJavaBean.class, MySLSBean.class, BusinessInterface.class);
      assertSame(bda, deployment.loadBeanDeploymentArchive(mySLSBeanClass));
      assertSame(bda, deployment.loadBeanDeploymentArchive(businessInterface));
   }
   
   
   public void testMixedEjbJarsInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME);
      JavaArchive ejbJar1 = createEjbJar("ejbJar1.jar", true, PlainJavaBean.class);
      ear.addModule(ejbJar1);
      JavaArchive ejbJar2 = createEjbJar("ejbJar2.jar", false, MySLSBean.class, BusinessInterface.class);
      ear.addModule(ejbJar2);
      MockArchiveManifest.addManifest(ear);
      DeploymentUnit unit = assertDeploy(ear);
      Class<?> plainJavaBeanClass = getClass(PlainJavaBean.class, unit);
      Class<?> mySLSBeanClass = getClass(MySLSBean.class, unit);
      Class<?> businessInterface = getClass(BusinessInterface.class, unit);
      
      Deployment deployment = initializeDeploymentBean();
      BeanDeploymentArchive bda = deployment.loadBeanDeploymentArchive(plainJavaBeanClass);
      assertBDAId(bda, EAR_NAME);
      assertExpectedClasses(bda, PlainJavaBean.class);
      assertSame(bda, deployment.loadBeanDeploymentArchive(mySLSBeanClass));
      assertBDAId(bda, EAR_NAME);
      assertExpectedClasses(bda, PlainJavaBean.class, MySLSBean.class);
      // make sure double invocation does not affect the bda contents
      assertSame(bda, deployment.loadBeanDeploymentArchive(mySLSBeanClass));
      assertBDAId(bda, EAR_NAME);
      assertExpectedClasses(bda, PlainJavaBean.class, MySLSBean.class);
      assertSame(bda, deployment.loadBeanDeploymentArchive(businessInterface));
      assertSame(bda, deployment.loadBeanDeploymentArchive(mySLSBeanClass));
      assertBDAId(bda, EAR_NAME);
      assertExpectedClasses(bda, PlainJavaBean.class, MySLSBean.class, BusinessInterface.class);
   }
   
   public void testEjbJarsInEars() throws Exception
   {
      // simple1.ear
      EnterpriseArchive ear1 = ShrinkWrap.create(EnterpriseArchive.class, "simple1.ear");
      JavaArchive ejbJar1 = createEjbJar("ejbJar1.jar", true, PlainJavaBean.class);
      ear1.addModule(ejbJar1);
      MockArchiveManifest.addManifest(ear1);
      DeploymentUnit unit = assertDeploy(ear1);
      Class<?> plainJavaBeanClass = getClass(PlainJavaBean.class, unit);
      Deployment deployment1 = initializeDeploymentBean(unit);
      // simple2.ear
      EnterpriseArchive ear2 = ShrinkWrap.create(EnterpriseArchive.class, "simple2.ear");
      JavaArchive ejbJar2 = createEjbJar("ejbJar2.jar", true, MySLSBean.class, BusinessInterface.class);
      ear2.addModule(ejbJar2);
      MockArchiveManifest.addManifest(ear2);
      unit = assertDeploy(ear2);
      Class<?> mySLSBeanClass = getClass(MySLSBean.class, unit);
      Class<?> businessInterface = getClass(BusinessInterface.class, unit);
      Deployment deployment2 = initializeDeploymentBean(unit);
      
      BeanDeploymentArchive bda1 = deployment1.getBeanDeploymentArchives().iterator().next();
      BeanDeploymentArchive bda2 = deployment2.getBeanDeploymentArchives().iterator().next();
      // double invocation should yield the same result
      assertSame(bda1, deployment1.loadBeanDeploymentArchive(plainJavaBeanClass));
      assertSame(bda1, deployment1.loadBeanDeploymentArchive(plainJavaBeanClass));
      
      assertSame(bda2, deployment1.loadBeanDeploymentArchive(mySLSBeanClass));
      assertSame(bda2, deployment1.loadBeanDeploymentArchive(businessInterface));
      assertSame(bda2, deployment1.loadBeanDeploymentArchive(mySLSBeanClass));
      assertSame(bda2, deployment1.loadBeanDeploymentArchive(businessInterface));
      
      assertSame(bda1, deployment2.loadBeanDeploymentArchive(plainJavaBeanClass));
      assertSame(bda1, deployment2.loadBeanDeploymentArchive(plainJavaBeanClass));
      assertSame(bda2, deployment2.loadBeanDeploymentArchive(mySLSBeanClass));
      assertSame(bda2, deployment2.loadBeanDeploymentArchive(mySLSBeanClass));
      assertSame(bda2, deployment2.loadBeanDeploymentArchive(businessInterface));
      assertSame(bda2, deployment2.loadBeanDeploymentArchive(businessInterface));
   }
   
   
   public void testMixedEjbJarsInEars() throws Exception
   {
      // simple1.ear
      EnterpriseArchive ear1 = ShrinkWrap.create(EnterpriseArchive.class, "simple1.ear");
      JavaArchive ejbJar1 = createEjbJar("ejbJar1.jar", false, PlainJavaBean.class);
      ear1.addModule(ejbJar1);
      MockArchiveManifest.addManifest(ear1);
      DeploymentUnit unit = assertDeploy(ear1);
      Class<?> plainJavaBeanClass = getClass(PlainJavaBean.class, unit);

      // simple2.ear
      EnterpriseArchive ear2 = ShrinkWrap.create(EnterpriseArchive.class, "simple2.ear");
      JavaArchive ejbJar2 = createEjbJar("ejbJar2.jar", true, MySLSBean.class, BusinessInterface.class);
      ear2.addModule(ejbJar2);
      MockArchiveManifest.addManifest(ear2);
      unit = assertDeploy(ear2);
      Class<?> mySLSBeanClass = getClass(MySLSBean.class, unit);
      Class<?> businessInterface = getClass(BusinessInterface.class, unit);
      Deployment deployment2 = initializeDeploymentBean(unit);
      
      BeanDeploymentArchive bda2 = deployment2.getBeanDeploymentArchives().iterator().next();
      // contents of BDA2
      assertBDAId(bda2, "simple2.ear");
      assertExpectedClasses(bda2, MySLSBean.class, BusinessInterface.class);
      // call loadBDA
      assertSame(bda2, deployment2.loadBeanDeploymentArchive(mySLSBeanClass));
      assertSame(bda2, deployment2.loadBeanDeploymentArchive(businessInterface));
      // make sure that loadBDA did not change the contents of BDA2
      assertBDAId(bda2, "simple2.ear");
      assertExpectedClasses(bda2, MySLSBean.class, BusinessInterface.class);
      
      // creation of bda1 on demand
      BeanDeploymentArchive bda1 = deployment2.loadBeanDeploymentArchive(plainJavaBeanClass);
      assertBDAId(bda1, "simple1.ear");
      assertExpectedClasses(bda1, PlainJavaBean.class);
      assertNoBeansXml(bda1);
      // double invocation
      assertSame(bda1, deployment2.loadBeanDeploymentArchive(plainJavaBeanClass));
      assertBDAId(bda1, "simple1.ear");
      assertExpectedClasses(bda1, PlainJavaBean.class);
      assertNoBeansXml(bda1);
   }
   
   public void testWars() throws Exception
   {
      WebArchive war1 = createWar("simple1.war", true, ServletWebBean.class);
      DeploymentUnit unit = assertDeploy(war1);
      Class<?> servletWebBeanClass = getClass(ServletWebBean.class, unit);
      Deployment deployment1 = initializeDeploymentBean(unit);

      WebArchive war2 = createWar("simple2.war", true, NotWBJsfBean.class);
      unit = assertDeploy(war2);
      Class<?> notWBJsfBeanClass = getClass(NotWBJsfBean.class, unit);
      Deployment deployment2 = initializeDeploymentBean(unit);

      // assertion deleted as loadBDA implementation does not check for unreachable classes anymore
      //assertCannotLoadBDA(deployment1, notWBJsfBeanClass);
      //assertCannotLoadBDA(deployment2, servletWebBeanClass);
      
      BeanDeploymentArchive bda1 = deployment1.getBeanDeploymentArchives().iterator().next();
      assertBDAId(bda1, "simple1.war");
      assertExpectedClasses(bda1, ServletWebBean.class);
      assertSame(bda1, deployment1.loadBeanDeploymentArchive(servletWebBeanClass));
      // make sure loadBDA didn't change the bda structure
      assertBDAId(bda1, "simple1.war");
      assertExpectedClasses(bda1, ServletWebBean.class);
      
      BeanDeploymentArchive bda2 = deployment2.getBeanDeploymentArchives().iterator().next();
      assertBDAId(bda2, "simple2.war");
      assertExpectedClasses(bda2, NotWBJsfBean.class);
      assertSame(bda2, deployment2.loadBeanDeploymentArchive(notWBJsfBeanClass));
      // make sure loadBDA didn't change the bda structure
      assertBDAId(bda2, "simple2.war");
      assertExpectedClasses(bda2, NotWBJsfBean.class);
   }
   
   public void testMixedWars() throws Exception
   {
      WebArchive war1 = createWar("simple1.war", true, ServletWebBean.class);
      /*DeploymentUnit unit = */assertDeploy(war1);
      //Deployment deployment1 = initializeDeploymentBean(unit));

      WebArchive war2 = createWar("simple2.war", false, NotWBJsfBean.class);
      /*unit = */assertDeploy(war2);
      
      // assertion deleted as loadBDA implementation does not check for unreachable classes anymore
      // Class<?> notWBJsfBeanClass = getClass(NotWBJsfBean.class, unit);
      // assertCannotLoadBDA(deployment1, notWBJsfBeanClass);
   }
   
   public void testWarWithLib() throws Exception
   {
      WebArchive war = createWarWithLib(true, true);
      DeploymentUnit unit = assertDeploy(war);
      Class<?> servletWebBeanClass = getClass(ServletWebBean.class, unit);
      Class<?> uiWebBeanClass = getClass(UIWebBean.class, unit);
      Deployment deployment = initializeDeploymentBean();
      
      BeanDeploymentArchive bda = deployment.getBeanDeploymentArchives().iterator().next();
      assertBDAId(bda, WAR_NAME);
      assertExpectedClasses(bda, ServletWebBean.class, UIWebBean.class);
      assertSame(bda, deployment.loadBeanDeploymentArchive(servletWebBeanClass));
      assertSame(bda, deployment.loadBeanDeploymentArchive(uiWebBeanClass));
      // make sure bda is unchanged
      assertBDAId(bda, WAR_NAME);
      assertExpectedClasses(bda, ServletWebBean.class, UIWebBean.class);
   }
   
   public void testWarWithLibWithoutXml() throws Exception
   {
      WebArchive war = createWarWithLib(true, false);
      DeploymentUnit unit = assertDeploy(war);
      Class<?> servletWebBeanClass = getClass(ServletWebBean.class, unit);
      Class<?> uiWebBeanClass = getClass(UIWebBean.class, unit);
      Deployment deployment = initializeDeploymentBean();
      
      BeanDeploymentArchive bda = deployment.getBeanDeploymentArchives().iterator().next();
      assertBDAId(bda, WAR_NAME);
      assertExpectedClasses(bda, ServletWebBean.class);
      assertSame(bda, deployment.loadBeanDeploymentArchive(servletWebBeanClass));
      assertBDAId(bda, WAR_NAME);
      assertExpectedClasses(bda, ServletWebBean.class);
      assertSame(bda, deployment.loadBeanDeploymentArchive(uiWebBeanClass));
      assertBDAId(bda, WAR_NAME);
      assertExpectedClasses(bda, ServletWebBean.class, UIWebBean.class);
      // duplicate call makes no difference in the bda contents
      assertSame(bda, deployment.loadBeanDeploymentArchive(uiWebBeanClass));
      assertBDAId(bda, WAR_NAME);
      assertExpectedClasses(bda, ServletWebBean.class, UIWebBean.class);
   }
   
   public void testWarWithoutXmlWithLib() throws Exception
   {
      WebArchive war = createWarWithLib(false, true);
      DeploymentUnit unit = assertDeploy(war);
      Class<?> servletWebBeanClass = getClass(ServletWebBean.class, unit);
      Class<?> uiWebBeanClass = getClass(UIWebBean.class, unit);
      Deployment deployment = initializeDeploymentBean();
      
      BeanDeploymentArchive bda = deployment.getBeanDeploymentArchives().iterator().next();
      assertBDAId(bda, WAR_NAME);
      assertExpectedClasses(bda, UIWebBean.class);
      assertSame(bda, deployment.loadBeanDeploymentArchive(servletWebBeanClass));
      assertBDAId(bda, WAR_NAME);
      assertExpectedClasses(bda, ServletWebBean.class, UIWebBean.class);
      // duplicate call makes no difference in the bda contents
      assertSame(bda, deployment.loadBeanDeploymentArchive(servletWebBeanClass));
      assertBDAId(bda, WAR_NAME);
      assertExpectedClasses(bda, ServletWebBean.class, UIWebBean.class);
      assertSame(bda, deployment.loadBeanDeploymentArchive(uiWebBeanClass));
      assertBDAId(bda, WAR_NAME);
      assertExpectedClasses(bda, ServletWebBean.class, UIWebBean.class);
   }
   
   public void testWarWithMixedLibs() throws Exception
   {
      WebArchive war = createWarWithLibs(true, true, false);
      DeploymentUnit unit = assertDeploy(war);
      Class<?> servletWebBeanClass = getClass(ServletWebBean.class, unit);
      Class<?> uiWebBeanClass = getClass(UIWebBean.class, unit);
      Class<?> crmWebBeanClass = getClass(CrmWebBean.class, unit);
      Deployment deployment = initializeDeploymentBean();
      
      BeanDeploymentArchive bda = deployment.getBeanDeploymentArchives().iterator().next();
      assertBDAId(bda, WAR_NAME);
      assertExpectedClasses(bda, ServletWebBean.class, UIWebBean.class);
      assertSame(bda, deployment.loadBeanDeploymentArchive(servletWebBeanClass));
      assertBDAId(bda, WAR_NAME);
      assertExpectedClasses(bda, ServletWebBean.class, UIWebBean.class);
      assertSame(bda, deployment.loadBeanDeploymentArchive(uiWebBeanClass));
      assertBDAId(bda, WAR_NAME);
      assertExpectedClasses(bda, ServletWebBean.class, UIWebBean.class);
      assertSame(bda, deployment.loadBeanDeploymentArchive(crmWebBeanClass));
      assertBDAId(bda, WAR_NAME);
      assertExpectedClasses(bda, ServletWebBean.class, UIWebBean.class, CrmWebBean.class);
      // duplicate call makes no difference in the bda contents
      assertSame(bda, deployment.loadBeanDeploymentArchive(crmWebBeanClass));
      assertBDAId(bda, WAR_NAME);
      assertExpectedClasses(bda, ServletWebBean.class, UIWebBean.class, CrmWebBean.class);
   }
   

   public void testWarInEars() throws Exception
   {
      EnterpriseArchive ear1 = ShrinkWrap.create(EnterpriseArchive.class, "warinear1.ear");
      WebArchive war1 = createWar(true);
      ear1.addModule(war1);
      MockArchiveManifest.addManifest(ear1);
      /*DeploymentUnit unit = */assertDeploy(ear1);
      //Class<?> servletWebBeanClass = getClass(ServletWebBean.class, unit.getChildren().iterator().next());
      //Deployment deployment1 = initializeDeploymentBean(unit));
      
      EnterpriseArchive ear2 = ShrinkWrap.create(EnterpriseArchive.class, "warinear2.ear");
      WebArchive war2 = createWar(WAR_NAME, true, NotWBJsfBean.class);
      ear2.addModule(war2);
      MockArchiveManifest.addManifest(ear2);
      /*unit = */assertDeploy(ear2);
      
      // assertion deleted as loadBDA implementation does not check for unreachable classes anymore
      // Class<?> notWBJsfBeanClass = getClass(NotWBJsfBean.class, unit.getChildren().iterator().next());
      // Deployment deployment2 = initializeDeploymentBean(unit));
      //assertCannotLoadBDA(deployment1, notWBJsfBeanClass);
      //assertCannotLoadBDA(deployment2, servletWebBeanClass);
   }

   public void testWarsInEar() throws Exception
   {
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "warinear.ear");
      WebArchive war1 = createWar("simple1.war", true, ServletWebBean.class);
      ear.addModule(war1);
      WebArchive war2 = createWar("simple2.war", true, NotWBJsfBean.class);
      ear.addModule(war2);
      MockArchiveManifest.addManifest(ear);
      DeploymentUnit unit = assertDeploy(ear);
      Iterator<DeploymentUnit> iterator = unit.getChildren().iterator();
      DeploymentUnit unit1 = iterator.next();
      DeploymentUnit unit2 = iterator.next();
      if (unit2.getName().contains("simple1.war"))
      {
         DeploymentUnit temp = unit2;
         unit2 = unit1;
         unit1 = temp;
      }
      Class<?> servletWebBeanClass = getClass(ServletWebBean.class, unit1);
      Class<?> notWBJsfBeanClass = getClass(NotWBJsfBean.class, unit2);
      Deployment deployment = initializeDeploymentBean();
      
      BeanDeploymentArchive bda1 = deployment.loadBeanDeploymentArchive(servletWebBeanClass);
      assertBDAId(bda1, "warinear.ear/simple1.war");
      assertExpectedClasses(bda1, ServletWebBean.class);
      
      BeanDeploymentArchive bda2 = deployment.loadBeanDeploymentArchive(notWBJsfBeanClass);
      assertBDAId(bda2, "warinear.ear/simple2.war");
      assertExpectedClasses(bda2, NotWBJsfBean.class);
   }
   
   public void testEjbJar_War() throws Exception
   {
      // ejb.jar
      JavaArchive ejbJar = createEjbJar(true);
      DeploymentUnit unit = assertDeploy(ejbJar);
      Class<?> plainJavaBeanClass = getClass(PlainJavaBean.class, unit);
      Deployment deployment1 = initializeDeploymentBean(unit);
      // simple.war
      WebArchive war = createWar(true);
      unit = assertDeploy(war);
      
      Deployment deployment2 = initializeDeploymentBean(unit);
      
      // assertion deleted as loadBDA implementation does not check for unreachable classes anymore
      //Class<?> servletWebBeanClass = getClass(ServletWebBean.class, unit);
      //assertCannotLoadBDA(deployment1, servletWebBeanClass);
      
      BeanDeploymentArchive bda = deployment1.getBeanDeploymentArchives().iterator().next();
      assertBDAId(bda, EJB_JAR_NAME);
      assertExpectedClasses(bda, PlainJavaBean.class);
      assertSame(bda, deployment2.loadBeanDeploymentArchive(plainJavaBeanClass));
      // make sure bda contents are unchanged
      assertBDAId(bda, EJB_JAR_NAME);
      assertExpectedClasses(bda, PlainJavaBean.class);
   }
   
   public void testEjbJarAndWarInEar() throws Exception
   {
      // ejb.jar
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME);
      JavaArchive ejbJar = createEjbJar(true);
      ear.addModule(ejbJar);
      // simple.war
      WebArchive war = createWar(true);
      ear.addModule(war);
      MockArchiveManifest.addManifest(ear);
      DeploymentUnit unit = assertDeploy(ear);
      Class<?> plainJavaBeanClass = getClass(PlainJavaBean.class, unit);
      Iterator<DeploymentUnit> iterator = unit.getChildren().iterator();
      DeploymentUnit warUnit = null;
      do
      {
         warUnit = iterator.next();
      } while (!warUnit.getName().contains(WAR_NAME));
      
      Class<?> servletWebBeanClass = getClass(ServletWebBean.class, warUnit);
      Deployment deployment = initializeDeploymentBean();
      
      
      BeanDeploymentArchive bda = deployment.loadBeanDeploymentArchive(plainJavaBeanClass);
      assertBDAId(bda, EAR_NAME);
      assertExpectedClasses(bda, PlainJavaBean.class);
      
      bda = deployment.loadBeanDeploymentArchive(servletWebBeanClass);
      assertBDAId(bda, EAR_NAME + "/" + WAR_NAME);
      assertExpectedClasses(bda, ServletWebBean.class);
   }
   
   public void testEjbJarInEar_War() throws Exception
   {
      // ejb.jar
      EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME);
      JavaArchive ejbJar = createEjbJar(true);
      ear.addModule(ejbJar);
      MockArchiveManifest.addManifest(ear);
      DeploymentUnit unit = assertDeploy(ear);
      Class<?> plainJavaBeanClass = getClass(PlainJavaBean.class, unit);
      Deployment deployment1 = initializeDeploymentBean(unit);
      // simple.war
      WebArchive war = createWar(true);
      unit = assertDeploy(war);
      
      Deployment deployment2 = initializeDeploymentBean(unit);
      
      // assertion deleted as loadBDA implementation does not check for unreachable classes anymore
      // Class<?> servletWebBeanClass = getClass(ServletWebBean.class, unit);
      //assertCannotLoadBDA(deployment1, servletWebBeanClass);
      
      BeanDeploymentArchive bda = deployment1.getBeanDeploymentArchives().iterator().next();
      assertBDAId(bda, EAR_NAME);
      assertExpectedClasses(bda, PlainJavaBean.class);
      assertSame(bda, deployment2.loadBeanDeploymentArchive(plainJavaBeanClass));
      // make sure bda contents are unchanged
      assertBDAId(bda, EAR_NAME);
      assertExpectedClasses(bda, PlainJavaBean.class);
   }
   
   
   public void testEjbJarInEar_WarWithLibInEar() throws Exception
   {
      // ejb.ear
      EnterpriseArchive ejbEar = ShrinkWrap.create(EnterpriseArchive.class, "ejb.ear");
      JavaArchive ejbJar = createEjbJar(true);
      ejbEar.addModule(ejbJar);
      MockArchiveManifest.addManifest(ejbEar);
      DeploymentUnit unit = assertDeploy(ejbEar);
      Class<?> plainJavaBeanClass = getClass(PlainJavaBean.class, unit);
      Deployment deployment1 = initializeDeploymentBean(unit);
      // war.ear
      EnterpriseArchive warEar = ShrinkWrap.create(EnterpriseArchive.class, "war.ear");
      WebArchive war = createWarWithLib(true, false);
      warEar.addModule(war);
      MockArchiveManifest.addManifest(warEar);
      unit = assertDeploy(warEar);
      DeploymentUnit warUnit = unit.getChildren().iterator().next();
      Class<?> servletWebBeanClass = getClass(ServletWebBean.class, warUnit);
      Class<?> uiWebBeanClass = getClass(UIWebBean.class, warUnit);
      Deployment deployment2 = initializeDeploymentBean(unit);
      
      // assertion deleted as loadBDA implementation does not check for unreachable classes anymore
      //assertCannotLoadBDA(deployment1, servletWebBeanClass);
      
      BeanDeploymentArchive bda = deployment1.getBeanDeploymentArchives().iterator().next();
      assertBDAId(bda, "ejb.ear");
      assertExpectedClasses(bda, PlainJavaBean.class);
      assertSame(bda, deployment2.loadBeanDeploymentArchive(plainJavaBeanClass));
      // make sure bda contents are unchanged
      assertBDAId(bda, "ejb.ear");
      assertExpectedClasses(bda, PlainJavaBean.class);
      
      bda = deployment2.loadBeanDeploymentArchive(servletWebBeanClass);
      String bdaName = "war.ear/" + WAR_NAME;
      assertBDAId(bda, bdaName);
      assertExpectedClasses(bda, ServletWebBean.class);
      
      BeanDeploymentArchive bda2 = deployment2.loadBeanDeploymentArchive(uiWebBeanClass);
      assertSame(bda, bda2);
      assertBDAId(bda, bdaName);
      assertExpectedClasses(bda, ServletWebBean.class, UIWebBean.class);
   }
   
   public void testMultipleArchives() throws Exception
   {
      WebArchive war1 = createWar("web1.war", true, ServletWebBean.class);
      DeploymentUnit war1Unit = assertDeploy(war1);
      Deployment war1Deployment = initializeDeploymentBean(war1Unit);
      BeanDeploymentArchive war1BDA = war1Deployment.getBeanDeploymentArchives().iterator().next();
      assertBDAId(war1BDA, "web1.war");
      assertExpectedClasses(war1BDA, ServletWebBean.class);
      Class<?> servletWebBeanWar1Class = getClass(ServletWebBean.class, war1Unit);
      
      WebArchive war2 = createWar("web2.war", true, NotWBJsfBean.class);
      createLib(war2, "crm.jar", false, CrmWebBean.class);
      DeploymentUnit war2Unit = assertDeploy(war2);
      Deployment war2Deployment = initializeDeploymentBean(war2Unit);
      BeanDeploymentArchive war2BDA = war2Deployment.getBeanDeploymentArchives().iterator().next();
      assertBDAId(war2BDA, "web2.war");
      assertExpectedClasses(war2BDA, NotWBJsfBean.class);
      Class<?> notWBJsfBeanWar2Class = getClass(NotWBJsfBean.class, war2Unit);
      Class<?> crmWebBeanWar2Class = getClass(CrmWebBean.class, war2Unit);
      
      JavaArchive ejbJar = createEjbJar("ejb.jar", true, BusinessInterface.class);
      DeploymentUnit ejbJarUnit = assertDeploy(ejbJar);
      Deployment ejbJarDeployment = initializeDeploymentBean(ejbJarUnit);
      BeanDeploymentArchive ejbJarBDA = ejbJarDeployment.getBeanDeploymentArchives().iterator().next();
      assertBDAId(ejbJarBDA, "ejb.jar");
      assertExpectedClasses(ejbJarBDA, BusinessInterface.class);
      Class<?> businessInterfaceClass = getClass(BusinessInterface.class, ejbJarUnit);
      
      EnterpriseArchive ear1 = ShrinkWrap.create(EnterpriseArchive.class, "full.ear");
      WebArchive warInEar1 = createWarWithLibs(false, true, true);
      ear1.addModule(warInEar1);
      ear1.addModule(war2);
      JavaArchive ejbJarInEar1 = createEjbJar("ejbInFullEar.jar", false, MySLSBean.class);
      ear1.addModule(ejbJarInEar1);
      createLib(ear1, "lib1.jar", false, ExternalWebBean.class);
      createLib(ear1, "lib2.jar", true, A.class);
      createLib(ear1, "lib3.jar", false, B.class);
      MockArchiveManifest.addManifest(ear1);
      DeploymentUnit ear1Unit = assertDeploy(ear1);
      Deployment ear1Deployment = initializeDeploymentBean(ear1Unit);
      BeanDeploymentArchive ear1BDA = null, ear1War1BDA = null, ear1War2BDA = null;
      for (BeanDeploymentArchive bda: ear1Deployment.getBeanDeploymentArchives())
      {
         if (bda.getId().contains(WAR_NAME))
         {
            ear1War1BDA = bda;
         }
         else if (bda.getId().contains("web2.war"))
         {
            ear1War2BDA = bda;
         }
         else
         {
            ear1BDA = bda;
         }
      }
      assertBDAId(ear1BDA, "full.ear");
      assertExpectedClasses(ear1BDA, A.class);
      assertBDAId(ear1War1BDA, "full.ear/" + WAR_NAME);
      assertExpectedClasses(ear1War1BDA, UIWebBean.class, CrmWebBean.class);
      assertBDAId(ear1War2BDA, "full.ear/web2.war");
      assertExpectedClasses(ear1War2BDA, NotWBJsfBean.class);
      Class<?> servletWebBeanEar1Class = null, uiWebBeanEar1Class = null,
               crmWebBeanEar1War1Class = null, notWBJsfBeanEar1Class = null,
               crmWebBeanEar1War2Class = null;
      for (DeploymentUnit ear1Child: ear1Unit.getChildren())
      {
         if (ear1Child.getName().contains(WAR_NAME))
         {
           servletWebBeanEar1Class = getClass(ServletWebBean.class, ear1Child);
           uiWebBeanEar1Class = getClass(UIWebBean.class, ear1Child);
           crmWebBeanEar1War1Class = getClass(CrmWebBean.class, ear1Child);
         }
         else if (ear1Child.getName().contains("web2.war"))
         {
            notWBJsfBeanEar1Class = getClass(NotWBJsfBean.class, ear1Child);
            crmWebBeanEar1War2Class = getClass(CrmWebBean.class, ear1Child);
         }
      }
      Class<?> externalWebBeanClass = getClass(ExternalWebBean.class, ear1Unit);
      Class<?> aClass = getClass(A.class, ear1Unit);
      Class<?> bClass = getClass(B.class, ear1Unit);
      
      EnterpriseArchive ear2 = ShrinkWrap.create(EnterpriseArchive.class, "ejbWLibs.ear");
      JavaArchive ejbJarInEar2 = createEjbJar("ejbInEar2.jar", true, PlainJavaBean.class);
      ear2.addModule(ejbJarInEar2);
      createLib(ear2, "lib1.jar", false, SomeUtil.class);
      createLib(ear2, "lib2.jar", true, CrmWebBean.class);
      MockArchiveManifest.addManifest(ear2);
      DeploymentUnit ear2Unit = assertDeploy(ear2);
      Deployment ear2Deployment = initializeDeploymentBean(ear2Unit);
      BeanDeploymentArchive ear2BDA = ear2Deployment.getBeanDeploymentArchives().iterator().next();
      assertBDAId(ear2BDA, "ejbWLibs.ear");
      assertExpectedClasses(ear2BDA, PlainJavaBean.class, CrmWebBean.class);
      Class<?> plainJavaBeanClass = getClass(PlainJavaBean.class, ear2Unit);
      Class<?> someUtilClass = getClass(SomeUtil.class, ear2Unit);
      Class<?> crmWebBeanClass = getClass(CrmWebBean.class, ear2Unit);
      
      // Assert on web classes
      
      // assertion deleted as loadBDA implementation does not check for unreachable classes anymore
      /*assertCannotLoadBDA(war1Deployment, servletWebBeanEar1Class);
      assertCannotLoadBDA(war1Deployment, notWBJsfBeanWar2Class);
      assertCannotLoadBDA(war1Deployment, notWBJsfBeanEar1Class);
      assertCannotLoadBDA(war1Deployment, crmWebBeanWar2Class);
      assertCannotLoadBDA(war1Deployment, crmWebBeanEar1War1Class);
      assertCannotLoadBDA(war1Deployment, crmWebBeanEar1War2Class);
      assertCannotLoadBDA(war1Deployment, uiWebBeanEar1Class);*/
      BeanDeploymentArchive bda = war1Deployment.loadBeanDeploymentArchive(servletWebBeanWar1Class);
      assertSame(war1BDA, bda);
      // verify the absence of collateral effects on the BDA
      assertBDAId(war1BDA, "web1.war");
      assertExpectedClasses(war1BDA, ServletWebBean.class);
      
      // assertion deleted as loadBDA implementation does not check for unreachable classes anymore
      /*assertCannotLoadBDA(war2Deployment, servletWebBeanWar1Class);
      assertCannotLoadBDA(war2Deployment, servletWebBeanEar1Class);
      assertCannotLoadBDA(war2Deployment, notWBJsfBeanEar1Class);
      assertCannotLoadBDA(war2Deployment, crmWebBeanEar1War1Class);
      assertCannotLoadBDA(war2Deployment, crmWebBeanEar1War2Class);
      assertCannotLoadBDA(war2Deployment, uiWebBeanEar1Class);*/
      bda = war2Deployment.loadBeanDeploymentArchive(notWBJsfBeanWar2Class);
      bda = war2Deployment.loadBeanDeploymentArchive(crmWebBeanWar2Class);
      assertSame(war2BDA, bda);
      assertBDAId(war2BDA, "web2.war");
      assertExpectedClasses(war2BDA, NotWBJsfBean.class, CrmWebBean.class);
      
      // assertion deleted as loadBDA implementation does not check for unreachable classes anymore
      /*assertCannotLoadBDA(ejbJarDeployment, servletWebBeanWar1Class);
      assertCannotLoadBDA(ejbJarDeployment, servletWebBeanEar1Class);
      assertCannotLoadBDA(ejbJarDeployment, notWBJsfBeanWar2Class);
      assertCannotLoadBDA(ejbJarDeployment, notWBJsfBeanEar1Class);
      assertCannotLoadBDA(ejbJarDeployment, crmWebBeanWar2Class);
      assertCannotLoadBDA(ejbJarDeployment, crmWebBeanEar1War1Class);
      assertCannotLoadBDA(ejbJarDeployment, crmWebBeanEar1War2Class);
      assertCannotLoadBDA(ejbJarDeployment, uiWebBeanEar1Class);
      
      assertCannotLoadBDA(ear1Deployment, servletWebBeanWar1Class);
      assertCannotLoadBDA(ear1Deployment, notWBJsfBeanWar2Class);
      assertCannotLoadBDA(ear1Deployment, crmWebBeanWar2Class);*/
      bda = ear1Deployment.loadBeanDeploymentArchive(servletWebBeanEar1Class);
      assertSame(ear1War1BDA, bda);
      bda = ear1Deployment.loadBeanDeploymentArchive(uiWebBeanEar1Class);
      assertSame(ear1War1BDA, bda);
      bda = ear1Deployment.loadBeanDeploymentArchive(crmWebBeanEar1War1Class);
      assertSame(ear1War1BDA, bda);
      assertBDAId(ear1War1BDA, "full.ear/" + WAR_NAME);
      assertExpectedClasses(ear1War1BDA, ServletWebBean.class, UIWebBean.class, CrmWebBean.class);
      
      bda = ear1Deployment.loadBeanDeploymentArchive(notWBJsfBeanEar1Class);
      assertSame(ear1War2BDA, bda);
      bda = ear1Deployment.loadBeanDeploymentArchive(crmWebBeanEar1War2Class);
      assertSame(ear1War2BDA, bda);
      assertBDAId(ear1War2BDA, "full.ear/web2.war");
      assertExpectedClasses(ear1War2BDA, NotWBJsfBean.class, CrmWebBean.class);
      
      // assertion deleted as loadBDA implementation does not check for unreachable classes anymore
      /*assertCannotLoadBDA(ear2Deployment, servletWebBeanWar1Class);
      assertCannotLoadBDA(ear2Deployment, servletWebBeanEar1Class);
      assertCannotLoadBDA(ear2Deployment, notWBJsfBeanWar2Class);
      assertCannotLoadBDA(ear2Deployment, notWBJsfBeanEar1Class);
      assertCannotLoadBDA(ear2Deployment, crmWebBeanWar2Class);
      assertCannotLoadBDA(ear2Deployment, crmWebBeanEar1War1Class);
      assertCannotLoadBDA(ear2Deployment, crmWebBeanEar1War2Class);
      assertCannotLoadBDA(ear2Deployment, uiWebBeanEar1Class);*/
      
      // Assert on business classes that are part of existing BDAs
      bda = ejbJarDeployment.loadBeanDeploymentArchive(businessInterfaceClass);
      assertSame(ejbJarBDA, bda);
      war1Deployment.loadBeanDeploymentArchive(businessInterfaceClass);
      assertSame(ejbJarBDA, bda);
      war2Deployment.loadBeanDeploymentArchive(businessInterfaceClass);
      assertSame(ejbJarBDA, bda);
      ear1Deployment.loadBeanDeploymentArchive(businessInterfaceClass);
      assertSame(ejbJarBDA, bda);
      war2Deployment.loadBeanDeploymentArchive(businessInterfaceClass);
      assertSame(ejbJarBDA, bda);
      assertBDAId(ejbJarBDA, "ejb.jar"); // no collateral effects on the BDA
      assertExpectedClasses(ejbJarBDA, BusinessInterface.class);
      
      bda = ear1Deployment.loadBeanDeploymentArchive(aClass);
      assertSame(ear1BDA, bda);
      bda = war1Deployment.loadBeanDeploymentArchive(aClass);
      assertSame(ear1BDA, bda);
      bda = war2Deployment.loadBeanDeploymentArchive(aClass);
      assertSame(ear1BDA, bda);
      bda = ejbJarDeployment.loadBeanDeploymentArchive(aClass);
      assertSame(ear1BDA, bda);
      bda = ear2Deployment.loadBeanDeploymentArchive(aClass);
      assertSame(ear1BDA, bda);
      assertBDAId(ear1BDA, "full.ear"); // no collateral effects on the BDA
      assertExpectedClasses(ear1BDA, A.class);
      
      bda = ear2Deployment.loadBeanDeploymentArchive(plainJavaBeanClass);
      assertSame(ear2BDA, bda);
      bda = ear2Deployment.loadBeanDeploymentArchive(crmWebBeanClass);
      assertSame(ear2BDA, bda);
      bda = war1Deployment.loadBeanDeploymentArchive(plainJavaBeanClass);
      assertSame(ear2BDA, bda);
      bda = war1Deployment.loadBeanDeploymentArchive(crmWebBeanClass);
      assertSame(ear2BDA, bda);
      bda = war2Deployment.loadBeanDeploymentArchive(plainJavaBeanClass);
      assertSame(ear2BDA, bda);
      bda = war2Deployment.loadBeanDeploymentArchive(crmWebBeanClass);
      assertSame(ear2BDA, bda);
      bda = ejbJarDeployment.loadBeanDeploymentArchive(plainJavaBeanClass);
      assertSame(ear2BDA, bda);
      bda = ejbJarDeployment.loadBeanDeploymentArchive(crmWebBeanClass);
      assertSame(ear2BDA, bda);
      bda = ear1Deployment.loadBeanDeploymentArchive(plainJavaBeanClass);
      assertSame(ear2BDA, bda);
      bda = ear1Deployment.loadBeanDeploymentArchive(crmWebBeanClass);
      assertSame(ear2BDA, bda);
      assertBDAId(ear2BDA, "ejbWLibs.ear"); // no collateral effects on the BDA
      assertExpectedClasses(ear2BDA, PlainJavaBean.class, CrmWebBean.class);
      
      // Assert on business classes that are not yet part of existing BDAs
      bda = war1Deployment.loadBeanDeploymentArchive(externalWebBeanClass);
      assertSame(ear1BDA, bda);
      assertBDAId(ear1BDA, "full.ear");
      assertExpectedClasses(ear1BDA, ExternalWebBean.class, A.class);
      bda = war2Deployment.loadBeanDeploymentArchive(externalWebBeanClass);
      assertSame(ear1BDA, bda);
      bda = ejbJarDeployment.loadBeanDeploymentArchive(externalWebBeanClass);
      assertSame(ear1BDA, bda);
      bda = ear1Deployment.loadBeanDeploymentArchive(externalWebBeanClass);
      assertSame(ear1BDA, bda);
      bda = ear2Deployment.loadBeanDeploymentArchive(externalWebBeanClass);
      assertSame(ear1BDA, bda);
      assertBDAId(ear1BDA, "full.ear"); // no collateral effects on the BDA
      assertExpectedClasses(ear1BDA, ExternalWebBean.class, A.class);
      
      bda = ear1Deployment.loadBeanDeploymentArchive(bClass);
      assertSame(ear1BDA, bda);
      assertBDAId(ear1BDA, "full.ear");
      assertExpectedClasses(ear1BDA, ExternalWebBean.class, A.class, B.class);
      bda = war1Deployment.loadBeanDeploymentArchive(bClass);
      assertSame(ear1BDA, bda);
      bda = war2Deployment.loadBeanDeploymentArchive(bClass);
      assertSame(ear1BDA, bda);
      bda = ejbJarDeployment.loadBeanDeploymentArchive(bClass);
      assertSame(ear1BDA, bda);
      bda = ear2Deployment.loadBeanDeploymentArchive(bClass);
      assertSame(ear1BDA, bda);
      assertBDAId(ear1BDA, "full.ear"); // no collateral effects on the BDA
      assertExpectedClasses(ear1BDA, ExternalWebBean.class, A.class, B.class);

      bda = ejbJarDeployment.loadBeanDeploymentArchive(someUtilClass);
      assertSame(ear2BDA, bda);
      assertBDAId(ear2BDA, "ejbWLibs.ear");
      assertExpectedClasses(ear2BDA, PlainJavaBean.class, SomeUtil.class, CrmWebBean.class);
      bda = war1Deployment.loadBeanDeploymentArchive(someUtilClass);
      assertSame(ear2BDA, bda);
      bda = war2Deployment.loadBeanDeploymentArchive(someUtilClass);
      assertSame(ear2BDA, bda);
      bda = ear1Deployment.loadBeanDeploymentArchive(someUtilClass);
      assertSame(ear2BDA, bda);
      bda = ear2Deployment.loadBeanDeploymentArchive(someUtilClass);
      assertSame(ear2BDA, bda);
      assertBDAId(ear2BDA, "ejbWLibs.ear"); // no collateraleffects on the BDA
      assertExpectedClasses(ear2BDA, PlainJavaBean.class, SomeUtil.class, CrmWebBean.class);
   }

   @Override
   protected Collection<String> getClasses(BeanDeploymentArchive bda)
   {
      return bda.getBeanClasses();
   }

   private Deployment initializeDeploymentBean(DeploymentUnit unit)
   {
      DeploymentImpl deployment = (DeploymentImpl) getBean(DeployersUtils.getDeploymentBeanName(unit));
      deployment.initialize(bootstrap);
      return deployment;
   }
   
   private Deployment initializeDeploymentBean()
   {
      DeploymentImpl deployment = (DeploymentImpl) getBean(Deployment.class);
      deployment.initialize(bootstrap);
      return deployment;
   }

   private void assertNoBeansXml (BeanDeploymentArchive bda)
   {
      assertSame(BeansXml.EMPTY_BEANS_XML, bda.getBeansXml());
   }
   
   private void assertBDAId(BeanDeploymentArchive bda, String name)
   {
      assertTrue("BDA id \""  + bda.getId() + "\" expected to end with suffix \"" + name + "/}\"",
               bda.getId().endsWith(name + "/}"));
   }

   private Class<?> getClass(Class<?> clazz, DeploymentUnit unit) throws ClassNotFoundException
   {
      ClassLoader classLoader = unit.getClassLoader();
      return classLoader.loadClass(clazz.getName());
   }
   
   // assertion deleted as loadBDA implementation does not check for unreachable classes anymore
   /*private void assertCannotLoadBDA(Deployment deployment, Class<?> beanClass)
   {
      boolean failed = false;
      try
      {
         deployment.loadBeanDeploymentArchive(beanClass);
      }
      catch (IllegalArgumentException e)
      {
         failed = true;
      }
      assertTrue(failed);
   }*/
}