/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.web.deployers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.deployment.AnnotationMetaDataDeployer;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.javaee.spec.EmptyMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.AbsoluteOrderingMetaData;
import org.jboss.metadata.web.spec.OrderingElementMetaData;
import org.jboss.metadata.web.spec.Web25MetaData;
import org.jboss.metadata.web.spec.Web30MetaData;
import org.jboss.metadata.web.spec.WebCommonMetaData;
import org.jboss.metadata.web.spec.WebFragmentMetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.vfs.VirtualFile;

/**
 * A deployer that merges annotation metadata, xml metadata, and jboss metadata
 * into a merged JBossWebMetaData. It also incorporates ear level overrides from
 * the top level JBossAppMetaData attachment.
 * 
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision: 110198 $
 */
public class MergedJBossWebMetaDataDeployer extends AbstractDeployer
{
   public static final String WEB_ORDER_ATTACHMENT_NAME = "order."+WebMetaData.class.getName();
   public static final String WEB_NOORDER_ATTACHMENT_NAME = "noOrder."+WebMetaData.class.getName();
   public static final String WEB_OVERLAYS_ATTACHMENT_NAME = "overlays."+WebMetaData.class.getName();
   public static final String WEB_SCIS_ATTACHMENT_NAME = "localscis."+WebMetaData.class.getName();

   private boolean metaDataCompleteIsDefault = false;

   public boolean isMetaDataCompleteIsDefault()
   {
      return metaDataCompleteIsDefault;
   }
   public void setMetaDataCompleteIsDefault(boolean metaDataCompleteIsDefault)
   {
      this.metaDataCompleteIsDefault = metaDataCompleteIsDefault;
   }

   /**
    * Create a new MergedJBossWebMetaDataDeployer.
    */
   public MergedJBossWebMetaDataDeployer()
   {
      setStage(DeploymentStages.POST_CLASSLOADER);
      // web.xml metadata
      addInput(WebMetaData.class);
      // jboss.xml metadata
      addInput(JBossWebMetaData.class);
      // annotated metadata view
      addInput(AnnotationMetaDataDeployer.WEB_ANNOTATED_ATTACHMENT_NAME);
      // Output is the merge JBossWebMetaData view
      setOutput(JBossWebMetaData.class);
      // Additional Webapp components
      addOutput(WEB_ORDER_ATTACHMENT_NAME);
      addOutput(WEB_OVERLAYS_ATTACHMENT_NAME);
      addOutput(WEB_SCIS_ATTACHMENT_NAME);
   }

   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      WebMetaData specMetaData = unit.getAttachment(WebMetaData.class);
      JBossWebMetaData metaData = unit.getAttachment(JBossWebMetaData.class);
      if(specMetaData == null && metaData == null)
         return;

      // Check metadata-complete (see AnnotationMetaDataDeployer)
      boolean isComplete = this.isMetaDataCompleteIsDefault();
      if(specMetaData != null)
      {
         if (specMetaData instanceof Web25MetaData)
         {
            isComplete |= ((Web25MetaData)specMetaData).isMetadataComplete();
         }
         else if (specMetaData instanceof Web30MetaData)
         {
            isComplete |= ((Web30MetaData)specMetaData).isMetadataComplete();
         }
         else
         {
            // Any web.xml 2.4 or earlier deployment is metadata complete
            isComplete = true;
         }
      }

