package chat;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;

public class APIUserServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
    Connection connection = null;
    PreparedStatement statement = null;
    ResultSet results = null;

    try {
      String sessionId = null;
      Cookie[] cookies = request.getCookies();
      if (cookies == null) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals("session")) {
          sessionId = cookie.getValue();
          break;
        }
      }
      if (sessionId == null) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      Class.forName("org.mariadb.jdbc.Driver");
      connection = DriverManager.getConnection
      ("jdbc:mariadb://localhost/chat", "root", "");
      int userId = DbHelper.mapSessionIdToUserId(sessionId, connection);
      if (userId < 0) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
      }

      String queryString = "SELECT name FROM users WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setInt(1, userId);
      results = statement.executeQuery();
      results.next();
      String userName = results.getString(1);
      response.setContentType("application/json");
      PrintWriter out = response.getWriter();
      out.println("{\"name\":\"" + userName + "\"}");
      response.setStatus(HttpServletResponse.SC_OK);
    }

    catch (SQLException error) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    catch (ClassNotFoundException error) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    finally {
      DbHelper.close(statement, results);
      try {
        connection.close();
      } catch (Exception error) { }
    }
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
    // TODO
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  public void doPut(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
    // TODO
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  public void doDelete(HttpServletRequest request,
  HttpServletResponse response)
  throws IOException, ServletException
  {
    // TODO
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }
}