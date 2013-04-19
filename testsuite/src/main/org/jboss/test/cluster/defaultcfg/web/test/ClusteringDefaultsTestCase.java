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
package org.jboss.test.cluster.defaultcfg.web.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import junit.framework.Assert;
import junit.framework.Test;

import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.metadata.web.jboss.ReplicationTrigger;
import org.jboss.metadata.web.jboss.SnapshotMode;
import org.jboss.test.JBossClusteredTestCase;
import org.jboss.web.tomcat.service.deployers.ClusteringDefaultsDeployer;

/**
 * @author Paul Ferraro
 *
 */
public class ClusteringDefaultsTestCase extends JBossClusteredTestCase
{
   public static Test suite() throws Exception
   {
      return JBossClusteredTestCase.getDeploySetup(ClusteringDefaultsTestCase.class, "http-default.war");
   }

   public ClusteringDefaultsTestCase(String name)
   {
      super(name);
   }

   public void testDefaults() throws Exception
   {
      ObjectName name = ObjectName.getInstance("jboss.web:type=Manager,host=localhost,path=/http-default");
      
      // JBAS-8540 - this was failing under IPv6 and not IPv4
      // MBeanServerConnection server = this.getServer();
      // we are in a clustered test case; non-clustered system properties may not always be defined  
      MBeanServerConnection server = this.getAdaptors()[0];
      
      String[] names = new String[] {
            "cacheConfigName",
            "useJK",
            "replicationGranularity",
            "replicationTrigger",
            "snapshotMode",
            "snapshotInterval",
            "maxUnreplicatedInterval",
            "passivationEnabled",
            "passivationMaxIdleTime",
            "passivationMinIdleTime"
      };
      
      List<Attribute> attributes = server.getAttributes(name, names).asList();
      Map<String, Attribute> namedAttributes = new HashMap<String, Attribute>();
      for (Attribute attr : attributes)
      {
         namedAttributes.put(attr.getName(), attr);
      }
      
      this.assertCorrectAttribute(namedAttributes, "cacheConfigName", "standard-session-cache");
      this.assertCorrectAttribute(namedAttributes, "useJK", Boolean.TRUE);
      this.assertCorrectAttribute(namedAttributes, "replicationGranularity", ReplicationGranularity.SESSION);
      this.assertCorrectAttribute(namedAttributes, "replicationTrigger", ReplicationTrigger.SET_AND_NON_PRIMITIVE_GET);
      this.assertCorrectAttribute(namedAttributes, "snapshotMode", SnapshotMode.INSTANT);
      this.assertCorrectAttribute(namedAttributes, "snapshotInterval", Integer.valueOf(1000));
      this.assertCorrectAttribute(namedAttributes, "maxUnreplicatedInterval", Integer.valueOf(60));
      this.assertCorrectAttribute(namedAttributes, "passivationEnabled", Boolean.FALSE);
      this.assertCorrectAttribute(namedAttributes, "passivationMaxIdleTime", Long.valueOf(ClusteringDefaultsDeployer.IGNORED));
      this.assertCorrectAttribute(namedAttributes, "passivationMinIdleTime", Long.valueOf(ClusteringDefaultsDeployer.IGNORED));
   }
   
   private void assertCorrectAttribute(Map<String, Attribute> namedAttributes, String name, Object value)
   {
      Attribute attribute = namedAttributes.get(name);
      Assert.assertNotNull("Attribute " + name + " found", attribute);
      Assert.assertEquals(name, attribute.getName());
      Assert.assertEquals(name, value, attribute.getValue());
   }
}
