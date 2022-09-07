package chat;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.file.*;

public class PageRootServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
    try {
      String sessionId = null;
      Cookie[] cookies = request.getCookies();
      if (cookies == null) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", "/login");
        return;
      }
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals("session")) {
          sessionId = cookie.getValue();
          break;
        }
      }
      if (Utilities.nullOrEmpty(sessionId)) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", "/login");
        return;
      }
      User user = PersistentModel.shared.getUserBySessionId(sessionId);
      if (user == null) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", "/login");
        return;
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