      // Find all fragments that have been processed by deployers, and place them in a map keyed by location
      LinkedList<String> order = new LinkedList<String>();
      List<WebOrdering> orderings = new ArrayList<WebOrdering>();
      HashSet<String> jarsSet = new HashSet<String>();
      Set<VirtualFile> overlays = new HashSet<VirtualFile>();
      Map<String, VirtualFile> scis = new HashMap<String, VirtualFile>();
      VirtualFile webInfLib = null;
      boolean fragmentFound = false;
      HashMap<String, WebFragmentMetaData> webFragments = new HashMap<String, WebFragmentMetaData>();
      if (unit instanceof VFSDeploymentUnit)
      {
         VFSDeploymentUnit vfsUnit = (VFSDeploymentUnit) unit;
         webInfLib = vfsUnit.getFile("WEB-INF/lib");
         if (webInfLib != null)
         {
            List<VirtualFile> jars = webInfLib.getChildren();
            for (VirtualFile jar : jars)
            {
               jarsSet.add(jar.getName());
               // Find overlays
               VirtualFile overlay = jar.getChild("META-INF/resources");
               if (overlay.exists())
               {
                  overlays.add(overlay);
               }
               // Find ServletContainerInitializer services
               VirtualFile sci = jar.getChild("META-INF/services/javax.servlet.ServletContainerInitializer");
               if (sci.exists())
               {
                  scis.put(jar.getName(), sci);
               }
            }
         }

         if (!isComplete)
         {
            
            String base = unit.getName();
            int pos = base.indexOf(':');
            if (pos > 0)
            {
               base = base.substring(pos);
            }

            Iterator<String> attachementNames = unit.getAttachments().keySet().iterator();
            HashSet<String> jarsWithoutFragmentsSet = new HashSet<String>();
            jarsWithoutFragmentsSet.addAll(jarsSet);
            while (attachementNames.hasNext())
            {
               String location = attachementNames.next();
               Object attachement = unit.getAttachment(location);
               if (attachement != null && attachement instanceof WebFragmentMetaData)
               {
                  if (!location.startsWith(WebFragmentMetaData.class.getName() + ":"))
                  {
                     // If there is only one fragment, it will also get mapped as this attachement
                     continue;
                  }
                  String relativeLocation = "/" + location.substring(WebFragmentMetaData.class.getName().length() + 1);
                  String jarName = null;
                  if (relativeLocation.startsWith("/WEB-INF/lib/"))
                  {
                     jarName = relativeLocation.substring("/WEB-INF/lib/".length());
                     pos = jarName.indexOf('/');
                     if (pos > 0)
                        jarName = jarName.substring(0, pos);
                  }
                  if (jarName == null)
                  {
                     continue;
                  }
                  fragmentFound = true;
                  WebFragmentMetaData fragmentMetaData = (WebFragmentMetaData) attachement;
                  webFragments.put(jarName, fragmentMetaData);
                  WebOrdering webOrdering = new WebOrdering();
                  webOrdering.setName(fragmentMetaData.getName());
                  webOrdering.setJar(jarName);
                  jarsWithoutFragmentsSet.remove(jarName);
                  if (fragmentMetaData.getOrdering() != null)
                  {
                     if (fragmentMetaData.getOrdering().getAfter() != null)
                     {
                        for (OrderingElementMetaData orderingElementMetaData : 
                           fragmentMetaData.getOrdering().getAfter().getOrdering())
                        {
                           if (orderingElementMetaData.isOthers())
                           {
                              webOrdering.setAfterOthers(true);
                           }
                           else
                           {
                              webOrdering.addAfter(orderingElementMetaData.getName());
                           }
                        }
                     }
                     if (fragmentMetaData.getOrdering().getBefore() != null)
                     {
                        for (OrderingElementMetaData orderingElementMetaData : 
                           fragmentMetaData.getOrdering().getBefore().getOrdering())
                        {
                           if (orderingElementMetaData.isOthers())
                           {
                              webOrdering.setBeforeOthers(true);
                           }
                           else
                           {
                              webOrdering.addBefore(orderingElementMetaData.getName());
                           }
                        }
                     }
                  }
                  orderings.add(webOrdering);
               }
            }
            // If there is no fragment, still consider it for ordering as a
            // fragment specifying no name and no order
            for (String jarName : jarsWithoutFragmentsSet)
            {
               WebOrdering ordering = new WebOrdering();
               ordering.setJar(jarName);
               orderings.add(ordering);
            }

         }
      }
      
