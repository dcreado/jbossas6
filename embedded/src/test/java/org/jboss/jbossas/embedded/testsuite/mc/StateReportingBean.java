/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.jbossas.embedded.testsuite.mc;

import org.jboss.beans.metadata.api.annotations.Start;
import org.jboss.beans.metadata.api.annotations.Stop;
import org.jboss.embedded.api.server.JBossASEmbeddedServer;

/**
 * Simple MC Bean to test URL and File deployment
 * into the {@link JBossASEmbeddedServer}.  Reports 
 * its current state.
 * 
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class StateReportingBean
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Possible states
    * 
    * 
    * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
    * @version $Revision: $
    */
   public enum State {
      STARTED, STOPPED
   }

   //-------------------------------------------------------------------------------------||
   // Instance Members -------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Current state of this bean
    */
   private State state = State.STOPPED;

   //-------------------------------------------------------------------------------------||
   // Lifecycle Methods ------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Reports that the bean has been started
    */
   @Start
   public void setStateStarted()
   {
      state = State.STARTED;
   }

   /**
    * Reports that the bean has been stopped
    */
   @Stop
   public void setStateStopped()
   {
      state = State.STOPPED;
   }

   //-------------------------------------------------------------------------------------||
   // Functional Methods -----------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * @return the state
    */
   public State getState()
   {
      return state;
   }
}
