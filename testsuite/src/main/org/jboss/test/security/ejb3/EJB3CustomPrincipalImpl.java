/*
 * JBoss, the OpenSource J2EE webOS
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.security.ejb3;

import java.io.Serializable;
import java.security.Principal;

public class EJB3CustomPrincipalImpl implements Principal, Serializable
{

   /** The serialVersionUID */
   private static final long serialVersionUID = -8696401639393346424L;
   
   private String name;
   
   public EJB3CustomPrincipalImpl(String name)
   {
      this.name = name;
   }

   public String getName()
   {
      return name;
   }

   public String toString()
   {
      return name;
   }
}
