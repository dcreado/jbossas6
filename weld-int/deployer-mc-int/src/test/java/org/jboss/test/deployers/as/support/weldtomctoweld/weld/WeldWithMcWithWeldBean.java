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
package org.jboss.test.deployers.as.support.weldtomctoweld.weld;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.logging.Logger;
import org.jboss.test.deployers.as.support.Blue;
import org.jboss.test.deployers.as.support.Red;
import org.jboss.test.deployers.as.support.weldtomctoweld.mc.BlueMcBeanWeldDependencies;
import org.jboss.test.deployers.as.support.weldtomctoweld.mc.McBeanWeldDependencies;
import org.jboss.test.deployers.as.support.weldtomctoweld.mc.RedMcBeanWeldDependencies;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@Named
@RequestScoped
public class WeldWithMcWithWeldBean
{
   Logger log = Logger.getLogger(WeldWithMcWithWeldBean.class);

   @Inject @Blue
   McBeanWeldDependencies blue;
   
   McBeanWeldDependencies red;
   
   @Inject
   public void setRed(@Red McBeanWeldDependencies red)
   {
      this.red = red;
   }
   
   public String getResult()
   {
      log.info("getting result");
      String error = validate(blue, BlueMcBeanWeldDependencies.class);
      if (error != null)
         return error;
      error = validate(red, RedMcBeanWeldDependencies.class);
      if (error != null)
         return error;
      error = red.getResult();
      if (error != null)
         return error;
      error = blue.getResult();
      if (error != null)
         return error;
      return "WeldWithMcWithWeldBean#ok#";
   }
   
   private String validate(McBeanWeldDependencies dependency, Class<?> expected)
   {
      if (dependency == null)
         return "Null dependency (" + expected.getName() + ")";
      if (!expected.isAssignableFrom(dependency.getClass()))
         return "Expected (" + expected.getName() + ")";
      return null;
   }

}
