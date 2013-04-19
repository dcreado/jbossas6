package org.jboss.test.mdbdefaultraname.bean;

import org.jboss.ejb.plugins.jms.JMSContainerInvoker;


/**
 * A MyJMSContainerInvoker.
 * 
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 * @version $Revision: 1.1 $
 */
public class MyJMSContainerInvoker extends JMSContainerInvoker
{
   public String getResourceAdapterName()
   {
      return resourceAdapterName;
   }
}
