package org.jboss.jbossas.embedded.testsuite.wsclient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;

/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.3-b02-
 * Generated source version: 2.0
 * 
 */
@WebServiceClient(name = "embedded", targetNamespace = Embedded.TARGET_NAMESPACE, wsdlLocation = "http://localhost:8080/testws/embedded?wsdl")
public class Embedded extends Service
{

   private final static URL EMBEDDED_WSDL_LOCATION;
   
   public static final String TARGET_NAMESPACE = "http://ws.testsuite.embedded.jbossas.jboss.org/";

   private final static Logger logger = Logger
         .getLogger(org.jboss.jbossas.embedded.testsuite.wsclient.Embedded.class.getName());

   static
   {
      URL url = null;
      try
      {
         URL baseUrl;
         baseUrl = org.jboss.jbossas.embedded.testsuite.wsclient.Embedded.class.getResource(".");
         url = new URL(baseUrl, "http://localhost:8080/webservice/embedded?wsdl");
      }
      catch (MalformedURLException e)
      {
         logger
               .warning("Failed to create URL for the wsdl Location: 'http://localhost:8080/testws/embedded?wsdl', retrying as a local file");
         logger.warning(e.getMessage());
      }
      EMBEDDED_WSDL_LOCATION = url;
   }

   public Embedded(URL wsdlLocation, QName serviceName)
   {
      super(wsdlLocation, serviceName);
   }

   public Embedded()
   {
      super(EMBEDDED_WSDL_LOCATION, new QName(TARGET_NAMESPACE, "embedded"));
   }

   /**
    * 
    * @return
    *     returns EmbeddedWs
    */
   @WebEndpoint(name = "EmbeddedWsPort")
   public EmbeddedWs getEmbeddedWsPort()
   {
      return super.getPort(new QName(TARGET_NAMESPACE, "EmbeddedWsPort"), EmbeddedWs.class);
   }

}
