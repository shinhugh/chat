package chat;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;
import java.util.*;
import java.util.stream.*;
import java.time.*;
import java.time.format.*;

class RequestHandler {
  public static void handleRequest(HttpServletRequest request,
  HttpServletResponse response, RequestHandlerCallback callback)
  throws IOException, ServletException {
    Connection connection = null;

    try {
      Class.forName("org.mariadb.jdbc.Driver");
      connection = DriverManager.getConnection
      ("jdbc:mariadb://localhost/chat", "root", "");

      RequestData requestData = new RequestData();
      requestData.connection = connection;
      requestData.contentType = request.getContentType();
      requestData.body = request.getReader().lines()
      .collect(Collectors.joining(System.lineSeparator()));
      if ("".equals(requestData.body)) {
        requestData.body = null;
      }
      ResponseData responseData = callback.call(requestData);

      response.setStatus(responseData.statusCode);
      if (responseData.cookies != null) {
        for (Map.Entry<String, Map.Entry<String, Long>> entry
        : responseData.cookies.entrySet()) {
          if (entry.getValue().getKey() == null) {
            Cookie expiredCookie = new Cookie(entry.getKey(), "");
            expiredCookie.setMaxAge(0);
            expiredCookie.setPath("/");
            response.addCookie(expiredCookie);
            continue;
          }

          int maxAge
          = (int) ((entry.getValue().getValue() - System.currentTimeMillis())
          / 1000);
          Instant expiresInstant
          = Instant.ofEpochMilli(entry.getValue().getValue());
          String expiresString
          = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z")
          .withZone(ZoneId.of("GMT")).format(expiresInstant);

          String cookieString
          = entry.getKey() + "=" + entry.getValue().getKey()
          + "; Path=/; SameSite=Strict; Max-Age=" + maxAge + "; Expires="
          + expiresString;
          response.setHeader("Set-Cookie", cookieString);
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
      DbHelper.close(connection);
    }
  }

  public static void handleRequestWithSession(HttpServletRequest request,
  HttpServletResponse response, RequestWithSessionHandlerCallback callback)
  throws IOException, ServletException {
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
      requestData.sessionId = sessionId;
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
        for (Map.Entry<String, Map.Entry<String, Long>> entry
        : responseData.cookies.entrySet()) {
          if (entry.getValue().getKey() == null) {
            Cookie expiredCookie = new Cookie(entry.getKey(), "");
            expiredCookie.setMaxAge(0);
            expiredCookie.setPath("/");
            response.addCookie(expiredCookie);
            continue;
          }

          int maxAge
          = (int) ((entry.getValue().getValue() - System.currentTimeMillis())
          / 1000);
          Instant expiresInstant
          = Instant.ofEpochMilli(entry.getValue().getValue());
          String expiresString
          = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z")
          .withZone(ZoneId.of("GMT")).format(expiresInstant);

          String cookieString
          = entry.getKey() + "=" + entry.getValue().getKey()
          + "; Path=/; SameSite=Strict; Max-Age=" + maxAge + "; Expires="
          + expiresString;
          response.setHeader("Set-Cookie", cookieString);
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
      DbHelper.close(connection);
    }
  }

  private RequestHandler() { }
}