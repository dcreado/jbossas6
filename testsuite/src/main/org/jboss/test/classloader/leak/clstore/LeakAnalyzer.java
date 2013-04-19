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

package org.jboss.test.classloader.leak.clstore;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.jboss.logging.Logger;
import org.jboss.profiler.jvmti.JVMTICallBack;
import org.jboss.profiler.jvmti.JVMTIInterface;
import org.jboss.profiler.jvmti.ReferenceDataPoint;

/**
 * A LeakAnalyzer.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 101719 $
 */
public class LeakAnalyzer extends JVMTIInterface
{
   private static final Logger log = Logger.getLogger(LeakAnalyzer.class);

   private static final String SEP = System.getProperty("line.separator");
   private Object root;
   /**
    * Create a new LeakAnalyzer.
    * 
    */
   public LeakAnalyzer()
   {
      super();
   }

   public boolean isActive()
   {
      // System.loadLibrary fails if it is called twice, which it will
      // be if this class is redeployed. So, the first time we get a positive
      // result, store it in a system property, and thereafter return
      // the system property
      String existing = System.getProperty("jboss.test.jbossAgent.avail");
      if (existing != null)
         return Boolean.parseBoolean(existing);
      
      boolean active = super.isActive();
      System.setProperty("jboss.test.jbossAgent.avail", Boolean.toString(active));
      return active;
   }



   /**
    * Show the reference holders tree of an object. This returns a report you
    * can visualize through MBean.
    */
   public String exploreObjectReferences(HashMap<Long, List<ReferenceDataPoint>> referencesMap, Object thatObject, int maxLevel, boolean useToString,
         boolean condensed)
   {
      this.root = thatObject;
      
      ReferenceReportNode root = new ReferenceReportNode(callToString(thatObject, useToString));

      Set<ReferenceReportNode> prunableLeaves = new HashSet<ReferenceReportNode>();

      StringBuilder sb = new StringBuilder();

      try
      {
         exploreObject(root, thatObject, 0, maxLevel, useToString, false, referencesMap, new HashSet<String>(), prunableLeaves);

         for (ReferenceReportNode nonCrit : prunableLeaves)
         {
            nonCrit.markNonCritical();
            if (condensed)
               nonCrit.removeBranch();
         }

         writeReport(root, 0, condensed, sb);
      }
      catch (Exception e)
      {
         CharArrayWriter charArray = new CharArrayWriter();
         PrintWriter out = new PrintWriter(charArray);
         e.printStackTrace(out);
         sb = new StringBuilder(charArray.toString());
      }

      return sb.toString();
   }
   
   
   public void notifyOnReferences(String temporaryFile, JVMTICallBack callback)
   {
      // We override the superclass version to pass 'true' for notifyOnClasses
      notifyInventory(true,temporaryFile,null,callback);
   }

   /** Explore references recursively */
   private void exploreObject(ReferenceReportNode node, Object source, int currentLevel, final int maxLevel,
         boolean useToString, boolean weakAndSoft, Map<Long, List<ReferenceDataPoint>> mapDataPoints, Set<String> alreadyExplored, Set<ReferenceReportNode> prunableLeaves)
   {
      if (maxLevel >= 0 && currentLevel >= maxLevel)
      {
         String msg = node.getMessage() == null ? "" : node.getMessage() + " -- ";
         node.setMessage(msg + "<i>MaxLevel</i>");
         return;
      }

      String index = source.getClass().getName() + "@" + System.identityHashCode(source);

      if (alreadyExplored.contains(index))
      {
         String message = node.getMessage() == null ? "" : node.getMessage() + " -- ";
         message += " object " + index + " was already described before on this report";

         node.setMessage(message);
         prunableLeaves.add(node);
         return;
      }

      alreadyExplored.add(index);

//      log.info("resolving references of " + callToString(source, useToString) + "...");
      log.info(currentLevel + " -- resolving references of " + source.getClass());
      Long sourceTag = new Long(this.getTagOnObject(source));
      List<ReferenceDataPoint> listPoints = mapDataPoints.get(sourceTag);
      if (listPoints == null)
      {
         log.debug("didn't find references");
         return;
      }

      log.debug("References found");

      for (ReferenceDataPoint point : listPoints)
      {         
         ReferenceReportNode child = new ReferenceReportNode();
         
         Object nextReference = treatReference(child, point, useToString, weakAndSoft, prunableLeaves);
         
         if (nextReference != null)
         {
            exploreObject(child, nextReference, currentLevel + 1, maxLevel, useToString, weakAndSoft, mapDataPoints,
                  alreadyExplored, prunableLeaves);
         } 
         
         if (child.getMessage() != null || child.getChildren().size() > 0)
            node.addChild(child);            
      }

   }

