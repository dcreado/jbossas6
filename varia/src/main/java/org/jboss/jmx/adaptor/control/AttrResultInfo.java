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
package org.jboss.jmx.adaptor.control;

import java.beans.PropertyEditor;
import org.snmp4j.smi.TimeTicks;

/** A simple tuple of an mbean operation name, sigature and result.

@author Scott.Stark@jboss.org
@version $Revision: 112094 $
 */
public class AttrResultInfo
{
   public String name;
   public PropertyEditor editor;
   public Object result;
   public Throwable throwable;

   public AttrResultInfo(String name, PropertyEditor editor, Object result, Throwable throwable)
   {
      this.name = name;
      this.editor = editor;
      this.result = result;
      this.throwable = throwable;
   }

   public String getAsText()
   {
      if (throwable != null)
      {
         return throwable.toString();
      }
      if( result != null )
      {
         try 
         {
            if( editor != null )
            {
               editor.setValue(result);
               return editor.getAsText();
            }
            else
            {
               if(result instanceof TimeTicks){
            	   return ((TimeTicks)result).toString("{0} d {1} h {2} m {3} s {4} hs");
               }
               return result.toString();
            }
         }
         catch (Exception e)
         {
            return "String representation of " + name + "unavailable";
         } // end of try-catch
      }
      return null;
   }
}
