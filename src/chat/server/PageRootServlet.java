package chat.server;

import chat.app.*;
import chat.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;

public class PageRootServlet extends HttpServlet {
  private static final String resourcePath = "/404.html";
  private static final String contentType = "text/html";

  public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    if (!"/".equals(request.getServletPath())) {
      response.setStatus(404);
      response.setContentType(contentType);
      PrintWriter bodyWriter = response.getWriter();
      bodyWriter.print(Utilities.readPublicFile(resourcePath));
      return;
    }
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