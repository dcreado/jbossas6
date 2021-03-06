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
package org.jboss.ejb.plugins;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Transaction;

import org.jboss.ejb.Container;
import org.jboss.invocation.Invocation;
import org.jboss.monitor.MetricsConstants;

/**
 * MetricsInterceptor collects data from the bean invocation call and publishes
 * them on a JMS topic (bound to <tt>topic/metrics</tt> in the name service).
 *
 * @author <a href="mailto:jplindfo@helsinki.fi">Juha Lindfors</a>
 * @author <a href="mailto:dimitris@jboss.org">Dimitris Anreadis</a>
 * @version $Revision: 89152 $
 * 
 * @since v2.0
 */
public class MetricsInterceptor extends AbstractInterceptor
   implements MetricsConstants
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   /** Application name this bean belongs to */
   private String applicationName = "<undefined>";
   
   /** Bean name in the container */
   private String beanName = "<undefined>";
   
   /** Publisher thread */
   private Thread publisher = null;

   /**
    * Message queue for the outgoing JMS messages. This list is accessed
    * by the interceptor when adding new messages, and by the publisher
    * thread when copying and clearing the contents of the queue. The list
    * must be locked for access and locks should be kept down to minimum
    * as they degrade the interceptor stack performance.
    */
   private List msgQueue = new ArrayList(2000);


   // Public --------------------------------------------------------
   /**
    * Stores the container reference and the application and bean JNDI
    * names.
    *
    * @param   container   set by the container initialization code
    */
   public void setContainer(Container container)
   {
      super.setContainer(container);
      if (container != null)
      {
         applicationName = container.getEjbModule().getName();
         beanName = container.getBeanMetaData().getJndiName();
      }
   }

   // Interceptor implementation ------------------------------------
   
   public Object invokeHome(Invocation mi) throws Exception
   {

      long begin = System.currentTimeMillis();

      try
      {
         return super.invokeHome(mi);
      }
      finally
      {
         if (mi.getMethod() != null && publisher.isAlive())
         {
            addEntry(mi, begin, System.currentTimeMillis());
         }
      }
   }

   public Object invoke(Invocation mi) throws Exception
   {

      long begin = System.currentTimeMillis();

      try
      {
         return super.invoke(mi);
      }
      finally
      {
         if (mi.getMethod() != null && publisher.isAlive())
         {
            addEntry(mi, begin, System.currentTimeMillis());
         }
      }
   }

   /**
    * Starts the JMS publisher thread.
    */
   public void create()
   {
      log.warn("\n" +
         "----------------------------------------------------------------------\n" +
         "Deprecated MetricsInterceptor activated for bean: '" + beanName + "'\n" +
         "Invocation metrics will be published in JMS Topic: 'topic/metrics'\n" +
         "----------------------------------------------------------------------"
         );
            
      // looks like create() is called after setContainer().
      // wonder if container method callback order is documented somewhere, it should be..
      publisher = new Thread(new Publisher());
      publisher.setName("Metrics Publisher Thread for " + beanName);
      publisher.setDaemon(true);
      publisher.start();
   }

   /**
    * Kills the publisher thread.
    */
   public void destroy()
   {
      publisher.interrupt();
   }

   // Private --------------------------------------------------------

   /**
    * Store the required information from this invocation: principal,
    * transaction, method, time.
    *
    * @param   begin   invocation begin time in ms
    * @param   end     invocation end time in ms
    */
   private final void addEntry(Invocation mi, long begin, long end)
   {

      /* this gets called by the interceptor */

      Transaction tx = mi.getTransaction();
      Principal princ = mi.getPrincipal();
      Method method = mi.getMethod();
      Entry start = new Entry(princ, method, tx, begin, "START");
      Entry stop = new Entry(princ, method, tx, end, "STOP");

      // add both entries, order is guaranteed, synchronized to prevent
      // publisher from touching the queue while working on it
      synchronized (msgQueue)
      {

         // Two entries for now, one should suffice but requires changes in
         // the client.
         msgQueue.add(start);
         msgQueue.add(stop);
      }
   }

   private Message createMessage(Session session, String principal, int txID,
         String method, String checkpoint, long time)
   {

      try
      {
         Message msg = session.createMessage();

         msg.setJMSType(INVOCATION_METRICS);
         msg.setStringProperty(CHECKPOINT, checkpoint);
         msg.setStringProperty(BEAN, beanName);
         msg.setObjectProperty(METHOD, method);
         msg.setLongProperty(TIME, time);

         if (txID != -1)
            msg.setStringProperty("ID", String.valueOf(txID));

         if (principal != null)
            msg.setStringProperty("PRINCIPAL", principal);

         return msg;
      }
      catch (Exception e)
      {
         // catch JMSExceptions, tx.SystemExceptions, and NPE's
         // don't want to bother the container even if the metrics fail.
         return null;
      }
   }

   /**
    * JMS Publisher thread implementation.
    */
   private class Publisher implements Runnable
   {

      /** Thread keep-alive field. */
      private boolean running = true;
      /** Thread sleep delay. */
      private int delay = 2000;
      /** JMS Connection */
      private TopicConnection connection = null;

      /**
       * Thread implementation. <p>
       *
       * When started, looks up a topic connection factory from the name
       * service, and attempts to create a publisher to <tt>topic/metrics</tt>
       * topic. <p>
       *
       * While alive, locks the <tt>msgQueue</tt> every two seconds to make a
       * copy of the contents and then clear it. <p>
       *
       * Interrupting this thread will kill it.
       *
       * @see #msgQueue
       * @see java.lang.Thread#interrupt()
       */
      public void run()
      {
         boolean intr = false;
         try
         {
            final boolean IS_TRANSACTED = true;
            final int ACKNOWLEDGE_MODE = Session.DUPS_OK_ACKNOWLEDGE;

            // lookup the connection factory and topic and create a JMS session
            Context namingContext = new InitialContext();
            TopicConnectionFactory fact = (TopicConnectionFactory) namingContext.lookup("java:/ConnectionFactory");

            connection = fact.createTopicConnection();

            Topic topic = (Topic) namingContext.lookup("topic/metrics");
            TopicSession session = connection.createTopicSession(IS_TRANSACTED, ACKNOWLEDGE_MODE);
            TopicPublisher pub = session.createPublisher(topic);

            pub.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            pub.setPriority(Message.DEFAULT_PRIORITY);
            pub.setTimeToLive(Message.DEFAULT_TIME_TO_LIVE);

            // start the JMS connection
            connection.start();

            // copy the message queue every x seconds, and publish the messages
            while (running)
            {

               Object[] array;
               long sleepTime = delay;

               try
               {
                  Thread.sleep(sleepTime);

                  // measure message processing cost and try to deal
                  // with congestion
                  long begin = System.currentTimeMillis();

                  // synchronized during the copy... the interceptor will
                  // have to wait til done
                  synchronized (msgQueue)
                  {
                     array = msgQueue.toArray();
                     msgQueue.clear();
                  }

                  // publish the messages
                  for (int i = 0; i < array.length; ++i)
                  {
                     Message msg = createMessage(session,
                           ((Entry) array[i]).principal,
                           ((Entry) array[i]).id,
                           ((Entry) array[i]).method,
                           ((Entry) array[i]).checkpoint,
                           ((Entry) array[i]).time
                     );

                     pub.publish(msg);
                  }

                  // try to deal with congestion a little better, alot of
                  // small messages fast will kill JBossMQ performance, this is
                  // a temp fix to group many messages into one operation
                  try
                  {
                     session.commit();
                  }
                  catch (Exception e)
                  {
                  }

                  // stop the clock and reduce the work time from our
                  // resting time
                  long end = System.currentTimeMillis();

                  sleepTime = delay - (end - begin);
               }
               catch (InterruptedException e)
               {
                  // kill this thread
                  intr = true;
                  running = false;
               }
            }
         }
         catch (NamingException e)
         {
            log.warn(Thread.currentThread().getName() + " exiting", e);
         }
         catch (JMSException e)
         {
            log.warn(Thread.currentThread().getName() + " exiting", e);
         }
         finally
         {
            // thread cleanup
            synchronized (msgQueue)
            {
               msgQueue.clear();
            }
            
            try
            {
               if (connection != null)
                  connection.close();
            }
            catch (JMSException e)
            {
                log.warn(e);
            }
            finally
            {
               if (intr) Thread.currentThread().interrupt();
            }
         }
      }
   }

   /**
    * Wrapper class for message queue entries.
    *
    * @see #msgQueue
    */
   private final class Entry
   {
      int id = -1;
      long time;
      String principal = null;
      String checkpoint;
      String method;

      Entry(Principal principal, Method method, Transaction tx, long time, String checkpoint)
      {
         this.time = time;
         this.checkpoint = checkpoint;
         this.method = method.getName();

         if (tx != null)
            this.id = tx.hashCode();
         if (principal != null)
            this.principal = principal.getName();
      }
   }
}