      if (!fragmentFound)
      {
         // Drop the order as there is no fragment in the webapp
         orderings.clear();
      }

      // Generate web fragments parsing order
      AbsoluteOrderingMetaData absoluteOrderingMetaData = null;
      if (!isComplete && specMetaData instanceof Web30MetaData)
      {
         absoluteOrderingMetaData = ((Web30MetaData) specMetaData).getAbsoluteOrdering();
      }
      if (absoluteOrderingMetaData != null) {
         // Absolute ordering from web.xml, any relative fragment ordering is ignored
         int otherPos = -1;
         int i = 0;
         for (OrderingElementMetaData orderingElementMetaData : absoluteOrderingMetaData.getOrdering())
         {
            if (orderingElementMetaData.isOthers())
            {
               if (otherPos >= 0) {
                  throw new DeploymentException("Duplicate others in absolute ordering"); 
               }
               otherPos = i;
            }
            else
            {
               for (WebOrdering ordering : orderings)
               {
                  if (orderingElementMetaData.getName().equals(ordering.getName())) {
                     order.add(ordering.getJar());
                     jarsSet.remove(ordering.getJar());
                     break;
                  }
               }
            }
            i++;
         }
         if (otherPos >= 0)
         {
            order.addAll(otherPos, jarsSet);
            jarsSet.clear();
         }
      }
      else if (orderings.size() > 0)
      {
         // Resolve relative ordering
         try
         {
            resolveOrder(orderings, order);
         }
         catch (IllegalStateException e)
         {
            DeploymentException.rethrowAsDeploymentException("Invalid ordering", e);
         }
         jarsSet.clear();
      }
      else
      {
         // No order specified
         order.addAll(jarsSet);
         jarsSet.clear();
         unit.addAttachment(WEB_NOORDER_ATTACHMENT_NAME, Boolean.TRUE);
      }

      if (log.isDebugEnabled())
      {
         StringBuilder builder = new StringBuilder();
         builder.append("Resolved order: [ ");
         for (String jar : order)
         {
            builder.append(jar).append(' ');
         }
         builder.append(']');
         log.debug(builder.toString());
      }
      
      unit.addAttachment(WEB_ORDER_ATTACHMENT_NAME, order);
      unit.addAttachment(WEB_OVERLAYS_ATTACHMENT_NAME, overlays);
      unit.addAttachment(WEB_SCIS_ATTACHMENT_NAME, scis);
      
