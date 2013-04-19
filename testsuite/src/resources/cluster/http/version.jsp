<%@page contentType="text/html"
   import="org.jboss.mx.util.*"
   import="javax.management.*"
   import="org.jboss.test.cluster.web.CacheHelperMBean"
   import="org.jboss.test.cluster.web.CacheHelperClient"
%>
<%
   MBeanServer server = MBeanServerLocator.locateJBoss();
   CacheHelperMBean helper = new CacheHelperClient(server);
   String webapp = "//localhost"  + request.getContextPath();
   Integer version = helper.getSessionVersion(webapp, session.getId());
%>
<%= version %>
