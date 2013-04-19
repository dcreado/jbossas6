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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.bootstrap.api.as.config.JBossASServerConfig;
import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.structure.spi.DeploymentContext;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.structure.spi.main.MainDeployerInternals;
import org.jboss.deployers.vfs.spi.client.VFSDeployment;
import org.jboss.deployers.vfs.spi.client.VFSDeploymentFactory;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.system.ServiceMBeanSupport;
import org.jboss.system.server.ServerConfigLocator;
import org.jboss.util.file.Files;
import org.jboss.util.file.JarUtils;
import org.jboss.util.stream.Streams;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * The legacy component for deployer management. This now simply delegates to the
 * Main
 *
 * @deprecated see org.jboss.deployers.spi.deployment.MainDeployer
 *
 * @author <a href="mailto:marc.fleury@jboss.org">Marc Fleury</a>
 * @author <a href="mailto:scott.stark@jboss.org">Scott Stark</a>
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 * @author adrian@jboss.org
 * @author ales.justin@jboss.org
 * @version $Revision: 109191 $
 */
public class MainDeployer extends ServiceMBeanSupport
   implements Deployer, MainDeployerMBean
{
   /** The controller */
   private KernelController controller;
   private DeployerClient delegate;
   private Map<URL, String> contextMap = Collections.synchronizedMap(new HashMap<URL, String>());

   /** The deployment factory */
   private VFSDeploymentFactory deploymentFactory = VFSDeploymentFactory.getInstance();

   /**
    * The variable <code>serviceController</code> is used by the
    * checkIncompleteDeployments method to ask for lists of mbeans
    * with deployment problems.
    */
   private ObjectName serviceController;

   /** Should local copies be made of resources on the local file system */
   private boolean copyFiles = true;

   /** The temporary directory for deployments. */
   private File tempDir;

   /** The string naming the tempDir **/
   private String tempDirString;

   /**
    * Explict no-args contsructor for JMX.
    */
   public MainDeployer()
   {
      // Is there a better place to obtain startup information?
      String localCopy = System.getProperty("jboss.deploy.localcopy");
      if (localCopy != null && (
          localCopy.equalsIgnoreCase("false") ||
          localCopy.equalsIgnoreCase("no") ||
          localCopy.equalsIgnoreCase("off")))
      {
         log.debug("Disabling local copies of file: urls");
         copyFiles = false;
      }
   }

   public DeployerClient getKernelMainDeployer()
   {
      return delegate;
   }
   public void setKernelMainDeployer(DeployerClient delegate)
   {
      this.delegate = delegate;
   }

   public KernelController getController()
   {
      return controller;
   }

   public void setController(KernelController controller)
   {
      this.controller = controller;
   }

   /** Get the flag indicating whether directory content will be deployed
    *
    * @return the file copy flag
    * @jmx.managed-attribute
    */
   public boolean getCopyFiles()
   {
      return copyFiles;
   }
   /** Set the flag indicating whether directory content will be deployed. The
    * default value is taken from the jboss.deploy.localcopy system
    * property.
    *
    * @param copyFiles the local copy flag value
    * @jmx.managed-attribute
    */
   public void setCopyFiles(boolean copyFiles)
   {
      this.copyFiles = copyFiles;
   }

   /** Get the temp directory
    *
    * @return the path to the local tmp directory
    * @jmx.managed-attribute
    */
   public File getTempDir()
   {
      return tempDir;
   }
   /** Set the temp directory
    *
    * @param tempDir the path to the local tmp directory
    * @jmx.managed-attribute
    */
   public void setTempDir(File tempDir)
   {
      this.tempDir = tempDir;
   }

   /** Get the temp directory
    *
    * @return the path to the local tmp directory
    * @jmx.managed-attribute
    */
   public String getTempDirString()
   {
      return tempDirString;
   }

   /**
    * Describe <code>setServiceController</code> method here.
    *
    * @param serviceController an <code>ObjectName</code> value
    * @jmx.managed-attribute
    */
   public void setServiceController(final ObjectName serviceController)
   {
      this.serviceController = serviceController;
   }

   // ServiceMBeanSupport overrides ---------------------------------

   protected ObjectName getObjectName(MBeanServer server, ObjectName name)
      throws MalformedObjectNameException
   {
      return name == null ? OBJECT_NAME : name;
   }

   /**
    * The <code>createService</code> method is one of the ServiceMBean lifecyle operations.
    * (no jmx tag needed from superinterface)
    * @exception Exception if an error occurs
    */
   protected void createService() throws Exception
   {
      JBossASServerConfig config = ServerConfigLocator.locate();
      // Get the temp directory location
      File basedir = new File(config.getServerTempLocation().toURI());
      // Set the local copy temp dir to tmp/deploy
      tempDir = new File(basedir, "deploy");
      // Delete any existing content
      Files.delete(tempDir);
      // Make sure the directory exists
      tempDir.mkdirs();

      // used in inLocalCopyDir
      tempDirString = tempDir.toURL().toString();
   }

   /**
    * The <code>shutdown</code> method undeploys all deployed packages in
    * reverse order of their deployement.
    *
    * @jmx.managed-operation
    */
   public void shutdown()
   {
      this.tempDir = null;
   }


   /**
    * Describe <code>redeploy</code> method here.
    *
    * @param urlspec a <code>String</code> value
    * @exception DeploymentException if an error occurs
    * @exception MalformedURLException if an error occurs
    * @jmx.managed-operation
    */
   public void redeploy(String urlspec)
      throws DeploymentException, MalformedURLException
   {
      redeploy(new URL(urlspec));
   }

   /**
    * Describe <code>redeploy</code> method here.
    *
    * @param url an <code>URL</code> value
    * @exception DeploymentException if an error occurs
    * @jmx.managed-operation
    */
   public void redeploy(URL url) throws DeploymentException
   {
      String deploymentName = contextMap.get(url);
      if (deploymentName != null)
      {
         try
         {
            Deployment deployment = delegate.getDeployment(deploymentName);
            delegate.addDeployment(deployment);
            delegate.process();
            delegate.checkComplete(deployment);
         }
         catch (org.jboss.deployers.spi.DeploymentException e)
         {
            throw new DeploymentException(e);
         }
      }
      else
      {
         deploy(url);
      }
   }

   /**
    * The <code>undeploy</code> method undeploys a package identified by a string
    * representation of a URL.
    *
    * @param urlspec the stringfied url to undeploy
    * @jmx.managed-operation
    */
   public void undeploy(String urlspec)
      throws DeploymentException, MalformedURLException
   {
      undeploy(new URL(urlspec));
   }

   /**
    * The <code>undeploy</code> method undeploys a package identified by a URL
    *
    * @param url the url to undeploy
    * @jmx.managed-operation
    */
   public void undeploy(URL url) throws DeploymentException
   {
      String deploymentName = contextMap.remove(url);
      if (deploymentName != null)
      {
         try
         {
            delegate.removeDeployment(deploymentName);
            delegate.process();
         }
         catch(Exception e)
         {
            DeploymentException ex = new DeploymentException("Error during undeploy of: "+url, e);
            throw ex;
         }
      }
      else
      {
         log.warn("undeploy '" + url + "' : package not deployed");
      }
   }

   /**
    * The <code>deploy</code> method deploys a package identified by a
    * string representation of a URL.
    *
    * @param urlspec a <code>String</code> value
    * @exception MalformedURLException if an error occurs
    * @jmx.managed-operation
    */
   public void deploy(String urlspec)
      throws DeploymentException, MalformedURLException
   {
      if( server == null )
         throw new DeploymentException("The MainDeployer has been unregistered");

      URL url;
      try
      {
         url = new URL(urlspec);
      }
      catch (MalformedURLException e)
      {
         File file = new File(urlspec);
         url = file.toURL();
      }

      deploy(url);
   }

   /**
    * The <code>deploy</code> method deploys a package identified by a URL
    *
    * @param url an <code>URL</code> value
    * @jmx.managed-operation
    */
   public void deploy(URL url) throws DeploymentException
   {
      log.info("deploy, url="+url);
      String deploymentName = contextMap.get(url);
      // if it does not exist create a new deployment
      if (deploymentName == null)
      {
         try
         {
            // 
            VirtualFile file = VFS.getChild(url);
            if (file == null || file.exists() == false) {
               // copy to temp location and deploy from there.
               final File temp = File.createTempFile("tmp", getShortName(url.getPath()), getTempDir());
               temp.deleteOnExit();
               copy(url, temp);
               file = VFS.getChild(temp.toURI());
               deploymentName = url.toExternalForm();
            } else {
               deploymentName = file.asFileURI().toString();               
            }
            
            final VFSDeployment deployment = deploymentFactory.createVFSDeployment(deploymentName, file);
            delegate.addDeployment(deployment);
            delegate.process();
            // TODO: JBAS-4292
            contextMap.put(url, deploymentName);
            delegate.checkComplete(deployment);
         }
         catch (DeploymentException e)
         {
            log.warn("Failed to deploy: "+url, e);
            throw e;
         }
         catch(Exception e)
         {
            log.warn("Failed to deploy: "+url, e);
            throw new DeploymentException("Failed to deploy: "+url, e);
         }
      }
   }

   protected void copy(URL src, File dest) throws IOException
   {
      log.debug("Copying " + src + " -> " + dest);

      // Validate that the dest parent directory structure exists
      File dir = dest.getParentFile();
      if (!dir.exists())
      {
         boolean created = dir.mkdirs();
         if( created == false )
            throw new IOException("mkdirs failed for: "+dir.getAbsolutePath());
      }

      // Remove any existing dest content
      if( dest.exists() == true )
      {
         boolean deleted = Files.delete(dest);
         if( deleted == false )
            throw new IOException("delete of previous content failed for: "+dest.getAbsolutePath());
      }

      if (src.getProtocol().equals("file"))
      {
         File srcFile = new File(src.getFile());
         if (srcFile.isDirectory())
         {
            log.debug("Making zip copy of: " + srcFile);
            // make a jar archive of the directory
            OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
            JarUtils.jar(out, srcFile.listFiles());
            out.close();
            return;
         }
      }

      InputStream in = new BufferedInputStream(src.openStream());
      OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
      Streams.copy(in, out);
      out.flush();
      out.close();
      in.close();
   }

   /**
    * The <code>isDeployed</code> method tells you if a package identified by a string
    * representation of a URL is currently deployed.
    *
    * @param url a <code>String</code> value
    * @return a <code>boolean</code> value
    * @exception MalformedURLException if an error occurs
    * @jmx.managed-operation
    */
   public boolean isDeployed(String url)
      throws MalformedURLException
   {
      return isDeployed(new URL(url));
   }

   /**
    * The <code>isDeployed</code> method tells you if a packaged identified by
    * a URL is deployed.
    * @param url an <code>URL</code> value
    * @return a <code>boolean</code> value
    * @jmx.managed-operation
    */
   public boolean isDeployed(URL url)
   {
      String name = contextMap.get(url);
      if (name == null)
      {
         if (log.isTraceEnabled())
            log.trace("No such context: " + url);
         if (url == null)
            throw new IllegalArgumentException("Null url");
         String urlString = url.toString();
         // remove this once the JBoss-test is updated with VFS usage
         if (urlString.startsWith("vfs") == false)
            return checkDeployed("vfs" + urlString);
         else
            return checkDeployed(urlString);
      }

      return checkDeployed(name);
   }

   /**
    * Is deployed.
    *
    * @param name the name of the deployment
    * @return true if deployed, false otherwise
    */
   protected boolean checkDeployed(String name)
   {
      org.jboss.deployers.spi.DeploymentState deploymentState = delegate.getDeploymentState(name);
      log.debug("isDeployed, url="+name+", state="+deploymentState);
      return deploymentState == org.jboss.deployers.spi.DeploymentState.DEPLOYED;
   }

   /**
    * The <code>getDeployment</code> method returns the Deployment
    * object for the URL supplied.
    *
    * @param url an <code>URL</code> value
    * @return a <code>Deployment</code> value
    * @jmx.managed-operation
    */
   public Deployment getDeployment(URL url)
   {
      String name = contextMap.get(url);
      if (name == null)
         return null;

      Deployment dc = delegate.getDeployment(name);
      log.debug("getDeployment, url="+url+", dc="+dc);
      return dc;
   }

   /**
    * The <code>getDeploymentContext</code> method returns the DeploymentContext
    * object for the URL supplied.
    *
    * @param url an <code>URL</code> value
    * @return a <code>DeploymentContext</code> value
    * @jmx.managed-operation
    */
   @Deprecated
   public DeploymentContext getDeploymentContext(URL url)
   {
      String name = contextMap.get(url);
      if (name == null)
         return null;

      MainDeployerInternals structure = (MainDeployerInternals) delegate;
      DeploymentContext dc = structure.getDeploymentContext(name);
      log.debug("getDeploymentContext, url="+url+", dc="+dc);
      return dc;
   }

   /**
    * The <code>getDeploymentUnit</code> method returns the DeploymentUnit
    * object for the URL supplied.
    *
    * @param url an <code>URL</code> value
    * @return a <code>DeploymentUnit</code> value
    * @jmx.managed-operation
    */
   public DeploymentUnit getDeploymentUnit(URL url)
   {
      String name = contextMap.get(url);
      if (name == null)
         return null;

      DeploymentContext context = getDeploymentContext(url);
      DeploymentUnit du = context.getDeploymentUnit();
      log.debug("getDeploymentUnit, url="+url+", du="+du);
      return du;
   }

   /**
    * The <code>getWatchUrl</code> method returns the URL that, when modified,
    * indicates that a redeploy is needed.
    *
    * @param url an <code>URL</code> value
    * @return a <code>URL</code> value
    * @jmx.managed-operation
    */
   public URL getWatchUrl(URL url)
   {
      return url;
   }

   /** Check the current deployment states and generate a IncompleteDeploymentException
    * if there are mbeans waiting for depedencies.
    * @exception IncompleteDeploymentException
    * @jmx.managed-operation
    */
   public void checkIncompleteDeployments() throws DeploymentException
   {
      try
      {
         delegate.checkComplete();
      }
      catch (Exception e)
      {
         throw new DeploymentException("Deployments are incomplete", e);
      }
   }

   @Deprecated
   public void deploy(Deployment deployment) throws org.jboss.deployers.spi.DeploymentException
   {
      delegate.deploy(deployment);
   }

   @Deprecated
   public void undeploy(Deployment deployment) throws org.jboss.deployers.spi.DeploymentException
   {
      delegate.undeploy(deployment);
   }
   
   private static String getShortName(String name) {
      if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
      name = name.substring(name.lastIndexOf("/") + 1);
      return name;
   }
}
