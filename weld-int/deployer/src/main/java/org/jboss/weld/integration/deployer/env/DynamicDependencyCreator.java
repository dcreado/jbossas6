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
package org.jboss.weld.integration.deployer.env;

import org.jboss.dependency.plugins.AbstractDependencyItem;
import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.dependency.spi.DependencyInfo;
import org.jboss.dependency.spi.DependencyItem;

/**
 * Bean which knows how to create dynamic dependencies to target bean.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class DynamicDependencyCreator
{
   /** The controller */
   private Controller controller;

   public DynamicDependencyCreator(Controller controller)
   {
      this.controller = controller;
   }

   /**
    * Create dependencies to target bean.
    *
    * @param targetName the target bean name
    * @param dependencies dependencies names
    * @param whenRequiredState when required state
    * @param dependentState dependencies dependent state
    */
   public void createDepenencies(Object targetName, Iterable<String> dependencies, String whenRequiredState, String dependentState)
   {
      if (targetName == null)
         throw new IllegalArgumentException("Null target name");
      if (dependencies == null)
         throw new IllegalArgumentException("Null dependecies");

      ControllerContext targetControllerContext = controller.getContext(targetName, null);
      if (targetControllerContext == null)
         throw new IllegalArgumentException("No such target bean installed: " + targetName);

      Throwable error = targetControllerContext.getError();
      if (error != null)
         throw new IllegalArgumentException("Target bean " + targetName + " is in Error state: " + error);

      ControllerState whenRequired;
      if (whenRequiredState == null)
         whenRequired = ControllerState.INSTALLED;
      else
         whenRequired = ControllerState.getInstance(whenRequiredState);

      ControllerState currentTargetState = targetControllerContext.getState();
      if (controller.getStates().isBeforeState(currentTargetState, whenRequired) == false)
         throw new IllegalArgumentException("Target bean " + targetName + " is already past " + whenRequiredState + " state: " + targetControllerContext);

      ControllerState dependent = null;
      if (dependentState != null)
         dependent = ControllerState.getInstance(dependentState);

      DependencyInfo di = targetControllerContext.getDependencyInfo();
      for (Object dependency : dependencies)
      {
         DependencyItem item = new AbstractDependencyItem(targetName, dependency, whenRequired, dependent);
         di.addIDependOn(item);
      }
   }
}