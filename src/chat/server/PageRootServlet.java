package chat.server;

import chat.app.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;

public class PageRootServlet extends HttpServlet {
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