      // The fragments and corresponding annotations will need to be merged in order
      // For each JAR in the order: 
      // - Merge the annotation metadata into the fragment meta data 
      //   (unless the fragment exists and is meta data complete)
      // - Merge the fragment metadata into merged fragment meta data
      WebCommonMetaData mergedFragmentMetaData = new WebCommonMetaData();
      if (specMetaData == null)
      {
         // If there is no web.xml, it has to be considered to be the latest version
         specMetaData = new Web30MetaData();
         specMetaData.setVersion("3.0");
      }
      String key = AnnotationMetaDataDeployer.WEB_ANNOTATED_ATTACHMENT_NAME + ":classes";
      // Augment with meta data from annotations in /WEB-INF/classes
      WebMetaData classesAnnotatedMetaData = unit.getAttachment(key, WebMetaData.class);
      if (classesAnnotatedMetaData != null)
      {
         if (isComplete)
         {
            // Discard @WebFilter, @WebListener and @WebServlet
            classesAnnotatedMetaData.setFilters(null);
            classesAnnotatedMetaData.setFilterMappings(null);
            classesAnnotatedMetaData.setListeners(null);
            classesAnnotatedMetaData.setServlets(null);
            classesAnnotatedMetaData.setServletMappings(null);
         }
         specMetaData.augment(classesAnnotatedMetaData, null, true);
      }
      // Augment with meta data from fragments and annotations from the corresponding JAR
      for (String jar : order)
      {
         WebFragmentMetaData webFragmentMetaData = webFragments.get(jar);
         if (webFragmentMetaData == null)
         {
            webFragmentMetaData = new WebFragmentMetaData();
            // Add non overriding default distributable flag
            webFragmentMetaData.setDistributable(new EmptyMetaData());
         }
         key = AnnotationMetaDataDeployer.WEB_ANNOTATED_ATTACHMENT_NAME + ":" + jar;
         WebMetaData jarAnnotatedMetaData = unit.getAttachment(key, WebMetaData.class);
         if ((isComplete || webFragmentMetaData.isMetadataComplete()) && jarAnnotatedMetaData != null)
         {
            // Discard @WebFilter, @WebListener and @WebServlet
            jarAnnotatedMetaData.setFilters(null);
            jarAnnotatedMetaData.setFilterMappings(null);
            jarAnnotatedMetaData.setListeners(null);
            jarAnnotatedMetaData.setServlets(null);
            jarAnnotatedMetaData.setServletMappings(null);
         }
         if (jarAnnotatedMetaData != null)
         {
            // Merge annotations corresponding to the JAR
            webFragmentMetaData.augment(jarAnnotatedMetaData, null, true);
         }
         // Merge fragment meta data according to the conflict rules
         try
         {
            mergedFragmentMetaData.augment(webFragmentMetaData, specMetaData, false);
         }
         catch (Exception e)
         {
            DeploymentException.rethrowAsDeploymentException("Deployment error processing fragment for JAR: " + jar, e);
         }
      }
      // Augment with meta data from annotations from JARs excluded from the order
      for (String jar : jarsSet)
      {
         WebFragmentMetaData webFragmentMetaData = new WebFragmentMetaData();
         // Add non overriding default distributable flag
         webFragmentMetaData.setDistributable(new EmptyMetaData());
         key = AnnotationMetaDataDeployer.WEB_ANNOTATED_ATTACHMENT_NAME + ":" + jar;
         WebMetaData jarAnnotatedMetaData = unit.getAttachment(key, WebMetaData.class);
         if (jarAnnotatedMetaData != null)
         {
            // Discard @WebFilter, @WebListener and @WebServlet
            jarAnnotatedMetaData.setFilters(null);
            jarAnnotatedMetaData.setFilterMappings(null);
            jarAnnotatedMetaData.setListeners(null);
            jarAnnotatedMetaData.setServlets(null);
            jarAnnotatedMetaData.setServletMappings(null);
         }
         if (jarAnnotatedMetaData != null)
         {
            // Merge annotations corresponding to the JAR
            webFragmentMetaData.augment(jarAnnotatedMetaData, null, true);
         }
         // Merge fragment meta data according to the conflict rules
         try
         {
            mergedFragmentMetaData.augment(webFragmentMetaData, specMetaData, false);
         }
         catch (Exception e)
         {
            DeploymentException.rethrowAsDeploymentException("Deployment error processing fragment for JAR: " + jar, e);
         }
      }
      specMetaData.augment(mergedFragmentMetaData, null, true);

