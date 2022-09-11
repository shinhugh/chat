package chat.server;

import chat.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.stream.*;

class RequestHandler {
  public static void handleRequest(HttpServletRequest request,
  HttpServletResponse response, Callback callback) {
    try {
      Callback.RequestData requestData = new Callback.RequestData();
      Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if (cookie.getName().equals("session")) {
            requestData.sessionToken = cookie.getValue();
            break;
          }
        }
      }
      requestData.contentType = request.getContentType();
      requestData.body = request.getReader().lines()
      .collect(Collectors.joining(System.lineSeparator()));
      if ("".equals(requestData.body)) {
        requestData.body = null;
      }

      Callback.ResponseData responseData = callback.call(requestData);

      response.setStatus(responseData.statusCode);
      if (responseData.cookies != null) {
        for (Callback.ResponseData.Cookie cookie : responseData.cookies) {
          String cookieString = null;
          if (Utilities.nullOrEmpty(cookie.value) || cookie.expiration < 1) {
            cookieString = cookie.key + "=; Path=/; SameSite=Strict;"
            + " Max-Age=0; Expires=Thu, 01 Jan 1970 12:00:00 GMT";
          } else {
            int maxAge = (int) ((cookie.expiration - System.currentTimeMillis())
            / 1000);
            Instant expiresInstant = Instant.ofEpochMilli(cookie.expiration);
            String expiresString
            = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z")
            .withZone(ZoneId.of("GMT")).format(expiresInstant);
            cookieString = cookie.key + "=" + cookie.value
            + "; Path=/; SameSite=Strict; Max-Age=" + maxAge + "; Expires="
            + expiresString;
          }
          response.setHeader("Set-Cookie", cookieString);
        }
      }
      if (!Utilities.nullOrEmpty(responseData.location)) {
        response.setHeader("Location", responseData.location);
      }
      if (!Utilities.nullOrEmpty(responseData.contentType)) {
        response.setContentType(responseData.contentType);
      }
      if (!Utilities.nullOrEmpty(responseData.body)) {
        PrintWriter out = response.getWriter();
        out.print(responseData.body);
      }
    }

    catch (Exception error) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  private RequestHandler() { }

  public static interface Callback {
    public ResponseData call(RequestData requestData);

    public static class RequestData {
      public String sessionToken;
      public String contentType;
      public String body;
    }

    public static class ResponseData {
      public int statusCode;
      public Cookie[] cookies;
      public String location;
      public String contentType;
      public String body;

      public static class Cookie {
        public String key;
        public String value;
        public long expiration;
      }
    }
  }
}