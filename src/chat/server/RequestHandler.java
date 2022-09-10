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
  HttpServletResponse response, RequestHandlerCallback callback)
  throws IOException, ServletException {
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

      RequestHandlerCallback.RequestData requestData
      = new RequestHandlerCallback.RequestData();
      requestData.sessionToken = sessionToken;
      requestData.contentType = request.getContentType();
      requestData.body = request.getReader().lines()
      .collect(Collectors.joining(System.lineSeparator()));
      if ("".equals(requestData.body)) {
        requestData.body = null;
      }
      RequestHandlerCallback.ResponseData responseData = callback
      .call(requestData);

      response.setStatus(responseData.statusCode);
      if (responseData.cookies != null) {
        for (RequestHandlerCallback.ResponseData.Cookie cookie
        : responseData.cookies) {
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