      // Override with meta data (JBossWebMetaData)
      // Create a merged view
      JBossWebMetaData mergedMetaData = new JBossWebMetaData();
      mergedMetaData.merge(metaData, specMetaData);
      // Incorporate any ear level overrides
      DeploymentUnit topUnit = unit.getTopLevel();
      if(topUnit != null && topUnit.getAttachment(JBossAppMetaData.class) != null)
      {
         JBossAppMetaData earMetaData = topUnit.getAttachment(JBossAppMetaData.class);
         // Security domain
         String securityDomain = earMetaData.getSecurityDomain();
         if(securityDomain != null && mergedMetaData.getSecurityDomain() == null)
            mergedMetaData.setSecurityDomain(securityDomain);
         //Security Roles
         SecurityRolesMetaData earSecurityRolesMetaData = earMetaData.getSecurityRoles();
         if(earSecurityRolesMetaData != null)
         {
            SecurityRolesMetaData mergedSecurityRolesMetaData = mergedMetaData.getSecurityRoles(); 
            if(mergedSecurityRolesMetaData == null)
               mergedMetaData.setSecurityRoles(earSecurityRolesMetaData);
            
            //perform a merge to rebuild the principalVersusRolesMap
            if(mergedSecurityRolesMetaData != null )
            {
                mergedSecurityRolesMetaData.merge(mergedSecurityRolesMetaData, 
                     earSecurityRolesMetaData);
            }
        }
      }

