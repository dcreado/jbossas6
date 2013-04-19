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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.management.ObjectName;

import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.structure.spi.DeploymentContext;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.mx.util.ObjectNameFactory;
import org.jboss.system.ServiceMBean;

/**
 * MainDeployer MBean interface
 * 
 * @version $Revision: 84385 $
 */
public interface MainDeployerMBean extends
   ServiceMBean, DeployerMBean, MainDeployerConstants
{
   /** The default ObjectName */
   ObjectName OBJECT_NAME = ObjectNameFactory.create("jboss.system:service=MainDeployer");

   // Attributes ----------------------------------------------------
   public DeployerClient getKernelMainDeployer();
   /** set the kernel MainDeployer which will handle deployments */
   public void setKernelMainDeployer(DeployerClient delegate);

   public KernelController getController();
   public void setController(KernelController controller);

   /** Flag indicating whether directory content will be deployed.
    * The default value is taken from the jboss.deploy.localcopy system property. */
   boolean getCopyFiles();
   void setCopyFiles(boolean copyFiles);
   
   /** The path to the local tmp directory */
   File getTempDir();
   void setTempDir(File tempDir);
   
   /** The ObjectName of the ServiceController */
   void setServiceController(ObjectName serviceController);
   
   /** The path to the local tmp directory in String form */
   String getTempDirString();

   // Operations ----------------------------------------------------

   /**
    * The <code>shutdown</code> method undeploys all deployed packages
    * in reverse order of their deployement.
    */
   void shutdown();

   /**
    * Describe <code>redeploy</code> method here.
    * @param urlspec a <code>String</code> value
    * @exception DeploymentException if an error occurs
    * @exception java.net.MalformedURLException if an error occurs
    */
   void redeploy(String urlspec) throws DeploymentException, MalformedURLException;

   /**
    * Describe <code>redeploy</code> method here.
    * @param url an <code>URL</code> value
    * @exception DeploymentException if an error occurs
    */
   void redeploy(URL url) throws DeploymentException;

   /**
    * The <code>undeploy</code> method undeploys a package identified by a URL
    * @param url an <code>URL</code> value
    */
   void undeploy(URL url) throws DeploymentException;

   /**
    * The <code>undeploy</code> method undeploys a package identified by a string representation of a URL.
    * @param urlspec a <code>String</code> value
    * @exception java.net.MalformedURLException if an error occurs
    */
   void undeploy(String urlspec) throws DeploymentException, MalformedURLException;

   /**
    * The <code>deploy</code> method deploys a package identified by a string representation of a URL.
    * @param urlspec a <code>String</code> value
    * @exception java.net.MalformedURLException if an error occurs
    */
   void deploy(String urlspec) throws DeploymentException, MalformedURLException;

   /**
    * The <code>deploy</code> method deploys a package identified by a URL
    * @param url an <code>URL</code> value
    */
   void deploy(URL url) throws DeploymentException;

   /**
    * The <code>isDeployed</code> method tells you if a package identified
    * by a string representation of a URL is currently deployed.
    * @param url a <code>String</code> value
    * @return a <code>boolean</code> value
    * @exception java.net.MalformedURLException if an error occurs
    */
   boolean isDeployed(String url) throws MalformedURLException;

   /**
    * The <code>isDeployed</code> method tells you if a packaged identified
    * by a URL is deployed.
    * @param url an <code>URL</code> value
    * @return a <code>boolean</code> value
    */
   boolean isDeployed(URL url);

   /**
    * The <code>getDeployment</code> method returns the Deployment object
    * for the URL supplied.
    * @param url an <code>URL</code> value
    * @return a <code>Deployment</code> value
    */
   Deployment getDeployment(URL url);

   /**
    * The <code>getDeploymentContext</code> method returns the DeploymentContext object
    * for the URL supplied.
    * @param url an <code>URL</code> value
    * @return a <code>DeploymentContext</code> value
    * @deprecated use getDeploymentUnit
    */
   DeploymentContext getDeploymentContext(URL url);

   /**
    * The <code>getDeploymentUnit</code> method returns the DeploymentUnit
    * object for the URL supplied.
    *
    * @param url an <code>URL</code> value
    * @return a <code>DeploymentUnit</code> value
    * @jmx.managed-operation
    */
   DeploymentUnit getDeploymentUnit(URL url);

   /**
    * The <code>getWatchUrl</code> method returns the URL that,
    * when modified, indicates that a redeploy is needed.
    * @param url an <code>URL</code> value
    * @return a <code>URL</code> value
    */
   URL getWatchUrl(URL url);

   /**
    * Check the current deployment states and generate an
    * IncompleteDeploymentException if there are mbeans
    * waiting for depedencies.
    * @exception IncompleteDeploymentException
    */
   void checkIncompleteDeployments() throws DeploymentException;

   /**
    * Deploy a deployment 
    * 
    * @param deployment the deployment
    * @deprecated this is for testing, it should really be handled by the deployment manager
    * @throws org.jboss.deployers.spi.DeploymentException for any error
    */
   void deploy(Deployment deployment) throws org.jboss.deployers.spi.DeploymentException;

   /**
    * Undeploy a deployment
    *  
    * @param deployment the deployment
    * @deprecated this is for testing, it should really be handled by the deployment manager
    * @throws org.jboss.deployers.spi.DeploymentException for any error
    */
   void undeploy(Deployment deployment) throws org.jboss.deployers.spi.DeploymentException;
}
