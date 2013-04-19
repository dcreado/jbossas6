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
package org.jboss.jmx.adaptor.snmp.generator;

import java.util.ArrayList;

import org.jboss.jmx.adaptor.snmp.generator.metrics.MappedAttribute;
import org.jboss.jmx.adaptor.snmp.generator.metrics.AttributeMappings;
import org.jboss.jmx.adaptor.snmp.generator.metrics.ManagedBean;
import org.jboss.xb.binding.ObjectModelFactory;
import org.jboss.xb.binding.UnmarshallingContext;
import org.xml.sax.Attributes;

/**
 * Parse the mapping of JMX mbean attributes to SNMP OIDs
 * 
 *      </mbean>
 * 
 * @author <a href="mailto:tom.hauser@gmail.com>Tom Hauser</a>
 * @version $Revision: 111505 $
 */
public class ParserAttributeBindings implements ObjectModelFactory
{
	
	public Object newRoot(Object root, UnmarshallingContext ctx,
			String namespaceURI, String localName, Attributes attrs)
   {
	   if (!localName.equals("attribute-mappings"))
      {
	      throw new IllegalStateException("Unexpected root " + localName + ". Expected <attribute-mappings>");
		}
	   return new AttributeMappings();
	}

	public Object completeRoot(Object root, UnmarshallingContext ctx, String uri, String name)
   {
	   return root;
	}

	public void setValue(AttributeMappings mappings, UnmarshallingContext navigator,
		      String namespaceUri, String localName, String value)
	{
	}	
	
	public Object newChild(AttributeMappings mappings, UnmarshallingContext navigator,
			String namespaceUri, String localName, Attributes attrs)
	{
		if ("mbean".equals(localName))
      {
			String name = attrs.getValue("name");
			String oidPrefix = attrs.getValue("oid-prefix");
			String oidDefinition = attrs.getValue("definition-name");
			String tableName = attrs.getValue("table-name");
			String desc = attrs.getValue("description");
			String status = attrs.getValue("status");
			ManagedBean child = new ManagedBean();
			child.setName(name);
			child.setOidPrefix(oidPrefix);
			child.setOidDefinition(oidDefinition);
			child.setTableName(tableName);
			child.setDesc(desc);
			child.setStatus(status);
			return child;
		}
		return null;
	}
	
	public void addChild(AttributeMappings mappings, ManagedBean mbean,
			UnmarshallingContext navigator, String namespaceURI, String localName) 
	{
		mappings.addMonitoredMBean(mbean);
	}
	
	public Object newChild(ManagedBean mbean, UnmarshallingContext navigator,
			String namespaceUri, String localName, Attributes attrs)
	{
		
		MappedAttribute attribute = null;
		if ("attribute".equals(localName)) {
			String name = attrs.getValue("name");
			String oid = attrs.getValue("oid");
			String mode = attrs.getValue("mode");
			String snmpType = attrs.getValue("snmp-type");
			String maxAccess = attrs.getValue("max-access");
			String desc = attrs.getValue("description");
			String status = attrs.getValue("status");
			String table = attrs.getValue("table");
			attribute = new MappedAttribute();
            attribute.setName(name);
			attribute.setOid(oid);
			attribute.setMode(mode);
			attribute.setSnmpType(snmpType);
			attribute.setMaxAccess(maxAccess);
			attribute.setSnmpDesc(desc);
			attribute.setStatus(status);
			attribute.setTable(table);
		}
		return attribute;
	}
	
	public void addChild(ManagedBean mbean, MappedAttribute attribute,
			UnmarshallingContext navigator, String namespaceURI, String localName)
	{
		if (mbean.getAttributes() == null)
         mbean.setAttributes(new ArrayList());
		
		mbean.getAttributes().add(attribute);
	}
}
