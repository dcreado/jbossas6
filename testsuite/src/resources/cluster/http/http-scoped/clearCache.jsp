<%@page contentType="text/html"
   import="org.jboss.mx.util.*"
   import="javax.management.*"
   import="java.util.*"
   import="org.jboss.test.cluster.web.CacheHelperMBean"
   import="org.jboss.test.cluster.web.CacheHelperClient"
%>

<html>
<center>
<% 
   MBeanServer server = MBeanServerLocator.locateJBoss();
   CacheHelperMBean helper = new CacheHelperClient(server);
   String webapp = "//localhost"  + request.getContextPath();
   helper.clear(webapp);
%>
<%=session.getId()%>

<h1><%=application.getServerInfo()%>:<%=request.getServerPort()%></h1>
</body>
</html>
