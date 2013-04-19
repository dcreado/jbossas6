package org.jboss.resteasy.test;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Provider
public class MyExceptionMapper implements ExceptionMapper<MyException>
{
   public Response toResponse(MyException e)
   {
      return Response.status(412).type("text/plain").entity("MyException thrown").build();
   }
}
