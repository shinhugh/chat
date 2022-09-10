package chat.server;

import chat.app.*;
import chat.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.file.*;

public class PageRootServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
    try {
      String sessionToken = null;
      Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if (cookie.getName().equals("session")) {
            sessionToken = cookie.getValue();
            break;
          }
        }
      }
      App.Result<Object> result = App.shared.verifySessionToken(sessionToken);
      if (!result.success) {
        switch(result.failureReason) {
          case Unauthorized:
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", "/login");
            return;
          default:
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
      }

      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      String path = System.getProperty("catalina.base")
      + "/webapps/ROOT/public/index.html";
      BufferedReader fileReader = Files.newBufferedReader(Paths.get(path));
      String line = fileReader.readLine();
      while (line != null) {
        out.println(line);
        line = fileReader.readLine();
      }
    }

    catch (Exception error) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}