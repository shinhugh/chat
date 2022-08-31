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
        if (cookie.getName() == "session") {
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
      String queryString = "SELECT user, expiration FROM sessions WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, sessionId);
      results = statement.executeQuery();
      if (!results.next()) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      int sessionExpiration = results.getInt(2);
      if (sessionExpiration <= System.currentTimeMillis()) {
        try {
          results.close();
        } catch (Exception error) { }
        try {
          statement.close();
        } catch (Exception error) { }
        queryString = "DELETE FROM sessions WHERE id = ?;";
        statement = connection.prepareStatement(queryString);
        statement.setString(1, sessionId);
        statement.executeUpdate();
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      int userId = results.getInt(1);
      try {
        results.close();
      } catch (Exception error) { }
      try {
        statement.close();
      } catch (Exception error) { }
      queryString = "SELECT name FROM users WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setInt(1, userId);
      results = statement.executeQuery();
      if (!results.next()) {
        try {
          results.close();
        } catch (Exception error) { }
        try {
          statement.close();
        } catch (Exception error) { }
        queryString = "DELETE FROM sessions WHERE user = ?;";
        statement = connection.prepareStatement(queryString);
        statement.setInt(1, userId);
        statement.executeUpdate();
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      String userName = results.getString(1);
      response.setContentType("application/json");
      PrintWriter out = response.getWriter();
      out.println("{\"name\":\"" + userName + "\"}");
      response.setStatus(HttpServletResponse.SC_OK);
    } catch (SQLException error) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (ClassNotFoundException error) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } finally {
      try {
        results.close();
      } catch (Exception error) { }
      try {
        statement.close();
      } catch (Exception error) { }
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