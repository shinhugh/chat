package chat;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;
import java.nio.file.*;

public class PageLoginServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
    Connection connection = null;

    try {
      String sessionId = null;
      Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if (cookie.getName().equals("session")) {
            sessionId = cookie.getValue();
            break;
          }
        }
      }
      if (sessionId != null) {
        Class.forName("org.mariadb.jdbc.Driver");
        connection = DriverManager.getConnection
        ("jdbc:mariadb://localhost/chat", "root", "");
        int userId = DbHelper.mapSessionIdToUserId(sessionId, connection);
        if (userId >= 0) {
          response.setStatus(HttpServletResponse.SC_FOUND);
          response.setHeader("Location", "/");
          return;
        }
      }

      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      String path
      = System.getProperty("catalina.base")
      + "/webapps/ROOT/public/login/index.html";
      BufferedReader fileReader = Files.newBufferedReader(Paths.get(path));
      String line = fileReader.readLine();
      while (line != null) {
        out.println(line);
        line = fileReader.readLine();
      }
    }

    catch (SQLException error) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    catch (ClassNotFoundException error) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    finally {
      DbHelper.close(connection);
    }
  }
}