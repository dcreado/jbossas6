/**
 * 
 */
package org.jboss.test.cluster.web;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jboss.logging.Logger;
import org.jboss.test.JBossTestClusteredServices;
import org.jboss.test.JBossTestServices;
import org.jboss.test.cluster.testutil.SessionTestUtil;
import org.jboss.test.cluster.testutil.TestSetupDelegate;

/**
 *
 *
 * @author Brian Stansberry
 * 
 * @version $Revision: $
 */
public class JBossCacheConfigTestSetupDelegate implements TestSetupDelegate
{
   public static final String SYSTEM_PROPS_SVC = "jboss:type=Service,name=SystemProperties";
   
   private static final Logger log = Logger.getLogger(JBossCacheConfigTestSetupDelegate.class);
   
   private String cacheConfigName;
   private JBossTestClusteredServices clusteredServices;

   public void setTestServices(JBossTestServices services)
   {
      this.clusteredServices = (JBossTestClusteredServices) services;      
   }

   /* (non-Javadoc)
    * @see org.jboss.test.cluster.testutil.TestSetupDelegate#setUp()
    */
   public void setUp() throws Exception
   {
      cacheConfigName = System.getProperty(SessionTestUtil.CACHE_CONFIG_PROP);
      if (cacheConfigName != null)
      {
         setServerSideCacheConfigProperties();
      }
   }

   /* (non-Javadoc)
    * @see org.jboss.test.cluster.testutil.TestSetupDelegate#tearDown()
    */
   public void tearDown() throws Exception
   {
      if (cacheConfigName != null)
      {
         this.clearServerSideCacheConfigProperties();
      }
   }
   
   private void setServerSideCacheConfigProperties() throws Exception
   {
      log.debug("configuring server with cacheConfigName=" + cacheConfigName);
      
      ObjectName on = new ObjectName(SYSTEM_PROPS_SVC);
      for (MBeanServerConnection adaptor : clusteredServices.getAdaptors())
      {
         adaptor.invoke(on, "set", 
                        new Object[]{SessionTestUtil.CACHE_CONFIG_PROP, cacheConfigName}, 
                        new String[] {String.class.getName(), String.class.getName()});
      }         
   }

   private void clearServerSideCacheConfigProperties() throws Exception
   {
      ObjectName on = new ObjectName(SYSTEM_PROPS_SVC);
      for (MBeanServerConnection adaptor : clusteredServices.getAdaptors())
      {
         adaptor.invoke(on, "remove", 
                        new Object[]{SessionTestUtil.CACHE_CONFIG_PROP}, 
                        new String[] {String.class.getName()});
      } 
   }
}
