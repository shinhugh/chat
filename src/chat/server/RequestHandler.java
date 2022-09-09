package chat.server;

import chat.*;
import chat.app.*;
import chat.server.structs.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

class RequestHandler {
  public static void handleRequest(HttpServletRequest request,
  HttpServletResponse response, RequestHandlerCallback callback)
  throws IOException, ServletException {
    try {
      RequestData requestData = new RequestData();
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
          String cookieString = null;
          if (Utilities.nullOrEmpty(entry.getValue().getKey())) {
            cookieString = entry.getKey() + "=; Path=/; SameSite=Strict;"
            + " Max-Age=0; Expires=Thu, 01 Jan 1970 12:00:00 GMT";
          } else {
            int maxAge
            = (int) ((entry.getValue().getValue() - System.currentTimeMillis())
            / 1000);
            Instant expiresInstant
            = Instant.ofEpochMilli(entry.getValue().getValue());
            String expiresString
            = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z")
            .withZone(ZoneId.of("GMT")).format(expiresInstant);

            cookieString
            = entry.getKey() + "=" + entry.getValue().getKey()
            + "; Path=/; SameSite=Strict; Max-Age=" + maxAge + "; Expires="
            + expiresString;
          }
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

    catch (Exception error) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  public static void handleRequestWithSession(HttpServletRequest request,
  HttpServletResponse response, RequestWithSessionHandlerCallback callback)
  throws IOException, ServletException {
    try {
      String sessionToken = null;
      Cookie[] cookies = request.getCookies();
      if (cookies == null) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals("session")) {
          sessionToken = cookie.getValue();
          break;
        }
      }
      if (Utilities.nullOrEmpty(sessionToken)) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      App.Result<Object> result = App.shared.verifySessionToken(sessionToken);
      if (!result.success) {
        if (result.failureReason == App.Result.FailureReason.Unauthorized) {
          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
          return;
        }
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
      }

      RequestWithSessionData requestData = new RequestWithSessionData();
      requestData.sessionToken = sessionToken;
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
          String cookieString = null;
          if (Utilities.nullOrEmpty(entry.getValue().getKey())) {
            cookieString = entry.getKey() + "=; Path=/; SameSite=Strict;"
            + " Max-Age=0; Expires=Thu, 01 Jan 1970 12:00:00 GMT";
          } else {
            int maxAge
            = (int) ((entry.getValue().getValue() - System.currentTimeMillis())
            / 1000);
            Instant expiresInstant
            = Instant.ofEpochMilli(entry.getValue().getValue());
            String expiresString
            = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z")
            .withZone(ZoneId.of("GMT")).format(expiresInstant);

            cookieString
            = entry.getKey() + "=" + entry.getValue().getKey()
            + "; Path=/; SameSite=Strict; Max-Age=" + maxAge + "; Expires="
            + expiresString;
          }
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

    catch (Exception error) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  private RequestHandler() { }
}