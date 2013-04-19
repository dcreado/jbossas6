/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 


package org.jboss.web.tomcat.service.deployers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.naming.NamingContextBindingsEnumeration;
import org.apache.naming.NamingContextEnumeration;
import org.apache.naming.NamingEntry;
import org.apache.naming.resources.BaseDirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import org.jboss.vfs.VirtualFile;

/**
 * VFS Directory Context implementation.
 *
 * @author Remy Maucherat
 */
public class VFSDirContext extends BaseDirContext {

    public VFSDirContext() {
        super();
    }

    public VFSDirContext(Hashtable env) {
        super(env);
    }

    protected VFSDirContext(VirtualFile base) {
        this.base = base;
    }

    protected VirtualFile base = null;

    public void setVirtualFile(VirtualFile base) {
        this.base = base;
    }

    public void release() {
        base = null;
        super.release();
    }

    public Object lookup(String name) throws NamingException {
        return lookup(new CompositeName(name));
    }

    public Object lookup(Name name) throws NamingException {
        if (name.isEmpty())
            return this;
        VirtualFile entry = treeLookup(name);
        if (entry == null)
            throw new NamingException(sm.getString("resources.notFound", name));

        if (entry.isDirectory()) {
            return new VFSDirContext(entry);
        } else {
            return new VFSResource(entry);
        }
    }

    public void unbind(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void rename(String oldName, String newName) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration list(String name) throws NamingException {
        return list(new CompositeName(name));
    }

    public NamingEnumeration list(Name name) throws NamingException {
        if (name.isEmpty()) {
            return new NamingContextEnumeration(list(base).iterator());
        }
        VirtualFile entry = treeLookup(name);
        if (entry == null)
            throw new NamingException(sm.getString("resources.notFound", name));

        return new NamingContextEnumeration(list(entry).iterator());
    }

    public NamingEnumeration listBindings(String name) throws NamingException {
        return listBindings(new CompositeName(name));
    }

    public NamingEnumeration listBindings(Name name) throws NamingException {
        if (name.isEmpty()) {
            return new NamingContextBindingsEnumeration(list(base).iterator(), this);
        }
        VirtualFile entry = treeLookup(name);
        if (entry == null)
            throw new NamingException(sm.getString("resources.notFound", name));

        return new NamingContextBindingsEnumeration(list(entry).iterator(), this);
    }

    public void destroySubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Object lookupLink(String name) throws NamingException {
        // Note : Links are not supported
        return lookup(name);
    }

    public String getNameInNamespace() throws NamingException {
        return docBase;
    }

    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        return getAttributes(new CompositeName(name), attrIds);
    }

    public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {

        VirtualFile entry = null;
        if (name.isEmpty()) {
            entry = base;
        } else {
            entry = treeLookup(name);
        }
        if (entry == null)
            throw new NamingException(sm.getString("resources.notFound", name));

        ResourceAttributes attrs = new ResourceAttributes();
        attrs.setCreationDate(new Date(entry.getLastModified()));
        attrs.setName(entry.getName());
        if (entry.isFile())
            attrs.setResourceType("");
        attrs.setContentLength(entry.getSize());
        attrs.setLastModified(entry.getLastModified());

        return attrs;

    }

    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public DirContext getSchema(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration search(String name, Attributes matchingAttributes, String[] attributesToReturn)
            throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration search(String name, Attributes matchingAttributes) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration search(String name, String filter, SearchControls cons) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration search(String name, String filterExpr, Object[] filterArgs, SearchControls cons)
            throws NamingException {
        throw new OperationNotSupportedException();
    }

    protected VirtualFile treeLookup(Name name) {
        if (base == null)
            return null;
        if (name.isEmpty())
            return base;
        VirtualFile currentFile = base;
        for (int i = 0; i < name.size(); i++) {
            if (name.get(i).length() == 0)
                continue;
            currentFile = currentFile.getChild(name.get(i));
            if (!currentFile.exists())
                return null;
        }
        return currentFile;
    }

    protected ArrayList<NamingEntry> list(VirtualFile entry) {

        ArrayList<NamingEntry> entries = new ArrayList<NamingEntry>();
        if (entry.isDirectory()) {
            Iterator<VirtualFile> children = entry.getChildren().iterator();
            NamingEntry namingEntry = null;

            while (children.hasNext()) {
                VirtualFile current = children.next();
                Object object = null;
                if (current.isDirectory()) {
                    object = new VFSDirContext(current);
                } else {
                    object = new VFSResource(current);
                }
                namingEntry = new NamingEntry(current.getName(), object, NamingEntry.ENTRY);
                entries.add(namingEntry);
            }
        }

        return entries;

    }

    protected class VFSResource extends Resource {

        public VFSResource(VirtualFile entry) {
            this.entry = entry;
        }

        protected VirtualFile entry;

        public InputStream streamContent() throws IOException {
            if (binaryContent == null) {
                InputStream is = entry.openStream();
                inputStream = is;
                return is;
            }
            return super.streamContent();
        }

    }

}
