package chat.server;

import chat.app.*;
import chat.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;

public class GatewayServlet extends HttpServlet {
  private static final String[] implementedMethods = {"GET", "POST", "PUT",
  "DELETE", "HEAD", "OPTIONS", "TRACE"};

  protected void service(HttpServletRequest request,
  HttpServletResponse response)
  throws IOException, ServletException {
    boolean methodImplemented = false;
    for (String implementedMethod : implementedMethods) {
      if (request.getMethod().equals(implementedMethod)) {
        methodImplemented = true;
        break;
      }
    }
    if (!methodImplemented) {
      response.setStatus(501);
      return;
    }
    if (!"/".equals(request.getServletPath())) {
      response.setStatus(404);
      return;
    }
    super.service(request, response);
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    RequestHandler.handleRequest(request, response,
    new GetRequestHandlerCallback());
  }

  private static class GetRequestHandlerCallback
  implements RequestHandler.Callback {
    private static final String redirectLocationAuthorized = "/chat";
    private static final String redirectLocationUnauthorized = "/login";

    public RequestHandler.Callback.ResponseData call(
    RequestHandler.Callback.RequestData requestData) {
      RequestHandler.Callback.ResponseData responseData = new ResponseData();
      App.Result<Object> result = App.shared
      .verifySessionToken(requestData.sessionToken);
      if (result.success) {
        responseData.statusCode = 302;
        responseData.location = redirectLocationAuthorized;
        return responseData;
      }
      switch (result.failureReason) {
        case Unauthorized:
          responseData.statusCode = 302;
          responseData.location = redirectLocationUnauthorized;
          break;
        default:
          responseData.statusCode = 500;
          break;
      }
      return responseData;
    }
  }
}