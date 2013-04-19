/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.jbossas.embedded.testsuite.ejb3.mdb;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 * MessageStoringMdb
 * 
 * A message-driven bean which stores the contents of a text
 * message into a publicly-available static field.
 * 
 * Here we control the test environment
 * so we'll permit this clash of design paradigms.  Don't
 * try this at home, kids.
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
@MessageDriven(activationConfig =
{@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
      @ActivationConfigProperty(propertyName = "destination", propertyValue = MessageStoringMdb.NAME_QUEUE)})
public class MessageStoringMdb implements MessageListener
{
   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(MessageStoringMdb.class.getName());

   /**
    * Exposed contents of the last message received, volatile 
    * so the thread visibility from the test will always get the 
    * right value
    */
   public volatile static String LAST_MESSAGE_CONTENTS = null;

   /**
    * Shared barrier, so tests can wait until the MDB is processed
    */
   public static CyclicBarrier BARRIER = new CyclicBarrier(2);

   /**
    * Name of the queue upon which we'll listen
    */
   public static final String NAME_QUEUE = "queue/EmbeddedQueue";

   //-------------------------------------------------------------------------------------||
   // Required Implementations -----------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /* (non-Javadoc)
    * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
    */
   @Override
   public void onMessage(final Message message)
   {
      // Log
      log.info("Received: " + message);

      // Cast
      final TextMessage notSms = (TextMessage) message;

      // Obtain contents
      final String contents;
      try
      {
         contents = notSms.getText();
      }
      catch (final JMSException jmse)
      {
         throw new RuntimeException("Could not obtain contents of message: " + notSms, jmse);
      }

      // Store contents
      LAST_MESSAGE_CONTENTS = contents;

      // Announce to the barrier we're here, and block on the test
      // (so that the test can block as well and know when we're done;
      // we should really be the second ones here)
      try
      {
         BARRIER.await(2, TimeUnit.SECONDS);
      }
      catch (final InterruptedException e)
      {
         // Clear the flag
         Thread.interrupted();
         // Throw up
         throw new RuntimeException("Unexpected interruption", e);
      }
      catch (final BrokenBarrierException e)
      {
         throw new RuntimeException(e);
      }
      catch (final TimeoutException e)
      {
         throw new RuntimeException("The test did not call upon the barrier in the allotted time", e);
      }
   }
}
