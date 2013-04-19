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
package org.jboss.test.deployers.as.support.weldtomctoweld.mc;

import java.io.Serializable;

import javax.inject.Inject;

import org.jboss.kernel.weld.metadata.api.annotations.Weld;
import org.jboss.kernel.weld.metadata.api.annotations.WeldEnabled;
import org.jboss.test.deployers.as.support.Blue;
import org.jboss.test.deployers.as.support.Red;
import org.jboss.test.deployers.as.support.weldtomctoweld.weld.BlueWeldMcDependency;
import org.jboss.test.deployers.as.support.weldtomctoweld.weld.RedWeldMcDependency;
import org.jboss.test.deployers.as.support.weldtomctoweld.weld.WeldMcDependency;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@WeldEnabled
@Red
public class RedMcBeanWeldDependencies implements McBeanWeldDependencies, Serializable
{
   private static final long serialVersionUID = 1L;

   @Inject 
   @Weld 
   @Red
   WeldMcDependency red;
   
   WeldMcDependency blue;
   
   @Inject @Weld 
   public void setBlue(@Blue WeldMcDependency blue)
   {
      this.blue = blue;
   }

   public String getResult()
   {
      if (red == null)
         return "null red (MC)";
      if (blue == null)
         return "null blue (MC)";
      if (red instanceof RedWeldMcDependency == false)
         return "red is not a RedWeldDependency";
      if (blue instanceof BlueWeldMcDependency == false)
         return "blue is not a BlueWeldDependency";
      return null;
   }
}
