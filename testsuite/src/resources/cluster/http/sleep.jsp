<%
   String sleep = request.getParameter("sleep");

   if (sleep != null)
   {
      long ms = Long.parseLong(sleep);
      
      if (ms > 0)
      {
         Thread.sleep(ms);
      }
      
      String value = (String) session.getAttribute("sleep");
      session.setAttribute("sleep", sleep);
      if (value != null)
      {
         response.addCookie(new javax.servlet.http.Cookie("sleep", value));
      }
   }
%>