   private static void writeReport(ReferenceReportNode node, int level, boolean condensed, StringBuilder sb)
   {
      sb.append("<br>");
      writeLevel(level, sb);
      if (condensed || node.isCritical())
      {
         sb.append("<b>");
         boolean leaf = node.isLeaf();
         if (leaf)
         {
            sb.append("<font color=\"red\">");
         }         
         sb.append(node.getMessage());  
         if (leaf)
         {
            sb.append("</font>");
         }
         sb.append("</b>");
      }
      else
      {
         sb.append(node.getMessage());
      }
      
      sb.append(SEP);

      for (ReferenceReportNode child : node.getChildren())
      {
         writeReport(child, level + 1, condensed, sb);
      }
   }

   private static String callToString(Object obj, boolean callToString)
   {
      try
      {
         if (obj == null)
         {
            return "null";
         }
         else
         {
            String base = obj.getClass().getName() + "@" + System.identityHashCode(obj);
            if (callToString || obj instanceof Class<?>)
            {
               base += "(" + obj.toString() + ")";
            }
            
            return base;
         }

      }
      catch (Throwable e)
      {
         return obj.getClass().getName() + " toString had an Exception ";
      }
   }

   private Object treatReference(ReferenceReportNode node, ReferenceDataPoint point, boolean useToString, boolean weakAndSoft, Set<ReferenceReportNode> prunableLeaves)
   {
      Object referenceHolder = null;
      if (point.getReferenceHolder() == 0 || point.getReferenceHolder() == -1)
      {
         referenceHolder = null;
      }
      else
      {
         referenceHolder = this.getObjectOnTag(point.getReferenceHolder());
      }

      Object nextReference = null;
      StringBuilder sb = new StringBuilder();

      switch (point.getReferenceType())
      {
         case JVMTICallBack.JVMTI_REFERENCE_CLASS :
            // Reference from an object to its class.
            sb.append("InstanceOfReference: ");
            sb.append(callToString(referenceHolder, useToString));

            nextReference = referenceHolder;
            break;
         case JVMTICallBack.JVMTI_REFERENCE_FIELD :
            // Reference from an objectb to the value of one of its
            // instance fields. For references of this kind
            // the referrer_index parameter to the
            // jvmtiObjectReferenceCallback is the index of the the
            // instance field. The index is based on the order of
            // all the object's fields. This includes all fields
            // of the directly declared static and instance fields
            // in the class, and includes all fields (both public
            // and private) fields declared in superclasses
            // and superinterfaces. The index is thus calculated
            // by summing the index of field in the directly
            // declared class (see GetClassFields), with the
            // total number of fields (both public and private)
            // declared in all superclasses and superinterfaces. 
            // The index starts at zero.
         {

            String fieldName = null;

            if (referenceHolder == null)
            {
               fieldName = "Reference GONE";
            }
            else
            {
               Class<?> clazz = referenceHolder.getClass();
               Field field = this.getObjectField(clazz, (int) point.getIndex());
               if (field == null)
               {
                  fieldName = "UndefinedField@" + referenceHolder;
               }
               else
               {
                  fieldName = field.toString();
               }
            }
            sb.append("FieldReference ").append(fieldName).append("=").append(callToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         }
         case JVMTICallBack.JVMTI_REFERENCE_ARRAY_ELEMENT :
            // Reference from an array to one of its elements. For
            // references of this kind the referrer_index parameter to the
            // jvmtiObjectReferenceCallback is the array index.
            
            if (referenceHolder == null)
            {
               sb.append("arrayRef Position " + point.getIndex() + " is gone");
            }
            else
            {
               sb.append("arrayRef " + referenceHolder.getClass().getName() + "[" + point.getIndex()
                     + "] id=@" + System.identityHashCode(referenceHolder));
            }
            nextReference = referenceHolder;
            break;
         case JVMTICallBack.JVMTI_REFERENCE_CLASS_LOADER :
            // Reference from a class to its class loader.            
            sb.append("ClassLoaderReference @ " + callToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         case JVMTICallBack.JVMTI_REFERENCE_SIGNERS :
            // Reference from a class to its signers array.
            sb.append("ReferenceSigner@" + callToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         case JVMTICallBack.JVMTI_REFERENCE_PROTECTION_DOMAIN :
            // Reference from a class to its protection domain.
            sb.append("ProtectionDomain@" + callToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         case JVMTICallBack.JVMTI_REFERENCE_INTERFACE :
            // Reference from a class to one of its interfaces.
            sb.append("ReferenceInterface@" + callToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         case JVMTICallBack.JVMTI_REFERENCE_STATIC_FIELD :// Reference from a
                                                            // class to the
                                                            // value of one of
                                                            // its static
                                                            // fields. For
                                                            // references of
                                                            // this kind the
                                                            // referrer_index
                                                            // parameter to the
                                                            // jvmtiObjectReferenceCallback
                                                            // is the index of
                                                            // the static field.
                                                            // The index is
                                                            // based on the
                                                            // order of the
                                                            // directly declared
                                                            // static and
                                                            // instance fields
                                                            // in the class (not
                                                            // inherited
                                                            // fields), starting
                                                            // at zero. See
                                                            // GetClassFields.
         {
            @SuppressWarnings("unchecked")
            Class<?> clazz = (Class) referenceHolder;
            Field field = null;
            String fieldName = null;
            try
            {
               field = this.getObjectField(clazz, (int) point.getIndex());
               if (field == null)
               {
                  fieldName = "UndefinedField@" + referenceHolder;
               }
               else
               {
                  fieldName = field.toString();
               }
            }
            catch (Exception e)
            {
               log.warn("Cannot analyze name of static field in " + clazz + "at point " + point.getIndex(), e);
            }
            catch (NoClassDefFoundError e)
            {
               log.warn("Cannot analyze name of static field in " + clazz + "at point " + point.getIndex(), e);
            }
            
            if (fieldName == null)
            {
               fieldName = "UnanalyzableField@" + referenceHolder;                              
            }
            sb.append("StaticFieldReference " + fieldName);
            
            // A static field reference is either a leak or a cycle, depending
            // on whether the field's class was loaded by our root CL Check for this
            // and cut short the analysis of this tree by simply recording
            // its ClassLoader
            ClassLoader cl = clazz.getClassLoader();
            if (root.equals(cl))
            {
               prunableLeaves.add(node);
            }
            else
            {
               log.info("Found a static reference leak to class " + clazz);
               ReferenceReportNode child = new ReferenceReportNode();
               child.setMessage("ClassLoaderReference @ " + callToString(cl, useToString));
               node.addChild(child);
            }
            nextReference = null;
            break;
         }
         case JVMTICallBack.JVMTI_REFERENCE_CONSTANT_POOL :
            // Reference from a class to a resolved entry in
            // the constant pool. For references of this kind the
            // referrer_index parameter to the jvmtiObjectReferenceCallback
            // is the index into constant pool table of the class, starting
            // at 1. See The Constant Pool in the Java Virtual Machine
            // Specification.
            sb.append(" ReferenceInterface@" + callToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         case JVMTICallBack.ROOT_REFERENCE :
            sb.append("Root");
            nextReference = null;
            break;
         case JVMTICallBack.THREAD_REFERENCE :

            Class<?> methodClass = this.getMethodClass(point.getMethod());
            if (methodClass != null)
            {
               String className = null;
               if (methodClass != null)
               {
                  className = methodClass.getName();
               }

               Thread.yield();

               // this is weird but without this sleep here, the JVM crashes.
               /*
                * try { Thread.sleep(10); } catch (InterruptedException e) {
                * e.printStackTrace(); }
                */

               String methodName = this.getMethodName(point.getMethod());
               sb.append("Reference inside a method - " + className + "::" + methodName);
               if (methodClass.isAssignableFrom(getClass()))
               {
                  prunableLeaves.add(node);
               }
            }
            nextReference = null;
            break;
         default :
            log.warn("unexpected reference " + point);
      }

      if (sb.length() > 0)
         node.setMessage(sb.toString());

      if (nextReference != null && !weakAndSoft)
      {
         if (nextReference instanceof Reference<?>)
         {
            // WeakHashMap$Entry and ThreadLocal$ThreadLocalMap$Entry are
            // special cases, where the Entry key is a weak ref, but the 
            // value is strong. We don't want to ignore similar cases. So
            // only mark as prunable if the ref is the standard
            // java.lang.ref.Referent.referent -- all others are potential
            // strong references
            // We also don't stop at SoftReferences, since if they survive
            // our attempts at flushing them out, we want to know about them
            String msg = node.getMessage();
            if (msg.indexOf("FieldReference private java.lang.Object java.lang.ref.Reference.referent=") >= 0
                  && !(nextReference instanceof SoftReference<?>))
            {                  
               if (nextReference instanceof Map.Entry<?,?>)
               {
                  // WeakHashMap$Entry is suspicious. 
                  // Put in some more info about the entry
                  @SuppressWarnings("unchecked")
                  Map.Entry entry = (Entry) nextReference;
                  Object key = entry.getKey();
                  msg += " KEY=" + (key == null ? " null" : key.getClass().getName() + "@" + System.identityHashCode(key));
                  Object val= entry.getValue();
                  msg += " VALUE=" + (val == null ? " null" : val.getClass().getName() + "@" + System.identityHashCode(val));
                  node.setMessage(msg);
               }
               
               prunableLeaves.add(node);                  
               nextReference = null;
            }
            else if (msg.indexOf("java.lang.ThreadLocal$ThreadLocalMap$Entry") >= 0)
            {
               // Get the key and follow that to see why it isn't released
               nextReference = ((Reference<?>) nextReference).get();
            }
            // else just keep going   
            
         }
      }

      return nextReference;
   }
   
   private static void writeLevel(int level, StringBuilder sb)
   {
      for (int i = 0; i <= level; i++)
      {
         sb.append("!--");
      }
   }
}
