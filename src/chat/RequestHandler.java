package chat;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;
import java.util.*;
import java.util.stream.*;

class RequestHandler {
  public static void handleRequestWithSession(HttpServletRequest request,
  HttpServletResponse response, RequestWithSessionHandlerCallback callback)
  throws IOException, ServletException
  {
    Connection connection = null;

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

      RequestWithSessionData requestData = new RequestWithSessionData();
      requestData.connection = connection;
      requestData.userId = userId;
      requestData.contentType = request.getContentType();
      requestData.body = request.getReader().lines()
      .collect(Collectors.joining(System.lineSeparator()));
      if ("".equals(requestData.body)) {
        requestData.body = null;
      }
      ResponseData responseData = callback.call(requestData);

      response.setStatus(responseData.statusCode);
      if (responseData.cookies != null) {
        for (Map.Entry<String, String> entry
        : responseData.cookies.entrySet()) {
          response.addCookie(new Cookie(entry.getKey(), entry.getValue()));
        }
      }
      if (responseData.contentType != null) {
        response.setContentType(responseData.contentType);
      }
      if (responseData.body != null) {
        PrintWriter out = response.getWriter();
        out.print(responseData.body);
      }
    }

    catch (SQLException error) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    catch (ClassNotFoundException error) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    finally {
      try {
        connection.close();
      } catch (Exception error) { }
    }
  }

  private RequestHandler() { }
}