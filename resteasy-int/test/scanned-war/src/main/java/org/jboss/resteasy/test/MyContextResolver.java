package org.jboss.resteasy.test;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Provider
public class MyContextResolver implements ContextResolver<Integer>
{
   public Integer getContext(Class<?> type)
   {
      return new Integer(42);
   }
}