      // Output the merged JBossWebMetaData
      unit.getTransientManagedObjects().addAttachment(JBossWebMetaData.class, mergedMetaData);
   }

   
   /**
    * Utility class to associate the logical name with the JAR name, needed during the
    * order resolving.
    * @author remm
    */
   protected class WebOrdering implements Serializable {

      private static final long serialVersionUID = 5603203103871892211L;

      protected String jar = null;
      protected String name = null;
      protected List<String> after = new ArrayList<String>();
      protected List<String> before = new ArrayList<String>();
      protected boolean afterOthers = false;
      protected boolean beforeOthers = false;
      
      public String getName() {
          return name;
      }
      
      public void setName(String name) {
          this.name = name;
      }

      public List<String> getAfter() {
          return after;
      }

      public void addAfter(String name) {
          after.add(name);
      }

      public List<String> getBefore() {
          return before;
      }

      public void addBefore(String name) {
          before.add(name);
      }

      public String getJar() {
          return jar;
      }
      
      public void setJar(String jar) {
          this.jar = jar;
      }
      
      public boolean isAfterOthers() {
          return afterOthers;
      }
      
      public void setAfterOthers(boolean afterOthers) {
          this.afterOthers = afterOthers;
      }
      
      public boolean isBeforeOthers() {
          return beforeOthers;
      }
      
      public void setBeforeOthers(boolean beforeOthers) {
          this.beforeOthers = beforeOthers;
      }

  }

   protected static class Ordering {
      protected WebOrdering ordering;
      protected Set<Ordering> after = new HashSet<Ordering>();
      protected Set<Ordering> before = new HashSet<Ordering>();
      protected boolean afterOthers = false;
      protected boolean beforeOthers = false;
      
      public boolean addAfter(Ordering ordering) {
          return after.add(ordering);
      }
      
      public boolean addBefore(Ordering ordering) {
          return before.add(ordering);
      }
      
      public void validate() {
          isBefore(new Ordering());
          isAfter(new Ordering());
      }
      
      /**
       * Check (recursively) if a fragment is before the specified fragment.
       * 
       * @param ordering
       * @return
       */
      public boolean isBefore(Ordering ordering) {
          return isBeforeInternal(ordering, new HashSet<Ordering>());
      }
      
      protected boolean isBeforeInternal(Ordering ordering, Set<Ordering> checked) {
          checked.add(this);
          if (before.contains(ordering)) {
              return true;
          }
          Iterator<Ordering> beforeIterator = before.iterator();
          while (beforeIterator.hasNext()) {
              Ordering check = beforeIterator.next();
              if (checked.contains(check)) {
                  //throw new IllegalStateException(sm.getString("ordering.orderConflict", this.ordering.getJar()));
                  throw new IllegalStateException("Ordering conflict with JAR: " + this.ordering.getJar());
              }
              if (check.isBeforeInternal(ordering, checked)) {
                  return false;
              }
          }
          return false;
      }
      
      /**
       * Check (recursively) if a fragment is after the specified fragment.
       * 
       * @param ordering
       * @return
       */
      public boolean isAfter(Ordering ordering) {
          return isAfterInternal(ordering, new HashSet<Ordering>());
      }
      
      protected boolean isAfterInternal(Ordering ordering, Set<Ordering> checked) {
          checked.add(this);
          if (after.contains(ordering)) {
              return true;
          }
          Iterator<Ordering> afterIterator = after.iterator();
          while (afterIterator.hasNext()) {
              Ordering check = afterIterator.next();
              if (checked.contains(check)) {
                  //throw new IllegalStateException(sm.getString("ordering.orderConflict", this.ordering.getJar()));
                 throw new IllegalStateException("Ordering conflict with JAR: " + this.ordering.getJar());
              }
              if (check.isAfterInternal(ordering, checked)) {
                  return false;
              }
          }
          return false;
      }
      
      /**
       * Check is a fragment marked as before others is after a fragment that is not.
       * 
       * @return true if a fragment marked as before others is after a fragment that is not
       */
      public boolean isLastBeforeOthers() {
          if (!beforeOthers) {
              throw new IllegalStateException();
          }
          Iterator<Ordering> beforeIterator = before.iterator();
          while (beforeIterator.hasNext()) {
              Ordering check = beforeIterator.next();
              if (!check.beforeOthers) {
                  return true;
              } else if (check.isLastBeforeOthers()) {
                  return true;
              }
          }
          return false;
      }

      /**
       * Check is a fragment marked as after others is before a fragment that is not.
       * 
       * @return true if a fragment marked as after others is before a fragment that is not
       */
      public boolean isFirstAfterOthers() {
          if (!afterOthers) {
              throw new IllegalStateException();
          }
          Iterator<Ordering> afterIterator = after.iterator();
          while (afterIterator.hasNext()) {
              Ordering check = afterIterator.next();
              if (!check.afterOthers) {
                  return true;
              } else if (check.isFirstAfterOthers()) {
                  return true;
              }
          }
          return false;
      }
      
  }

  /**
   * Generate the Jar processing order.
   * 
   * @param webOrderings The list of orderings, as parsed from the fragments
   * @param order The generated order list
   */
  protected static void resolveOrder(List<WebOrdering> webOrderings, List<String> order) {
      List<Ordering> work = new ArrayList<Ordering>();
      
      // Populate the work Ordering list
      Iterator<WebOrdering> webOrderingsIterator = webOrderings.iterator();
      while (webOrderingsIterator.hasNext()) {
          WebOrdering webOrdering = webOrderingsIterator.next();
          Ordering ordering = new Ordering();
          ordering.ordering = webOrdering;
          ordering.afterOthers = webOrdering.isAfterOthers();
          ordering.beforeOthers = webOrdering.isBeforeOthers();
          if (ordering.afterOthers && ordering.beforeOthers) {
              // Cannot be both after and before others
              //throw new IllegalStateException(sm.getString("ordering.afterAndBeforeOthers", webOrdering.getJar()));
              throw new IllegalStateException("Ordering includes both before and after others in JAR: " + webOrdering.getJar());
          }
          work.add(ordering);
      }
      
      // Create double linked relationships between the orderings,
      // and resolve names
      Iterator<Ordering> workIterator = work.iterator();
      while (workIterator.hasNext()) {
          Ordering ordering = workIterator.next();
          WebOrdering webOrdering = ordering.ordering;
          Iterator<String> after = webOrdering.getAfter().iterator();
          while (after.hasNext()) {
              String name = after.next();
              Iterator<Ordering> workIterator2 = work.iterator();
              boolean found = false;
              while (workIterator2.hasNext()) {
                  Ordering ordering2 = workIterator2.next();
                  if (name.equals(ordering2.ordering.getName())) {
                      if (found) {
                          // Duplicate name
                          //throw new IllegalStateException(sm.getString("ordering.duplicateName", webOrdering.getJar()));
                         throw new IllegalStateException("Duplicate name declared in JAR: " + webOrdering.getJar());
                      }
                      ordering.addAfter(ordering2);
                      ordering2.addBefore(ordering);
                      found = true;
                  }
              }
          }
          Iterator<String> before = webOrdering.getBefore().iterator();
          while (before.hasNext()) {
              String name = before.next();
              Iterator<Ordering> workIterator2 = work.iterator();
              boolean found = false;
              while (workIterator2.hasNext()) {
                  Ordering ordering2 = workIterator2.next();
                  if (name.equals(ordering2.ordering.getName())) {
                      if (found) {
                          // Duplicate name
                          //throw new IllegalStateException(sm.getString("ordering.duplicateName", webOrdering.getJar()));
                         throw new IllegalStateException("Duplicate name declared in JAR: " + webOrdering.getJar());
                      }
                      ordering.addBefore(ordering2);
                      ordering2.addAfter(ordering);
                      found = true;
                  }
              }
          }
      }
      
      // Validate ordering
      workIterator = work.iterator();
      while (workIterator.hasNext()) {
          workIterator.next().validate();
      }
      
      // Create three ordered lists that will then be merged
      List<Ordering> tempOrder = new ArrayList<Ordering>();

      // Create the ordered list of fragments which are before others
      workIterator = work.iterator();
      while (workIterator.hasNext()) {
          Ordering ordering = workIterator.next();
          if (ordering.beforeOthers) {
              // Insert at the first possible position
              int insertAfter = -1;
              boolean last = ordering.isLastBeforeOthers();
              int lastBeforeOthers = -1;
              for (int i = 0; i < tempOrder.size(); i++) {
                  if (ordering.isAfter(tempOrder.get(i))) {
                      insertAfter = i;
                  }
                  if (tempOrder.get(i).beforeOthers) {
                      lastBeforeOthers = i;
                  }
              }
              int pos = insertAfter;
              if (last && lastBeforeOthers > insertAfter) {
                  pos = lastBeforeOthers;
              }
              tempOrder.add(pos + 1, ordering);
          } else if (ordering.afterOthers) {
              // Insert at the last possible element
              int insertBefore = tempOrder.size();
              boolean first = ordering.isFirstAfterOthers();
              int firstAfterOthers = tempOrder.size();
              for (int i = tempOrder.size() - 1; i >= 0; i--) {
                  if (ordering.isBefore(tempOrder.get(i))) {
                      insertBefore = i;
                  }
                  if (tempOrder.get(i).afterOthers) {
                      firstAfterOthers = i;
                  }
              }
              int pos = insertBefore;
              if (first && firstAfterOthers < insertBefore) {
                  pos = firstAfterOthers;
              }
              tempOrder.add(pos, ordering);
          } else {
              // Insert according to other already inserted elements
              int insertAfter = -1;
              int insertBefore = tempOrder.size();
              for (int i = 0; i < tempOrder.size(); i++) {
                  if (ordering.isAfter(tempOrder.get(i)) || tempOrder.get(i).beforeOthers) {
                      insertAfter = i;
                  }
                  if (ordering.isBefore(tempOrder.get(i)) || tempOrder.get(i).afterOthers) {
                      insertBefore = i;
                  }
              }
              if (insertAfter > insertBefore) {
                  // Conflicting order (probably caught earlier)
                  //throw new IllegalStateException(sm.getString("ordering.orderConflict", ordering.ordering.getJar()));
                 throw new IllegalStateException("Fragment ordering conflict with JAR: " + ordering.ordering.getJar());
              }
              // Insert somewhere in the range
              tempOrder.add(insertAfter + 1, ordering);
          }
      }
      
      // Create the final ordered list
      Iterator<Ordering> tempOrderIterator = tempOrder.iterator();
      while (tempOrderIterator.hasNext()) {
          Ordering ordering = tempOrderIterator.next();
          order.add(ordering.ordering.getJar());
      }
      
  }
   
}
